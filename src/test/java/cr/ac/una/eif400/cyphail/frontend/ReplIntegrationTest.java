package cr.ac.una.eif400.cyphail.frontend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for complete Cyphail REPL sessions.
 *
 * Project: Cyphail
 * Course: EIF400 - Paradigmas de Programación
 * University: Universidad Nacional de Costa Rica
 * School: Escuela de Informática
 * Work Group: 04
 * Schedule: 10:00 a.m.
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Núñez Jiménez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 */
class ReplIntegrationTest {

    private InputStream originalIn;
    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        originalIn = System.in;
        originalOut = System.out;

        output = new ByteArrayOutputStream();

        System.setOut(
                new PrintStream(output, true, StandardCharsets.UTF_8)
        );
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @Test
    void shouldStartReplAndProcessHelpCommand() {
        setInput(".help\n.exit\n");

        new Repl().start();

        String result = output.toString(StandardCharsets.UTF_8);

        assertTrue(result.contains("Welcome to Cyphail-04-10am v.0.1"));
        assertTrue(result.contains(">>> "));
        assertTrue(result.contains(".help"));
        assertTrue(result.contains(".about"));
        assertTrue(result.contains(".use"));
        assertTrue(result.contains(".exit"));
    }

    @Test
    void shouldIgnoreEmptyInputAndPromptAgain() {
        setInput("\n.exit\n");

        new Repl().start();

        String result = output.toString(StandardCharsets.UTF_8);

        assertTrue(countOccurrences(result, ">>> ") >= 2);
    }

    @Test
    void shouldProcessFakeMatchQueryInsideReplSession() {
        setInput(
                "MATCH (p:Persona) RETURN p.nombre, p.edad\n"
                        + ".exit\n"
        );

        new Repl().start();

        String result = output.toString(StandardCharsets.UTF_8);

        assertTrue(result.contains("p.nombre"));
        assertTrue(result.contains("p.edad"));
        assertTrue(result.contains("Ana"));
        assertTrue(result.contains("28"));
        assertTrue(result.contains("OK. Query available after 42 ms."));
    }

    private void setInput(String commands) {
        System.setIn(
                new ByteArrayInputStream(
                        commands.getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    private int countOccurrences(String text, String value) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(value, index)) != -1) {
            count++;
            index += value.length();
        }

        return count;
    }
}