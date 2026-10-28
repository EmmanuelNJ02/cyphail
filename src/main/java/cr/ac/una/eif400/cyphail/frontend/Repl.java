
package cr.ac.una.eif400.cyphail.frontend;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * National University of Costa Rica
 * School of Informatics
 *
 * Course: EIF400 - Programming Paradigms
 * Project: Cyphail
 * Sprint: P2
 * Group Code: 04-10am
 * Date: November 2026
 *
 * Class: Repl
 *
 * Description:
 *
 * Implements the interactive Read-Eval-Print Loop
 * for the Cyphail graph query language.
 *
 * Sprint P2 supports:
 *
 * 1. Existing REPL commands.
 * 2. Multiline Cyphail statements.
 * 3. Semicolon-terminated queries.
 * 4. AST visualization through .tree.
 * 5. Loading .cyphail source files.
 * 6. Real query execution through SWI-Prolog/MQI.
 * 7. Persistent graph state during a REPL session.
 *
 * The REPL receives a preconfigured CommandRouter.
 * It does not create or manage its own MQI broker.
 *
 * ReplCommand starts the broker, creates the router,
 * supplies it to this REPL, and closes the broker
 * when the interactive session terminates.
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
public class Repl {

    // ============================================
    // REPL PROMPTS
    // ============================================

    private static final String PROMPT = ">>> ";

    private static final String CONTINUATION_PROMPT = "... ";

    // ============================================
    // REPL DEPENDENCIES
    // ============================================

    /**
     * Routes REPL commands and completed Cyphail
     * statements to their registered handlers.
     *
     * In production, this router is configured
     * with the active SWI-Prolog MQI broker.
     */
    private final CommandRouter commandRouter;

    /**
     * Collects multiline Cyphail statements
     * until their terminating semicolon.
     */
    private final CyphailStatementBuffer statementBuffer;

    // ============================================
    // CONSTRUCTOR
    // ============================================

    /**
     * Creates the Cyphail REPL using explicitly
     * provided dependencies.
     *
     * The command router is created by
     * ReplCommand using the active MQI broker.
     *
     * The same router remains available
     * throughout the REPL session.
     *
     * @param commandRouter configured command router
     * @param statementBuffer multiline statement buffer
     */
    Repl(
            CommandRouter commandRouter,
            CyphailStatementBuffer statementBuffer
    ) {

        this.commandRouter = Objects.requireNonNull(
                commandRouter,
                "Command router cannot be null"
        );

        this.statementBuffer = Objects.requireNonNull(
                statementBuffer,
                "Statement buffer cannot be null"
        );
    }

    // ============================================
    // START REPL
    // ============================================

    /**
     * Starts the interactive Cyphail REPL.
     *
     * Input is processed until:
     *
     * 1. The user enters .exit.
     * 2. The input stream reaches its end.
     * 3. A command handler requests termination.
     *
     * The MQI connection is not closed here.
     * Its lifecycle is managed by ReplCommand.
     */
    public void start() {

        printWelcome();

        try (Scanner scanner = new Scanner(System.in)) {

            boolean running = true;

            while (running) {

                printPrompt();

                if (!scanner.hasNextLine()) {

                    reportIncompleteStatement();

                    break;
                }

                String line = scanner.nextLine();

                running = processInputLine(line);
            }
        }
    }

    // ============================================
    // INPUT PROCESSING
    // ============================================

    /**
     * Processes a single line of user input.
     *
     * REPL commands are dispatched immediately
     * when there is no pending statement.
     *
     * Cyphail statements are collected until
     * their terminating semicolon is received.
     *
     * AST visualization is delegated to the
     * dedicated .tree command handler.
     *
     * @param line input line
     * @return true if the REPL should continue
     */
    private boolean processInputLine(String line) {

        String normalized = line.trim();

        // ----------------------------------------
        // HANDLE EXIT
        // ----------------------------------------

        if (isExitCommand(normalized)) {

            statementBuffer.reset();

            return commandRouter.route(normalized);
        }

        // ----------------------------------------
        // HANDLE AST TREE VISUALIZATION
        // ----------------------------------------

        /*
         * TreeCommandHandler has its own
         * multiline input capture mechanism.
         *
         * Therefore, .tree statements are
         * routed directly without passing
         * through the regular statement buffer.
         */

        if (commandRouter.isAwaitingTreeQuery()
                || isTreeCommand(normalized)) {

            return commandRouter.route(line);
        }

        // ----------------------------------------
        // HANDLE SPECIAL REPL COMMANDS
        // ----------------------------------------

        /*
         * Commands such as .help, .about,
         * .use and .load do not require
         * a terminating semicolon.
         */

        if (!statementBuffer.hasPendingStatement()
                && normalized.startsWith(".")) {

            return commandRouter.route(normalized);
        }

        // ----------------------------------------
        // IGNORE EMPTY INPUT
        // ----------------------------------------

        if (normalized.isEmpty()
                && !statementBuffer.hasPendingStatement()) {

            return true;
        }

        // ----------------------------------------
        // COLLECT CYPHAIL STATEMENTS
        // ----------------------------------------

        /*
         * The statement buffer supports
         * multiline input and separates
         * completed semicolon-terminated
         * Cyphail statements.
         */

        List<String> completed =
                statementBuffer.appendLine(line);

        // ----------------------------------------
        // DISPATCH COMPLETED STATEMENTS
        // ----------------------------------------

        return dispatchStatements(completed);
    }

    // ============================================
    // STATEMENT DISPATCHING
    // ============================================

    /**
     * Routes completed Cyphail statements
     * in their original input order.
     *
     * Uses a sequential Java Stream and a method
     * reference for functional-style dispatching.
     * Each statement is routed to PrologQueryHandler
     * through the configured CommandRouter.
     *
     * Processing stops when a handler requests
     * termination, preserving the original behavior.
     *
     * @param statements completed statements
     * @return true unless a handler requests exit
     */
    private boolean dispatchStatements(
            List<String> statements
    ) {

        return statements.stream()
                .allMatch(commandRouter::route);
    }

    // ============================================
    // TREE COMMAND DETECTION
    // ============================================

    /**
     * Checks whether the input starts
     * an AST visualization command.
     *
     * Supports:
     *
     * .tree
     *
     * .tree MATCH (p:Persona)
     * RETURN p.nombre;
     *
     * @param input normalized input
     * @return true for .tree commands
     */
    private boolean isTreeCommand(String input) {

        if (".tree".equalsIgnoreCase(input)) {

            return true;
        }

        return input.length() > 5
                && input.regionMatches(
                        true,
                        0,
                        ".tree",
                        0,
                        5
                )
                && Character.isWhitespace(
                        input.charAt(5)
                );
    }

    // ============================================
    // EXIT COMMAND DETECTION
    // ============================================

    /**
     * Determines whether input requests
     * termination of the REPL session.
     *
     * @param input normalized input
     * @return true for .exit
     */
    private boolean isExitCommand(String input) {

        return ".exit".equalsIgnoreCase(input);
    }

    // ============================================
    // PRINT REPL PROMPT
    // ============================================

    /**
     * Prints the standard REPL prompt or
     * the multiline continuation prompt.
     *
     * The continuation prompt is used when:
     *
     * 1. A Cyphail statement is incomplete.
     * 2. The .tree handler is capturing input.
     */
    private void printPrompt() {

        if (statementBuffer.hasPendingStatement()
                || commandRouter.isAwaitingTreeQuery()) {

            System.out.print(CONTINUATION_PROMPT);

        } else {

            System.out.print(PROMPT);
        }
    }

    // ============================================
    // INCOMPLETE STATEMENT REPORTING
    // ============================================

    /**
     * Reports incomplete Cyphail input
     * when the input stream ends before
     * a terminating semicolon.
     */
    private void reportIncompleteStatement() {

        if (statementBuffer.hasPendingStatement()) {

            System.out.println(
                    "Incomplete Cyphail statement: "
                            + "expected ';' before end of input."
            );
        }
    }

    // ============================================
    // WELCOME MESSAGE
    // ============================================

    /**
     * Prints the Cyphail Sprint P2 welcome
     * message and REPL instructions.
     *
     * The .about command provides additional
     * project information and identifies
     * all five team members.
     */
    private void printWelcome() {

        System.out.println(
                "Welcome to Cyphail v0.1.0 - Sprint P2"
        );

        System.out.println(
                "EIF400 - Programming Paradigms | Group 04-10am"
        );

        System.out.println(
                "National University of Costa Rica - School of Informatics"
        );

        System.out.println(
                "November 2026"
        );

        System.out.println();

        System.out.println(
                "Type \".help\" for available commands"
        );

        System.out.println(
                "Type \".about\" for project details and team members"
        );

        System.out.println(
                "Type \".exit\" to quit"
        );

        System.out.println();
    }
}
