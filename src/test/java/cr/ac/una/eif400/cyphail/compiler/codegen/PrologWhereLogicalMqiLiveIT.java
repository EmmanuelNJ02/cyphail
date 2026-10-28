
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;
import cr.ac.una.eif400.cyphail.engine.mqi.PrologResult;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * Real SWI-Prolog/MQI integration tests for logical WHERE
 * expressions parsed from Cyphail source code.
 *
 * These tests validate:
 * - AND filtering.
 * - OR filtering.
 * - AND precedence over OR.
 * - Explicit parentheses.
 * - Non-duplicating OR semantics.
 *
 * The last test is a regression test intended to detect
 * whether overlapping OR branches incorrectly multiply rows.
 *
 * Run with:
 * mvn -Dtest=PrologWhereLogicalMqiLiveIT test
 */
class PrologWhereLogicalMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Locates the SWI-Prolog engine.
     */
    private static Path enginePath() {

        Path path = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath().normalize();

        assertTrue(
                Files.isRegularFile(path),
                "The SWI-Prolog engine must exist."
        );

        return path;
    }

    /**
     * Parses a Cyphail query and generates its Prolog goal.
     */
    private String compile(String source) {

        Query query = parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result);
        assertNotNull(result.goal());
        assertFalse(result.goal().isBlank());

        return result.goal();
    }

    /**
     * Executes a generated query through real MQI.
     */
    private static String execute(
            MqiBroker broker,
            String goal
    ) throws Exception {

        PrologResult result = broker.execute(goal);

        PrologResult.Success success =
                assertInstanceOf(
                        PrologResult.Success.class,
                        result,
                        "The Prolog query must execute successfully: "
                                + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "The query must return one result table."
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        assertTrue(
                bindings.containsKey("Rows"),
                "MQI must return the Rows binding."
        );

        return assertInstanceOf(
                String.class,
                bindings.get("Rows")
        );
    }

    /**
     * Parses, generates and executes one Cyphail query.
     */
    private String runQuery(String source) throws Exception {

        String goal = compile(source);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            return execute(broker, goal);
        }
    }

    /**
     * Extracts non-empty lines from a returned table.
     */
    private static List<String> lines(String table) {

        return Arrays.stream(table.split("\\R"))
                .filter(line -> !line.isBlank())
                .toList();
    }

    /**
     * AND requires both conditions to be true.
     *
     * The sample graph has Jorge Soto, age 43.
     */
    @Test
    void shouldExecuteAndCondition() throws Exception {

        String table = runQuery(
                """
                MATCH (a:Person)
                WHERE a.age > 40
                  AND a.name = 'Jorge Soto'
                RETURN a
                """
        );

        assertEquals(
                List.of("a", "id2"),
                lines(table)
        );
    }

    /**
     * OR must accept either condition.
     *
     * The expected matching nodes are Juan and Jorge.
     */
    @Test
    void shouldExecuteOrCondition() throws Exception {

        String table = runQuery(
                """
                MATCH (a:Person)
                WHERE a.name = 'Juan Perez'
                   OR a.name = 'Jorge Soto'
                RETURN a
                """
        );

        List<String> result = lines(table);

        assertEquals("a", result.getFirst());

        assertEquals(3, result.size());

        assertTrue(result.contains("id1"));
        assertTrue(result.contains("id2"));
    }

    /**
     * AND must have higher precedence than OR.
     *
     * Equivalent to:
     *
     * a.name = 'Juan Perez'
     * OR (
     *   a.age > 40
     *   AND a.name = 'Jorge Soto'
     * )
     */
    @Test
    void shouldRespectAndPrecedenceOverOr()
            throws Exception {

        String table = runQuery(
                """
                MATCH (a:Person)
                WHERE a.name = 'Juan Perez'
                   OR a.age > 40
                  AND a.name = 'Jorge Soto'
                RETURN a
                """
        );

        List<String> result = lines(table);

        assertEquals("a", result.getFirst());

        assertEquals(3, result.size());

        assertTrue(result.contains("id1"));
        assertTrue(result.contains("id2"));
    }

    /**
     * Parentheses must override operator precedence.
     *
     * Only Jorge Soto satisfies the grouped condition.
     */
    @Test
    void shouldRespectParenthesizedConditions()
            throws Exception {

        String table = runQuery(
                """
                MATCH (a:Person)
                WHERE (
                    a.name = 'Juan Perez'
                    OR a.name = 'Jorge Soto'
                )
                AND a.age > 40
                RETURN a
                """
        );

        assertEquals(
                List.of("a", "id2"),
                lines(table)
        );
    }

    /**
     * Regression test for overlapping OR conditions.
     *
     * A Cyphail WHERE expression is a boolean filter.
     * One matched node should not be returned twice
     * merely because both sides of OR are true.
     *
     * Juan Perez has age 30, so both branches succeed.
     */
    @Test
    void shouldNotDuplicateRowsWhenBothOrBranchesMatch()
            throws Exception {

        String table = runQuery(
                """
                MATCH (a:Person {name: 'Juan Perez'})
                WHERE a.age > 18
                   OR a.age < 40
                RETURN a
                """
        );

        assertEquals(
                List.of("a", "id1"),
                lines(table),
                "Overlapping OR branches must not "
                        + "duplicate a matched row."
        );
    }
}
