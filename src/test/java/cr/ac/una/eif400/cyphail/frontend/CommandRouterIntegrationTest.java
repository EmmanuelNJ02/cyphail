
package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 *
 * Course: EIF400 - Paradigmas de Programacion
 * Project: Cyphail
 * Sprint: P2
 * Group: 04-10am
 *
 * Class: CommandRouterIntegrationTest
 *
 * Description:
 *
 * Integration tests for the Cyphail Sprint P2
 * command router.
 *
 * Sprint P2 replaces the fake query execution
 * engine from Sprint P1 with the real SWI-Prolog
 * engine accessed through the MQI protocol.
 *
 * These integration tests verify:
 *
 * 1. Correct routing of the .help command.
 * 2. Correct routing of the .about command.
 * 3. Preservation of the .use command.
 * 4. Real MATCH query execution through MQI.
 * 5. Graph state persistence between queries.
 * 6. Correct handling of the .exit command.
 *
 * Each test uses an independent SWI-Prolog MQI
 * session to prevent interference between tests.
 *
 * The simulated query engine from Sprint P1
 * is not used by these tests.
 *
 * Team Members:
 *
 * Emmanuel Núñez Jiménez
 * Valery Alfaro Morales
 * Roy Arias Mejía
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * @author Emmanuel Núñez Jiménez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejía
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
class CommandRouterIntegrationTest {

    // ============================================
    // TEST RESOURCES
    // ============================================

    private PrintStream originalOut;

    private PrintStream testOutput;

    private ByteArrayOutputStream capturedOutput;

    private MqiBroker broker;

    private CommandRouter router;

    // ============================================
    // TEST SETUP
    // ============================================

    /**
     * Initializes a new SWI-Prolog MQI session
     * before each integration test.
     *
     * The original standard output is saved
     * so test results can be captured without
     * affecting the console after execution.
     *
     * Each test receives an independent graph state.
     *
     * @throws IOException if the MQI session
     *                     cannot be initialized
     */
    @BeforeEach
    void setUp() throws IOException {

        // Preserve the original standard output.

        originalOut = System.out;

        // Prepare the output capture stream.

        capturedOutput = new ByteArrayOutputStream();

        testOutput = new PrintStream(
                capturedOutput,
                true,
                StandardCharsets.UTF_8
        );

        System.setOut(testOutput);

        // Locate the real SWI-Prolog engine.

        Path engineFile = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath().normalize();

        // Start a fresh SWI-Prolog MQI session.

        broker = MqiBroker.start(engineFile);

        // Initialize the Sprint P2 command router.
        //
        // The router receives the real MQI broker,
        // so queries are executed through Prolog
        // instead of the old fake query engine.

        router = new CommandRouter(broker);
    }

    // ============================================
    // TEST CLEANUP
    // ============================================

    /**
     * Restores standard output and closes MQI.
     *
     * Closing the broker also releases
     * the associated SWI-Prolog process.
     *
     * @throws IOException if the MQI session
     *                     cannot be closed
     */
    @AfterEach
    void tearDown() throws IOException {

        // Restore standard output.

        if (originalOut != null) {
            System.setOut(originalOut);
        }

        try {

            // Close the SWI-Prolog MQI session.

            if (broker != null) {
                broker.close();
            }

        } finally {

            // Release the test output stream.

            if (testOutput != null) {
                testOutput.close();
            }
        }
    }

    // ============================================
    // TEST 1 - HELP COMMAND
    // ============================================

    /**
     * Verifies that the command router
     * dispatches .help correctly.
     *
     * The existing Sprint P1 command
     * must remain functional in Sprint P2.
     */
    @Test
    void shouldRouteHelpCommand() {

        boolean shouldContinue =
                router.route(".help");

        String output = consoleOutput();

        // The REPL should remain active.

        assertTrue(shouldContinue);

        // Verify that help information is displayed.

        assertTrue(
                output.contains(
                        "Cyphail REPL commands:"
                )
        );

        assertTrue(
                output.contains(".help")
        );

        assertTrue(
                output.contains(".use <graph>")
        );

        assertTrue(
                output.contains(".exit")
        );
    }

    // ============================================
    // TEST 2 - ABOUT COMMAND
    // ============================================

    /**
     * Verifies that the command router
     * dispatches .about correctly.
     *
     * The command must preserve the existing
     * project and group identification.
     */
    @Test
    void shouldRouteAboutCommand() {

        boolean shouldContinue =
                router.route(".about");

        String output = consoleOutput();

        // The REPL must remain active.

        assertTrue(shouldContinue);

        // Verify the project information.

        assertTrue(
                output.contains("Cyphail")
        );

        assertTrue(
                output.contains("Work Group: 04")
        );

        assertTrue(
                output.contains(
                        "Group Code: 04-10am"
                )
        );
    }

    // ============================================
    // TEST 3 - USE COMMAND
    // ============================================

    /**
     * Verifies integration between the router,
     * the .use command and the graph catalog.
     *
     * The graph catalog from Sprint P1
     * must remain available in Sprint P2.
     */
    @Test
    void shouldRouteUseCommandThroughGraphCatalog() {

        boolean shouldContinue =
                router.route(".use");

        String output = consoleOutput();

        // Verify that the REPL continues.

        assertTrue(shouldContinue);

        // Verify the graph catalog output.

        assertTrue(
                output.contains("Graph")
        );

        assertTrue(
                output.contains("Description")
        );

        assertTrue(
                output.contains("amigos")
        );

        assertTrue(
                output.contains("planets")
        );
    }

    // ============================================
    // TEST 4 - REAL MATCH QUERY
    // ============================================

    /**
     * Executes a real MATCH query using
     * the SWI-Prolog MQI engine.
     *
     * This test replaces the old Sprint P1
     * test that expected simulated data.
     *
     * Query:
     *
     * MATCH (p:Person) RETURN p.name
     *
     * The query must return the original
     * graph data supplied by engine.pl.
     *
     * Internal generated Prolog variables
     * must not appear in the REPL output.
     */
    @Test
    void shouldRouteRealMatchQueryThroughMqi() {

        boolean shouldContinue = router.route(
                "MATCH (p:Person) RETURN p.name"
        );

        String output = consoleOutput();

        // The query must not stop the REPL.

        assertTrue(shouldContinue);

        // Verify the expected output header.

        assertTrue(
                output.contains("p.name")
        );

        // Verify actual SWI-Prolog graph data.

        assertTrue(
                output.contains("Juan Perez")
        );

        assertTrue(
                output.contains("Jorge Soto")
        );

        // Internal Prolog implementation variables
        // must not be displayed as result columns.

        assertFalse(
                output.contains("__QueryRef")
        );

        assertFalse(
                output.contains("__P_NAME")
        );

        // The old fake execution message
        // must not appear.

        assertFalse(
                output.contains("42 ms")
        );
    }

    // ============================================
    // TEST 5 - PERSISTENT MQI SESSION
    // ============================================

    /**
     * Verifies that successive Cyphail queries
     * share the same SWI-Prolog MQI session.
     *
     * The first query creates a new node.
     *
     * The second query must retrieve the node
     * and its properties from the same graph.
     *
     * This validates the persistent-session
     * behavior required by Sprint P2.
     */
    @Test
    void shouldPreserveGraphChangesBetweenQueries() {

        // =====================================
        // STEP 1 - CREATE A TEST NODE
        // =====================================

        boolean created = router.route(
                """
                CREATE (p:RouterP2Test {
                    name: 'Nodo_Router_P2',
                    age: 25
                })
                RETURN p
                """
        );

        // Verify that CREATE was processed.

        assertTrue(created);

        // The engine should generate a node ID.

        assertTrue(
                consoleOutput().contains("cy_node_")
        );

        // =====================================
        // STEP 2 - RESET CAPTURED OUTPUT
        // =====================================

        /*
         * Only the captured console output
         * is cleared.
         *
         * The MQI broker and SWI-Prolog process
         * remain active.
         *
         * Therefore, the created graph node
         * must still be available.
         */

        capturedOutput.reset();

        // =====================================
        // STEP 3 - QUERY THE CREATED NODE
        // =====================================

        boolean queried = router.route(
                """
                MATCH (p:RouterP2Test)
                RETURN p.name, p.age
                """
        );

        String output = consoleOutput();

        // Verify the query was processed.

        assertTrue(queried);

        // Verify that the original property
        // values were retained.

        assertTrue(
                output.contains("Nodo_Router_P2")
        );

        assertTrue(
                output.contains("25")
        );

        // The result must not contain the
        // old simulated execution message.

        assertFalse(
                output.contains("42 ms")
        );

        // Verify no MQI communication error
        // was displayed during execution.

        assertFalse(
                output.contains(
                        "MQI communication error:"
                )
        );

        // Verify no Cyphail compilation
        // or query execution error occurred.

        assertFalse(
                output.contains(
                        "Cyphail query error:"
                )
        );
    }

    // ============================================
    // TEST 6 - EXIT COMMAND
    // ============================================

    /**
     * Verifies that the .exit command
     * requests termination of the REPL.
     *
     * Returning false indicates that
     * the interactive session should stop.
     */
    @Test
    void shouldStopWhenExitCommandIsRouted() {

        boolean shouldContinue =
                router.route(".exit");

        // The router must request termination.

        assertFalse(shouldContinue);
    }

    // ============================================
    // HELPER - CONSOLE OUTPUT
    // ============================================

    /**
     * Retrieves the output captured
     * during the current test.
     *
     * The returned text uses UTF-8
     * to preserve console characters.
     *
     * @return captured console output
     */
    private String consoleOutput() {

        return capturedOutput.toString(
                StandardCharsets.UTF_8
        );
    }
}
