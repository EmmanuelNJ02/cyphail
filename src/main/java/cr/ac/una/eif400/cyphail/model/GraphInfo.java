package cr.ac.una.eif400.cyphail.model;

/**
 * Represents the basic information of a graph available in Cyphail.
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
public final class GraphInfo {

    private final String name;
    private final String description;

    /**
     * Creates information for an available graph.
     *
     * @param name graph name
     * @param description graph description
     */
    public GraphInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Returns the graph name.
     *
     * @return graph name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the graph description.
     *
     * @return graph description
     */
    public String getDescription() {
        return description;
    }
}