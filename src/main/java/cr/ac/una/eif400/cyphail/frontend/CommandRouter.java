package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;
import cr.ac.una.eif400.cyphail.frontend.handler.AboutCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.CommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.ExitCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.HelpCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.LoadCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.PrologQueryHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.TreeCommandHandler;
import cr.ac.una.eif400.cyphail.frontend.handler.UseCommandHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * National University of Costa Rica
 * School of Informatics
 *
 * Course: EIF400 - Programming Paradigms
 * Project: Cyphail
 * Sprint: P2
 * Group Code: 04-10am
 *
 * Class: CommandRouter
 *
 * Description:
 *
 * Routes REPL input to the appropriate Cyphail
 * command handler.
 *
 * Sprint P2 supports:
 *
 * 1. Existing REPL commands.
 * 2. Multiline Cyphail statements.
 * 3. Loading .cyphail source files.
 * 4. AST visualization through .tree.
 * 5. Real query execution through SWI-Prolog/MQI.
 * 6. Persistent graph state during a REPL session.
 *
 * Cyphail queries are executed through
 * PrologQueryHandler using an externally managed
 * MQI broker.
 *
 * The fake query engine from Sprint P1 is no
 * longer registered or used by this router.
 *
 * The MQI broker lifecycle is managed by the
 * component that starts the REPL session.
 *
 * Team Members:
 *
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

    // ============================================
    // COMMAND HANDLERS
    // ============================================

    /**
     * Registered handlers used to dispatch
     * REPL commands and Cyphail statements.
     */
    private final List<CommandHandler> handlers;

    /**
     * Dedicated handler for AST visualization.
     *
     * The REPL checks its state to determine
     * whether a multiline .tree command
     * is still being entered.
     */
    private final TreeCommandHandler treeHandler;

    // ============================================
    // PRODUCTION CONSTRUCTOR
    // ============================================

    /**
     * Creates the production command router.
     *
     * Registers all supported REPL commands
     * and connects Cyphail query execution
     * to the real SWI-Prolog engine through MQI.
     *
     * The broker is shared throughout the
     * REPL session to preserve graph state.
     *
     * The router does not start or close
     * the MQI broker.
     *
     * @param broker active MQI broker used
     *               for executing Prolog queries
     * @throws NullPointerException if broker is null
     */
    public CommandRouter(MqiBroker broker) {

        Objects.requireNonNull(
                broker,
                "MQI broker cannot be null"
        );

        handlers = new ArrayList<>();

        treeHandler = new TreeCommandHandler();

        // ----------------------------------------
        // INFORMATION AND GRAPH COMMANDS
        // ----------------------------------------

        handlers.add(
                new HelpCommandHandler()
        );

        handlers.add(
                new AboutCommandHandler()
        );

        handlers.add(
                new UseCommandHandler()
        );

        // ----------------------------------------
        // ABSTRACT SYNTAX TREE VISUALIZATION
        // ----------------------------------------

        handlers.add(
                treeHandler
        );

        // ----------------------------------------
        // CYPHAIL SCRIPT LOADING
        // ----------------------------------------

        /*
         * The .load command dispatches completed
         * statements through this same router.
         *
         * Interactive and file-based queries
         * therefore share the same execution path.
         */

        handlers.add(
                new LoadCommandHandler(this::route)
        );

        // ----------------------------------------
        // REAL SWI-PROLOG QUERY EXECUTION
        // ----------------------------------------

        /*
         * Sprint P2 uses PrologQueryHandler
         * instead of the simulated P1 engine.
         *
         * Queries are parsed, validated,
         * transpiled into Prolog goals and
         * executed through the MQI broker.
         *
         * The broker remains active during
         * the REPL session.
         */

        handlers.add(
                new PrologQueryHandler(broker)
        );

        // ----------------------------------------
        // REPL TERMINATION
        // ----------------------------------------

        handlers.add(
                new ExitCommandHandler()
        );
    }

    // ============================================
    // COMMAND DISPATCHING
    // ============================================

    /**
     * Routes input to the first registered
     * handler that supports it.
     *
     * Cyphail statements are processed by
     * PrologQueryHandler.
     *
     * REPL commands are processed by their
     * corresponding command handlers.
     *
     * @param input REPL command or completed
     *              Cyphail statement
     * @return true if the REPL should continue
     *         running; false if it should terminate
     * @throws NullPointerException if input is null
     */
    public boolean route(String input) {

        Objects.requireNonNull(
                input,
                "REPL input cannot be null"
        );

        for (CommandHandler handler : handlers) {

            if (handler.supports(input)) {

                return handler.handle(input);
            }
        }

        System.out.println(
                "Command or query not implemented yet: "
                        + input
        );

        System.out.println();

        return true;
    }

    // ============================================
    // MULTILINE TREE COMMAND SUPPORT
    // ============================================

    /**
     * Checks whether the AST tree handler
     * is waiting for additional source lines.
     *
     * The REPL uses this state to preserve
     * multiline .tree functionality while
     * processing semicolon-terminated
     * Cyphail statements.
     *
     * @return true if the tree handler is
     *         waiting for additional input;
     *         false otherwise
     */
    public boolean isAwaitingTreeQuery() {

        return treeHandler.isAwaitingQuery();
    }
}