package cr.ac.una.eif400.cyphail.frontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collects Cyphail statements across multiple input lines.
 *
 * Statements are completed by a semicolon outside string
 * literals and comments. The terminator is removed before
 * passing the statement to the Cyphail parser.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
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
public final class CyphailStatementBuffer {

    private final StringBuilder pending = new StringBuilder();

    private char quote;
    private boolean escaped;
    private boolean inBlockComment;
    private boolean hasCode;

    /**
     * Appends a line and returns all completed statements.
     *
     * @param line input line
     * @return immutable list of completed statements
     */
    public List<String> appendLine(String line) {
        Objects.requireNonNull(line, "Input line cannot be null");

        if (!pending.isEmpty()) {
            pending.append('\n');
        }

        List<String> completed = new ArrayList<>();
        boolean inLineComment = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);

            char next = index + 1 < line.length()
                    ? line.charAt(index + 1)
                    : '\0';

            if (inLineComment) {
                pending.append(current);
                continue;
            }

            if (inBlockComment) {
                pending.append(current);

                if (current == '*' && next == '/') {
                    pending.append(next);
                    index++;
                    inBlockComment = false;
                }

                continue;
            }

            if (quote != '\0') {
                pending.append(current);

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (quote == '"' && current == '\\') {
                    escaped = true;
                    continue;
                }

                if (current == quote) {
                    if (quote == '\'' && next == '\'') {
                        pending.append(next);
                        index++;
                    } else {
                        quote = '\0';
                    }
                }

                continue;
            }

            if (current == '/' && next == '/') {
                pending.append("//");
                index++;
                inLineComment = true;
                continue;
            }

            if (current == '/' && next == '*') {
                pending.append("/*");
                index++;
                inBlockComment = true;
                continue;
            }

            if (current == '"' || current == '\'') {
                pending.append(current);
                quote = current;
                hasCode = true;
                continue;
            }

            if (current == ';') {
                if (hasCode) {
                    completed.add(
                            pending.toString().trim()
                    );
                }

                pending.setLength(0);
                hasCode = false;
                continue;
            }

            pending.append(current);

            if (!Character.isWhitespace(current)) {
                hasCode = true;
            }
        }

        return List.copyOf(completed);
    }

    /**
     * Checks whether an unfinished statement is buffered.
     *
     * @return true when executable source is pending
     */
    public boolean hasPendingStatement() {
        return hasCode;
    }

    /**
     * Returns the source accumulated so far.
     *
     * @return pending source text
     */
    public String pendingSource() {
        return pending.toString();
    }

    /**
     * Discards the pending statement and parsing state.
     */
    public void reset() {
        pending.setLength(0);
        quote = '\0';
        escaped = false;
        inBlockComment = false;
        hasCode = false;
    }
}