package cr.ac.una.eif400.cyphail.frontend.handler;

/**
 * Handles the .exit command in the Cyphail REPL.
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
public class ExitCommandHandler implements CommandHandler {

    private static final String COMMAND = ".exit";

    /**
     * Checks whether the input corresponds to the .exit command.
     *
     * @param input command entered in the REPL
     * @return true when the command is .exit
     */
    @Override
    public boolean supports(String input) {
        return COMMAND.equalsIgnoreCase(input.trim());
    }

    /**
     * Stops the REPL.
     *
     * @param input command entered in the REPL
     * @return false because the REPL must terminate
     */
    @Override
    public boolean handle(String input) {
        return false;
    }
}