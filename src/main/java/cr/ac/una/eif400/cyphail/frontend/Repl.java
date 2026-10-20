package cr.ac.una.eif400.cyphail.frontend;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Interactive Read-Eval-Print-Loop for Cyphail.
 *
 * Sprint P2 adds multiline statement processing.
 * Statements are collected until a terminating semicolon
 * is encountered outside string literals and comments.
 *
 * Existing REPL commands and AST tree functionality
 * remain available.
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
public class Repl {

    private static final String PROMPT = ">>> ";
    private static final String CONTINUATION_PROMPT = "... ";

    private final CommandRouter commandRouter;
    private final CyphailStatementBuffer statementBuffer;

    /**
     * Creates the production Cyphail REPL.
     */
    public Repl() {
        this(
                new CommandRouter(),
                new CyphailStatementBuffer()
        );
    }

    /**
     * Creates a REPL using explicit collaborators.
     *
     * @param commandRouter command dispatcher
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

    /**
     * Starts the interactive Cyphail REPL.
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

    /**
     * Processes a single line of REPL input.
     *
     * Commands are dispatched immediately when no
     * Cyphail statement is pending. Query statements
     * are dispatched only after their semicolon.
     *
     * @param line user input
     * @return true when the REPL should continue
     */
    private boolean processInputLine(String line) {
        String normalized = line.trim();

        if (isExitCommand(normalized)) {
            statementBuffer.reset();
            return commandRouter.route(normalized);
        }

        if (commandRouter.isAwaitingTreeQuery()
                || isTreeCommand(normalized)) {
            return commandRouter.route(line);
        }

        if (!statementBuffer.hasPendingStatement()
                && normalized.startsWith(".")) {
            return commandRouter.route(normalized);
        }

        if (normalized.isEmpty()
                && !statementBuffer.hasPendingStatement()) {
            return true;
        }

        List<String> completed =
                statementBuffer.appendLine(line);

        return dispatchStatements(completed);
    }

    /**
     * Dispatches completed Cyphail statements in order.
     *
     * @param statements completed source statements
     * @return true unless a handler requests termination
     */
    private boolean dispatchStatements(
            List<String> statements
    ) {
        for (String statement : statements) {
            if (!commandRouter.route(statement)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines whether input starts the AST tree command.
     *
     * @param input normalized user input
     * @return true for .tree or .tree followed by a query
     */
    private boolean isTreeCommand(String input) {
        if (".tree".equalsIgnoreCase(input)) {
            return true;
        }

        return input.length() > 5
                && input.regionMatches(
                        true, 0, ".tree", 0, 5
                )
                && Character.isWhitespace(
                        input.charAt(5)
                );
    }

    /**
     * Determines whether input requests REPL termination.
     *
     * @param input normalized user input
     * @return true for .exit
     */
    private boolean isExitCommand(String input) {
        return ".exit".equalsIgnoreCase(input);
    }

    /**
     * Prints the normal or continuation prompt.
     */
    private void printPrompt() {
        if (statementBuffer.hasPendingStatement()
                || commandRouter.isAwaitingTreeQuery()) {
            System.out.print(CONTINUATION_PROMPT);
        } else {
            System.out.print(PROMPT);
        }
    }

    /**
     * Reports incomplete input when the input stream ends.
     */
    private void reportIncompleteStatement() {
        if (statementBuffer.hasPendingStatement()) {
            System.out.println(
                    "Incomplete Cyphail statement: "
                            + "expected ';' before end of input."
            );
        }
    }

    /**
     * Prints the project identification and REPL help.
     */
    private void printWelcome() {
        System.out.println(
                "Welcome to Cyphail-04-10am v.0.1. September 2026. "
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