package cr.ac.una.eif400.cyphail.frontend.handler;

/**
 * Handles the .help command in the Cyphail REPL.
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
public class HelpCommandHandler implements CommandHandler {

    private static final String COMMAND = ".help";

    /**
     * Checks whether the input corresponds to the .help command.
     *
     * @param input command entered in the REPL
     * @return true when the command is .help
     */
    @Override
    public boolean supports(String input) {
        return COMMAND.equalsIgnoreCase(input.trim());
    }

    /**
     * Displays the available REPL commands.
     *
     * @param input command entered in the REPL
     * @return true because the REPL should continue running
     */
    @Override
    public boolean handle(String input) {
        System.out.println("Cyphail REPL commands:");
        System.out.println();
        System.out.println(".help             Show available commands");
        System.out.println(".about            Show project and author information");
        System.out.println(".use              Show available graphs");
        System.out.println(".use <graph>      Select an available graph");
        System.out.println(".exit             Exit the Cyphail REPL");
        System.out.println();
        System.out.println(
                "Cyphail queries can also be entered directly at the prompt."
        );
        System.out.println();

        return true;
    }
}