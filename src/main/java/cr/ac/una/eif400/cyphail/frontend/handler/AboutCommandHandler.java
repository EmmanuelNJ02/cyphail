package cr.ac.una.eif400.cyphail.frontend.handler;

/**
 * Handles the .about command in the Cyphail REPL.
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
public class AboutCommandHandler implements CommandHandler {

    private static final String COMMAND = ".about";

    /**
     * Checks whether the input corresponds to the .about command.
     *
     * @param input command entered in the REPL
     * @return true when the command is .about
     */
    @Override
    public boolean supports(String input) {
        return COMMAND.equalsIgnoreCase(input.trim());
    }

    /**
     * Displays project and author information.
     *
     * @param input command entered in the REPL
     * @return true because the REPL should continue running
     */
    @Override
    public boolean handle(String input) {
        System.out.println("Cyphail");
        System.out.println("Version: 0.1");
        System.out.println("Course: EIF400 - Paradigmas de Programacion");
        System.out.println("University: Universidad Nacional de Costa Rica");
        System.out.println("School: Escuela de Informatica");
        System.out.println("Work Group: 04");
        System.out.println("Schedule: 10:00 a.m.");
        System.out.println("Group Code: 04-10am");
        System.out.println();
        System.out.println("Authors:");
        System.out.println("- Emmanuel Nunez Jimenez");
        System.out.println("- Valery Alfaro Morales");
        System.out.println("- Roy Arias Mejia");
        System.out.println("- Keynell Molina Mora");
        System.out.println("- Julissa Solano Valverde");
        System.out.println();

        return true;
    }
}