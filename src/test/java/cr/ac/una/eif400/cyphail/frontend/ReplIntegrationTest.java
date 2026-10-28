

package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * National University of Costa Rica
 * School of Informatics
 *
 * Course: EIF400 - Programming Paradigms
 * Project: Cyphail
 * Sprint: P2
 * Group Code: 04-10am
 * Date: November 2026
 *
 * Class: ReplIntegrationTest
 *
 * Description:
 *
 * Integration tests for the Cyphail
 * Read-Eval-Print Loop.
 *
 * These tests verify:
 *
 * 1. REPL initialization and help commands.
 * 2. Empty input handling.
 * 3. Real Cyphail query execution via MQI.
 * 4. Multiline AST visualization.
 *
 * Sprint P2 uses a real SWI-Prolog MQI broker.
 * The simulated Sprint P1 query engine is
 * not used by these integration tests.
 *
 * A new broker is started for each test.
 * Each broker is closed after the test.
 *
 * Team Members:
 *
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
class ReplIntegrationTest {

    // ============================================
    // TEST DEPENDENCIES
    // ============================================

    private InputStream originalIn;

    private PrintStream originalOut;

    private ByteArrayOutputStream output;

    private MqiBroker broker;

    private Repl repl;

    // ============================================
    // TEST SETUP
    // ============================================

    /**
     * Initializes the real Sprint P2 environment.
     *
     * Redirects standard output before creating
     * the command router, so handlers that retain
     * System.out write into the captured stream.
     *
     * Each test uses an independent SWI-Prolog
     * process and authenticated MQI connection.
     *
     * @throws IOException if MQI cannot start
     */
    @BeforeEach
    void setUp() throws IOException {

        originalIn = System.in;
        originalOut = System.out;

        // Capture output before constructing handlers.

        output = new ByteArrayOutputStream();

        System.setOut(
                new PrintStream(
                        output,
                        true,
                        StandardCharsets.UTF_8
                )
        );

        // Locate the Prolog engine.

        Path engineFile = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath().normalize();

        // Start the SWI-Prolog MQI broker.

        broker = MqiBroker.start(engineFile);

        // Create the production router
        // using the authenticated broker.

        CommandRouter router =
                new CommandRouter(broker);

        // Create the REPL using the same router
        // and a fresh statement buffer.

        repl = new Repl(
                router,
                new CyphailStatementBuffer()
        );
    }

    // ============================================
    // TEST CLEANUP
    // ============================================

    /**
     * Closes the MQI broker and restores
     * standard input and output.
     *
     * Original streams are restored even
     * if broker shutdown raises an exception.
     *
     * @throws IOException if broker closure fails
     */
    @AfterEach
    void tearDown() throws IOException {

        try {

            if (broker != null) {
                broker.close();
            }

        } finally {

            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }

    // ============================================
    // TEST 1 - REPL STARTUP AND HELP
    // ============================================

    /**
     * Verifies that the REPL starts correctly,
     * displays the Sprint P2 welcome message,
     * and processes the .help command.
     *
     * The welcome message must identify
     * the correct version, group and date.
     */
    @Test
    void shouldStartReplAndProcessHelpCommand() {

        setInput(
                ".help\n"
                        + ".exit\n"
        );

        repl.start();

        String result = consoleOutput();

        // Verify the updated welcome message.

        assertTrue(
                result.contains(
                        "Welcome to Cyphail v0.1.0 - Sprint P2"
                )
        );

        assertTrue(
                result.contains(
                        "November 2026"
                )
        );

        assertTrue(
                result.contains(
                        "Group 04-10am"
                )
        );

        // Verify the REPL prompt.

        assertTrue(
                result.contains(">>> ")
        );

        // Verify the available REPL commands.

        assertTrue(
                result.contains(".help")
        );

        assertTrue(
                result.contains(".about")
        );

        assertTrue(
                result.contains(".use")
        );

        assertTrue(
                result.contains(".exit")
        );
    }

    // ============================================
    // TEST 2 - EMPTY INPUT
    // ============================================

    /**
     * Verifies that empty input does not
     * terminate the interactive session.
     *
     * The REPL must display another prompt
     * and continue accepting commands.
     */
    @Test
    void shouldIgnoreEmptyInputAndPromptAgain() {

        setInput(
                "\n"
                        + ".exit\n"
        );

        repl.start();

        String result = consoleOutput();

        assertTrue(
                countOccurrences(
                        result,
                        ">>> "
                ) >= 2
        );

        assertTrue(
                result.contains(
                        "Welcome to Cyphail v0.1.0 - Sprint P2"
                )
        );
    }

    // ============================================
    // TEST 3 - REAL MQI QUERY EXECUTION
    // ============================================

    /**
     * Verifies that the REPL executes
     * Cyphail statements through the
     * real SWI-Prolog MQI connection.
     *
     * The test creates a node and then
     * retrieves it within the same session.
     *
     * This also verifies that the graph
     * state persists between statements.
     */
    @Test
    void shouldExecuteCyphailQueriesWithPersistentMqiSession() {

        setInput(
                "CREATE (p:Persona "
                        + "{nombre: 'ReplP2Test'});\n"
                        + "MATCH (p:Persona "
                        + "{nombre: 'ReplP2Test'}) "
                        + "RETURN p.nombre;\n"
                        + ".exit\n"
        );

        repl.start();

        String result = consoleOutput();

        // Verify query execution returned the node.

        assertTrue(
                result.contains(
                        "ReplP2Test"
                )
        );

        // Verify the requested RETURN property.

        assertTrue(
                result.contains(
                        "p.nombre"
                )
        );

        // Verify that the execution path did
        // not report an unsupported command.

        assertFalse(
                result.contains(
                        "Command or query not implemented yet:"
                )
        );
    }

    // ============================================
    // TEST 4 - MULTILINE TREE VISUALIZATION
    // ============================================

    /**
     * Verifies that the REPL supports
     * multiline .tree queries terminated
     * by a semicolon.
     *
     * The handler must print a valid AST
     * without reporting a syntax error.
     */
    @Test
    void shouldRenderMultilineTreeWithSemicolon() {

        setInput(
                ".tree\n"
                        + "MATCH (p:Persona)\n"
                        + "RETURN p.nombre;\n"
                        + ".exit\n"
        );

        repl.start();

        String result = consoleOutput();

        // Verify AST rendering.

        assertTrue(
                result.contains("Query{")
        );

        assertTrue(
                result.contains("Match:{")
        );

        assertTrue(
                result.contains("labels: [Persona]")
        );

        assertTrue(
                result.contains("{(. p nombre)}")
        );

        // Verify the continuation prompt.

        assertTrue(
                result.contains("... ")
        );

        // Verify that the semicolon is accepted.

        assertFalse(
                result.contains("Syntax error:")
        );
    }

    // ============================================
    // TEST UTILITIES
    // ============================================

    /**
     * Replaces standard input with simulated
     * interactive REPL commands.
     *
     * @param commands complete input sequence
     */
    private void setInput(String commands) {

        System.setIn(
                new ByteArrayInputStream(
                        commands.getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );
    }

    /**
     * Returns the captured console output.
     *
     * @return UTF-8 output text
     */
    private String consoleOutput() {

        return output.toString(
                StandardCharsets.UTF_8
        );
    }

    /**
     * Counts occurrences of a text fragment.
     *
     * @param source text to inspect
     * @param fragment searched fragment
     * @return number of occurrences
     */
    private int countOccurrences(
            String source,
            String fragment
    ) {

        int count = 0;
        int index = 0;

        while (
                (index = source.indexOf(
                        fragment,
                        index
                )) != -1
        ) {

            count++;

            index += fragment.length();
        }

        return count;
    }
}
