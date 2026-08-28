package cr.ac.una.eif400.cyphail.frontend.handler;

/**
 * Common contract for Cyphail REPL command handlers.
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
public interface CommandHandler {

    /**
     * Determines whether this handler can process the given input.
     *
     * @param input command entered in the REPL
     * @return true if this handler supports the command
     */
    boolean supports(String input);

    /**
     * Processes the command.
     *
     * @param input command entered in the REPL
     * @return true if the REPL should continue running;
     *         false if it should terminate
     */
    boolean handle(String input);
}