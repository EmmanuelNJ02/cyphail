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
 * Class: GraphPropertyRemovalMqiLiveIT
 *
 * Description:
 * Integration tests for removing node and
 * relationship properties through SWI-Prolog MQI.
 *
 * The tests verify that REMOVE deletes the
 * requested properties while preserving
 * the remaining graph data.
 *
 * Empty results are checked using findall/3,
 * allowing MQI to return a successful query
 * with an empty result list.
 */
class GraphPropertyRemovalMqiLiveIT {

    /**
     * Locates the project's SWI-Prolog engine.
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
     * Verifies property removal from nodes
     * and relationships using the MQI broker.
     *
     * @throws Exception if MQI execution fails
     */
    @Test
    void shouldRemoveNodeAndRelationshipPropertiesThroughMqi()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            assertTrue(
                    broker.isOpen(),
                    "The MQI connection must be open."
            );

            // Create two nodes and one relationship.

            String creationGoal =
                    "cy_create_node("
                    + "amigos, ['Persona'], "
                    + "[nombre-'Marta', edad-25], A), "
                    + "cy_create_node("
                    + "amigos, ['Empresa'], "
                    + "[nombre-'UCR'], B), "
                    + "cy_create_edge("
                    + "amigos, A, B, 'TRABAJA_EN', "
                    + "[rol-'Dev', desde-2024], E)";

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
                            "The node ID must be a string."
                    );

            String edgeId =
                    assertInstanceOf(
                            String.class,
                            identifiers.get("E"),
                            "The relationship ID must be a string."
                    );

            // Remove the node age and relationship role.

            String removalGoal =
                    "cy_remove_node_property("
                    + "amigos, " + nodeId + ", edad), "
                    + "cy_remove_edge_property("
                    + "amigos, " + edgeId + ", rol)";

            PrologResult removalResult =
                    broker.execute(removalGoal);

            assertInstanceOf(
                    PrologResult.Success.class,
                    removalResult,
                    "Property removal must succeed. "
                            + "Actual result: "
                            + removalResult
            );

            // Check that the removed age property
            // produces an empty list of values.

            String ageGoal =
                    "findall(Age, "
                    + "cy_get_node_property("
                    + "amigos, " + nodeId
                    + ", edad, Age), Ages)";

            PrologResult ageResult =
                    broker.execute(ageGoal);

            PrologResult.Success ageSuccess =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            ageResult,
                            "The age verification must succeed. "
                                    + "Actual result: "
                                    + ageResult
                    );

            assertEquals(
                    1,
                    ageSuccess.rows().size(),
                    "The age verification must return one result."
            );

            assertEquals(
                    List.of(),
                    ageSuccess.rows().get(0).get("Ages"),
                    "The removed age property must not exist."
            );

            // Verify that the node name still exists.

            PrologResult nameResult =
                    broker.execute(
                            "cy_get_node_property("
                            + "amigos, " + nodeId
                            + ", nombre, Name)"
                    );

            PrologResult.Success nameSuccess =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            nameResult,
                            "The name property must remain. "
                                    + "Actual result: "
                                    + nameResult
                    );

            assertEquals(
                    1,
                    nameSuccess.rows().size(),
                    "Exactly one name property must remain."
            );

            assertEquals(
                    "Marta",
                    nameSuccess.rows().get(0).get("Name"),
                    "The node name must be preserved."
            );

            // Check that the removed relationship
            // role produces an empty list.

            String roleGoal =
                    "findall(Role, "
                    + "cy_get_edge_property("
                    + "amigos, " + edgeId
                    + ", rol, Role), Roles)";

            PrologResult roleResult =
                    broker.execute(roleGoal);

            PrologResult.Success roleSuccess =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            roleResult,
                            "The role verification must succeed. "
                                    + "Actual result: "
                                    + roleResult
                    );

            assertEquals(
                    1,
                    roleSuccess.rows().size(),
                    "The role verification must return one result."
            );

            assertEquals(
                    List.of(),
                    roleSuccess.rows().get(0).get("Roles"),
                    "The removed role property must not exist."
            );

            // Verify that the relationship year remains.

            PrologResult yearResult =
                    broker.execute(
                            "cy_get_edge_property("
                            + "amigos, " + edgeId
                            + ", desde, Year)"
                    );

            PrologResult.Success yearSuccess =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            yearResult,
                            "The year property must remain. "
                                    + "Actual result: "
                                    + yearResult
                    );

            assertEquals(
                    1,
                    yearSuccess.rows().size(),
                    "Exactly one year property must remain."
            );

            Object year =
                    yearSuccess.rows().get(0).get("Year");

            assertInstanceOf(
                    Number.class,
                    year,
                    "The year property must be numeric."
            );

            assertEquals(
                    2024L,
                    ((Number) year).longValue(),
                    "The year property must be preserved."
            );

            // Verify that MQI remains connected.

            assertTrue(
                    broker.isOpen(),
                    "The MQI connection must remain open."
            );
        }
    }
}