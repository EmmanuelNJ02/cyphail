package cr.ac.una.eif400.cyphail.starter;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Cyphail command-line interface.
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
class CyphailCliIntegrationTest {

    @Test
    void shouldDisplayMainHelpFromCommandLine() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        CommandLine commandLine = new CommandLine(new CyphailApplication());
        commandLine.setOut(new PrintWriter(output, true));

        int exitCode = commandLine.execute("--help");
        String result = output.toString();

        assertEquals(0, exitCode);
        assertTrue(result.contains("Usage: cyphail"));
        assertTrue(result.contains("repl"));
    }

    @Test
    void shouldDisplayApplicationVersionFromCommandLine() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        CommandLine commandLine = new CommandLine(new CyphailApplication());
        commandLine.setOut(new PrintWriter(output, true));

        int exitCode = commandLine.execute("--version");
        String result = output.toString();

        assertEquals(0, exitCode);
        assertTrue(result.contains("Cyphail 0.1.0"));
    }

    @Test
    void shouldExposeReplAsAvailableSubcommand() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        CommandLine commandLine = new CommandLine(new CyphailApplication());
        commandLine.setOut(new PrintWriter(output, true));

        int exitCode = commandLine.execute("--help");
        String result = output.toString();

        assertEquals(0, exitCode);
        assertTrue(result.contains("repl"));
    }
}