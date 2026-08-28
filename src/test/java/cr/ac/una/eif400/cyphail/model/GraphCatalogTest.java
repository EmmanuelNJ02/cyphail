package cr.ac.una.eif400.cyphail.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the fake graph catalog used during Cyphail Sprint P1.1.
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
class GraphCatalogTest {

    /**
     * Verifies that the graph catalog contains the graphs required
     * for the Sprint P1.1 demonstration.
     */
    @Test
    void shouldContainRequiredGraphs() {
        List<String> graphNames = GraphCatalog.getGraphs()
                .stream()
                .map(GraphInfo::getName)
                .toList();

        assertTrue(graphNames.contains("amigos"));
        assertTrue(graphNames.contains("tasks"));
        assertTrue(graphNames.contains("teams"));
        assertTrue(graphNames.contains("planets"));
    }

    /**
     * Verifies that an existing graph can be found by name.
     */
    @Test
    void shouldFindAvailableGraphByName() {
        assertTrue(GraphCatalog.findByName("amigos").isPresent());
    }

    /**
     * Verifies that graph lookup is case insensitive.
     */
    @Test
    void shouldFindGraphIgnoringCase() {
        assertTrue(GraphCatalog.findByName("AMIGOS").isPresent());
    }

    /**
     * Verifies that a graph not registered in the fake catalog
     * is reported as unavailable.
     */
    @Test
    void shouldNotFindUnavailableGraph() {
        assertFalse(GraphCatalog.findByName("pokemon").isPresent());
    }
}