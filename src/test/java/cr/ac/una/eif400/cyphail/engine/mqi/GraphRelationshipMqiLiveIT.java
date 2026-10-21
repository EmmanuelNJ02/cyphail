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
 * Class: GraphRelationshipMqiLiveIT
 *
 * Description:
 * Integration tests for graph relationship storage
 * using the Cyphail MQI broker and SWI-Prolog engine.
 *
 * The tests verify relationship creation,
 * retrieval, direction, type, and properties.
 */
class GraphRelationshipMqiLiveIT {

    /**
     * Locates the project's Prolog engine.
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
     * Verifies that nodes and a directed relationship
     * can be created and queried through MQI.
     *
     * @throws Exception if MQI execution fails
     */
    @Test
    void shouldCreateAndRetrieveRelationshipThroughMqi()
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
                    + "[nombre-'Marta'], A), "
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
                            "Relationship creation must succeed. "
                                    + "Actual result: "
                                    + creationResult
                    );

            assertEquals(
                    1,
                    creation.rows().size(),
                    "The creation query must return one result."
            );

            Map<String, Object> identifiers =
                    creation.rows().get(0);

            String sourceId =
                    assertInstanceOf(
                            String.class,
                            identifiers.get("A"),
                            "The source node ID must be a string."
                    );

            String destinationId =
                    assertInstanceOf(
                            String.class,
                            identifiers.get("B"),
                            "The destination node ID must be a string."
                    );

            String relationshipId =
                    assertInstanceOf(
                            String.class,
                            identifiers.get("E"),
                            "The relationship ID must be a string."
                    );

            assertTrue(
                    sourceId.matches("cy_node_[0-9]+"),
                    "The source node ID must be valid."
            );

            assertTrue(
                    destinationId.matches("cy_node_[0-9]+"),
                    "The destination node ID must be valid."
            );

            assertTrue(
                    relationshipId.matches("cy_edge_[0-9]+"),
                    "The relationship ID must be valid."
            );

            assertNotEquals(
                    sourceId,
                    destinationId,
                    "The source and destination must be distinct."
            );

            // Retrieve the stored relationship.

            String relationshipGoal =
                    "cy_get_edge(amigos, "
                    + relationshipId
                    + ", Type, From, To, Properties)";

            PrologResult relationshipResult =
                    broker.execute(relationshipGoal);

            PrologResult.Success relationship =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            relationshipResult,
                            "Relationship retrieval must succeed. "
                                    + "Actual result: "
                                    + relationshipResult
                    );

            assertEquals(
                    1,
                    relationship.rows().size(),
                    "Exactly one relationship must be retrieved."
            );

            Map<String, Object> relationshipData =
                    relationship.rows().get(0);

            assertEquals(
                    "TRABAJA_EN",
                    relationshipData.get("Type"),
                    "The relationship type must match."
            );

            assertEquals(
                    sourceId,
                    relationshipData.get("From"),
                    "The source node must match."
            );

            assertEquals(
                    destinationId,
                    relationshipData.get("To"),
                    "The destination node must match."
            );

            assertNotNull(
                    relationshipData.get("Properties"),
                    "The relationship properties must be returned."
            );

            // Retrieve the relationship's role property.

            String propertyGoal =
                    "cy_get_edge_property(amigos, "
                    + relationshipId
                    + ", rol, Role)";

            PrologResult propertyResult =
                    broker.execute(propertyGoal);

            PrologResult.Success property =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            propertyResult,
                            "Relationship property retrieval must succeed. "
                                    + "Actual result: "
                                    + propertyResult
                    );

            assertEquals(
                    1,
                    property.rows().size(),
                    "The role property must return one result."
            );

            assertEquals(
                    "Dev",
                    property.rows().get(0).get("Role"),
                    "The relationship role must match."
            );

            assertTrue(
                    broker.isOpen(),
                    "The MQI session must remain open."
            );
        }
    }
}