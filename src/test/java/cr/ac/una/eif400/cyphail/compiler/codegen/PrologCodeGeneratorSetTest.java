
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;

import org.junit.jupiter.api.Test;

import java.util.List;

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
 * Julissa Solano Valverde
 * Valery Alfaro Morales
 * Keynell Molina Mora
 * Roy Arias Mejia
 *
 * Unit and regression tests for SET CodeGen.
 *
 * Covered functionality:
 * - SET node properties.
 * - SET relationship properties.
 * - Decimal property values.
 * - Multiple assignments.
 * - CREATE followed by SET.
 * - RETURN of updated property values.
 * - References to existing properties.
 * - Rebinding a previously read property.
 * - Rejection of undefined variables.
 *
 * Real execution will be tested separately
 * through SWI-Prolog and MQI.
 */
class PrologCodeGeneratorSetTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Parses and generates an executable goal.
     */
    private PrologGenerationResult generate(
            String source
    ) {

        Query query = parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result);

        assertNotNull(result.goal());

        assertFalse(result.goal().isBlank());

        return result;
    }

    /**
     * Verifies SET of a decimal node property.
     */
    @Test
    void shouldGenerateSetNodeProperty() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Product {code: 'PROD-99'})
                SET p.precio = 19.99
                RETURN p
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "cy_set_node_property("
                )
        );

        assertTrue(
                goal.contains(
                        "cy_set_node_property("
                                + "amigos, __P, 'precio', 19.99)"
                )
        );

        assertEquals(
                List.of("p"),
                result.columnHeaders()
        );
    }

    /**
     * Verifies SET of a relationship property.
     */
    @Test
    void shouldGenerateSetRelationshipProperty() {

        PrologGenerationResult result = generate(
                """
                MATCH (u:Student)
                      -[r:attends]->
                      (c:Course)
                SET r.grade = 9.5
                RETURN r
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "cy_set_edge_property("
                                + "amigos, __R, 'grade', 9.5)"
                )
        );

        assertEquals(
                List.of("r"),
                result.columnHeaders()
        );
    }

    /**
     * Multiple SET assignments must preserve
     * their original source order.
     */
    @Test
    void shouldGenerateMultipleSetAssignments() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Product)
                SET p.precio = 19.99,
                    p.estado = 'activo'
                RETURN p
                """
        );

        String goal = result.goal();

        String priceGoal =
                "cy_set_node_property("
                        + "amigos, __P, 'precio', 19.99)";

        String statusGoal =
                "cy_set_node_property("
                        + "amigos, __P, 'estado', 'activo')";

        assertTrue(goal.contains(priceGoal));

        assertTrue(goal.contains(statusGoal));

        assertTrue(
                goal.indexOf(priceGoal)
                        < goal.indexOf(statusGoal),
                "SET assignments must preserve source order."
        );
    }

    /**
     * CREATE followed by SET must create
     * the node before updating its property.
     */
    @Test
    void shouldGenerateCreateFollowedBySet() {

        PrologGenerationResult result = generate(
                """
                CREATE (p:Product {code: 'PROD-99'})
                SET p.precio = 19.99
                RETURN p
                """
        );

        String goal = result.goal();

        int createPosition =
                goal.indexOf("cy_create_node(");

        int setPosition =
                goal.indexOf("cy_set_node_property(");

        assertTrue(createPosition >= 0);

        assertTrue(setPosition >= 0);

        assertTrue(
                createPosition < setPosition,
                "CREATE must execute before SET."
        );

        assertEquals(
                List.of("p"),
                result.columnHeaders()
        );
    }

    /**
     * RETURN must retrieve the property
     * after SET has updated the node.
     */
    @Test
    void shouldReturnUpdatedProperty() {

        PrologGenerationResult result = generate(
                """
                CREATE (p:Product {code: 'P2-TEST'})
                SET p.precio = 19.99
                RETURN p.precio
                """
        );

        String goal = result.goal();

        int setPosition =
                goal.indexOf("cy_set_node_property(");

        int propertyPosition =
                goal.indexOf(
                        "user_amigos_prop("
                                + "__P, 'precio', "
                );

        assertTrue(setPosition >= 0);

        assertTrue(propertyPosition >= 0);

        assertTrue(
                setPosition < propertyPosition,
                "RETURN must read the updated property."
        );

        assertEquals(
                List.of("p.precio"),
                result.columnHeaders()
        );
    }

    /**
     * A SET assignment can use a property
     * obtained from another bound variable.
     */
    @Test
    void shouldSetPropertyUsingExistingProperty() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person), (b:Person)
                SET a.edad = b.age
                RETURN a
                """
        );

        String goal = result.goal();

        String lookup =
                "user_amigos_prop("
                        + "__B, 'age', __B_AGE)";

        String update =
                "cy_set_node_property("
                        + "amigos, __A, 'edad', __B_AGE)";

        assertTrue(goal.contains(lookup));

        assertTrue(goal.contains(update));

        assertTrue(
                goal.indexOf(lookup)
                        < goal.indexOf(update),
                "The source property must be read before SET."
        );
    }

    /**
     * SET must not cause RETURN to reuse
     * a stale property binding from MATCH.
     *
     * This regression test verifies that
     * RETURN obtains a fresh value after
     * the update operation.
     */
    @Test
    void shouldNotReuseOldPropertyValueAfterSet() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Product {
                    code: 'PROD-99',
                    precio: 10
                })
                SET p.precio = 19.99
                RETURN p.precio
                """
        );

        String goal = result.goal();

        int updatePosition =
                goal.indexOf(
                        "cy_set_node_property("
                );

        int latestPropertyLookup =
                goal.lastIndexOf(
                        "user_amigos_prop("
                                + "__P, 'precio', "
                );

        assertTrue(updatePosition >= 0);

        assertTrue(
                latestPropertyLookup > updatePosition,
                "RETURN must read the property after SET, "
                        + "instead of reusing its old MATCH value."
        );
    }

    /**
     * An undefined SET target must not
     * generate a graph mutation.
     */
    @Test
    void shouldRejectUndefinedSetTarget() {

        assertThrows(
                IllegalArgumentException.class,
                () -> generate(
                        """
                        MATCH (p:Product)
                        SET q.precio = 19.99
                        RETURN p
                        """
                )
        );
    }
}
