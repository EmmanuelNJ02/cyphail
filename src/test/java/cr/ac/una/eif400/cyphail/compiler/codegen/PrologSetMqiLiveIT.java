
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
 * Real SWI-Prolog and MQI integration tests for SET.
 *
 * Covered functionality:
 *
 * - CREATE followed by multiple SET assignments.
 * - SET of an existing node property.
 * - RETURN of the updated property value.
 * - SET of a relationship property.
 * - SET using a property from a MATCH variable.
 * - SET without RETURN.
 * - SET on original sample graph nodes.
 * - SET on original sample graph relationships.
 * - Persistence across queries in one MQI session.
 *
 * Every test uses an independent SWI-Prolog process.
 *
 * Run explicitly with:
 *
 * mvn -Dtest=PrologSetMqiLiveIT test
 */
class PrologSetMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Returns the path to the real P2 Prolog engine.
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
     * Converts Cyphail source into an executable
     * SWI-Prolog goal.
     */
    private String compile(String source) {

        Query query =
                parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result);

        assertNotNull(result.goal());

        assertFalse(
                result.goal().isBlank()
        );

        return result.goal();
    }

    /**
     * Executes a generated Prolog goal using
     * an already open MQI session.
     *
     * Returns the table contained in Rows.
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
                        "The Prolog goal must succeed. Actual: "
                                + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "The query must return one table."
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
     * Parses, compiles and executes Cyphail
     * source using the same MQI session.
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
    // TEST 1 - CREATE FOLLOWED BY MULTIPLE SET
    // =====================================================

    /**
     * Verifies two SET assignments in source order.
     *
     * The values must be returned immediately
     * and persist for a subsequent MATCH query.
     *
     * The SWI-Prolog formatter represents the
     * simple atom activo without quotation marks.
     */
    @Test
    void shouldCreateAndSetMultipleNodeProperties()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String updated = run(
                    broker,
                    """
                    CREATE (p:Product {
                        code: 'P2_SET_MULTIPLE'
                    })
                    SET p.precio = 19.99,
                        p.estado = 'activo'
                    RETURN p.precio, p.estado
                    """
            );

            assertEquals(
                    "p.precio p.estado\n19.99 activo",
                    updated,
                    "SET must return both updated properties."
            );

            String found = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_SET_MULTIPLE'
                    })
                    RETURN p.precio, p.estado
                    """
            );

            assertEquals(
                    "p.precio p.estado\n19.99 activo",
                    found,
                    "Both values must persist in the graph."
            );
        }
    }

    // =====================================================
    // TEST 2 - EXISTING NODE PROPERTY AND REBINDING
    // =====================================================

    /**
     * Verifies that MATCH may read an old value,
     * SET can replace it, and RETURN obtains
     * the new value rather than the old binding.
     *
     * Also verifies that the property update
     * persists in the graph storage.
     */
    @Test
    void shouldReplaceMatchedPropertyAndReturnNewValue()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (p:Product {
                        code: 'P2_SET_REBIND',
                        precio: 10
                    })
                    """
            );

            String updated = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_SET_REBIND',
                        precio: 10
                    })
                    SET p.precio = 19.99
                    RETURN p.precio
                    """
            );

            assertEquals(
                    "p.precio\n19.99",
                    updated,
                    "RETURN must use the value after SET."
            );

            String found = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_SET_REBIND'
                    })
                    RETURN p.precio
                    """
            );

            assertEquals(
                    "p.precio\n19.99",
                    found,
                    "MATCH must retrieve the updated value."
            );
        }
    }

    // =====================================================
    // TEST 3 - RELATIONSHIP PROPERTY UPDATE
    // =====================================================

    /**
     * Verifies SET on a relationship created
     * in the same statement.
     *
     * The modified decimal grade must persist
     * and be returned by a subsequent MATCH.
     */
    @Test
    void shouldSetRelationshipProperty()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String updated = run(
                    broker,
                    """
                    CREATE (u:Student {
                        id: 'P2_SET_STUDENT'
                    })
                    -[r:attends {grade: 7.0}]->
                    (c:Course {
                        nrc: 'P2_SET_COURSE'
                    })
                    SET r.grade = 9.5
                    RETURN r.grade
                    """
            );

            assertEquals(
                    "r.grade\n9.5",
                    updated,
                    "SET must update the relationship grade."
            );

            String found = run(
                    broker,
                    """
                    MATCH (u:Student {
                        id: 'P2_SET_STUDENT'
                    })
                    -[r:attends]->
                    (c:Course {
                        nrc: 'P2_SET_COURSE'
                    })
                    RETURN r.grade
                    """
            );

            assertEquals(
                    "r.grade\n9.5",
                    found,
                    "The updated edge property must persist."
            );
        }
    }

    // =====================================================
    // TEST 4 - VALUE FROM A MATCH PROPERTY
    // =====================================================

    /**
     * A SET assignment can use a property
     * belonging to another matched node.
     *
     * The original sample graph contains:
     *
     * Juan Perez, age 30.
     *
     * The property must be copied into the
     * newly created Product node.
     */
    @Test
    void shouldSetPropertyFromMatchedNode()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String updated = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    CREATE (p:Product {
                        code: 'P2_SET_FROM_MATCH'
                    })
                    SET p.edad = a.age
                    RETURN p.edad
                    """
            );

            assertEquals(
                    "p.edad\n30",
                    updated,
                    "SET must reuse the matched age value."
            );

            String found = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_SET_FROM_MATCH'
                    })
                    RETURN p.edad
                    """
            );

            assertEquals(
                    "p.edad\n30",
                    found,
                    "The referenced value must persist."
            );
        }
    }

    // =====================================================
    // TEST 5 - SET WITHOUT RETURN
    // =====================================================

    /**
     * SET without RETURN must still modify
     * an existing graph node.
     *
     * A subsequent query verifies persistence.
     */
    @Test
    void shouldSetPropertyWithoutReturn()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (p:Product {
                        code: 'P2_SET_NO_RETURN',
                        precio: 5
                    })
                    """
            );

            run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_SET_NO_RETURN'
                    })
                    SET p.precio = 12.5
                    """
            );

            String found = run(
                    broker,
                    """
                    MATCH (p:Product {
                        code: 'P2_SET_NO_RETURN'
                    })
                    RETURN p.precio
                    """
            );

            assertEquals(
                    "p.precio\n12.5",
                    found,
                    "SET without RETURN must persist changes."
            );
        }
    }

    // =====================================================
    // TEST 6 - UPDATE ORIGINAL SAMPLE GRAPH NODE
    // =====================================================

    /**
     * Verifies that SET also works on nodes
     * originally supplied by engine.pl.
     *
     * This is an important compatibility case:
     *
     * The original graph uses Prolog facts,
     * whereas CREATE uses the recorded database.
     *
     * The engine must support updating both kinds.
     *
     * Original sample:
     *
     * Juan Perez has age 30.
     *
     * Expected result after SET:
     *
     * Juan Perez has age 31.
     *
     * This regression test protects support
     * for mutations of original graph nodes.
     */
    @Test
    void shouldUpdateOriginalSampleGraphNode()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String updated = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    SET a.age = 31
                    RETURN a.age
                    """
            );

            assertEquals(
                    "a.age\n31",
                    updated,
                    "SET must also support original graph nodes."
            );

            String found = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    RETURN a.age
                    """
            );

            assertEquals(
                    "a.age\n31",
                    found,
                    "The original node must retain its update."
            );
        }
    }

    // =====================================================
    // TEST 7 - UPDATE ORIGINAL SAMPLE GRAPH RELATIONSHIP
    // =====================================================

    /**
     * Verifies SET on an original relationship from engine.pl.
     *
     * The original sample graph contains an Amigo relationship
     * e1 between Juan Perez (id1) and Jorge Soto (id2).
     * Unlike CREATE relationships, e1 is not stored in the
     * SWI-Prolog recorded database.
     *
     * The test verifies creation of a property, persistence
     * across queries, replacement of its value and that
     * the original relationship keeps its identifier.
     */
    @Test
    void shouldSetAndReplacePropertyOnOriginalSampleRelationship()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String updated = run(
                    broker,
                    """
                    MATCH (a:Person {name: 'Juan Perez'})
                          -[r:Amigo]->
                          (b:Person {name: 'Jorge Soto'})
                    SET r.since = 2020
                    RETURN r, r.since
                    """
            );

            assertEquals(
                    "r r.since\ne1 2020",
                    updated,
                    "SET must support original sample relationships."
            );

            String found = run(
                    broker,
                    """
                    MATCH (a:Person {name: 'Juan Perez'})
                          -[r:Amigo]->
                          (b:Person {name: 'Jorge Soto'})
                    RETURN r, r.since
                    """
            );

            assertEquals(
                    "r r.since\ne1 2020",
                    found,
                    "The new edge property must persist across queries."
            );

            String replaced = run(
                    broker,
                    """
                    MATCH (a:Person {name: 'Juan Perez'})
                          -[r:Amigo]->
                          (b:Person {name: 'Jorge Soto'})
                    SET r.since = 2024
                    RETURN r, r.since
                    """
            );

            assertEquals(
                    "r r.since\ne1 2024",
                    replaced,
                    "SET must replace the existing edge property."
            );

            String foundAgain = run(
                    broker,
                    """
                    MATCH (a:Person {name: 'Juan Perez'})
                          -[r:Amigo]->
                          (b:Person {name: 'Jorge Soto'})
                    RETURN r, r.since
                    """
            );

            assertEquals(
                    "r r.since\ne1 2024",
                    foundAgain,
                    "The original edge must retain the new value and ID."
            );
        }
    }
}
