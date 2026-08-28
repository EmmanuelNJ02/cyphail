package cr.ac.una.eif400.cyphail.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Stores the fake graph catalog used during Cyphail Sprint P1.1.
 *
 * The catalog is intentionally simple and easy to modify during
 * the project demonstration and rebuild process.
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

    private static final List<GraphInfo> GRAPHS = new ArrayList<>();

    static {
        GRAPHS.add(new GraphInfo("amigos", "Social Network"));
        GRAPHS.add(new GraphInfo("tasks", "Tasks and resources"));
        GRAPHS.add(new GraphInfo("teams", "Soccer Teams"));
        GRAPHS.add(new GraphInfo("planets", "Planets in Solar System"));
    }

    /**
     * Prevents instances of this utility class.
     */
    private GraphCatalog() {
    }

    /**
     * Returns all graphs available in the fake catalog.
     *
     * @return read-only list of available graphs
     */
    public static List<GraphInfo> getGraphs() {
        return Collections.unmodifiableList(GRAPHS);
    }

    /**
     * Finds a graph by its name.
     *
     * @param name graph name
     * @return graph information when found
     */
    public static Optional<GraphInfo> findByName(String name) {
        return GRAPHS.stream()
                .filter(graph -> graph.getName().equalsIgnoreCase(name))
                .findFirst();
    }
}