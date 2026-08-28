package cr.ac.una.eif400.cyphail.starter;

import cr.ac.una.eif400.cyphail.frontend.ReplCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main entry point for the Cyphail command-line application.
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
@Command(
        name = "cyphail",
        description = "Cyphail graph query language prototype.",
        mixinStandardHelpOptions = true,
        version = "Cyphail 0.1.0",
        subcommands = {
                ReplCommand.class
        }
)
public class CyphailApplication implements Runnable {

    /**
     * Starts the Cyphail command-line application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new CyphailApplication()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Displays the general command-line usage when no specific
     * Cyphail command is provided.
     */
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}