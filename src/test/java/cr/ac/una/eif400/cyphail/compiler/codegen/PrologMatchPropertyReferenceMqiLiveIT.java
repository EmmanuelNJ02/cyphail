
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;
import cr.ac.una.eif400.cyphail.engine.mqi.PrologResult;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
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
 * Julissa Solano Valverde
 * Valery Alfaro Morales
 * Keynell Molina Mora
 * Roy Arias Mejia
 *
 * Real SWI-Prolog / MQI integration test.
 *
 * Verifies Sprint P1 compatibility for MATCH
 * patterns that reference properties of
 * previously declared variables.
 *
 * Example:
 *
 * MATCH (p:Person {id: 123}),
 *       (o:Order {personId: p.id})
 * RETURN p.name, o.total
 *
 * Uses the complete P2 pipeline:
 *
 * Parser -> AST -> CodeGen -> MQI -> SWI-Prolog
 */
class PrologMatchPropertyReferenceMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Finds the real SWI-Prolog engine.
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
     * Executes a Cyphail query through
     * the real SWI-Prolog MQI connection.
     *
     * Returns the result table.
     */
    private String run(
            MqiBroker broker,
            String source
    ) throws Exception {

        Query query = parser.parseOrThrow(source);

        PrologGenerationResult generated =
                generator.generate(query);

        assertNotNull(
                generated,
                "CodeGen must return a result."
        );

        assertFalse(
                generated.goal().isBlank(),
                "Generated Prolog goal cannot be blank."
        );

        PrologResult result =
                broker.execute(generated.goal());

        PrologResult.Success success =
                assertInstanceOf(
                        PrologResult.Success.class,
                        result,
                        "The generated goal must succeed. Actual: "
                                + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "MQI must return one result table."
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        assertTrue(
                bindings.containsKey("Rows"),
                "MQI must return a Rows binding."
        );

        return assertInstanceOf(
                String.class,
                bindings.get("Rows"),
                "Rows must contain a table string."
        );
    }

    /**
     * Verifies a property-to-property reference
     * during a real MATCH query.
     *
     * First, three nodes are created:
     *
     * Person: id = 202610298761
     *
     * Correct Order:
     * personId = 202610298761
     * total = 150
     *
     * Unrelated Order:
     * personId = 202610298762
     * total = 999
     *
     * MATCH must return only the correct order.
     */
    @Test
    void shouldMatchPropertyReferenceUsingRealMqi()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            // -----------------------------------------
            // CREATE TEST PERSON
            // -----------------------------------------

            String person = run(
                    broker,
                    """
                    CREATE (p:Person {
                        id: 202610298761,
                        name: 'MatchReferenceP2'
                    })
                    RETURN p
                    """
            );

            assertTrue(
                    person.contains("cy_node_"),
                    "The person must be created."
            );

            // -----------------------------------------
            // CREATE CORRECT ORDER
            // -----------------------------------------

            String correctOrder = run(
                    broker,
                    """
                    CREATE (o:Order {
                        personId: 202610298761,
                        total: 150
                    })
                    RETURN o
                    """
            );

            assertTrue(
                    correctOrder.contains("cy_node_"),
                    "The matching order must be created."
            );

            // -----------------------------------------
            // CREATE UNRELATED ORDER
            // -----------------------------------------

            String unrelatedOrder = run(
                    broker,
                    """
                    CREATE (o:Order {
                        personId: 202610298762,
                        total: 999
                    })
                    RETURN o
                    """
            );

            assertTrue(
                    unrelatedOrder.contains("cy_node_"),
                    "The unrelated order must be created."
            );

            // -----------------------------------------
            // MATCH PROPERTY REFERENCE
            // -----------------------------------------

            String table = run(
                    broker,
                    """
                    MATCH (p:Person {
                        id: 202610298761
                    }),
                    (o:Order {
                        personId: p.id
                    })
                    RETURN p.name, o.total
                    """
            );

            // -----------------------------------------
            // VERIFY REAL RESULT
            // -----------------------------------------

            assertEquals(
                    "p.name o.total\n'MatchReferenceP2' 150",
                    table,
                    "MATCH must return only the order "
                            + "whose personId equals p.id."
            );

            assertFalse(
                    table.contains("999"),
                    "An unrelated order must not match."
            );
        }
    }
}
