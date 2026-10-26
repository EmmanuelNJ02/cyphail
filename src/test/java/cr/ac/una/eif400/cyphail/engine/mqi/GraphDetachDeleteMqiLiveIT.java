
package cr.ac.una.eif400.cyphail.engine.mqi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cyphail - Sprint P2
 * EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 *
 * Real MQI integration tests for DETACH DELETE.
 *
 * Verifies:
 * - Deletion of outgoing relationships.
 * - Deletion of incoming relationships.
 * - Deletion of self-loop relationships.
 * - Preservation of unrelated graph elements.
 * - Protection provided by normal DELETE.
 * - Deletion of isolated nodes.
 * - Persistence of mutations in one MQI session.
 *
 * The test uses temporary nodes and relationships.
 * No original sample graph data is modified.
 */
class GraphDetachDeleteMqiLiveIT {

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
     * Executes a real Prolog goal through MQI.
     */
    private static PrologResult.Success executeSuccess(
            MqiBroker broker,
            String goal
    ) throws IOException {

        PrologResult result = broker.execute(goal);

        return assertInstanceOf(
                PrologResult.Success.class,
                result,
                "Expected success. Actual result: " + result
        );
    }

    /**
     * Extracts an identifier returned by SWI-Prolog.
     */
    private static String identifier(
            Map<String, Object> bindings,
            String variable
    ) {

        return assertInstanceOf(
                String.class,
                bindings.get(variable),
                "Missing identifier: " + variable
        );
    }

    /**
     * Creates three relationships attached to A:
     *
     * A -> B
     * C -> A
     * A -> A
     *
     * Also creates an unrelated relationship:
     *
     * B -> D
     *
     * DETACH DELETE A must remove the first
     * three relationships but preserve B, C, D
     * and the unrelated relationship.
     */
    @Test
    void shouldDetachNodeAndPreserveUnrelatedGraphElements()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            assertTrue(broker.isOpen());

            String creationGoal = """
                    cy_create_node(
                        amigos, ['Persona'],
                        [nombre-'TestA'], A
                    ),
                    cy_create_node(
                        amigos, ['Persona'],
                        [nombre-'TestB'], B
                    ),
                    cy_create_node(
                        amigos, ['Persona'],
                        [nombre-'TestC'], C
                    ),
                    cy_create_node(
                        amigos, ['Persona'],
                        [nombre-'TestD'], D
                    ),
                    cy_create_edge(
                        amigos, A, B, 'SALE', [], E1
                    ),
                    cy_create_edge(
                        amigos, C, A, 'ENTRA', [], E2
                    ),
                    cy_create_edge(
                        amigos, A, A, 'CICLO', [], E3
                    ),
                    cy_create_edge(
                        amigos, B, D, 'PERMANECE', [], E4
                    )
                    """;

            PrologResult.Success creation =
                    executeSuccess(broker, creationGoal);

            assertEquals(1, creation.rows().size());

            Map<String, Object> ids =
                    creation.rows().getFirst();

            String a = identifier(ids, "A");
            String b = identifier(ids, "B");
            String c = identifier(ids, "C");
            String d = identifier(ids, "D");

            String e1 = identifier(ids, "E1");
            String e2 = identifier(ids, "E2");
            String e3 = identifier(ids, "E3");
            String e4 = identifier(ids, "E4");

            // Normal DELETE must not remove A
            // while connected relationships exist.

            PrologResult.Success normalDelete =
                    executeSuccess(
                            broker,
                            "findall(Id, "
                                    + "(Id = " + a
                                    + ", cy_delete_node(amigos, Id)), "
                                    + "Deleted)"
                    );

            assertEquals(
                    List.of(),
                    normalDelete.rows()
                            .getFirst()
                            .get("Deleted"),
                    "Normal DELETE must protect connected nodes."
            );

            // Execute DETACH DELETE in the same
            // persistent MQI session.

            executeSuccess(
                    broker,
                    "cy_detach_delete_node(amigos, " + a + ")"
            );

            // Read the graph in a later MQI request.
            // Only B, C and D must remain.

            PrologResult.Success nodes =
                    executeSuccess(
                            broker,
                            "findall(Id, "
                                    + "cy_get_node(amigos, Id, _, _), "
                                    + "RemainingNodes)"
                    );

            assertEquals(
                    List.of(b, c, d),
                    nodes.rows()
                            .getFirst()
                            .get("RemainingNodes"),
                    "DETACH DELETE must preserve B, C and D."
            );

            // The only remaining relationship
            // must be the unrelated B -> D edge.

            PrologResult.Success edges =
                    executeSuccess(
                            broker,
                            "findall(Id, "
                                    + "cy_get_edge(amigos, Id, _, _, _, _), "
                                    + "RemainingEdges)"
                    );

            assertEquals(
                    List.of(e4),
                    edges.rows()
                            .getFirst()
                            .get("RemainingEdges"),
                    "Only the unrelated edge must remain."
            );

            // Explicitly verify all deleted edges.

            executeSuccess(
                    broker,
                    "\\+ cy_get_edge(amigos, "
                            + e1 + ", _, _, _, _), "
                            + "\\+ cy_get_edge(amigos, "
                            + e2 + ", _, _, _, _), "
                            + "\\+ cy_get_edge(amigos, "
                            + e3 + ", _, _, _, _)"
            );

            // Confirm the unrelated relationship
            // still connects the correct nodes.

            executeSuccess(
                    broker,
                    "cy_get_edge(amigos, "
                            + e4
                            + ", 'PERMANECE', "
                            + b
                            + ", "
                            + d
                            + ", _)"
            );

            assertTrue(
                    broker.isOpen(),
                    "MQI must remain connected."
            );
        }
    }

    /**
     * DETACH DELETE must also work on an
     * isolated node with no relationships.
     */
    @Test
    void shouldDetachDeleteIsolatedNode()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            PrologResult.Success creation =
                    executeSuccess(
                            broker,
                            "cy_create_node("
                                    + "amigos, ['Temporal'], "
                                    + "[nombre-'Aislado'], NodeId)"
                    );

            String nodeId = identifier(
                    creation.rows().getFirst(),
                    "NodeId"
            );

            executeSuccess(
                    broker,
                    "cy_detach_delete_node(amigos, "
                            + nodeId + ")"
            );

            PrologResult.Success remaining =
                    executeSuccess(
                            broker,
                            "findall(Id, "
                                    + "cy_get_node(amigos, Id, _, _), "
                                    + "RemainingNodes)"
                    );

            assertEquals(
                    List.of(),
                    remaining.rows()
                            .getFirst()
                            .get("RemainingNodes"),
                    "The isolated node must be deleted."
            );
        }
    }
}
