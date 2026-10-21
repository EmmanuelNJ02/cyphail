package cr.ac.una.eif400.cyphail.engine.mqi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Universidad Nacional de Costa Rica
 * School of Informatics
 *
 * Course: EIF400 - Programming Paradigms
 * Project: Cyphail
 * Sprint: P2
 * Group: 04-10am
 *
 * Team Members:
 * - Emmanuel Núñez Jiménez
 * - Julissa Solano Valverde
 * - Valery Alfaro Morales
 * - Keynell Molina Mora
 * - Roy Arias Mejia
 *
 * Class: GraphDeletionMqiLiveIT
 *
 * Description:
 * Integration tests for deleting graph relationships
 * and nodes through the Cyphail MQI broker.
 *
 * The tests verify that connected nodes cannot be
 * deleted and that removing their relationship
 * allows both nodes to be deleted safely.
 */
class GraphDeletionMqiLiveIT {

    /**
     * Locates the SWI-Prolog engine file.
     *
     * @return the absolute path to engine.pl
     */
    private static Path enginePath() {

        Path engine = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath();

        assertTrue(
                Files.isRegularFile(engine),
                "The engine.pl file must exist."
        );

        return engine;
    }

    /**
     * Executes a Prolog goal and verifies success.
     *
     * @param broker the active MQI broker
     * @param goal the Prolog goal
     * @return the successful result
     * @throws IOException if communication fails
     */
    private static PrologResult.Success executeSuccess(
            MqiBroker broker,
            String goal
    ) throws IOException {

        PrologResult result = broker.execute(goal);

        return assertInstanceOf(
                PrologResult.Success.class,
                result,
                "The Prolog goal must succeed. Actual result: "
                        + result
        );
    }

    /**
     * Verifies relationship deletion and node deletion
     * using a persistent MQI session.
     *
     * @throws Exception if MQI execution fails
     */
    @Test
    void shouldDeleteRelationshipsAndNodesThroughMqi()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            assertTrue(
                    broker.isOpen(),
                    "The MQI connection must be open."
            );

            // Create two nodes and their relationship.

            String creationGoal =
                    "cy_create_node("
                    + "amigos, ['Persona'], "
                    + "[nombre-'Marta'], A), "
                    + "cy_create_node("
                    + "amigos, ['Empresa'], "
                    + "[nombre-'UCR'], B), "
                    + "cy_create_edge("
                    + "amigos, A, B, 'TRABAJA_EN', "
                    + "[rol-'Dev'], E)";

            PrologResult.Success creation =
                    executeSuccess(broker, creationGoal);

            assertEquals(
                    1,
                    creation.rows().size(),
                    "Graph creation must return one result."
            );

            Map<String, Object> identifiers =
                    creation.rows().get(0);

            String sourceId = assertInstanceOf(
                    String.class,
                    identifiers.get("A"),
                    "The source node ID must be a string."
            );

            String destinationId = assertInstanceOf(
                    String.class,
                    identifiers.get("B"),
                    "The destination node ID must be a string."
            );

            String edgeId = assertInstanceOf(
                    String.class,
                    identifiers.get("E"),
                    "The relationship ID must be a string."
            );

            // Both connected nodes must be protected.
            // findall/3 returns an empty list when
            // neither node can be deleted.

            String protectedGoal =
                    "findall(Id, ("
                    + "member(Id, ["
                    + sourceId + ", "
                    + destinationId + "]), "
                    + "cy_delete_node(amigos, Id)"
                    + "), Deleted)";

            PrologResult.Success protectedResult =
                    executeSuccess(broker, protectedGoal);

            assertEquals(
                    1,
                    protectedResult.rows().size(),
                    "The protection check must return one result."
            );

            assertEquals(
                    List.of(),
                    protectedResult.rows().get(0).get("Deleted"),
                    "Connected nodes must not be deleted."
            );

            // Verify that the nodes still exist.

            PrologResult.Success sourceResult =
                    executeSuccess(
                            broker,
                            "cy_get_node(amigos, "
                                    + sourceId
                                    + ", _, _)"
                    );

            PrologResult.Success destinationResult =
                    executeSuccess(
                            broker,
                            "cy_get_node(amigos, "
                                    + destinationId
                                    + ", _, _)"
                    );

            assertEquals(
                    1,
                    sourceResult.rows().size(),
                    "The source node must be preserved."
            );

            assertEquals(
                    1,
                    destinationResult.rows().size(),
                    "The destination node must be preserved."
            );

            // Delete the relationship first.

            executeSuccess(
                    broker,
                    "cy_delete_edge(amigos, " + edgeId + ")"
            );

            // Verify that no stored relationships remain.

            PrologResult.Success edgeCheck =
                    executeSuccess(
                            broker,
                            "findall(Id, "
                                    + "cy_get_edge("
                                    + "amigos, Id, _, _, _, _), "
                                    + "RemainingEdges)"
                    );

            assertEquals(
                    List.of(),
                    edgeCheck.rows().get(0).get("RemainingEdges"),
                    "The deleted relationship must not exist."
            );

            // Delete the nodes after removing
            // the relationship connecting them.

            executeSuccess(
                    broker,
                    "cy_delete_node(amigos, "
                            + sourceId + "), "
                            + "cy_delete_node(amigos, "
                            + destinationId + ")"
            );

            // Verify that no stored nodes remain.

            PrologResult.Success nodeCheck =
                    executeSuccess(
                            broker,
                            "findall(Id, "
                                    + "cy_get_node("
                                    + "amigos, Id, _, _), "
                                    + "RemainingNodes)"
                    );

            assertEquals(
                    List.of(),
                    nodeCheck.rows().get(0).get("RemainingNodes"),
                    "The deleted nodes must not exist."
            );

            assertTrue(
                    broker.isOpen(),
                    "The MQI session must remain open."
            );
        }
    }
}