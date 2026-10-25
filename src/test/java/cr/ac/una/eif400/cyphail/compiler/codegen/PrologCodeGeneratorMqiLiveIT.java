
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;
import cr.ac.una.eif400.cyphail.engine.mqi.PrologResult;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * Real integration tests for the Cyphail code generator.
 *
 * These tests verify the complete execution pipeline:
 *
 * Cyphail source -> Parser -> AST -> Prolog CodeGen
 * -> MQI broker -> SWI-Prolog engine -> Result rows.
 *
 * Unlike unit tests, these tests execute generated
 * Prolog goals against the real graph engine.
 *
 * Requirements:
 * - SWI-Prolog installed and available through PATH.
 * - MQI support enabled.
 * - src/main/prolog/engine.pl available.
 *
 * Run with:
 * mvn -Dtest=PrologCodeGeneratorMqiLiveIT test
 */
class PrologCodeGeneratorMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Locates the real SWI-Prolog graph engine.
     */
    private static Path enginePath() {

        Path path = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath().normalize();

        assertTrue(
                Files.isRegularFile(path),
                "The Prolog engine file must exist."
        );

        return path;
    }

    /**
     * Parses Cyphail source and compiles it
     * to an executable Prolog goal.
     */
    private String compile(String source) {

        Query query = parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result.goal());

        assertFalse(result.goal().isBlank());

        return result.goal();
    }

    /**
     * Executes a generated goal through the
     * real MQI broker and returns its table.
     */
    private static String execute(
            MqiBroker broker,
            String goal
    ) throws Exception {

        PrologResult result = broker.execute(goal);

        PrologResult.Success success =
                assertInstanceOf(
                        PrologResult.Success.class,
                        result,
                        "Generated Prolog must execute successfully. "
                                + "Actual result: " + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "The generated query must return one table."
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        assertTrue(
                bindings.containsKey("Rows"),
                "MQI must return the Rows binding."
        );

        Object rows = bindings.get("Rows");

        assertInstanceOf(
                String.class,
                rows,
                "Rows must contain formatted text."
        );

        return (String) rows;
    }

    /**
     * Verifies a generated MATCH query
     * for node property retrieval.
     */
    @Test
    void shouldExecuteGeneratedNodeQuery()
            throws Exception {

        String goal = compile(
                """
                MATCH (a:Person)
                RETURN a.name AS nombre
                """
        );

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertTrue(
                    table.startsWith("nombre"),
                    "The table must contain the alias header."
            );

            assertTrue(
                    table.contains("'Juan Perez'"),
                    "The graph must return Juan Perez."
            );

            assertTrue(
                    table.contains("'Jorge Soto'"),
                    "The graph must return Jorge Soto."
            );
        }
    }

    /**
     * Verifies a generated outgoing relationship
     * query against the sample amigos graph.
     */
    @Test
    void shouldExecuteGeneratedOutgoingRelationship()
            throws Exception {

        String goal = compile(
                """
                MATCH (a:Person)-[r:Amigo]->(b:Person)
                RETURN a.name AS origen,
                       r AS relacion,
                       b.name AS destino
                """
        );

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertTrue(
                    table.startsWith(
                            "origen relacion destino"
                    )
            );

            assertTrue(
                    table.contains(
                            "'Juan Perez' e1 'Jorge Soto'"
                    ),
                    "Outgoing edge e1 must be returned."
            );

            assertTrue(
                    table.contains(
                            "'Jorge Soto' e3 'Juan Perez'"
                    ),
                    "Outgoing edge e3 must be returned."
            );
        }
    }

    /**
     * Verifies that incoming relationships
     * reverse the endpoint matching order.
     */
    @Test
    void shouldExecuteGeneratedIncomingRelationship()
            throws Exception {

        String goal = compile(
                """
                MATCH (a:Person)<-[r:Amigo]-(b:Person)
                RETURN a.name AS destino,
                       b.name AS origen
                """
        );

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertTrue(
                    table.startsWith("destino origen")
            );

            assertTrue(
                    table.contains(
                            "'Jorge Soto' 'Juan Perez'"
                    ),
                    "Incoming edge e1 must reverse endpoints."
            );

            assertTrue(
                    table.contains(
                            "'Juan Perez' 'Jorge Soto'"
                    ),
                    "Incoming edge e3 must reverse endpoints."
            );
        }
    }

    /**
     * Verifies both orientations of an
     * undirected relationship pattern.
     */
    @Test
    void shouldExecuteGeneratedUndirectedRelationship()
            throws Exception {

        String goal = compile(
                """
                MATCH (a:Person)-[r:Amigo]-(b:Person)
                RETURN a.name AS persona,
                       b.name AS contacto
                """
        );

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = execute(broker, goal);

            assertTrue(
                    table.startsWith("persona contacto")
            );

            assertTrue(
                    table.contains(
                            "'Juan Perez' 'Jorge Soto'"
                    ),
                    "The forward orientation must be returned."
            );

            assertTrue(
                    table.contains(
                            "'Jorge Soto' 'Juan Perez'"
                    ),
                    "The reverse orientation must be returned."
            );
        }
    }
}
