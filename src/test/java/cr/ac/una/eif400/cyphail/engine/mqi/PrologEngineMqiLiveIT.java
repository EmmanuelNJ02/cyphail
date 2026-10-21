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
 * Class: PrologEngineMqiLiveIT
 *
 * Description:
 * Integration tests for the SWI-Prolog engine
 * using the Cyphail MQI broker.
 *
 * This class verifies that the project engine.pl
 * file can be loaded successfully and that queries
 * against the sample graph return the expected
 * relationship bindings.
 */
class PrologEngineMqiLiveIT {

    /**
     * Verifies that the MQI broker loads the Prolog
     * engine and executes a relationship query.
     *
     * The query retrieves relationship identifiers,
     * source nodes, and destination nodes from
     * the sample "amigos" graph.
     *
     * @throws Exception if the engine cannot start
     *                   or the MQI query fails
     */
    @Test
    void shouldLoadEngineAndQueryRelationships()
            throws Exception {

        // Locate the project's Prolog engine file.
        Path engine = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath();

        // Verify that the engine file exists.
        assertTrue(
                Files.isRegularFile(engine),
                "The engine.pl file must exist."
        );

        // Start the SWI-Prolog engine through MQI.
        try (MqiBroker broker = MqiBroker.start(engine)) {

            assertTrue(
                    broker.isOpen(),
                    "The MQI connection must be open."
            );

            // Execute a real relationship query.
            PrologResult result = broker.execute(
                    "user_amigos_edge(E, 'Amigo', A, B)"
            );

            // Verify successful query execution.
            PrologResult.Success success =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            result,
                            "The Prolog query must succeed."
                    );

            // Retrieve the query result bindings.
            List<Map<String, Object>> rows =
                    success.rows();

            // The sample graph contains four relationships.
            assertEquals(
                    4,
                    rows.size(),
                    "The sample graph must contain "
                            + "four relationships."
            );

            // Verify the bindings returned by Prolog.
            for (Map<String, Object> row : rows) {

                assertTrue(
                        row.containsKey("E"),
                        "The relationship identifier "
                                + "must be returned."
                );

                assertTrue(
                        row.containsKey("A"),
                        "The source node must be returned."
                );

                assertTrue(
                        row.containsKey("B"),
                        "The destination node must be returned."
                );

                assertNotNull(
                        row.get("E"),
                        "The relationship identifier "
                                + "must not be null."
                );

                assertNotNull(
                        row.get("A"),
                        "The source node must not be null."
                );

                assertNotNull(
                        row.get("B"),
                        "The destination node must not be null."
                );
            }
        }
    }
}