package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;
import cr.ac.una.eif400.cyphail.engine.mqi.PrologResult;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Real integration tests for the Cyphail code generator.
 *
 * Tests the complete execution pipeline:
 *
 * Cyphail source -> Parser -> AST -> Prolog CodeGen
 * -> MQI broker -> SWI-Prolog engine -> Result rows.
 *
 * Covered functionality:
 * - MATCH with isolated nodes.
 * - Outgoing relationships.
 * - Incoming relationships.
 * - Undirected relationships.
 * - WHERE numeric comparisons.
 * - WHERE string equality.
 * - WHERE property-to-property comparisons.
 * - Property reuse between WHERE and RETURN.
 * - WHERE filters producing no matching rows.
 * - Node-variable joins with repeated labels.
 * - Node-variable joins with incompatible labels.
 * - Node-variable joins with two distinct labels.
 *
 * Requirements:
 * - SWI-Prolog available through PATH.
 * - MQI support enabled.
 * - src/main/prolog/engine.pl available.
 *
 * Run with:
 * mvn -Dtest=PrologCodeGeneratorMqiLiveIT test
 */
class PrologCodeGeneratorMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    // =====================================================
    // HELPER - ENGINE PATH
    // =====================================================

    /**
     * Returns the absolute path of the real
     * SWI-Prolog engine used by Cyphail.
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
                "The Prolog engine file must exist."
        );

        return path;
    }

    // =====================================================
    // HELPER - COMPILE CYPHAIL QUERY
    // =====================================================

    /**
     * Parses Cyphail source and compiles it into
     * an executable Prolog goal.
     */
    private String compile(String source) {

        Query query = parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result.goal());

        assertFalse(result.goal().isBlank());

        return result.goal();
    }

    // =====================================================
    // HELPER - EXECUTE REAL MQI QUERY
    // =====================================================

    /**
     * Executes a generated goal using the real
     * MQI broker and returns the formatted table.
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
                        "Generated Prolog must execute successfully. "
                                + "Actual result: " + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "The generated query must return one table."
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        assertTrue(
                bindings.containsKey("Rows"),
                "MQI must return the Rows binding."
        );

        Object rows = bindings.get("Rows");

        assertInstanceOf(
                String.class,
                rows,
                "Rows must contain formatted text."
        );

        return (String) rows;
    }

    // =====================================================
    // TEST 1 - MATCH WITH ISOLATED NODES
    // =====================================================

    /**
     * Verifies MATCH with node property retrieval.
     */
    @Test
    void shouldExecuteGeneratedNodeQuery()
            throws Exception {

        String goal = compile("""
                MATCH (a:Person)
                RETURN a.name AS nombre
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertTrue(
                    table.startsWith("nombre"),
                    "The table must contain the alias header."
            );

            assertTrue(
                    table.contains("'Juan Perez'"),
                    "The graph must return Juan Perez."
            );

            assertTrue(
                    table.contains("'Jorge Soto'"),
                    "The graph must return Jorge Soto."
            );
        }
    }

    // =====================================================
    // TEST 2 - OUTGOING RELATIONSHIP
    // =====================================================

    /**
     * Verifies outgoing relationship matching.
     */
    @Test
    void shouldExecuteGeneratedOutgoingRelationship()
            throws Exception {

        String goal = compile("""
                MATCH (a:Person)-[r:Amigo]->(b:Person)
                RETURN a.name AS origen,
                       r AS relacion,
                       b.name AS destino
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertTrue(
                    table.startsWith(
                            "origen relacion destino"
                    )
            );

            assertTrue(
                    table.contains(
                            "'Juan Perez' e1 'Jorge Soto'"
                    ),
                    "Outgoing edge e1 must be returned."
            );

            assertTrue(
                    table.contains(
                            "'Jorge Soto' e3 'Juan Perez'"
                    ),
                    "Outgoing edge e3 must be returned."
            );
        }
    }

    // =====================================================
    // TEST 3 - INCOMING RELATIONSHIP
    // =====================================================

    /**
     * Verifies incoming relationship matching.
     */
    @Test
    void shouldExecuteGeneratedIncomingRelationship()
            throws Exception {

        String goal = compile("""
                MATCH (a:Person)<-[r:Amigo]-(b:Person)
                RETURN a.name AS destino,
                       b.name AS origen
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertTrue(
                    table.startsWith("destino origen")
            );

            assertTrue(
                    table.contains(
                            "'Jorge Soto' 'Juan Perez'"
                    ),
                    "Incoming edge e1 must reverse endpoints."
            );

            assertTrue(
                    table.contains(
                            "'Juan Perez' 'Jorge Soto'"
                    ),
                    "Incoming edge e3 must reverse endpoints."
            );
        }
    }

    // =====================================================
    // TEST 4 - UNDIRECTED RELATIONSHIP
    // =====================================================

    /**
     * Verifies both orientations of an
     * undirected relationship.
     */
    @Test
    void shouldExecuteGeneratedUndirectedRelationship()
            throws Exception {

        String goal = compile("""
                MATCH (a:Person)-[r:Amigo]-(b:Person)
                RETURN a.name AS persona,
                       b.name AS contacto
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertTrue(
                    table.startsWith("persona contacto")
            );

            assertTrue(
                    table.contains(
                            "'Juan Perez' 'Jorge Soto'"
                    ),
                    "The forward orientation must be returned."
            );

            assertTrue(
                    table.contains(
                            "'Jorge Soto' 'Juan Perez'"
                    ),
                    "The reverse orientation must be returned."
            );
        }
    }

    // =====================================================
    // TEST 5 - WHERE NUMERIC COMPARISON
    // =====================================================

    /**
     * Verifies that WHERE filters nodes using
     * a numeric property comparison.
     *
     * In the sample graph, Jorge Soto is the
     * only Person with an age greater than 40.
     */
    @Test
    void shouldExecuteWhereWithNumericComparison()
            throws Exception {

        String goal = compile("""
                MATCH (a:Person)
                WHERE a.age > 40
                RETURN a.name AS nombre
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertEquals(
                    "nombre\n'Jorge Soto'",
                    table
            );
        }
    }

    // =====================================================
    // TEST 6 - WHERE STRING EQUALITY
    // =====================================================

    /**
     * Verifies WHERE equality with a text literal.
     */
    @Test
    void shouldExecuteWhereWithStringEquality()
            throws Exception {

        String goal = compile("""
                MATCH (a:Person)
                WHERE a.name = 'Juan Perez'
                RETURN a
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertEquals(
                    "a\nid1",
                    table
            );
        }
    }

    // =====================================================
    // TEST 7 - WHERE NODE PROPERTY COMPARISON
    // =====================================================

    /**
     * Verifies comparison between properties
     * belonging to two related nodes.
     *
     * The sample graph contains:
     *
     * e3: Jorge Soto -> Juan Perez
     *
     * Jorge is older than Juan.
     */
    @Test
    void shouldExecuteWhereComparingNodeProperties()
            throws Exception {

        String goal = compile("""
                MATCH (a:Person)-[r:Amigo]->(b:Person)
                WHERE a.age > b.age
                RETURN r AS relacion,
                       a.name AS mayor,
                       b.name AS menor
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertTrue(
                    table.startsWith(
                            "relacion mayor menor"
                    )
            );

            assertTrue(
                    table.contains(
                            "e3 'Jorge Soto' 'Juan Perez'"
                    ),
                    "Relationship e3 must satisfy "
                            + "the age comparison."
            );

            assertFalse(
                    table.contains(
                            "e1 'Juan Perez' 'Jorge Soto'"
                    ),
                    "Relationship e1 must not satisfy "
                            + "the age comparison."
            );
        }
    }

    // =====================================================
    // TEST 8 - REUSE PROPERTY IN WHERE AND RETURN
    // =====================================================

    /**
     * Verifies that a property accessed in WHERE
     * can also be returned without an additional
     * property lookup.
     *
     * This test covers the duplicate-lookup
     * regression fixed in PrologCodeGenerator.
     */
    @Test
    void shouldReuseWherePropertyInReturn()
            throws Exception {

        String goal = compile("""
                MATCH (a:Person)
                WHERE a.name = 'Juan Perez'
                RETURN a.name AS nombre
                """);

        String propertyGoal =
                "user_amigos_prop(__A, 'name', __A_NAME)";

        assertEquals(
                1,
                countOccurrences(goal, propertyGoal),
                "WHERE and RETURN must share the "
                        + "same property lookup."
        );

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertEquals(
                    "nombre\n'Juan Perez'",
                    table
            );
        }
    }

    // =====================================================
    // TEST 9 - WHERE WITH NO MATCHES
    // =====================================================

    /**
     * Verifies that a WHERE condition matching
     * no nodes returns a table header without
     * incorrectly producing result rows.
     */
    @Test
    void shouldExecuteWhereWithNoMatches()
            throws Exception {

        String goal = compile("""
                MATCH (a:Person)
                WHERE a.age > 1000
                RETURN a.name AS nombre
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertEquals(
                    "nombre",
                    table
            );
        }
    }

    // =====================================================
    // TEST 10 - REUSED NODE VARIABLE WITH SAME LABEL
    // =====================================================

    /**
     * A reused node variable identifies one physical node.
     * Repeating a compatible label must not create a Cartesian
     * product or introduce duplicate result rows.
     *
     * The sample engine contains three Person nodes:
     * id1, id2 and id3.
     */
    @Test
    void shouldJoinRepeatedNodeVariableWithSameLabel()
            throws Exception {

        String goal = compile("""
                MATCH (e:Person), (e:Person)
                RETURN e
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            List<String> lines =
                    table.lines().toList();

            assertFalse(
                    lines.isEmpty(),
                    "The result must have a header."
            );

            assertEquals(
                    "e",
                    lines.getFirst()
            );

            List<String> ids =
                    lines.subList(1, lines.size());

            assertEquals(
                    3,
                    ids.size(),
                    "The shared variable must return "
                            + "exactly three rows."
            );

            assertEquals(
                    Set.of("id1", "id2", "id3"),
                    new HashSet<>(ids),
                    "All three Person nodes must "
                            + "appear exactly once."
            );
        }
    }

    // =====================================================
    // TEST 11 - INCOMPATIBLE LABEL CONSTRAINTS
    // =====================================================

    /**
     * The same variable with different labels means that the
     * bound node must satisfy BOTH label constraints.
     *
     * No node in the initial sample graph has JoinTestLabel.
     */
    @Test
    void shouldReturnNoRowsForIncompatibleNodeLabels()
            throws Exception {

        String goal = compile("""
                MATCH (e:Person), (e:JoinTestLabel)
                RETURN e
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertEquals(
                    "e",
                    table,
                    "No initial node should satisfy both labels."
            );
        }
    }

    // =====================================================
    // TEST 12 - TWO DISTINCT LABELS ON SAME NODE
    // =====================================================

    /**
     * Proves that two DIFFERENT labels can both match the same
     * node, rather than merely testing that incompatible labels
     * produce no rows.
     *
     * A single MQI session first creates a node with two labels,
     * then looks it up through two separate MATCH patterns that
     * reuse the same node variable.
     */
    @Test
    void shouldMatchCreatedNodeWithBothDistinctLabels()
            throws Exception {

        String createGoal = compile("""
                CREATE (d:Person:JoinTestLabel {
                    name: 'CyphailLabelJoinTestOnly'
                })
                RETURN d
                """);

        String matchGoal = compile("""
                MATCH (e:Person), (e:JoinTestLabel)
                WHERE e.name = 'CyphailLabelJoinTestOnly'
                RETURN e
                """);

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String createdTable = execute(broker, createGoal);

            assertTrue(
                    createdTable.startsWith("d\n"),
                    "CREATE must return the new node ID."
            );

            String createdId =
                    createdTable.substring("d\n".length()).trim();

            assertFalse(
                    createdId.isBlank(),
                    "The created node must have an ID."
            );

            assertFalse(
                    createdId.contains("\n"),
                    "CREATE must return exactly one node."
            );

            String matchedTable =
                    execute(broker, matchGoal);

            assertEquals(
                    "e\n" + createdId,
                    matchedTable,
                    "A node with both labels must satisfy the "
                            + "combined constraints on variable e."
            );
        }
    }

    // =====================================================
    // HELPER - COUNT GOAL OCCURRENCES
    // =====================================================

    /**
     * Counts occurrences of a Prolog goal in
     * the generated source string.
     */
    private static int countOccurrences(
            String text,
            String fragment
    ) {

        int count = 0;
        int index = 0;

        while (
                (index = text.indexOf(fragment, index)) >= 0
        ) {
            count++;
            index += fragment.length();
        }

        return count;
    }
}
