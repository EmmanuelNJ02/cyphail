package cr.ac.una.eif400.cyphail.frontend;

import picocli.CommandLine.Command;

/**
 * Picocli command that starts the Cyphail interactive REPL.
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
        name = "repl",
        description = "Starts the interactive Cyphail REPL."
)
public class ReplCommand implements Runnable {

    /**
     * Starts the Cyphail REPL.
     */
    @Override
    public void run() {
        Repl repl = new Repl();
        repl.start();
    }
}