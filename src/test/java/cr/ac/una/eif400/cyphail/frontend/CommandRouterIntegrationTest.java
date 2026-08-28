package cr.ac.una.eif400.cyphail.frontend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Cyphail command router and its real handlers.
 *
 * Project: Cyphail
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * School: Escuela de Informatica
 * Work Group: 04
 * Schedule: 10:00 a.m.
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
class CommandRouterIntegrationTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOutput;
    private CommandRouter router;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        capturedOutput = new ByteArrayOutputStream();

        System.setOut(
                new PrintStream(
                        capturedOutput,
                        true,
                        StandardCharsets.UTF_8
                )
        );

        router = new CommandRouter();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    /**
     * Verifies that the router dispatches .help to the help handler.
     */
    @Test
    void shouldRouteHelpCommand() {
        boolean shouldContinue = router.route(".help");

        String output =
                capturedOutput.toString(StandardCharsets.UTF_8);

        assertTrue(shouldContinue);
        assertTrue(output.contains("Cyphail REPL commands:"));
        assertTrue(output.contains(".help"));
        assertTrue(output.contains(".use <graph>"));
        assertTrue(output.contains(".exit"));
    }

    /**
     * Verifies that the router dispatches .about to the about handler.
     */
    @Test
    void shouldRouteAboutCommand() {
        boolean shouldContinue = router.route(".about");

        String output =
                capturedOutput.toString(StandardCharsets.UTF_8);

        assertTrue(shouldContinue);
        assertTrue(output.contains("Cyphail"));
        assertTrue(output.contains("Work Group: 04"));
        assertTrue(output.contains("Group Code: 04-10am"));
    }

    /**
     * Verifies integration between the router, .use handler and graph catalog.
     */
    @Test
    void shouldRouteUseCommandThroughGraphCatalog() {
        boolean shouldContinue = router.route(".use");

        String output =
                capturedOutput.toString(StandardCharsets.UTF_8);

        assertTrue(shouldContinue);
        assertTrue(output.contains("Graph"));
        assertTrue(output.contains("Description"));
        assertTrue(output.contains("amigos"));
        assertTrue(output.contains("planets"));
    }

    /**
     * Verifies integration between the router, query handler and fake engine.
     */
    @Test
    void shouldRouteFakeMatchQueryThroughEngine() {
        boolean shouldContinue = router.route(
                "MATCH (p:Persona) RETURN p.nombre, p.edad"
        );

        String output =
                capturedOutput.toString(StandardCharsets.UTF_8);

        assertTrue(shouldContinue);
        assertTrue(output.contains("p.nombre"));
        assertTrue(output.contains("p.edad"));
        assertTrue(output.contains("\"Ana\""));
        assertTrue(output.contains("42 ms."));
    }

    /**
     * Verifies that routing .exit requests termination of the REPL.
     */
    @Test
    void shouldStopWhenExitCommandIsRouted() {
        boolean shouldContinue = router.route(".exit");

        assertFalse(shouldContinue);
    }
}