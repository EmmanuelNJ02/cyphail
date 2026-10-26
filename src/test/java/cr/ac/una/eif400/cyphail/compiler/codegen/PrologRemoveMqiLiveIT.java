
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
 * Real SWI-Prolog/MQI integration tests for REMOVE.
 *
 * Verifies the complete execution flow:
 *
 * Cyphail source -> Parser -> AST -> CodeGen
 * -> MQI -> SWI-Prolog -> result table.
 *
 * Covered scenarios:
 *
 * 1. Remove a stored node property.
 * 2. Remove a stored relationship property.
 * 3. Remove multiple properties.
 * 4. REMOVE without RETURN.
 * 5. Invalidate an old MATCH property binding.
 * 6. Remove a nonexistent property.
 * 7. Remove a property from the original graph.
 *
 * Every test starts an independent Prolog process.
 *
 * Execute with:
 *
 * mvn -Dtest=PrologRemoveMqiLiveIT test
 */
class PrologRemoveMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Returns the actual SWI-Prolog engine path.
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
     * Parses Cyphail source and generates
     * an executable Prolog goal.
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
     * Executes Prolog and extracts the formatted
     * table returned in the Rows binding.
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
                        "Prolog execution must succeed. Actual: "
                                + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "Exactly one result table is expected."
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        assertTrue(
                bindings.containsKey("Rows"),
                "MQI must provide a Rows binding."
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

    // =====================================================
    // TEST 1 - STORED NODE PROPERTY
    // =====================================================

    @Test
    void shouldRemoveStoredNodeProperty()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (p:Product {
                        code: 'P2_REMOVE_NODE',
                        discount: 10,
                        price: 99
                    })
                    """
            );

            String updated = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_NODE'
                    })
                    REMOVE p.discount
                    RETURN p.code
                    """
            );

            assertEquals(
                    "p.code\n'P2_REMOVE_NODE'",
                    updated
            );

            String removed = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_NODE'
                    })
                    RETURN p.discount
                    """
            );

            assertEquals(
                    "p.discount",
                    removed,
                    "Removed property must not produce a value."
            );

            String preserved = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_NODE'
                    })
                    RETURN p.price
                    """
            );

            assertEquals(
                    "p.price\n99",
                    preserved,
                    "Other node properties must remain."
            );
        }
    }

    // =====================================================
    // TEST 2 - STORED RELATIONSHIP PROPERTY
    // =====================================================

    @Test
    void shouldRemoveStoredRelationshipProperty()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (a:Person {
                        name: 'P2_REMOVE_EDGE_A'
                    })
                    -[r:KNOWS {
                        since: 2020,
                        strength: 5
                    }]->
                    (b:Person {
                        name: 'P2_REMOVE_EDGE_B'
                    })
                    """
            );

            String updated = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_REMOVE_EDGE_A'
                    })
                    -[r:KNOWS]->
                    (b:Person {
                        name: 'P2_REMOVE_EDGE_B'
                    })
                    REMOVE r.since
                    RETURN r
                    """
            );

            assertTrue(
                    updated.startsWith("r\ncy_edge_"),
                    "The relationship must still exist."
            );

            String removed = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_REMOVE_EDGE_A'
                    })
                    -[r:KNOWS]->
                    (b:Person {
                        name: 'P2_REMOVE_EDGE_B'
                    })
                    RETURN r.since
                    """
            );

            assertEquals(
                    "r.since",
                    removed,
                    "Removed relationship property must be absent."
            );

            String preserved = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_REMOVE_EDGE_A'
                    })
                    -[r:KNOWS]->
                    (b:Person {
                        name: 'P2_REMOVE_EDGE_B'
                    })
                    RETURN r.strength
                    """
            );

            assertEquals(
                    "r.strength\n5",
                    preserved,
                    "Other relationship properties must remain."
            );
        }
    }

    // =====================================================
    // TEST 3 - MULTIPLE PROPERTY REMOVALS
    // =====================================================

    @Test
    void shouldRemoveMultipleProperties()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String updated = run(
                    broker,
                    """
                    CREATE (p:Product {
                        code: 'P2_REMOVE_MULTIPLE',
                        discount: 10,
                        oldPrice: 20,
                        price: 30
                    })
                    REMOVE p.discount, p.oldPrice
                    RETURN p.price
                    """
            );

            assertEquals(
                    "p.price\n30",
                    updated
            );

            String discount = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_MULTIPLE'
                    })
                    RETURN p.discount
                    """
            );

            assertEquals(
                    "p.discount",
                    discount
            );

            String oldPrice = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_MULTIPLE'
                    })
                    RETURN p.oldPrice
                    """
            );

            assertEquals(
                    "p.oldPrice",
                    oldPrice
            );

            String preserved = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_MULTIPLE'
                    })
                    RETURN p.price
                    """
            );

            assertEquals(
                    "p.price\n30",
                    preserved
            );
        }
    }

    // =====================================================
    // TEST 4 - REMOVE WITHOUT RETURN
    // =====================================================

    @Test
    void shouldRemovePropertyWithoutReturn()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (p:Product {
                        code: 'P2_REMOVE_NO_RETURN',
                        discount: 15
                    })
                    """
            );

            run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_NO_RETURN'
                    })
                    REMOVE p.discount
                    """
            );

            String removed = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_NO_RETURN'
                    })
                    RETURN p.discount
                    """
            );

            assertEquals(
                    "p.discount",
                    removed,
                    "REMOVE without RETURN must persist."
            );

            String existing = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_NO_RETURN'
                    })
                    RETURN p.code
                    """
            );

            assertEquals(
                    "p.code\n'P2_REMOVE_NO_RETURN'",
                    existing
            );
        }
    }

    // =====================================================
    // TEST 5 - OLD PROPERTY BINDING
    // =====================================================

    @Test
    void shouldNotReusePropertyBindingAfterRemove()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (p:Product {
                        code: 'P2_REMOVE_BINDING',
                        discount: 10
                    })
                    """
            );

            String result = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_BINDING',
                        discount: 10
                    })
                    REMOVE p.discount
                    RETURN p.discount
                    """
            );

            assertEquals(
                    "p.discount",
                    result,
                    "RETURN must not reuse the removed value."
            );

            String later = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_BINDING'
                    })
                    RETURN p.discount
                    """
            );

            assertEquals(
                    "p.discount",
                    later,
                    "The removed value must not reappear."
            );
        }
    }

    // =====================================================
    // TEST 6 - NONEXISTENT PROPERTY
    // =====================================================

    @Test
    void shouldPreserveNodeWhenPropertyDoesNotExist()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (p:Product {
                        code: 'P2_REMOVE_MISSING',
                        price: 50
                    })
                    """
            );

            String updated = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_REMOVE_MISSING'
                    })
                    REMOVE p.unknown
                    RETURN p.price
                    """
            );

            assertEquals(
                    "p.price\n50",
                    updated,
                    "Removing an absent property must preserve "
                            + "the other properties."
            );
        }
    }

    // =====================================================
    // TEST 7 - ORIGINAL PROFESSOR SAMPLE GRAPH
    // =====================================================

    @Test
    void shouldRemovePropertyFromOriginalSampleNode()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String original = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    RETURN a.age
                    """
            );

            assertEquals(
                    "a.age\n30",
                    original,
                    "The original graph must contain age 30."
            );

            String updated = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    REMOVE a.age
                    RETURN a.name
                    """
            );

            assertEquals(
                    "a.name\n'Juan Perez'",
                    updated,
                    "REMOVE must preserve the original node."
            );

            String removed = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    RETURN a.age
                    """
            );

            assertEquals(
                    "a.age",
                    removed,
                    "The removed original property must be absent."
            );
        }
    }
}
