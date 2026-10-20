package cr.ac.una.eif400.cyphail.frontend.handler;

import cr.ac.una.eif400.cyphail.frontend.CyphailStatementBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the Cyphail Sprint P2 .load command.
 *
 * Reads a Cyphail source file and dispatches complete
 * semicolon-terminated statements in their original order.
 *
 * The statement dispatcher is supplied by the command router,
 * allowing file input and interactive input to share the same
 * query processing path.
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
public final class LoadCommandHandler implements CommandHandler {

    private static final String COMMAND = ".load";

    private static final Pattern LOAD_PATTERN =
            Pattern.compile(
                    "^\\.load\\s+\"([^\"]+)\"\\s*$",
                    Pattern.CASE_INSENSITIVE
            );

    private final Function<String, Boolean> statementDispatcher;

    /**
     * Creates the file-loading command handler.
     *
     * @param statementDispatcher function responsible for
     *                            processing a completed statement
     */
    public LoadCommandHandler(
            Function<String, Boolean> statementDispatcher
    ) {
        this.statementDispatcher = Objects.requireNonNull(
                statementDispatcher,
                "Statement dispatcher cannot be null"
        );
    }

    /**
     * Checks whether input begins with the .load command.
     *
     * @param input REPL input
     * @return true when input is a .load command
     */
    @Override
    public boolean supports(String input) {
        Objects.requireNonNull(input, "Input cannot be null");

        String normalized = input.trim();

        if (COMMAND.equalsIgnoreCase(normalized)) {
            return true;
        }

        return normalized.length() > COMMAND.length()
                && normalized.regionMatches(
                        true,
                        0,
                        COMMAND,
                        0,
                        COMMAND.length()
                )
                && Character.isWhitespace(
                        normalized.charAt(COMMAND.length())
                );
    }

    /**
     * Validates and processes a .load command.
     *
     * Expected syntax:
     *
     * .load "path/to/script.cyphail"
     *
     * @param input REPL command
     * @return true to continue the REPL, or false when
     *         a dispatched command requests termination
     */
    @Override
    public boolean handle(String input) {
        Objects.requireNonNull(input, "Input cannot be null");

        Matcher matcher = LOAD_PATTERN.matcher(input.trim());

        if (!matcher.matches()
                || matcher.group(1).isBlank()) {
            System.out.println(
                    "Usage: .load \"path/to/script.cyphail\""
            );
            return true;
        }

        try {
            Path path = Path.of(matcher.group(1));

            return loadFile(path);
        } catch (InvalidPathException exception) {
            System.out.println(
                    "Invalid file path: " + exception.getInput()
            );
            return true;
        }
    }

    /**
     * Reads a source file using UTF-8 and dispatches its
     * complete Cyphail statements in sequence.
     *
     * @param path source file path
     * @return false only when a dispatched command
     *         requests termination
     */
    private boolean loadFile(Path path) {
        if (!Files.isRegularFile(path)) {
            System.out.println(
                    "Cyphail source file not found: " + path
            );
            return true;
        }

        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        int statementCount = 0;

        try (BufferedReader reader =
                     Files.newBufferedReader(
                             path,
                             StandardCharsets.UTF_8
                     )) {

            String line;

            while ((line = reader.readLine()) != null) {
                for (String statement : buffer.appendLine(line)) {
                    statementCount++;

                    if (!statementDispatcher.apply(statement)) {
                        return false;
                    }
                }
            }

            if (buffer.hasPendingStatement()) {
                System.out.println(
                        "Incomplete Cyphail statement in file: "
                                + path
                                + ". Expected ';' before end of file."
                );
                return true;
            }

            System.out.println(
                    "File processed: " + path
                            + " (" + statementCount
                            + " statement(s) dispatched)."
            );

        } catch (IOException exception) {
            System.out.println(
                    "Error reading Cyphail source file: "
                            + path
                            + " (" + exception.getMessage() + ")"
            );
        }

        return true;
    }
}