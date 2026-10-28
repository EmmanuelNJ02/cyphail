
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
 *
 * Unit tests for REMOVE code generation.
 *
 * Coverage:
 * - Node property removal.
 * - Relationship property removal.
 * - Multiple property removals.
 * - REMOVE without RETURN.
 * - CREATE followed by REMOVE.
 * - SET followed by REMOVE.
 * - Property binding invalidation.
 * - Undefined variable rejection.
 * - Mixed node and relationship removals.
 *
 * These tests validate generated Prolog goals.
 * Actual graph mutations must also be tested
 * separately through SWI-Prolog/MQI.
 */
class PrologCodeGeneratorRemoveTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Parses a Cyphail statement and generates
     * its executable Prolog goal.
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
     * Official P2 example:
     *
     * MATCH (p:Product {code: 'PROD-99'})
     * REMOVE p.discount
     * RETURN p.id
     */
    @Test
    void shouldGenerateRemoveNodeProperty() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Product {code: 'PROD-99'})
                REMOVE p.discount
                RETURN p.id
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "cy_remove_node_property("
                                + "amigos, __P, 'discount')"
                )
        );

        assertFalse(
                goal.contains(
                        "cy_remove_edge_property("
                )
        );

        assertEquals(
                List.of("p.id"),
                result.columnHeaders()
        );
    }

    /**
     * REMOVE on a relationship must call
     * the relationship property predicate.
     */
    @Test
    void shouldGenerateRemoveRelationshipProperty() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)
                      -[r:KNOWS]->
                      (b:Person)
                REMOVE r.since
                RETURN r
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "cy_remove_edge_property("
                                + "amigos, __R, 'since')"
                )
        );

        assertEquals(
                List.of("r"),
                result.columnHeaders()
        );
    }

    /**
     * Multiple property removals must preserve
     * their original source order.
     */
    @Test
    void shouldGenerateMultipleRemovePropertiesInOrder() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Product)
                REMOVE p.discount, p.oldPrice
                RETURN p
                """
        );

        String goal = result.goal();

        String first =
                "cy_remove_node_property("
                        + "amigos, __P, 'discount')";

        String second =
                "cy_remove_node_property("
                        + "amigos, __P, 'oldPrice')";

        assertTrue(goal.contains(first));

        assertTrue(goal.contains(second));

        assertTrue(
                goal.indexOf(first) < goal.indexOf(second),
                "REMOVE operations must preserve source order."
        );
    }

    /**
     * A mutation does not require RETURN.
     */
    @Test
    void shouldGenerateRemoveWithoutReturn() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Product)
                REMOVE p.discount
                """
        );

        assertTrue(
                result.goal().contains(
                        "cy_remove_node_property("
                                + "amigos, __P, 'discount')"
                )
        );

        assertFalse(
                result.hasOutputColumns()
        );
    }

    /**
     * CREATE must execute before REMOVE.
     */
    @Test
    void shouldGenerateCreateThenRemove() {

        PrologGenerationResult result = generate(
                """
                CREATE (p:Product {
                    code: 'P2-REMOVE',
                    discount: 10
                })
                REMOVE p.discount
                RETURN p
                """
        );

        String goal = result.goal();

        int createPosition =
                goal.indexOf("cy_create_node(");

        int removePosition =
                goal.indexOf(
                        "cy_remove_node_property("
                );

        assertTrue(createPosition >= 0);

        assertTrue(removePosition >= 0);

        assertTrue(
                createPosition < removePosition,
                "CREATE must execute before REMOVE."
        );

        assertEquals(
                List.of("p"),
                result.columnHeaders()
        );
    }

    /**
     * SET and REMOVE must execute in source order.
     */
    @Test
    void shouldGenerateSetThenRemove() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Product)
                SET p.discount = 10
                REMOVE p.discount
                RETURN p
                """
        );

        String goal = result.goal();

        int setPosition =
                goal.indexOf(
                        "cy_set_node_property("
                );

        int removePosition =
                goal.indexOf(
                        "cy_remove_node_property("
                );

        assertTrue(setPosition >= 0);

        assertTrue(removePosition >= 0);

        assertTrue(
                setPosition < removePosition,
                "SET must execute before REMOVE."
        );
    }

    /**
     * REMOVE invalidates an old property binding.
     *
     * This test checks the generated Prolog
     * variable mappings. It does not prescribe
     * the value returned for a missing property.
     */
    @Test
    void shouldInvalidateOldPropertyBinding() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Product {
                    code: 'PROD-99',
                    discount: 10
                })
                REMOVE p.discount
                RETURN p
                """
        );

        assertTrue(
                result.goal().contains(
                        "cy_remove_node_property("
                                + "amigos, __P, 'discount')"
                )
        );

        assertNotEquals(
                "__P_DISCOUNT",
                result.propertyMappings().get(
                        "p.discount"
                ),
                "REMOVE must invalidate the old property mapping."
        );
    }

    /**
     * A REMOVE target must be introduced by
     * MATCH or CREATE before being used.
     */
    @Test
    void shouldRejectUndefinedRemoveVariable() {

        assertThrows(
                IllegalArgumentException.class,
                () -> generate(
                        """
                        MATCH (p:Product)
                        REMOVE q.discount
                        RETURN p
                        """
                )
        );
    }

    /**
     * A single REMOVE clause can target both
     * node properties and relationship properties.
     */
    @Test
    void shouldGenerateMixedNodeAndRelationshipRemove() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)
                      -[r:KNOWS]->
                      (b:Person)
                REMOVE a.oldCode, r.since
                RETURN a, r
                """
        );

        String goal = result.goal();

        String nodeRemoval =
                "cy_remove_node_property("
                        + "amigos, __A, 'oldCode')";

        String edgeRemoval =
                "cy_remove_edge_property("
                        + "amigos, __R, 'since')";

        assertTrue(goal.contains(nodeRemoval));

        assertTrue(goal.contains(edgeRemoval));

        assertTrue(
                goal.indexOf(nodeRemoval)
                        < goal.indexOf(edgeRemoval),
                "Node and relationship removals must "
                        + "preserve their source order."
        );

        assertEquals(
                List.of("a", "r"),
                result.columnHeaders()
        );
    }
}
