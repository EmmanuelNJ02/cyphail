package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.mqi.MqiResponseParser;
import cr.ac.una.eif400.cyphail.engine.mqi.PrologResult;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Prints SWI-Prolog MQI results in a readable REPL format.
 *
 * Sprint P2 supports two result representations:
 *
 * 1. Preformatted tables returned through the Rows binding.
 * 2. Individual Prolog variable bindings displayed in columns.
 *
 * Preformatted Rows content preserves its original line
 * breaks and spacing. Ordinary bindings are formatted as
 * aligned tables.
 *
 * Query failures and Prolog errors are also supported.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * School: School of Informatics
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
public final class PrologTablePrinter {

    private static final String ROWS_BINDING = "Rows";

    private final PrintStream output;

    /**
     * Creates a printer using the standard console output.
     */
    public PrologTablePrinter() {
        this(System.out);
    }

    /**
     * Creates a printer with an explicit output stream.
     *
     * @param output destination for formatted results
     */
    public PrologTablePrinter(PrintStream output) {
        this.output = Objects.requireNonNull(
                output,
                "Output stream cannot be null"
        );
    }

    /**
     * Prints a result returned by SWI-Prolog through MQI.
     *
     * @param result Prolog execution result
     */
    public void print(PrologResult result) {
        Objects.requireNonNull(
                result,
                "Prolog result cannot be null"
        );

        if (result instanceof PrologResult.Success success) {
            printSuccess(success.rows());
            return;
        }

        if (result instanceof PrologResult.Failure) {
            output.println("No solutions.");
            output.println();
            return;
        }

        if (result instanceof PrologResult.PrologError error) {
            output.println(
                    "Prolog error: " + formatValue(error.term())
            );
            output.println();
        }
    }

    /**
     * Prints successful query solutions.
     *
     * Preformatted Rows bindings are handled separately
     * from regular variable bindings.
     *
     * @param rows variable bindings returned by MQI
     */
    private void printSuccess(
            List<Map<String, Object>> rows
    ) {
        Objects.requireNonNull(
                rows,
                "Result rows cannot be null"
        );

        if (rows.isEmpty()) {
            output.println(
                    "Query completed. No rows returned."
            );
            output.println();
            return;
        }

        /*
         * The Sprint P2 engine may return a single binding
         * named Rows containing an already formatted table.
         *
         * Its line breaks and whitespace must be preserved
         * instead of being escaped as an ordinary table cell.
         */
        if (isPreformattedRowsResult(rows)) {
            printPreformattedRows(rows);
            return;
        }

        /*
         * Other MQI responses contain individual bindings
         * that must be displayed as an aligned table.
         */
        printBindingTable(rows);
    }

    /**
     * Detects preformatted Rows results.
     *
     * Every solution must contain only the Rows binding,
     * and its value must be a string.
     *
     * This prevents ordinary bindings from being treated
     * as preformatted output.
     *
     * @param rows MQI solutions
     * @return true if all solutions are Rows-only bindings
     */
    private boolean isPreformattedRowsResult(
            List<Map<String, Object>> rows
    ) {
        return rows.stream().allMatch(row ->
                row.size() == 1
                        && row.get(ROWS_BINDING) instanceof String
        );
    }

    /**
     * Prints the original table text returned by Prolog.
     *
     * Newline characters, spaces and column names are
     * preserved exactly as provided by the MQI result.
     *
     * @param rows solutions containing preformatted tables
     */
    private void printPreformattedRows(
            List<Map<String, Object>> rows
    ) {
        for (Map<String, Object> row : rows) {
            String table = (String) row.get(ROWS_BINDING);

            output.println(table);
        }

        output.println();
    }

    /**
     * Prints ordinary variable bindings as an aligned table.
     *
     * @param rows MQI solutions
     */
    private void printBindingTable(
            List<Map<String, Object>> rows
    ) {
        List<String> headers = collectHeaders(rows);

        if (headers.isEmpty()) {
            output.println(
                    "Query succeeded. Solutions: "
                            + rows.size()
                            + " (no variable bindings)."
            );
            output.println();
            return;
        }

        List<List<String>> formattedRows =
                formatRows(rows, headers);

        int[] widths =
                calculateColumnWidths(
                        headers,
                        formattedRows
                );

        printBorder(widths);
        printRow(headers, widths);
        printBorder(widths);

        for (List<String> row : formattedRows) {
            printRow(row, widths);
        }

        printBorder(widths);

        output.println("Rows: " + rows.size());
        output.println();
    }

    /**
     * Collects variable names in their first-seen order.
     *
     * @param rows result rows
     * @return ordered column names
     */
    private List<String> collectHeaders(
            List<Map<String, Object>> rows
    ) {
        Set<String> headers = new LinkedHashSet<>();

        for (Map<String, Object> row : rows) {
            headers.addAll(row.keySet());
        }

        return new ArrayList<>(headers);
    }

    /**
     * Converts Prolog values into printable table cells.
     *
     * @param rows original query results
     * @param headers ordered column names
     * @return formatted result rows
     */
    private List<List<String>> formatRows(
            List<Map<String, Object>> rows,
            List<String> headers
    ) {
        List<List<String>> formatted = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<>();

            for (String header : headers) {
                if (row.containsKey(header)) {
                    values.add(
                            formatValue(row.get(header))
                    );
                } else {
                    values.add("");
                }
            }

            formatted.add(values);
        }

        return formatted;
    }

    /**
     * Calculates the minimum width of every column.
     *
     * @param headers column names
     * @param rows formatted result rows
     * @return column widths
     */
    private int[] calculateColumnWidths(
            List<String> headers,
            List<List<String>> rows
    ) {
        int[] widths = new int[headers.size()];

        for (int column = 0;
             column < headers.size();
             column++) {

            widths[column] = headers.get(column).length();
        }

        for (List<String> row : rows) {
            for (int column = 0;
                 column < row.size();
                 column++) {

                widths[column] = Math.max(
                        widths[column],
                        row.get(column).length()
                );
            }
        }

        return widths;
    }

    /**
     * Prints a horizontal table border.
     *
     * @param widths column widths
     */
    private void printBorder(int[] widths) {
        output.print("+");

        for (int width : widths) {
            output.print("-".repeat(width + 2));
            output.print("+");
        }

        output.println();
    }

    /**
     * Prints a table row with aligned columns.
     *
     * @param values printable cell values
     * @param widths column widths
     */
    private void printRow(
            List<String> values,
            int[] widths
    ) {
        output.print("|");

        for (int column = 0;
             column < values.size();
             column++) {

            String value = values.get(column);

            output.print(" ");
            output.print(value);

            int padding =
                    widths[column] - value.length() + 1;

            output.print(" ".repeat(padding));
            output.print("|");
        }

        output.println();
    }

    /**
     * Formats a value returned by the MQI response parser.
     *
     * Supports primitive values, lists and compound
     * Prolog terms without relying on fake query data.
     *
     * @param value decoded Prolog value
     * @return printable representation
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof MqiResponseParser.Compound compound) {
            if (compound.args().isEmpty()) {
                return sanitize(compound.functor());
            }

            StringJoiner arguments =
                    new StringJoiner(", ");

            for (Object argument : compound.args()) {
                arguments.add(formatValue(argument));
            }

            return sanitize(compound.functor())
                    + "(" + arguments + ")";
        }

        if (value instanceof List<?> list) {
            StringJoiner elements =
                    new StringJoiner(", ", "[", "]");

            for (Object element : list) {
                elements.add(formatValue(element));
            }

            return elements.toString();
        }

        return sanitize(String.valueOf(value));
    }

    /**
     * Escapes control characters inside ordinary table
     * cells to prevent broken column alignment.
     *
     * Preformatted Rows content does not use this method.
     *
     * @param text original value
     * @return printable single-line value
     */
    private String sanitize(String text) {
        return text
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}