package cr.ac.una.eif400.cyphail.frontend.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Sprint P2 .load command.
 *
 * Verifies file loading, multiline queries, statement
 * ordering, quoted strings, missing files, malformed
 * commands and incomplete statements.
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
class LoadCommandHandlerTest {

    @TempDir
    Path temporaryDirectory;

    private PrintStream originalOut;
    private ByteArrayOutputStream output;
    private List<String> dispatchedStatements;
    private LoadCommandHandler handler;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        output = new ByteArrayOutputStream();

        System.setOut(
                new PrintStream(
                        output,
                        true,
                        StandardCharsets.UTF_8
                )
        );

        dispatchedStatements = new ArrayList<>();

        handler = new LoadCommandHandler(statement -> {
            dispatchedStatements.add(statement);
            return true;
        });
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void shouldRecognizeLoadCommand() {
        assertTrue(handler.supports(".load \"script.cyphail\""));
        assertTrue(handler.supports(".LOAD \"script.cyphail\""));
        assertTrue(handler.supports(".load"));

        assertFalse(handler.supports(".help"));
        assertFalse(handler.supports(".tree"));
        assertFalse(handler.supports(".loader"));
    }

    @Test
    void shouldLoadSingleCyphailStatement() throws IOException {
        Path file = createScript(
                "single.cyphail",
                "MATCH (p:Persona) RETURN p.nombre;"
        );

        assertTrue(handler.handle(command(file)));

        assertEquals(
                List.of("MATCH (p:Persona) RETURN p.nombre"),
                dispatchedStatements
        );

        assertTrue(
                consoleOutput().contains(
                        "File processed:"
                )
        );

        assertTrue(
                consoleOutput().contains(
                        "(1 statement(s) dispatched)"
                )
        );
    }

    @Test
    void shouldLoadMultilineCyphailStatement() throws IOException {
        Path file = createScript(
                "multiline.cyphail",
                "MATCH (p:Persona)\n"
                        + "WHERE p.edad > 18\n"
                        + "RETURN p.nombre;"
        );

        assertTrue(handler.handle(command(file)));

        assertEquals(1, dispatchedStatements.size());

        assertEquals(
                "MATCH (p:Persona)\n"
                        + "WHERE p.edad > 18\n"
                        + "RETURN p.nombre",
                dispatchedStatements.get(0)
        );
    }

    @Test
    void shouldDispatchMultipleStatementsInOrder()
            throws IOException {

        Path file = createScript(
                "multiple.cyphail",
                "MATCH (a:Persona) RETURN a.nombre;\n"
                        + "MATCH (b:Persona) RETURN b.edad;"
        );

        assertTrue(handler.handle(command(file)));

        assertEquals(
                List.of(
                        "MATCH (a:Persona) RETURN a.nombre",
                        "MATCH (b:Persona) RETURN b.edad"
                ),
                dispatchedStatements
        );

        assertTrue(
                consoleOutput().contains(
                        "(2 statement(s) dispatched)"
                )
        );
    }

    @Test
    void shouldHandleMultipleStatementsOnSameLine()
            throws IOException {

        Path file = createScript(
                "same-line.cyphail",
                "MATCH (a) RETURN a; MATCH (b) RETURN b;"
        );

        assertTrue(handler.handle(command(file)));

        assertEquals(
                List.of(
                        "MATCH (a) RETURN a",
                        "MATCH (b) RETURN b"
                ),
                dispatchedStatements
        );
    }

    @Test
    void shouldIgnoreSemicolonsInsideQuotedStrings()
            throws IOException {

        Path file = createScript(
                "strings.cyphail",
                "MATCH (p) WHERE p.nombre = \"Ana;Maria\" "
                        + "RETURN p.nombre;"
        );

        assertTrue(handler.handle(command(file)));

        assertEquals(1, dispatchedStatements.size());

        assertEquals(
                "MATCH (p) WHERE p.nombre = \"Ana;Maria\" "
                        + "RETURN p.nombre",
                dispatchedStatements.get(0)
        );
    }

    @Test
    void shouldHandlePathContainingSpaces()
            throws IOException {

        Path file = createScript(
                "my script.cyphail",
                "MATCH (p) RETURN p;"
        );

        assertTrue(handler.handle(command(file)));

        assertEquals(
                List.of("MATCH (p) RETURN p"),
                dispatchedStatements
        );
    }

    @Test
    void shouldReportMissingSourceFile() {
        Path missingFile = temporaryDirectory.resolve(
                "missing.cyphail"
        );

        assertTrue(
                handler.handle(command(missingFile))
        );

        assertTrue(dispatchedStatements.isEmpty());

        assertTrue(
                consoleOutput().contains(
                        "Cyphail source file not found:"
                )
        );
    }

    @Test
    void shouldRejectLoadCommandWithoutQuotedPath() {
        assertTrue(
                handler.handle(".load script.cyphail")
        );

        assertTrue(dispatchedStatements.isEmpty());

        assertTrue(
                consoleOutput().contains(
                        "Usage: .load"
                )
        );
    }

    @Test
    void shouldReportIncompleteStatement()
            throws IOException {

        Path file = createScript(
                "incomplete.cyphail",
                "MATCH (p:Persona)\n"
                        + "RETURN p.nombre"
        );

        assertTrue(handler.handle(command(file)));

        assertTrue(dispatchedStatements.isEmpty());

        assertTrue(
                consoleOutput().contains(
                        "Incomplete Cyphail statement in file:"
                )
        );
    }

    @Test
    void shouldAllowEmptySourceFile()
            throws IOException {

        Path file = createScript(
                "empty.cyphail",
                ""
        );

        assertTrue(handler.handle(command(file)));

        assertTrue(dispatchedStatements.isEmpty());

        assertTrue(
                consoleOutput().contains(
                        "(0 statement(s) dispatched)"
                )
        );
    }

    @Test
    void shouldStopDispatchingWhenRequested()
            throws IOException {

        Path file = createScript(
                "stop.cyphail",
                "MATCH (a) RETURN a;\n"
                        + "MATCH (b) RETURN b;"
        );

        LoadCommandHandler stoppingHandler =
                new LoadCommandHandler(statement -> {
                    dispatchedStatements.add(statement);
                    return false;
                });

        assertFalse(
                stoppingHandler.handle(command(file))
        );

        assertEquals(
                List.of("MATCH (a) RETURN a"),
                dispatchedStatements
        );

        assertFalse(
                consoleOutput().contains(
                        "File processed:"
                )
        );
    }

    /**
     * Creates a temporary Cyphail source file.
     *
     * @param filename file name
     * @param source source code
     * @return path to the created file
     * @throws IOException when writing fails
     */
    private Path createScript(
            String filename,
            String source
    ) throws IOException {

        Path file = temporaryDirectory.resolve(filename);

        Files.writeString(
                file,
                source,
                StandardCharsets.UTF_8
        );

        return file;
    }

    /**
     * Builds a valid .load command for a file.
     *
     * @param file source file
     * @return quoted .load command
     */
    private String command(Path file) {
        return ".load \"" + file + "\"";
    }

    /**
     * Returns the captured console output.
     *
     * @return console output as a string
     */
    private String consoleOutput() {
        return output.toString(StandardCharsets.UTF_8);
    }
}