
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
 *
 * Integration tests for REMOVE on original
 * relationships from the professor's graph.
 *
 * Covered functionality:
 *
 * 1. REMOVE of a property from original edge e1.
 * 2. Preservation of remaining edge properties.
 * 3. Preservation of the original relationship.
 * 4. Persistence within the same MQI session.
 * 5. REMOVE of a nonexistent property.
 *
 * These tests use the real SWI-Prolog engine.
 *
 * Run with:
 *
 * mvn -Dtest=PrologRemoveOriginalEdgeMqiLiveIT test
 */
class PrologRemoveOriginalEdgeMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Locates the project's real Prolog engine.
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

    /**
     * Parses a Cyphail statement and generates
     * the corresponding executable Prolog goal.
     */
    private String compile(String source) {

        Query query =
                parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result);
        assertNotNull(result.goal());
        assertFalse(result.goal().isBlank());

        return result.goal();
    }

    /**
     * Executes a generated Prolog goal
     * and retrieves its formatted Rows value.
     */
    private static String execute(
            MqiBroker broker,
            String goal
    ) throws Exception {

        PrologResult result =
                broker.execute(goal);

        PrologResult.Success success =
                assertInstanceOf(
                        PrologResult.Success.class,
                        result,
                        "SWI-Prolog must execute the goal. "
                                + "Actual result: " + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "The query must return one result table."
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        assertTrue(
                bindings.containsKey("Rows"),
                "MQI must return the Rows binding."
        );

        return assertInstanceOf(
                String.class,
                bindings.get("Rows")
        );
    }

    /**
     * Executes Cyphail source through the
     * active MQI session.
     */
    private String run(
            MqiBroker broker,
            String source
    ) throws Exception {

        return execute(
                broker,
                compile(source)
        );
    }

    /**
     * Executes a Prolog setup goal.
     *
     * This is used only to prepare temporary
     * test properties for original relationships.
     *
     * The professor's initial graph does not
     * define properties for these relationships.
     */
    private static void prepare(
            MqiBroker broker,
            String goal
    ) throws Exception {

        PrologResult result =
                broker.execute(goal);

        assertInstanceOf(
                PrologResult.Success.class,
                result,
                "Test setup must succeed. Actual: "
                        + result
        );
    }

    // =====================================================
    // TEST 1 - ORIGINAL RELATIONSHIP PROPERTY REMOVAL
    // =====================================================

    /**
     * Verifies the complete Cyphail execution
     * flow for REMOVE on original edge e1.
     *
     * e1 connects:
     *
     * Juan Perez -> Jorge Soto
     *
     * Two temporary test properties are added:
     *
     * since = 2020
     * level = 5
     *
     * REMOVE must delete only since.
     *
     * The level property and the original
     * relationship must remain available.
     */
    @Test
    void shouldRemovePropertyFromOriginalRelationship()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            // Create temporary test properties
            // for the original relationship e1.

            prepare(
                    broker,
                    "assertz("
                            + "user_amigos_edge_prop("
                            + "e1, since, 2020)), "
                            + "assertz("
                            + "user_amigos_edge_prop("
                            + "e1, level, 5))"
            );

            // Confirm that both properties can
            // be read from Cyphail before REMOVE.

            String before = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'Jorge Soto'
                    })
                    RETURN r.since, r.level
                    """
            );

            assertEquals(
                    "r.since r.level\n2020 5",
                    before,
                    "Both original relationship properties "
                            + "must be visible before REMOVE."
            );

            // Remove one property through
            // the complete Cyphail pipeline.

            String updated = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'Jorge Soto'
                    })
                    REMOVE r.since
                    RETURN r
                    """
            );

            assertEquals(
                    "r\ne1",
                    updated,
                    "REMOVE must preserve the original "
                            + "relationship identifier."
            );

            // The removed property must no
            // longer produce a value.

            String removed = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'Jorge Soto'
                    })
                    RETURN r.since
                    """
            );

            assertEquals(
                    "r.since",
                    removed,
                    "The removed property must be absent."
            );

            // The other relationship property
            // must remain unchanged.

            String preserved = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'Jorge Soto'
                    })
                    RETURN r.level
                    """
            );

            assertEquals(
                    "r.level\n5",
                    preserved,
                    "The remaining property must be preserved."
            );

            // Check that the MQI session remains
            // available after graph mutations.

            assertTrue(
                    broker.isOpen(),
                    "The MQI connection must remain open."
            );
        }
    }

    // =====================================================
    // TEST 2 - NONEXISTENT PROPERTY ON ORIGINAL EDGE
    // =====================================================

    /**
     * Verifies that removing a nonexistent
     * property does not delete the relationship.
     *
     * This test uses original edge e3:
     *
     * Jorge Soto -> Juan Perez
     */
    @Test
    void shouldPreserveOriginalRelationshipWhenPropertyIsMissing()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String updated = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Jorge Soto'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'Juan Perez'
                    })
                    REMOVE r.unknownProperty
                    RETURN r
                    """
            );

            assertEquals(
                    "r\ne3",
                    updated,
                    "Removing a nonexistent property "
                            + "must preserve the original edge."
            );

            // The relationship must still
            // be accessible in a later query.

            String existing = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Jorge Soto'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'Juan Perez'
                    })
                    RETURN r
                    """
            );

            assertEquals(
                    "r\ne3",
                    existing,
                    "The original relationship must "
                            + "remain in the graph."
            );
        }
    }
}
