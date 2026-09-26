package cr.ac.una.eif400.cyphail.model;

import cr.ac.una.eif400.cyphail.data.GraphCatalogJsonRepository;

import java.util.List;
import java.util.Optional;

/**
 * Provides access to the fake graph catalog used by Cyphail.
 *
 * The catalog entries are no longer wired directly in Java.
 * Graph information is loaded from data/graphs.json through
 * {@link GraphCatalogJsonRepository}.
 *
 * The JSON file is read again whenever the catalog is requested,
 * allowing changes made on disk to be observed without recompiling
 * the application.
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
public final class GraphCatalog {

    /**
     * Prevents instances of this utility class.
     */
    private GraphCatalog() {
    }

    /**
     * Returns all graphs currently available in the fake catalog.
     *
     * The catalog is loaded from data/graphs.json every time this
     * method is invoked.
     *
     * @return immutable list of available graphs
     */
    public static List<GraphInfo> getGraphs() {
        return GraphCatalogJsonRepository.findAll();
    }

    /**
     * Finds a graph by name.
     *
     * Graph lookup remains case insensitive, preserving the
     * behavior provided by Sprint P1.1.
     *
     * @param name graph name
     * @return graph information when found
     */
    public static Optional<GraphInfo> findByName(String name) {
        return getGraphs()
                .stream()
                .filter(graph ->
                        graph.getName().equalsIgnoreCase(name)
                )
                .findFirst();
    }
}