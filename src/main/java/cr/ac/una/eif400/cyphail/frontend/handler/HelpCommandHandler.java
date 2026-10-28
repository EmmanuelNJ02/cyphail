
package cr.ac.una.eif400.cyphail.frontend.handler;

/**
 * Handles the .help command in the Cyphail REPL.
 *
 * Displays the available commands and explains the
 * main features introduced in Cyphail Sprint P2.
 *
 * Sprint P2 preserves the existing Sprint P1 commands
 * and extends the REPL with multiline statements,
 * script execution, relationship patterns, graph
 * updates, and real SWI-Prolog/MQI query execution.
 *
 * Project: Cyphail - Sprint P2
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
     * Checks whether the input corresponds to
     * the .help command.
     *
     * @param input command entered in the REPL
     * @return true when the input is .help
     */
    @Override
    public boolean supports(String input) {
        return COMMAND.equalsIgnoreCase(input.trim());
    }

    /**
     * Displays the available REPL commands and
     * the main Cyphail Sprint P2 features.
     *
     * The original Sprint P1 heading is preserved
     * for backward compatibility.
     *
     * @param input command entered in the REPL
     * @return true because the REPL should continue running
     */
    @Override
    public boolean handle(String input) {

        // Preserve the original heading for existing tests.
        System.out.println("Cyphail REPL commands:");
        System.out.println();

        System.out.println(
                "Sprint P2 - SWI-Prolog/MQI Execution"
        );

        System.out.println();

        System.out.println(
                ".help                 Show available commands"
        );

        System.out.println(
                ".about                Show project and author information"
        );

        System.out.println(
                ".use                  Show graphs from the catalog"
        );

        System.out.println(
                ".use <graph>          Check graph availability in the catalog"
        );

        System.out.println(
                ".tree                 Start multiline AST query mode"
        );

        System.out.println(
                ".tree <query>         Parse a query and display its AST"
        );

        System.out.println(
                ".load \"path/to/script.cyphail\""
                        + "   Execute a Cyphail script"
        );

        System.out.println(
                ".exit                 Exit the Cyphail REPL"
        );

        System.out.println();

        System.out.println("Cyphail Query Execution:");
        System.out.println();

        System.out.println(
                "- Enter Cyphail statements directly at the REPL prompt."
        );

        System.out.println(
                "- Statements may span multiple lines."
        );

        System.out.println(
                "- End each executable statement with a semicolon (;)."
        );

        System.out.println(
                "- Use .load to execute statements from a .cyphail file."
        );

        System.out.println(
                "- Use .tree to inspect the AST without executing a query."
        );

        System.out.println();

        System.out.println("Supported Sprint P2 Features:");
        System.out.println();

        System.out.println(
                "- MATCH and WHERE queries."
        );

        System.out.println(
                "- Node and relationship patterns."
        );

        System.out.println(
                "- CREATE nodes and relationships."
        );

        System.out.println(
                "- SET and REMOVE properties."
        );

        System.out.println(
                "- DELETE and DETACH DELETE."
        );

        System.out.println(
                "- RETURN query results in tabular format."
        );

        System.out.println(
                "- Real query execution using SWI-Prolog and MQI."
        );

        System.out.println();

        System.out.println(
                "Note: .use checks the legacy graph catalog."
        );

        System.out.println(
                "It does not switch the active Prolog engine graph."
        );

        System.out.println();

        return true;
    }
}
