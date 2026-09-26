package cr.ac.una.eif400.cyphail.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a fake tabular query result used by Cyphail.
 *
 * During Sprint P1, fake query data is loaded from JSON documents
 * stored on disk instead of being wired directly in Java.
 *
 * This class remains responsible only for representing the decoded
 * tabular result: column headers, rows and the informational status
 * message displayed by the REPL.
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
public final class FakeQueryResult {

    private final List<String> headers;
    private final List<List<String>> rows;
    private final String statusMessage;

    /**
     * Creates a fake tabular query result.
     *
     * @param headers column headers
     * @param rows result rows
     * @param statusMessage informational message displayed after the table
     */
    public FakeQueryResult(
            List<String> headers,
            List<List<String>> rows,
            String statusMessage
    ) {
        this.headers = List.copyOf(headers);

        List<List<String>> copiedRows = new ArrayList<>();

        for (List<String> row : rows) {
            copiedRows.add(List.copyOf(row));
        }

        this.rows = Collections.unmodifiableList(copiedRows);
        this.statusMessage = statusMessage;
    }

    /**
     * Returns the table headers.
     *
     * @return result headers
     */
    public List<String> getHeaders() {
        return headers;
    }

    /**
     * Returns the table rows.
     *
     * @return result rows
     */
    public List<List<String>> getRows() {
        return rows;
    }

    /**
     * Returns the informational message shown after the result.
     *
     * @return status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }
}