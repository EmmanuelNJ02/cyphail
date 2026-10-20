package cr.ac.una.eif400.cyphail.frontend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Sprint P2 multiline REPL processing.
 *
 * Verifies semicolon-terminated statements, continuation
 * prompts, command compatibility and incomplete input.
 *
 * The existing P1 query handler is used temporarily.
 * Real Prolog execution will replace it during P2 integration.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * School: Escuela de Informatica
 * Group Code: 04-10am
 *
 * Authors:
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
class ReplMultilineIntegrationTest {

    private InputStream originalIn;
    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        originalIn = System.in;
        originalOut = System.out;

        output = new ByteArrayOutputStream();

        System.setOut(
                new PrintStream(
                        output,
                        true,
                        StandardCharsets.UTF_8
                )
        );
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    void shouldProcessSingleLineStatementWithSemicolon() {
        String result = runSession(
                "MATCH (p:Persona) RETURN p.nombre, p.edad;\n"
                        + ".exit\n"
        );

        assertTrue(result.contains("p.nombre"));
        assertTrue(result.contains("p.edad"));
        assertTrue(result.contains("Ana"));
        assertTrue(result.contains("28"));
    }

    @Test
    void shouldProcessMultilineStatementWithSemicolon() {
        String result = runSession(
                "MATCH (p:Persona)\n"
                        + "RETURN p.nombre, p.edad;\n"
                        + ".exit\n"
        );

        assertTrue(result.contains("p.nombre"));
        assertTrue(result.contains("p.edad"));
        assertTrue(result.contains("Ana"));
        assertTrue(result.contains("28"));
    }

    @Test
    void shouldNotExecuteStatementBeforeSemicolon() {
        String result = runSession(
                "MATCH (p:Persona) RETURN p.nombre, p.edad\n"
        );

        assertTrue(
                result.contains("Incomplete Cyphail statement:")
        );

        assertFalse(
                result.contains("OK. Query available after")
        );
    }

    @Test
    void shouldProcessMultipleStatementsOnOneLine() {
        String result = runSession(
                "MATCH (p:Persona) RETURN p.nombre, p.edad;"
                        + " MATCH (p:Persona) RETURN p.nombre, p.edad;\n"
                        + ".exit\n"
        );

        assertEquals(
                2,
                countOccurrences(
                        result,
                        "OK. Query available after"
                )
        );
    }

    @Test
    void shouldShowContinuationPromptForMultilineQuery() {
        String result = runSession(
                "MATCH (p:Persona)\n"
                        + "RETURN p.nombre, p.edad;\n"
                        + ".exit\n"
        );

        assertTrue(result.contains(">>> "));
        assertTrue(result.contains("... "));
    }

    @Test
    void shouldKeepHelpCommandAvailable() {
        String result = runSession(
                ".help\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains("Cyphail REPL commands:")
        );

        assertTrue(result.contains(".help"));
        assertTrue(result.contains(".exit"));

        assertFalse(
                result.contains("Incomplete Cyphail statement:")
        );
    }

    @Test
    void shouldPreserveInlineTreeCommand() {
        String result = runSession(
                ".tree MATCH (m:Movie) RETURN m.title\n"
                        + ".exit\n"
        );

        assertTrue(result.contains("Query{"));
        assertTrue(result.contains("Match:{"));

        assertFalse(result.contains("Syntax error:"));
    }

    @Test
    void shouldPreserveMultilineTreeCommand() {
        String result = runSession(
                ".tree\n"
                        + "MATCH (m:Movie)\n"
                        + "RETURN m.title\n"
                        + ".exit\n"
        );

        assertTrue(result.contains("Query{"));
        assertTrue(result.contains("Match:{"));
        assertTrue(result.contains("... "));

        assertFalse(result.contains("Syntax error:"));
    }

    @Test
    void shouldAllowExitWhileStatementIsPending() {
        String result = runSession(
                "MATCH (p:Persona)\n"
                        + ".exit\n"
        );

        assertTrue(result.contains("... "));

        assertFalse(
                result.contains("Incomplete Cyphail statement:")
        );

        assertFalse(
                result.contains("OK. Query available after")
        );
    }

    /**
     * Runs a complete REPL session using simulated console input.
     *
     * @param input commands and queries supplied to the REPL
     * @return captured console output
     */
    private String runSession(String input) {
        System.setIn(
                new ByteArrayInputStream(
                        input.getBytes(StandardCharsets.UTF_8)
                )
        );

        new Repl().start();

        return output.toString(StandardCharsets.UTF_8);
    }

    /**
     * Counts occurrences of a given text in the REPL output.
     *
     * @param text full output
     * @param value text to count
     * @return number of occurrences
     */
    private int countOccurrences(
            String text,
            String value
    ) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(value, index)) != -1) {
            count++;
            index += value.length();
        }

        return count;
    }
}