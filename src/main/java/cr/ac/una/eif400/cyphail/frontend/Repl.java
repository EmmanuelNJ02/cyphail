package cr.ac.una.eif400.cyphail.frontend;

import java.util.Scanner;

/**
 * Interactive Read-Eval-Print-Loop for Cyphail.
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
public class Repl {

    private static final String PROMPT = ">>> ";

    private final CommandRouter commandRouter;

    /**
     * Creates a Cyphail REPL with its command router.
     */
    public Repl() {
        this.commandRouter = new CommandRouter();
    }

    /**
     * Starts the interactive Cyphail REPL.
     */
    public void start() {
        printWelcome();

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;

            while (running) {
                System.out.print(PROMPT);

                if (!scanner.hasNextLine()) {
                    break;
                }

                String input = scanner.nextLine().trim();

                if (input.isEmpty()) {
                    continue;
                }

                running = commandRouter.route(input);
            }
        }
    }

    /**
     * Prints the official identification message for Sprint P1.1.
     */
    private void printWelcome() {
        System.out.println(
                "Welcome to Cyphail-04-10am v.0.1. August 2026. "
                        + "ESCINF/UNA EIF400-II-2026"
        );
        System.out.println();
        System.out.println(
                "Visit www.whatiscyphail.com for more information"
        );
        System.out.println(
                "Type \".help\" for more information and commands"
        );
        System.out.println();
        System.out.println(
                "Type \".exit\" to quit"
        );
        System.out.println();
    }
}