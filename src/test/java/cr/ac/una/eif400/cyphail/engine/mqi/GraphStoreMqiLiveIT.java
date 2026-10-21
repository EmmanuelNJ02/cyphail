package cr.ac.una.eif400.cyphail.engine.mqi;

import org.junit.jupiter.api.Test;

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
 * Class: GraphStoreMqiLiveIT
 *
 * Description:
 * Integration tests for the Cyphail graph
 * storage operations through SWI-Prolog MQI.
 *
 * The tests verify that nodes can be created,
 * retrieved, and queried using the same
 * active MQI session.
 */
class GraphStoreMqiLiveIT {

    /**
     * Returns the absolute path to the main
     * SWI-Prolog engine file.
     *
     * @return the engine file path
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
     * Verifies node creation and retrieval
     * using the real MQI broker.
     *
     * The test creates a node, obtains its
     * generated identifier, retrieves the
     * stored node, and reads its name property.
     *
     * @throws Exception if MQI execution fails
     */
    @Test
    void shouldCreateAndRetrieveNodeThroughMqi()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            assertTrue(
                    broker.isOpen(),
                    "The MQI session must be open."
            );

            // Create a node using the graph store.

            PrologResult creationResult =
                    broker.execute(
                            "cy_create_node("
                                    + "amigos, "
                                    + "['Persona'], "
                                    + "[nombre-'Marta', edad-25], "
                                    + "Id)"
                    );

            PrologResult.Success creation =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            creationResult,
                            "Node creation must succeed. "
                                    + "Actual result: "
                                    + creationResult
                    );

            List<Map<String, Object>> creationRows =
                    creation.rows();

            assertEquals(
                    1,
                    creationRows.size(),
                    "Node creation must return one result."
            );

            Object identifier =
                    creationRows.get(0).get("Id");

            String nodeId =
                    assertInstanceOf(
                            String.class,
                            identifier,
                            "The generated node ID must be a string."
                    );

            assertTrue(
                    nodeId.matches("cy_node_[0-9]+"),
                    "The generated node ID has an unexpected format."
            );

            // Verify the stored node in the
            // same active MQI session.

            String nodeQuery =
                    "cy_get_node("
                            + "amigos, "
                            + nodeId + ", "
                            + "['Persona'], "
                            + "[nombre-'Marta', edad-25])";

            PrologResult nodeResult =
                    broker.execute(nodeQuery);

            PrologResult.Success node =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            nodeResult,
                            "The created node must be retrievable. "
                                    + "Actual result: "
                                    + nodeResult
                    );

            assertEquals(
                    1,
                    node.rows().size(),
                    "The stored node must be found exactly once."
            );

            // Retrieve the node's name property
            // using a separate MQI request.

            String propertyQuery =
                    "cy_get_node_property("
                            + "amigos, "
                            + nodeId + ", "
                            + "nombre, Name)";

            PrologResult propertyResult =
                    broker.execute(propertyQuery);

            PrologResult.Success properties =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            propertyResult,
                            "The node property query must succeed. "
                                    + "Actual result: "
                                    + propertyResult
                    );

            assertEquals(
                    1,
                    properties.rows().size(),
                    "The name property must return one value."
            );

            assertEquals(
                    "Marta",
                    properties.rows().get(0).get("Name"),
                    "The stored name must match the created value."
            );

            // Confirm that the broker remains
            // connected after all operations.

            assertTrue(
                    broker.isOpen(),
                    "The MQI session must remain open."
            );
        }
    }
}