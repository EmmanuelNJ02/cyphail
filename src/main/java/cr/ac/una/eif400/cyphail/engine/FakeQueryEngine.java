package cr.ac.una.eif400.cyphail.engine;

import cr.ac.una.eif400.cyphail.model.FakeQueryResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Provides the fake query execution required for Cyphail Sprint P1.1.
 *
 * No lexer, parser, AST or Prolog execution is performed in this sprint.
 * Queries are matched against an intentionally simple set of wired
 * examples so they can be easily modified during the demonstration.
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

    private static final Map<String, FakeQueryResult> RESULTS =
            new LinkedHashMap<>();

    static {
        register(
                "MATCH (p:Persona) RETURN p.nombre, p.edad",
                new FakeQueryResult(
                        List.of("p.nombre", "p.edad"),
                        List.of(
                                List.of("\"Ana\"", "28"),
                                List.of("\"Luis\"", "31"),
                                List.of("\"Carlos\"", "25"),
                                List.of("\"Beatriz\"", "34"),
                                List.of("\"David\"", "29"),
                                List.of("\"Elena\"", "22")
                        ),
                        "OK. Query available after 42 ms."
                )
        );

        register(
                "MATCH (p1:Persona)-[r:AMIGO_DE]->(p2:Persona) "
                        + "RETURN p1.nombre AS Persona, "
                        + "type(r) AS Relacion, "
                        + "p2.nombre AS AmigoDe",
                new FakeQueryResult(
                        List.of("Persona", "Relacion", "AmigoDe"),
                        List.of(
                                List.of("\"Ana\"", "\"AMIGO_DE\"", "\"Luis\""),
                                List.of("\"Ana\"", "\"AMIGO_DE\"", "\"Beatriz\""),
                                List.of("\"Luis\"", "\"AMIGO_DE\"", "\"Carlos\""),
                                List.of("\"Luis\"", "\"AMIGO_DE\"", "\"David\""),
                                List.of("\"Carlos\"", "\"AMIGO_DE\"", "\"Elena\""),
                                List.of("\"Beatriz\"", "\"AMIGO_DE\"", "\"Elena\"")
                        ),
                        "OK. Query resolved after 666 ms."
                )
        );

        register(
                "MATCH (p:Persona) WHERE p.edad > 28 "
                        + "RETURN p.nombre, p.edad",
                new FakeQueryResult(
                        List.of("p.nombre", "p.edad"),
                        List.of(
                                List.of("\"Luis\"", "31"),
                                List.of("\"Beatriz\"", "34"),
                                List.of("\"David\"", "29")
                        ),
                        "OK. Query available after 18 ms."
                )
        );

        register(
                "MATCH (p:Persona)-[:AMIGO_DE]->(a:Persona) "
                        + "RETURN p.nombre, a.nombre",
                new FakeQueryResult(
                        List.of("p.nombre", "a.nombre"),
                        List.of(
                                List.of("\"Ana\"", "\"Luis\""),
                                List.of("\"Ana\"", "\"Beatriz\""),
                                List.of("\"Luis\"", "\"Carlos\""),
                                List.of("\"Luis\"", "\"David\""),
                                List.of("\"Carlos\"", "\"Elena\""),
                                List.of("\"Beatriz\"", "\"Elena\"")
                        ),
                        "OK. Query available after 27 ms."
                )
        );
    }

    /**
     * Prevents instances of this utility class.
     */
    private FakeQueryEngine() {
    }

    /**
     * Looks for a wired fake result for the supplied query.
     *
     * @param query Cyphail query entered in the REPL
     * @return fake result when the query is currently supported
     */
    public static Optional<FakeQueryResult> execute(String query) {
        return Optional.ofNullable(
                RESULTS.get(normalize(query))
        );
    }

    /**
     * Registers a fake query and its corresponding result.
     *
     * @param query supported query
     * @param result wired result
     */
    private static void register(
            String query,
            FakeQueryResult result
    ) {
        RESULTS.put(normalize(query), result);
    }

    /**
     * Normalizes spaces and capitalization so the fake matching is
     * slightly more tolerant while remaining intentionally simple.
     *
     * @param query query entered by the user
     * @return normalized query
     */
    private static String normalize(String query) {
        return query
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}