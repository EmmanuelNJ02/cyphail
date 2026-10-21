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
 * Integration tests for the SWI-Prolog graph engine
 * using the Cyphail MQI broker.
 *
 * The tests verify that the Prolog engine can be
 * loaded, graph relationships can be queried, and
 * generated Prolog queries can be executed through MQI.
 *
 * Query references remain internal to Prolog to
 * prevent unsupported values from being serialized
 * into the MQI JSON response.
 */
class PrologEngineMqiLiveIT {

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
     * Verifies that the MQI broker loads the Prolog
     * engine and retrieves the relationships stored
     * in the sample amigos graph.
     *
     * @throws Exception if MQI execution fails
     */
    @Test
    void shouldLoadEngineAndQueryRelationships()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            assertTrue(
                    broker.isOpen(),
                    "The MQI connection must be open."
            );

            PrologResult result = broker.execute(
                    "user_amigos_edge(E, 'Amigo', A, B)"
            );

            PrologResult.Success success =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            result,
                            "The relationship query must succeed."
                    );

            List<Map<String, Object>> rows =
                    success.rows();

            assertEquals(
                    4,
                    rows.size(),
                    "The sample graph must contain "
                            + "four relationships."
            );

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

    /**
     * Verifies the complete lifecycle of a generated
     * Prolog query through the MQI broker.
     *
     * The test performs the following operations:
     *
     * 1. Creates a temporary Prolog query.
     * 2. Retrieves reciprocal graph relationships.
     * 3. Applies numeric and inequality comparisons.
     * 4. Generates tabular output.
     * 5. Removes the temporary query.
     *
     * The internal query reference is isolated
     * inside findall/3 so MQI only returns the
     * serializable Rows binding.
     *
     * @throws Exception if MQI execution fails
     */
    @Test
    void shouldExecuteGeneratedQueryThroughMqi()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            assertTrue(
                    broker.isOpen(),
                    "The MQI connection must be open."
            );

            // Execute the generated query inside findall/3.
            //
            // Query references created by assertz/2 are
            // internal Prolog clause references.
            //
            // Those references must not be exposed
            // as MQI JSON result bindings.
            //
            // Only Rows is returned to the Java client.

            String goal =
                    "findall(Rows, ("
                    + "cy_create_query(Q, "
                    + "(cy_temp_query(AName, BName) :- "
                    + "user_amigos_edge(E1, 'Amigo', A, B), "
                    + "user_amigos_edge(E2, 'Amigo', B, A), "
                    + "cy_neq(E1, E2), "
                    + "user_amigos_prop(A, age, Age), "
                    + "cy_gt(Age, 18), "
                    + "user_amigos_prop(A, name, AName), "
                    + "user_amigos_prop(B, name, BName))), "
                    + "cy_exec_query(Q, "
                    + "['a.name', 'b.name'], Rows), "
                    + "cy_erase_query(Q)"
                    + "), [Rows])";

            PrologResult result = broker.execute(goal);

            // Verify that Prolog executed the query
            // successfully through MQI.

            PrologResult.Success success =
                    assertInstanceOf(
                            PrologResult.Success.class,
                            result,
                            "The generated query must succeed "
                                    + "through MQI. Actual result: "
                                    + result
                    );

            // The query must return one Rows binding
            // containing the complete tabular output.

            assertEquals(
                    1,
                    success.rows().size(),
                    "The query must produce exactly "
                            + "one tabular result."
            );

            Map<String, Object> bindings =
                    success.rows().get(0);

            assertTrue(
                    bindings.containsKey("Rows"),
                    "MQI must return the Rows binding."
            );

            assertNotNull(
                    bindings.get("Rows"),
                    "The Rows binding must not be null."
            );

            assertInstanceOf(
                    String.class,
                    bindings.get("Rows"),
                    "The Rows binding must contain "
                            + "a formatted table."
            );

            // Verify the exact tabular output.
            //
            // The sample graph contains reciprocal
            // relationships between Juan and Jorge.

            String expected =
                    "a.name b.name\n"
                    + "'Juan Perez' 'Jorge Soto'\n"
                    + "'Jorge Soto' 'Juan Perez'";

            assertEquals(
                    expected,
                    bindings.get("Rows"),
                    "The tabular output must match "
                            + "the expected relationships."
            );

            // Verify that the MQI session remains
            // available after query execution.

            assertTrue(
                    broker.isOpen(),
                    "The MQI connection must remain "
                            + "open after execution."
            );
        }
    }
}