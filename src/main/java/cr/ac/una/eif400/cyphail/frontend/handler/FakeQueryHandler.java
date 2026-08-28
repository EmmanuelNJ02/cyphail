package cr.ac.una.eif400.cyphail.frontend.handler;

import cr.ac.una.eif400.cyphail.engine.FakeQueryEngine;
import cr.ac.una.eif400.cyphail.model.FakeQueryResult;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Handles fake MATCH queries during Cyphail Sprint P1.1.
 *
 * No lexer, parser, AST or Prolog execution is performed in this sprint.
 * Supported queries are matched against the wired examples stored in
 * FakeQueryEngine.
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
public class FakeQueryHandler implements CommandHandler {

    private static final String MATCH_COMMAND = "MATCH";

    /**
     * Checks whether the input looks like a Cyphail MATCH query.
     *
     * @param input text entered in the REPL
     * @return true when the input begins with MATCH
     */
    @Override
    public boolean supports(String input) {
        return input.trim()
                .toUpperCase(Locale.ROOT)
                .startsWith(MATCH_COMMAND + " ");
    }

    /**
     * Executes a wired fake query and prints its tabular result.
     *
     * @param input query entered in the REPL
     * @return true because the REPL should continue running
     */
    @Override
    public boolean handle(String input) {
        Optional<FakeQueryResult> result =
                FakeQueryEngine.execute(input);

        if (result.isEmpty()) {
            System.out.println(
                    "ERROR. This MATCH query is not available "
                            + "in Sprint P1.1."
            );
            System.out.println();

            return true;
        }

        printResult(result.get());

        return true;
    }

    /**
     * Prints a fake query result as a formatted text table.
     *
     * @param result fake query result
     */
    private void printResult(FakeQueryResult result) {
        List<String> headers = result.getHeaders();
        List<List<String>> rows = result.getRows();

        int[] widths = calculateColumnWidths(headers, rows);

        printRow(headers, widths);
        printSeparator(widths);

        for (List<String> row : rows) {
            printRow(row, widths);
        }

        System.out.println();
        System.out.println(result.getStatusMessage());
        System.out.println();
    }

    /**
     * Calculates the width needed for each table column.
     *
     * @param headers table headers
     * @param rows table rows
     * @return calculated column widths
     */
    private int[] calculateColumnWidths(
            List<String> headers,
            List<List<String>> rows
    ) {
        int[] widths = new int[headers.size()];

        for (int column = 0; column < headers.size(); column++) {
            widths[column] = headers.get(column).length();
        }

        for (List<String> row : rows) {
            for (int column = 0; column < row.size(); column++) {
                widths[column] = Math.max(
                        widths[column],
                        row.get(column).length()
                );
            }
        }

        return widths;
    }

    /**
     * Prints one row of the result table.
     *
     * @param values row values
     * @param widths column widths
     */
    private void printRow(
            List<String> values,
            int[] widths
    ) {
        for (int column = 0; column < values.size(); column++) {
            String format = "%-" + (widths[column] + 4) + "s";
            System.out.printf(format, values.get(column));
        }

        System.out.println();
    }

    /**
     * Prints the separator located below the table header.
     *
     * @param widths column widths
     */
    private void printSeparator(int[] widths) {
        int totalWidth = 0;

        for (int width : widths) {
            totalWidth += width + 4;
        }

        System.out.println("-".repeat(totalWidth));
    }
}