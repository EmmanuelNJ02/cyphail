
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Class: ReplMultilineIntegrationTest
 *
 * Description:
 *
 * Integration tests for multiline statement
 * processing in the Cyphail Sprint P2 REPL.
 *
 * Sprint P2 executes Cyphail queries using
 * the real SWI-Prolog engine through MQI.
 *
 * These tests verify:
 *
 * 1. Single-line statements with semicolons.
 * 2. Multiline statements with semicolons.
 * 3. Statements without a final semicolon.
 * 4. Multiple statements on the same line.
 * 5. Continuation prompts.
 * 6. Compatibility with the .help command.
 * 7. Compatibility with inline .tree.
 * 8. Compatibility with multiline .tree.
 * 9. Exit while a statement is incomplete.
 *
 * Each test uses an independent MQI session.
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
class ReplMultilineIntegrationTest {

    // ============================================
    // TEST RESOURCES
    // ============================================

    private InputStream originalIn;

    private PrintStream originalOut;

    private PrintStream testOutput;

    private ByteArrayOutputStream output;

    // ============================================
    // TEST SETUP
    // ============================================

    /**
     * Saves the original console streams
     * and prepares output capture.
     *
     * The actual MQI connection is created
     * by runSession().
     */
    @BeforeEach
    void setUp() {

        originalIn = System.in;

        originalOut = System.out;

        output = new ByteArrayOutputStream();

        testOutput = new PrintStream(
                output,
                true,
                StandardCharsets.UTF_8
        );

        System.setOut(testOutput);
    }

    // ============================================
    // TEST CLEANUP
    // ============================================

    /**
     * Restores standard input and output
     * after every test execution.
     */
    @AfterEach
    void tearDown() {

        System.setIn(originalIn);

        System.setOut(originalOut);

        if (testOutput != null) {
            testOutput.close();
        }
    }

    // ============================================
    // TEST 1 - SINGLE-LINE STATEMENT
    // ============================================

    /**
     * Verifies that a complete Cyphail query
     * on one line is executed when it ends
     * with a semicolon.
     *
     * The returned values must come from
     * the real SWI-Prolog graph.
     */
    @Test
    void shouldProcessSingleLineStatementWithSemicolon()
            throws IOException {

        String result = runSession(
                "MATCH (p:Person) "
                        + "RETURN p.name, p.age;\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains("p.name")
        );

        assertTrue(
                result.contains("p.age")
        );

        assertTrue(
                result.contains("Juan Perez")
        );

        assertTrue(
                result.contains("Jorge Soto")
        );

        assertFalse(
                result.contains(
                        "OK. Query available after"
                )
        );
    }

    // ============================================
    // TEST 2 - MULTILINE STATEMENT
    // ============================================

    /**
     * Verifies that the REPL combines
     * multiple lines into one statement.
     *
     * The query must execute only after
     * receiving its final semicolon.
     */
    @Test
    void shouldProcessMultilineStatementWithSemicolon()
            throws IOException {

        String result = runSession(
                "MATCH (p:Person)\n"
                        + "RETURN p.name, p.age;\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains("p.name")
        );

        assertTrue(
                result.contains("p.age")
        );

        assertTrue(
                result.contains("Juan Perez")
        );

        assertTrue(
                result.contains("Jorge Soto")
        );

        assertTrue(
                result.contains("... ")
        );

        assertFalse(
                result.contains(
                        "MQI communication error:"
                )
        );
    }

    // ============================================
    // TEST 3 - MISSING SEMICOLON
    // ============================================

    /**
     * Verifies that an incomplete Cyphail
     * statement is not executed.
     *
     * When input ends without a semicolon,
     * the REPL must report the incomplete query.
     */
    @Test
    void shouldNotExecuteStatementBeforeSemicolon()
            throws IOException {

        String result = runSession(
                "MATCH (p:Person) "
                        + "RETURN p.name, p.age\n"
        );

        assertTrue(
                result.contains(
                        "Incomplete Cyphail statement:"
                )
        );

        assertFalse(
                result.contains("Juan Perez")
        );

        assertFalse(
                result.contains("p.name p.age")
        );

        assertFalse(
                result.contains(
                        "OK. Query available after"
                )
        );
    }

    // ============================================
    // TEST 4 - MULTIPLE STATEMENTS
    // ============================================

    /**
     * Verifies that two statements on the
     * same input line are executed separately.
     *
     * Both statements must be dispatched
     * to the real MQI engine.
     */
    @Test
    void shouldProcessMultipleStatementsOnOneLine()
            throws IOException {

        String result = runSession(
                "MATCH (p:Person) "
                        + "RETURN p.name, p.age;"
                        + " MATCH (p:Person) "
                        + "RETURN p.name, p.age;\n"
                        + ".exit\n"
        );

        assertEquals(
                2,
                countOccurrences(
                        result,
                        "p.name p.age"
                )
        );

        assertEquals(
                2,
                countOccurrences(
                        result,
                        "'Juan Perez'"
                )
        );

        assertFalse(
                result.contains(
                        "MQI communication error:"
                )
        );

        assertFalse(
                result.contains(
                        "OK. Query available after"
                )
        );
    }

    // ============================================
    // TEST 5 - CONTINUATION PROMPT
    // ============================================

    /**
     * Verifies that the REPL shows its
     * continuation prompt while waiting
     * for the final semicolon.
     */
    @Test
    void shouldShowContinuationPromptForMultilineQuery()
            throws IOException {

        String result = runSession(
                "MATCH (p:Person)\n"
                        + "RETURN p.name, p.age;\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains(">>> ")
        );

        assertTrue(
                result.contains("... ")
        );

        assertTrue(
                result.contains("Juan Perez")
        );
    }

    // ============================================
    // TEST 6 - HELP COMMAND
    // ============================================

    /**
     * Verifies that the .help command
     * remains available in Sprint P2.
     *
     * This preserves functionality
     * previously implemented in Sprint P1.
     */
    @Test
    void shouldKeepHelpCommandAvailable()
            throws IOException {

        String result = runSession(
                ".help\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains(
                        "Cyphail REPL commands:"
                )
        );

        assertTrue(
                result.contains(".help")
        );

        assertTrue(
                result.contains(".exit")
        );

        assertFalse(
                result.contains(
                        "Incomplete Cyphail statement:"
                )
        );
    }

    // ============================================
    // TEST 7 - INLINE TREE COMMAND
    // ============================================

    /**
     * Verifies that the existing inline
     * AST tree command remains functional.
     *
     * .tree parses the query and displays
     * the AST without executing it through MQI.
     */
    @Test
    void shouldPreserveInlineTreeCommand()
            throws IOException {

        String result = runSession(
                ".tree MATCH (m:Movie) RETURN m.title\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains("Query{")
        );

        assertTrue(
                result.contains("Match:{")
        );

        assertFalse(
                result.contains("Syntax error:")
        );
    }

    // ============================================
    // TEST 8 - MULTILINE TREE COMMAND
    // ============================================

    /**
     * Verifies that the .tree command
     * supports a query on multiple lines.
     *
     * The AST must be displayed correctly
     * without losing the continuation prompt.
     */
    @Test
    void shouldPreserveMultilineTreeCommand()
            throws IOException {

        String result = runSession(
                ".tree\n"
                        + "MATCH (m:Movie)\n"
                        + "RETURN m.title\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains("Query{")
        );

        assertTrue(
                result.contains("Match:{")
        );

        assertTrue(
                result.contains("... ")
        );

        assertFalse(
                result.contains("Syntax error:")
        );
    }

    // ============================================
    // TEST 9 - EXIT WITH PENDING STATEMENT
    // ============================================

    /**
     * Verifies that .exit immediately
     * terminates the REPL, even when
     * a Cyphail statement is incomplete.
     *
     * The pending statement must be discarded.
     */
    @Test
    void shouldAllowExitWhileStatementIsPending()
            throws IOException {

        String result = runSession(
                "MATCH (p:Person)\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains("... ")
        );

        assertFalse(
                result.contains(
                        "Incomplete Cyphail statement:"
                )
        );

        assertFalse(
                result.contains("Juan Perez")
        );

        assertFalse(
                result.contains(
                        "OK. Query available after"
                )
        );
    }

    // ============================================
    // HELPER - REAL REPL SESSION
    // ============================================

    /**
     * Runs a complete Cyphail REPL session
     * using the real SWI-Prolog MQI engine.
     *
     * A fresh MQI connection is created for
     * each invocation.
     *
     * The broker is closed automatically
     * after the REPL terminates.
     *
     * @param input commands and queries
     * @return captured console output
     * @throws IOException if MQI initialization
     *                     or communication fails
     */
    private String runSession(String input)
            throws IOException {

        System.setIn(
                new ByteArrayInputStream(
                        input.getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        Path engineFile = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath().normalize();

        try (MqiBroker broker = MqiBroker.start(engineFile)) {

            CommandRouter router =
                    new CommandRouter(broker);

            CyphailStatementBuffer statementBuffer =
                    new CyphailStatementBuffer();

            Repl repl = new Repl(
                    router,
                    statementBuffer
            );

            repl.start();
        }

        return output.toString(
                StandardCharsets.UTF_8
        );
    }

    // ============================================
    // HELPER - COUNT OCCURRENCES
    // ============================================

    /**
     * Counts occurrences of a specified
     * text fragment.
     *
     * @param text complete console output
     * @param value fragment being counted
     * @return total occurrences
     */
    private int countOccurrences(
            String text,
            String value
    ) {

        int count = 0;
        int index = 0;

        while (
                (index = text.indexOf(
                        value,
                        index
                )) != -1
        ) {

            count++;

            index += value.length();
        }

        return count;
    }
}
