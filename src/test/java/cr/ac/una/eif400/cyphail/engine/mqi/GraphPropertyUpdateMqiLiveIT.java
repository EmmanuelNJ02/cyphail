package cr.ac.una.eif400.cyphail.engine.mqi;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
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
 * Class: GraphPropertyUpdateMqiLiveIT
 *
 * Description:
 * Integration tests for updating graph node
 * and relationship properties through MQI.
 *
 * These tests verify the SET operations
 * implemented in the SWI-Prolog graph store.
 */
class GraphPropertyUpdateMqiLiveIT {

    /**
     * Locates the project's SWI-Prolog engine.
     *
     * @return the absolute engine file path
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
     * Verifies that node and relationship
     * properties can be updated through MQI.
     *
     * @throws Exception if MQI execution fails
     */
    @Test
    void shouldUpdateNodeAndRelationshipPropertiesThroughMqi()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            assertTrue(
                    broker.isOpen(),
                    "The MQI connection must be open."
            );

            // Create the initial graph elements.

            String creationGoal =
                    "cy_create_node("
                    + "amigos, ['Persona'], "
                    + "[nombre-'Marta', edad-25], A), "
                    + "cy_create_node("
                    + "amigos, ['Empresa'], "
                    + "[nombre-'UCR'], B), "
                    + "cy_create_edge("
                    + "amigos, A, B, 'TRABAJA_EN', "
                    + "[rol-'Dev'], E)";

            PrologResult creationResult =
                    broker.execute(creationGoal);

            PrologResult.Success creation =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            creationResult,
                            "Graph creation must succeed. "
                                    + "Actual result: "
                                    + creationResult
                    );

            assertEquals(
                    1,
                    creation.rows().size(),
                    "Graph creation must return one result."
            );

            Map<String, Object> identifiers =
                    creation.rows().get(0);

            String nodeId =
                    assertInstanceOf(
                            String.class,
                            identifiers.get("A"),
                            "The node identifier must be a string."
                    );

            String edgeId =
                    assertInstanceOf(
                            String.class,
                            identifiers.get("E"),
                            "The relationship identifier must be a string."
                    );

            // Update the existing node and
            // relationship properties.

            String updateGoal =
                    "cy_set_node_property("
                    + "amigos, " + nodeId + ", edad, 26), "
                    + "cy_set_edge_property("
                    + "amigos, " + edgeId
                    + ", rol, 'Senior Dev')";

            PrologResult updateResult =
                    broker.execute(updateGoal);

            assertInstanceOf(
                    PrologResult.Success.class,
                    updateResult,
                    "Property updates must succeed. "
                            + "Actual result: "
                            + updateResult
            );

            // Verify the updated node property.

            PrologResult ageResult =
                    broker.execute(
                            "cy_get_node_property("
                            + "amigos, " + nodeId
                            + ", edad, Age)"
                    );

            PrologResult.Success ageSuccess =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            ageResult,
                            "Node property retrieval must succeed."
                    );

            assertEquals(
                    1,
                    ageSuccess.rows().size(),
                    "The node must contain exactly "
                            + "one age property."
            );

            Object age =
                    ageSuccess.rows().get(0).get("Age");

            assertInstanceOf(
                    Number.class,
                    age,
                    "The updated age must be numeric."
            );

            assertEquals(
                    26L,
                    ((Number) age).longValue(),
                    "The updated age must be 26."
            );

            // Verify the updated relationship property.

            PrologResult roleResult =
                    broker.execute(
                            "cy_get_edge_property("
                            + "amigos, " + edgeId
                            + ", rol, Role)"
                    );

            PrologResult.Success roleSuccess =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            roleResult,
                            "Relationship property retrieval must succeed."
                    );

            assertEquals(
                    1,
                    roleSuccess.rows().size(),
                    "The relationship must contain "
                            + "exactly one role property."
            );

            assertEquals(
                    "Senior Dev",
                    roleSuccess.rows().get(0).get("Role"),
                    "The updated role must match."
            );

            // Verify that the MQI session
            // remains open after both updates.

            assertTrue(
                    broker.isOpen(),
                    "The MQI session must remain open."
            );
        }
    }
}