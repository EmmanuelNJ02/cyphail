
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
 *
 * Complete Cyphail-to-MQI integration tests
 * for DELETE and DETACH DELETE.
 *
 * Execution pipeline:
 *
 * Cyphail -> Parser -> AST -> CodeGen
 *         -> MQI -> SWI-Prolog -> Rows
 *
 * Scenarios:
 *
 * 1. DETACH DELETE of a connected node.
 * 2. DELETE of a relationship.
 * 3. DELETE of an isolated node.
 * 4. DETACH DELETE without RETURN.
 *
 * Each test runs in an independent MQI session.
 *
 * Original professor graph elements are not
 * modified by these tests.
 */
class PrologDeleteMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Locates the real SWI-Prolog engine.
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
                "engine.pl must exist."
        );

        return path;
    }

    /**
     * Parses and transpiles Cyphail source
     * into an executable Prolog goal.
     */
    private String compile(String source) {

        Query query =
                parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result);

        assertNotNull(result.goal());

        assertFalse(
                result.goal().isBlank()
        );

        return result.goal();
    }

    /**
     * Executes Prolog through MQI and
     * retrieves the formatted Rows binding.
     */
    private static String execute(
            MqiBroker broker,
            String goal
    ) throws Exception {

        PrologResult result =
                broker.execute(goal);

        PrologResult.Success success =
                assertInstanceOf(
                        PrologResult.Success.class,
                        result,
                        "Expected a successful Prolog result. "
                                + "Actual: " + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "Exactly one Rows result is expected."
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        assertTrue(
                bindings.containsKey("Rows"),
                "MQI must return Rows."
        );

        return assertInstanceOf(
                String.class,
                bindings.get("Rows")
        );
    }

    /**
     * Executes a complete Cyphail query
     * through the active MQI broker.
     */
    private String run(
            MqiBroker broker,
            String source
    ) throws Exception {

        return execute(
                broker,
                compile(source)
        );
    }

    // =====================================================
    // TEST 1 - DETACH DELETE CONNECTED NODE
    // =====================================================

    @Test
    void shouldDetachDeleteConnectedNodeFromCyphail()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            // Create the graph using Cyphail.

            run(
                    broker,
                    """
                    CREATE (a:Person {
                        name: 'P2_DETACH_A'
                    })
                    -[r:KNOWS {
                        since: 2020
                    }]->
                    (b:Person {
                        name: 'P2_DETACH_B'
                    })
                    """
            );

            // Confirm that the relationship exists.

            String before = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_DETACH_A'
                    })
                    -[r:KNOWS]->
                    (b:Person {
                        name: 'P2_DETACH_B'
                    })
                    RETURN r
                    """
            );

            assertTrue(
                    before.startsWith("r\ncy_edge_"),
                    "The relationship must exist before DELETE."
            );

            // DETACH DELETE A.
            // Relationship r must also disappear.

            String updated = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_DETACH_A'
                    })
                    -[r:KNOWS]->
                    (b:Person {
                        name: 'P2_DETACH_B'
                    })
                    DETACH DELETE a
                    RETURN b
                    """
            );

            assertTrue(
                    updated.startsWith("b\ncy_node_"),
                    "Node B must remain available."
            );

            // Node A must no longer exist.

            String deletedNode = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_DETACH_A'
                    })
                    RETURN a
                    """
            );

            assertEquals(
                    "a",
                    deletedNode,
                    "DETACH DELETE must remove node A."
            );

            // The relationship must no longer exist.

            String deletedEdge = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_DETACH_A'
                    })
                    -[r:KNOWS]->
                    (b:Person {
                        name: 'P2_DETACH_B'
                    })
                    RETURN r
                    """
            );

            assertEquals(
                    "r",
                    deletedEdge,
                    "The attached relationship must disappear."
            );

            // Node B must preserve its original data.

            String remainingNode = run(
                    broker,
                    """
                    MATCH (b:Person {
                        name: 'P2_DETACH_B'
                    })
                    RETURN b.name
                    """
            );

            assertEquals(
                    "b.name\n'P2_DETACH_B'",
                    remainingNode,
                    "The unrelated node must be preserved."
            );

            assertTrue(broker.isOpen());
        }
    }

    // =====================================================
    // TEST 2 - DELETE RELATIONSHIP
    // =====================================================

    @Test
    void shouldDeleteRelationshipAndPreserveNodes()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (a:Person {
                        name: 'P2_EDGE_A'
                    })
                    -[r:KNOWS]->
                    (b:Person {
                        name: 'P2_EDGE_B'
                    })
                    """
            );

            // Delete only relationship r.

            String updated = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_EDGE_A'
                    })
                    -[r:KNOWS]->
                    (b:Person {
                        name: 'P2_EDGE_B'
                    })
                    DELETE r
                    RETURN a, b
                    """
            );

            assertTrue(
                    updated.startsWith("a b\n"),
                    "Both node identifiers must remain."
            );

            // The relationship must be absent.

            String deleted = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_EDGE_A'
                    })
                    -[r:KNOWS]->
                    (b:Person {
                        name: 'P2_EDGE_B'
                    })
                    RETURN r
                    """
            );

            assertEquals(
                    "r",
                    deleted,
                    "DELETE r must remove the relationship."
            );

            // Source node must remain.

            String sourceNode = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_EDGE_A'
                    })
                    RETURN a.name
                    """
            );

            assertEquals(
                    "a.name\n'P2_EDGE_A'",
                    sourceNode
            );

            // Destination node must remain.

            String destinationNode = run(
                    broker,
                    """
                    MATCH (b:Person {
                        name: 'P2_EDGE_B'
                    })
                    RETURN b.name
                    """
            );

            assertEquals(
                    "b.name\n'P2_EDGE_B'",
                    destinationNode
            );
        }
    }

    // =====================================================
    // TEST 3 - DELETE ISOLATED NODE
    // =====================================================

    @Test
    void shouldDeleteIsolatedNodeFromCyphail()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            // Create a node with no relationships.

            run(
                    broker,
                    """
                    CREATE (p:Person {
                        name: 'P2_DELETE_ISOLATED'
                    })
                    """
            );

            // DELETE the isolated node.

            run(
                    broker,
                    """
                    MATCH (p:Person {
                        name: 'P2_DELETE_ISOLATED'
                    })
                    DELETE p
                    """
            );

            // A new query must not find the node.

            String remaining = run(
                    broker,
                    """
                    MATCH (p:Person {
                        name: 'P2_DELETE_ISOLATED'
                    })
                    RETURN p
                    """
            );

            assertEquals(
                    "p",
                    remaining,
                    "DELETE must remove an isolated node."
            );
        }
    }

    // =====================================================
    // TEST 4 - DETACH DELETE WITHOUT RETURN
    // =====================================================

    @Test
    void shouldDetachDeleteWithoutReturn()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (p:Person {
                        name: 'P2_DETACH_NO_RETURN'
                    })
                    """
            );

            // Updating queries do not require RETURN.

            run(
                    broker,
                    """
                    MATCH (p:Person {
                        name: 'P2_DETACH_NO_RETURN'
                    })
                    DETACH DELETE p
                    """
            );

            // Check in a later query that
            // the mutation was not lost.

            String remaining = run(
                    broker,
                    """
                    MATCH (p:Person {
                        name: 'P2_DETACH_NO_RETURN'
                    })
                    RETURN p
                    """
            );

            assertEquals(
                    "p",
                    remaining,
                    "DETACH DELETE without RETURN "
                            + "must persist in the session."
            );

            assertTrue(
                    broker.isOpen(),
                    "MQI must remain connected."
            );
        }
    }
}
