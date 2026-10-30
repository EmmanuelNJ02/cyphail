
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
 * Emmanuel Núñez Jiménez
 * Julissa Solano Valverde
 * Valery Alfaro Morales
 * Keynell Molina Mora
 * Roy Arias Mejía
 *
 * Real SWI-Prolog / MQI integration tests.
 *
 * Verifies that different MATCH relationship
 * occurrences cannot reuse the same physical edge.
 *
 * Covered scenarios:
 * 1. A self-loop cannot satisfy two relationship
 *    occurrences simultaneously.
 * 2. Two different physical edges can satisfy
 *    a reciprocal relationship MATCH.
 * 3. Anonymous relationship occurrences must
 *    also use different physical edges.
 *
 * Each test uses an independent SWI-Prolog process.
 */
class PrologDistinctEdgesMqiLiveIT {

    // =====================================================
    // PARSER AND CODE GENERATOR
    // =====================================================

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    // =====================================================
    // ENGINE LOCATION
    // =====================================================

    /**
     * Returns the absolute location of the
     * SWI-Prolog engine used by Cyphail P2.
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
                "The SWI-Prolog engine must exist."
        );

        return path;
    }

    // =====================================================
    // QUERY EXECUTION HELPER
    // =====================================================

    /**
     * Executes a Cyphail query using the complete
     * P2 execution pipeline:
     *
     * Parser -> AST -> CodeGen -> MQI -> SWI-Prolog
     *
     * Returns the formatted table received from MQI.
     */
    private String run(
            MqiBroker broker,
            String source
    ) throws Exception {

        Query query = parser.parseOrThrow(source);

        PrologGenerationResult generated =
                generator.generate(query);

        assertNotNull(
                generated,
                "CodeGen must produce a result."
        );

        assertFalse(
                generated.goal().isBlank(),
                "Generated Prolog goal cannot be blank."
        );

        PrologResult result =
                broker.execute(generated.goal());

        PrologResult.Success success =
                assertInstanceOf(
                        PrologResult.Success.class,
                        result,
                        "The generated Prolog goal must execute "
                                + "successfully. Actual: " + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "MQI must return exactly one result table."
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        assertTrue(
                bindings.containsKey("Rows"),
                "The MQI response must contain Rows."
        );

        return assertInstanceOf(
                String.class,
                bindings.get("Rows"),
                "Rows must contain the formatted table."
        );
    }

    // =====================================================
    // TEST 1: SELF-LOOP EDGE CANNOT BE REUSED
    // =====================================================

    /**
     * Reproduces the regression discovered
     * during the P2 manual verification.
     *
     * Creates one node with one self-loop edge.
     *
     * Two different MATCH relationship occurrences
     * must not be allowed to bind that same edge.
     *
     * Expected result:
     * Only the table headers, without data rows.
     */
    @Test
    void shouldNotReuseSelfLoopEdgeAcrossTwoPatterns()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String created = run(
                    broker,
                    """
                    CREATE (a:Person {
                        name: 'DistinctEdges_Loop_P2',
                        age: 25
                    })-[r:Amigo]->(a)
                    RETURN a, r
                    """
            );

            assertTrue(
                    created.contains("cy_node_"),
                    "CREATE must return the new node ID."
            );

            assertTrue(
                    created.contains("cy_edge_"),
                    "CREATE must return the new edge ID."
            );

            String table = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'DistinctEdges_Loop_P2'
                    })-[r1:Amigo]->(a),
                    (a)-[r2:Amigo]->(a)
                    RETURN r1, r2
                    """
            );

            assertEquals(
                    "r1 r2",
                    table,
                    "One self-loop edge must not satisfy "
                            + "two distinct relationship patterns."
            );
        }
    }

    // =====================================================
    // TEST 2: TWO DIFFERENT EDGES MUST WORK
    // =====================================================

    /**
     * The sample graph contains:
     *
     * e1: Juan Perez -> Jorge Soto
     * e3: Jorge Soto -> Juan Perez
     *
     * Because these are two distinct physical
     * edges, the reciprocal MATCH must succeed.
     *
     * This ensures the inequality restriction
     * does not incorrectly reject valid matches.
     */
    @Test
    void shouldMatchTwoDifferentReciprocalEdges()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })-[r1:Amigo]->(b:Person {
                        name: 'Jorge Soto'
                    }),
                    (b)-[r2:Amigo]->(a)
                    RETURN r1, r2
                    """
            );

            assertEquals(
                    "r1 r2\ne1 e3",
                    table,
                    "Two distinct reciprocal edges "
                            + "must produce a valid MATCH result."
            );
        }
    }

    // =====================================================
    // TEST 3: ANONYMOUS EDGES CANNOT BE REUSED
    // =====================================================

    /**
     * Two anonymous relationship patterns
     * must also refer to different physical edges.
     *
     * The sample graph contains only one
     * Juan Perez -> Jorge Soto edge of type Amigo.
     *
     * Therefore, repeating that relationship
     * pattern must not produce a data row.
     */
    @Test
    void shouldNotReuseAnonymousRelationshipEdge()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String table = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })-[:Amigo]->(b:Person {
                        name: 'Jorge Soto'
                    }),
                    (a)-[:Amigo]->(b)
                    RETURN a.name
                    """
            );

            assertEquals(
                    "a.name",
                    table,
                    "Anonymous relationship patterns "
                            + "must not reuse the same edge."
            );
        }
    }
}
