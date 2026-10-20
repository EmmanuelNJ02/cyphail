package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.frontend.handler.AboutCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.CommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.ExitCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.FakeQueryHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.HelpCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.TreeCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.UseCommandHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes REPL input to the appropriate Cyphail command handler.
 *
 * Supports the existing commands and exposes the current
 * AST capture state for Sprint P2 multiline processing.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * School: Escuela de Informatica
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
public final class CommandRouter {

    private final List<CommandHandler> handlers;

    private final TreeCommandHandler treeHandler;

    /**
     * Creates the router and registers the REPL commands.
     *
     * The tree handler is retained so the REPL can check
     * whether an AST query is currently being captured.
     */
    public CommandRouter() {
        handlers = new ArrayList<>();

        treeHandler = new TreeCommandHandler();

        handlers.add(new HelpCommandHandler());
        handlers.add(new AboutCommandHandler());
        handlers.add(new UseCommandHandler());
        handlers.add(treeHandler);

        // Temporary P1 execution handler.
        // It will be replaced during the final P2
        // integration with the real Prolog engine.
        handlers.add(new FakeQueryHandler());

        handlers.add(new ExitCommandHandler());
    }

    /**
     * Routes input to the first supporting handler.
     *
     * @param input user input
     * @return true if the REPL should continue running
     */
    public boolean route(String input) {
        for (CommandHandler handler : handlers) {
            if (handler.supports(input)) {
                return handler.handle(input);
            }
        }

        System.out.println(
                "Command or query not implemented yet: " + input
        );
        System.out.println();

        return true;
    }

    /**
     * Checks whether the AST tree command is waiting
     * for additional source lines.
     *
     * The Sprint P2 REPL uses this state to preserve
     * existing multiline .tree behavior while handling
     * semicolon-terminated Cyphail statements.
     *
     * @return true while .tree is capturing a query
     */
    public boolean isAwaitingTreeQuery() {
        return treeHandler.isAwaitingQuery();
    }
}