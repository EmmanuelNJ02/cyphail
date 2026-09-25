package cr.ac.una.eif400.cyphail.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the fake graph catalog used by Cyphail.
 *
 * During Sprint P1, graph information is loaded from the
 * data/graphs.json file stored on disk instead of being wired
 * directly in Java.
 *
 * These tests verify that the graph catalog behavior from Sprint P1.1
 * is preserved after the migration to disk-based JSON data.
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
     * Verifies that the graph catalog loaded from JSON contains
     * the graphs required for the Sprint P1.1 demonstration.
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
     * Verifies that an existing graph loaded from JSON can be
     * found by name.
     */
    @Test
    void shouldFindAvailableGraphByName() {
        assertTrue(GraphCatalog.findByName("amigos").isPresent());
    }

    /**
     * Verifies that graph lookup remains case insensitive.
     */
    @Test
    void shouldFindGraphIgnoringCase() {
        assertTrue(GraphCatalog.findByName("AMIGOS").isPresent());
    }

    /**
     * Verifies that a graph not present in the JSON catalog
     * is reported as unavailable.
     */
    @Test
    void shouldNotFindUnavailableGraph() {
        assertFalse(GraphCatalog.findByName("pokemon").isPresent());
    }
}