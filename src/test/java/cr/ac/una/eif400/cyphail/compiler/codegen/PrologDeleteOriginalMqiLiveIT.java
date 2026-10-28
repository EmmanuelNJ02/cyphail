
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
 * Cyphail - Sprint P2
 * EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 *
 * Real MQI integration tests for DELETE
 * on the professor's original sample graph.
 *
 * Execution pipeline:
 *
 * Cyphail -> Parser -> AST -> CodeGen
 *         -> MQI -> SWI-Prolog -> Rows
 *
 * Each test starts an independent Prolog
 * process to avoid modifying another test.
 */
class PrologDeleteOriginalMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Locates the actual SWI-Prolog engine.
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
     * Compiles Cyphail into a Prolog goal.
     */
    private String compile(String source) {

        Query query =
                parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result);
        assertNotNull(result.goal());
        assertFalse(result.goal().isBlank());

        return result.goal();
    }

    /**
     * Executes a Prolog goal through MQI.
     */
    private static PrologResult.Success executeSuccess(
            MqiBroker broker,
            String goal
    ) throws Exception {

        PrologResult result =
                broker.execute(goal);

        return assertInstanceOf(
                PrologResult.Success.class,
                result,
                "Prolog goal must succeed. Actual: " + result
        );
    }

    /**
     * Executes Cyphail and retrieves Rows.
     */
    private String run(
            MqiBroker broker,
            String source
    ) throws Exception {

        PrologResult.Success success =
                executeSuccess(
                        broker,
                        compile(source)
                );

        assertEquals(
                1,
                success.rows().size()
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        return assertInstanceOf(
                String.class,
                bindings.get("Rows"),
                "Cyphail must return Rows."
        );
    }

    // ============================================
    // TEST 1 - ORIGINAL RELATIONSHIP
    // ============================================

    @Test
    void shouldDeleteOriginalRelationship()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String before = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'Jorge Soto'
                    })
                    RETURN r
                    """
            );

            assertEquals(
                    "r\ne1",
                    before,
                    "Original relationship e1 must exist."
            );

            run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'Jorge Soto'
                    })
                    DELETE r
                    """
            );

            String after = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'Jorge Soto'
                    })
                    RETURN r
                    """
            );

            assertEquals(
                    "r",
                    after,
                    "Original relationship e1 must disappear."
            );

            // Verify physical graph state.
            // Both nodes and e2 must remain.

            executeSuccess(
                    broker,
                    "\\+ user_amigos_edge(e1, _, _), "
                            + "\\+ user_amigos_label(e1, _), "
                            + "user_amigos_node(id1), "
                            + "user_amigos_node(id2), "
                            + "user_amigos_edge(e2, id1, id4)"
            );
        }
    }

    // ============================================
    // TEST 2 - NORMAL DELETE PROTECTION
    // ============================================

    @Test
    void shouldProtectConnectedOriginalNode()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String before = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    RETURN a
                    """
            );

            assertEquals(
                    "a\nid1",
                    before
            );

            // Normal DELETE must not remove a
            // node with connected relationships.

            run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    DELETE a
                    """
            );

            String after = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    RETURN a
                    """
            );

            assertEquals(
                    "a\nid1",
                    after,
                    "Connected original node must remain."
            );

            executeSuccess(
                    broker,
                    "user_amigos_node(id1), "
                            + "user_amigos_edge(e1, id1, id2), "
                            + "user_amigos_edge(e2, id1, id4), "
                            + "user_amigos_edge(e3, id2, id1)"
            );
        }
    }

    // ============================================
    // TEST 3 - ORIGINAL DETACH DELETE
    // ============================================

    @Test
    void shouldDetachDeleteOriginalNode()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    DETACH DELETE a
                    """
            );

            String deleted = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    RETURN a
                    """
            );

            assertEquals(
                    "a",
                    deleted,
                    "Original node id1 must disappear."
            );

            // Other original graph elements
            // must remain intact.

            String preserved = run(
                    broker,
                    """
                    MATCH (b:Person {
                        name: 'Jorge Soto'
                    })
                    RETURN b.age
                    """
            );

            assertEquals(
                    "b.age\n43",
                    preserved
            );

            executeSuccess(
                    broker,
                    "\\+ user_amigos_node(id1), "
                            + "\\+ user_amigos_edge(e1, _, _), "
                            + "\\+ user_amigos_edge(e2, _, _), "
                            + "\\+ user_amigos_edge(e3, _, _), "
                            + "\\+ user_amigos_label(id1, _), "
                            + "\\+ user_amigos_prop(id1, _, _), "
                            + "user_amigos_node(id2), "
                            + "user_amigos_node(id3), "
                            + "user_amigos_node(id4), "
                            + "user_amigos_edge(e4, id2, id3)"
            );
        }
    }

    // ============================================
    // TEST 4 - DELETE DISCONNECTED NODE
    // ============================================

    @Test
    void shouldDeleteDisconnectedOriginalNode()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            // Disconnect original node id1.
            // Use the existing Prolog operations
            // to prepare this particular scenario.

            executeSuccess(
                    broker,
                    "cy_delete_edge(amigos, e1), "
                            + "cy_delete_edge(amigos, e2), "
                            + "cy_delete_edge(amigos, e3)"
            );

            // Now DELETE can remove the node
            // through a complete Cyphail query.

            run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    DELETE a
                    """
            );

            String remaining = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    RETURN a
                    """
            );

            assertEquals(
                    "a",
                    remaining,
                    "Disconnected original node must be deleted."
            );

            executeSuccess(
                    broker,
                    "\\+ user_amigos_node(id1), "
                            + "\\+ user_amigos_label(id1, _), "
                            + "\\+ user_amigos_prop(id1, _, _), "
                            + "user_amigos_node(id2), "
                            + "user_amigos_node(id3), "
                            + "user_amigos_node(id4), "
                            + "user_amigos_edge(e4, id2, id3)"
            );
        }
    }
}
