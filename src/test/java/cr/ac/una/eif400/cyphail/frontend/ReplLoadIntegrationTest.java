
package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;

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
 * Integration tests for the Sprint P2 .load command.
 *
 * These tests verify that Cyphail reads source files,
 * processes semicolon-terminated statements and
 * executes queries using the real SWI-Prolog MQI engine.
 *
 * The previous simulated query results are no longer used.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * School: Escuela de Informatica
 * Group Code: 04-10am
 *
 * Team Members:
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
class ReplLoadIntegrationTest {

    private static final String SAMPLE_QUERY =
            "MATCH (p:Person) RETURN p.name, p.age;";

    @TempDir
    Path temporaryDirectory;

    private InputStream originalIn;

    private PrintStream originalOut;

    private PrintStream testOutput;

    private ByteArrayOutputStream output;

    /**
     * Prepares the console for the integration test.
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

    /**
     * Restores the console after each test.
     */
    @AfterEach
    void tearDown() {

        System.setIn(originalIn);
        System.setOut(originalOut);

        if (testOutput != null) {
            testOutput.close();
        }
    }

    /**
     * Test 1:
     * Loads and executes a single Cyphail statement.
     */
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

        assertTrue(result.contains("p.name"));
        assertTrue(result.contains("p.age"));

        assertTrue(result.contains("Juan Perez"));
        assertTrue(result.contains("Jorge Soto"));

        assertTrue(
                result.contains(
                        "(1 statement(s) dispatched)"
                )
        );

        assertFalse(result.contains("42 ms"));
    }

    /**
     * Test 2:
     * Loads a Cyphail statement written on
     * several lines.
     */
    @Test
    void shouldLoadMultilineStatement()
            throws IOException {

        Path file = createScript(
                "multiline.cyphail",
                """
                MATCH (p:Person)
                RETURN p.name, p.age;
                """
        );

        String result = runSession(
                loadCommand(file) + "\n.exit\n"
        );

        assertTrue(result.contains("p.name"));
        assertTrue(result.contains("p.age"));

        assertTrue(result.contains("Juan Perez"));
        assertTrue(result.contains("Jorge Soto"));

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

    /**
     * Test 3:
     * Executes two statements from the same file
     * using a persistent MQI session.
     */
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
                        "p.name p.age"
                )
        );

        assertTrue(
                result.contains(
                        "(2 statement(s) dispatched)"
                )
        );

        assertFalse(
                result.contains(
                        "MQI communication error:"
                )
        );
    }

    /**
     * Test 4:
     * Accepts a quoted file path containing spaces.
     */
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

        assertTrue(result.contains("Juan Perez"));

        assertTrue(
                result.contains("File processed:")
        );

        assertTrue(
                result.contains(
                        "(1 statement(s) dispatched)"
                )
        );
    }

    /**
     * Test 5:
     * Reports a missing file without stopping
     * the interactive REPL.
     */
    @Test
    void shouldReportMissingFileAndKeepReplRunning()
            throws IOException {

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

    /**
     * Test 6:
     * Rejects a file path without quotes
     * and keeps the REPL running.
     */
    @Test
    void shouldRejectUnquotedPathAndKeepReplRunning()
            throws IOException {

        String result = runSession(
                ".load example.cyphail\n"
                        + ".help\n"
                        + ".exit\n"
        );

        assertTrue(
                result.contains("Usage: .load")
        );

        assertTrue(
                result.contains(
                        "Cyphail REPL commands:"
                )
        );
    }

    /**
     * Test 7:
     * Reports incomplete source statements
     * instead of executing them.
     */
    @Test
    void shouldReportIncompleteStatementInLoadedFile()
            throws IOException {

        Path file = createScript(
                "incomplete.cyphail",
                """
                MATCH (p:Person)
                RETURN p.name, p.age
                """
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
                result.contains("p.name p.age")
        );
    }

    /**
     * Creates a temporary UTF-8 Cyphail script.
     *
     * @param filename source file name
     * @param contents source code
     * @return created script path
     * @throws IOException if file creation fails
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
     * Builds a valid .load command using a quoted path.
     *
     * @param file source script
     * @return complete .load command
     */
    private String loadCommand(Path file) {

        return ".load \"" + file + "\"";
    }

    /**
     * Starts the real Sprint P2 REPL.
     *
     * Creates an independent SWI-Prolog MQI session,
     * executes all supplied commands and closes
     * the session when processing finishes.
     *
     * @param commands input lines
     * @return captured REPL output
     * @throws IOException if MQI communication fails
     */
    private String runSession(String commands)
            throws IOException {

        System.setIn(
                new ByteArrayInputStream(
                        commands.getBytes(
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

    /**
     * Counts occurrences of a fragment.
     *
     * @param source complete output
     * @param fragment expected text
     * @return occurrence count
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
