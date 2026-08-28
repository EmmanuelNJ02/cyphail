package cr.ac.una.eif400.cyphail.engine;

import cr.ac.una.eif400.cyphail.model.FakeQueryResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the fake query engine used during Cyphail Sprint P1.1.
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
class FakeQueryEngineTest {

    /**
     * Verifies that the four MATCH queries required for the Sprint P1.1
     * demonstration have wired fake results.
     */
    @Test
    void shouldSupportTheFourSprintQueries() {
        List<String> queries = List.of(
                "MATCH (p:Persona) RETURN p.nombre, p.edad",

                "MATCH (p1:Persona)-[r:AMIGO_DE]->(p2:Persona) "
                        + "RETURN p1.nombre AS Persona, "
                        + "type(r) AS Relacion, "
                        + "p2.nombre AS AmigoDe",

                "MATCH (p:Persona) WHERE p.edad > 28 "
                        + "RETURN p.nombre, p.edad",

                "MATCH (p:Persona)-[:AMIGO_DE]->(a:Persona) "
                        + "RETURN p.nombre, a.nombre"
        );

        for (String query : queries) {
            Optional<FakeQueryResult> result =
                    FakeQueryEngine.execute(query);

            assertTrue(
                    result.isPresent(),
                    "Expected a fake result for: " + query
            );

            assertFalse(result.get().getHeaders().isEmpty());
            assertFalse(result.get().getRows().isEmpty());
        }
    }

    /**
     * Verifies that fake query matching tolerates differences in
     * capitalization and extra spaces.
     */
    @Test
    void shouldNormalizeQueryBeforeMatching() {
        Optional<FakeQueryResult> result =
                FakeQueryEngine.execute(
                        "  match   (p:persona)   return   "
                                + "p.nombre,   p.edad  "
                );

        assertTrue(result.isPresent());
    }

    /**
     * Verifies that an unsupported MATCH query does not receive
     * an invented result.
     */
    @Test
    void shouldRejectUnsupportedMatchQuery() {
        Optional<FakeQueryResult> result =
                FakeQueryEngine.execute(
                        "MATCH (x:Animal) RETURN x.nombre"
                );

        assertTrue(result.isEmpty());
    }
}