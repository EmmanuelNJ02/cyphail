package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.frontend.handler.AboutCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.CommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.ExitCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.FakeQueryHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.HelpCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.LoadCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.TreeCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.UseCommandHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes REPL input to the appropriate Cyphail command handler.
 *
 * Sprint P2 supports multiline statements and loading Cyphail
 * source files while preserving existing REPL commands.
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
     * The .load command dispatches completed statements
     * through this same router, ensuring that interactive
     * and file-based queries share the execution path.
     */
    public CommandRouter() {
        handlers = new ArrayList<>();

        treeHandler = new TreeCommandHandler();

        handlers.add(new HelpCommandHandler());
        handlers.add(new AboutCommandHandler());
        handlers.add(new UseCommandHandler());
        handlers.add(treeHandler);

        // Sprint P2: load and dispatch statements from files.
        handlers.add(
                new LoadCommandHandler(this::route)
        );

        // Temporary Sprint P1 query handler.
        // Real Prolog execution will replace this path
        // during the final Sprint P2 integration.
        handlers.add(new FakeQueryHandler());

        handlers.add(new ExitCommandHandler());
    }

    /**
     * Routes input to the first supporting handler.
     *
     * @param input command or completed Cyphail statement
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
     * The REPL uses this state to preserve existing
     * multiline .tree behavior while processing
     * semicolon-terminated Cyphail statements.
     *
     * @return true while .tree is capturing a query
     */
    public boolean isAwaitingTreeQuery() {
        return treeHandler.isAwaitingQuery();
    }
}