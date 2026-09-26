package cr.ac.una.eif400.cyphail.engine;

import cr.ac.una.eif400.cyphail.data.FakeQueryJsonRepository;
import cr.ac.una.eif400.cyphail.model.FakeQueryResult;

import java.util.Optional;

/**
 * Provides fake query execution for Cyphail using JSON data stored on disk.
 *
 * The fake results used during Sprint P1 are no longer wired directly
 * in Java. Query data is loaded from the JSON documents located under
 * data/queries through {@link FakeQueryJsonRepository}.
 *
 * The repository reads the JSON files for every query lookup, allowing
 * fake data to be changed on disk and observed without recompiling the
 * application.
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
public final class FakeQueryEngine {

    /**
     * Prevents instances of this utility class.
     */
    private FakeQueryEngine() {
    }

    /**
     * Executes a fake Cyphail query using data loaded from JSON files
     * stored on disk.
     *
     * Query normalization and matching are handled by
     * {@link FakeQueryJsonRepository}, preserving the case-insensitive
     * and whitespace-tolerant behavior from Sprint P1.1.
     *
     * @param query Cyphail query entered by the user
     * @return fake result when a matching JSON document exists
     */
    public static Optional<FakeQueryResult> execute(String query) {
        return FakeQueryJsonRepository.findByQuery(query);
    }
}