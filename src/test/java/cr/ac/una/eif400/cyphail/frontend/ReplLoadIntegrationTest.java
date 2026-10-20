package cr.ac.una.eif400.cyphail.frontend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Sprint P2 .load REPL command.
 *
 * Verifies that the complete REPL can load Cyphail
 * source files, dispatch semicolon-terminated statements
 * and recover from file-related errors.
 *
 * The existing Sprint P1 query handler is used
 * temporarily to verify statement dispatch.
 * Real Prolog execution remains part of the
 * final Sprint P2 integration.
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
class ReplLoadIntegrationTest {

    private static final String SAMPLE_QUERY =
            "MATCH (p:Persona) RETURN p.nombre, p.edad;";

    @TempDir
    Path temporaryDirectory;

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
    void shouldLoadAndProcessSingleStatement()
            throws IOException {

        Path file = createScript(
                "single.cyphail",
                SAMPLE_QUERY
        );

        String result = runSession(
                loadCommand(file) + "\n.exit\n"
        );

        assertTrue(result.contains("p.nombre"));
        assertTrue(result.contains("p.edad"));
        assertTrue(result.contains("Ana"));
        assertTrue(result.contains("28"));

        assertTrue(
                result.contains(
                        "(1 statement(s) dispatched)"
                )
        );
    }

    @Test
    void shouldLoadMultilineStatement()
            throws IOException {

        Path file = createScript(
                "multiline.cyphail",
                "MATCH (p:Persona)\n"
                        + "RETURN p.nombre, p.edad;\n"
        );

        String result = runSession(
                loadCommand(file) + "\n.exit\n"
        );

        assertTrue(result.contains("Ana"));
        assertTrue(result.contains("28"));

        assertTrue(
                result.contains(
                        "(1 statement(s) dispatched)"
                )
        );

        assertFalse(
                result.contains(
                        "Incomplete Cyphail statement in file:"
                )
        );
    }

    @Test
    void shouldProcessTwoStatementsFromFile()
            throws IOException {

        Path file = createScript(
                "multiple.cyphail",
                SAMPLE_QUERY + "\n"
                        + SAMPLE_QUERY + "\n"
        );

        String result = runSession(
                loadCommand(file) + "\n.exit\n"
        );

        assertEquals(
                2,
                countOccurrences(
                        result,
                        "OK. Query available after"
                )
        );

        assertTrue(
                result.contains(
                        "(2 statement(s) dispatched)"
                )
        );
    }

    @Test
    void shouldAcceptFilePathWithSpaces()
            throws IOException {

        Path file = createScript(
                "example script.cyphail",
                SAMPLE_QUERY
        );

        String result = runSession(
                loadCommand(file) + "\n.exit\n"
        );

        assertTrue(result.contains("Ana"));

        assertTrue(
                result.contains("File processed:")
        );
    }

    @Test
    void shouldReportMissingFileAndKeepReplRunning() {
        Path missingFile = temporaryDirectory.resolve(
                "missing.cyphail"
        );

        String result = runSession(
                loadCommand(missingFile)
                        + "\n.help\n.exit\n"
        );

        assertTrue(
                result.contains(
                        "Cyphail source file not found:"
                )
        );

        assertTrue(
                result.contains(
                        "Cyphail REPL commands:"
                )
        );
    }

    @Test
    void shouldRejectUnquotedPathAndKeepReplRunning() {
        String result = runSession(
                ".load example.cyphail\n"
                        + ".help\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains(
                        "Usage: .load"
                )
        );

        assertTrue(
                result.contains(
                        "Cyphail REPL commands:"
                )
        );
    }

    @Test
    void shouldReportIncompleteStatementInLoadedFile()
            throws IOException {

        Path file = createScript(
                "incomplete.cyphail",
                "MATCH (p:Persona)\n"
                        + "RETURN p.nombre, p.edad\n"
        );

        String result = runSession(
                loadCommand(file) + "\n.exit\n"
        );

        assertTrue(
                result.contains(
                        "Incomplete Cyphail statement in file:"
                )
        );

        assertFalse(
                result.contains(
                        "OK. Query available after"
                )
        );
    }

    /**
     * Creates a temporary UTF-8 Cyphail source file.
     *
     * @param filename source file name
     * @param contents file contents
     * @return created file path
     * @throws IOException when writing the file fails
     */
    private Path createScript(
            String filename,
            String contents
    ) throws IOException {

        Path file = temporaryDirectory.resolve(filename);

        Files.writeString(
                file,
                contents,
                StandardCharsets.UTF_8
        );

        return file;
    }

    /**
     * Builds a valid REPL .load command.
     *
     * @param file source file path
     * @return command with a quoted path
     */
    private String loadCommand(Path file) {
        return ".load \"" + file + "\"";
    }

    /**
     * Starts a complete REPL session with simulated input.
     *
     * @param commands input lines
     * @return captured console output
     */
    private String runSession(String commands) {
        System.setIn(
                new ByteArrayInputStream(
                        commands.getBytes(
                                StandardCharsets.UTF_8
                        )
                )
        );

        new Repl().start();

        return output.toString(StandardCharsets.UTF_8);
    }

    /**
     * Counts occurrences of a text fragment.
     *
     * @param source full text
     * @param fragment searched fragment
     * @return number of occurrences
     */
    private int countOccurrences(
            String source,
            String fragment
    ) {
        int count = 0;
        int index = 0;

        while ((index = source.indexOf(fragment, index)) != -1) {
            count++;
            index += fragment.length();
        }

        return count;
    }
}