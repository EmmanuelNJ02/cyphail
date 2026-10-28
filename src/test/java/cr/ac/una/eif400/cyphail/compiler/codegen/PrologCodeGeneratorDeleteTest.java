
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
 * Unit tests for DELETE CodeGen.
 *
 * Coverage:
 *
 * 1. Official P2 relationship DELETE.
 * 2. Node DELETE without RETURN.
 * 3. DETACH DELETE of nodes.
 * 4. Multiple relationship deletions.
 * 5. Relationship deletion before node deletion.
 * 6. DELETE after standalone CREATE.
 * 7. DELETE after CREATE of a relationship.
 * 8. Incoming relationship DELETE.
 * 9. Undefined DELETE variable.
 * 10. DETACH DELETE of a relationship.
 *
 * These tests validate generated Prolog goals.
 *
 * Real graph mutations must be verified
 * separately using SWI-Prolog and MQI.
 */
class PrologCodeGeneratorDeleteTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Parses Cyphail source and generates
     * a complete Prolog execution goal.
     */
    private PrologGenerationResult generate(
            String source
    ) {

        Query query =
                parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result);

        assertNotNull(result.goal());

        assertFalse(
                result.goal().isBlank(),
                "Generated Prolog goal cannot be empty."
        );

        return result;
    }

    /**
     * Checks that one goal appears before another.
     */
    private static void assertGoalOrder(
            String completeGoal,
            String first,
            String second
    ) {

        int firstPosition =
                completeGoal.indexOf(first);

        int secondPosition =
                completeGoal.indexOf(second);

        assertTrue(
                firstPosition >= 0,
                "Missing first goal: " + first
        );

        assertTrue(
                secondPosition >= 0,
                "Missing second goal: " + second
        );

        assertTrue(
                firstPosition < secondPosition,
                "Goals must preserve the expected "
                        + "execution order."
        );
    }

    // =====================================================
    // TEST 1 - OFFICIAL P2 EXAMPLE
    // =====================================================

    /**
     * Official P2 example:
     *
     * MATCH (a:Person:Female)-[r:KNOWS]->(b:Person:Male)
     * WHERE r.since < 2000
     * DELETE r
     *
     * WHERE must execute before DELETE.
     */
    @Test
    void shouldGenerateOfficialRelationshipDelete() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person:Female)
                      -[r:KNOWS]->
                      (b:Person:Male)
                WHERE r.since < 2000
                DELETE r
                """
        );

        String goal = result.goal();

        String delete =
                "cy_delete_edge(amigos, __R)";

        assertTrue(
                goal.contains(delete)
        );

        assertFalse(
                goal.contains(
                        "cy_delete_node(amigos, __R)"
                )
        );

        assertGoalOrder(
                goal,
                "user_amigos_prop(__R, 'since', ",
                delete
        );

        assertFalse(
                result.hasOutputColumns()
        );
    }

    // =====================================================
    // TEST 2 - NODE DELETE
    // =====================================================

    /**
     * A normal DELETE of a node must call
     * cy_delete_node/2.
     */
    @Test
    void shouldGenerateNodeDeleteWithoutReturn() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Person)
                DELETE p
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "cy_delete_node(amigos, __P)"
                )
        );

        assertFalse(
                goal.contains(
                        "cy_detach_delete_node("
                )
        );

        assertFalse(
                result.hasOutputColumns()
        );

        assertEquals(
                "__P",
                result.variableMappings().get("p")
        );
    }

    // =====================================================
    // TEST 3 - DETACH DELETE NODE
    // =====================================================

    /**
     * DETACH DELETE of a node must use
     * the dedicated detach deletion predicate.
     */
    @Test
    void shouldGenerateDetachDeleteNode() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Person)
                DETACH DELETE p
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "cy_detach_delete_node(amigos, __P)"
                )
        );

        assertFalse(
                goal.contains(
                        "cy_delete_edge(amigos, __P)"
                )
        );

        assertFalse(
                result.hasOutputColumns()
        );
    }

    // =====================================================
    // TEST 4 - MULTIPLE RELATIONSHIP DELETE
    // =====================================================

    /**
     * Multiple relationships must be deleted
     * in their original source order.
     */
    @Test
    void shouldGenerateMultipleRelationshipDeletes() {

        PrologGenerationResult result = generate(
                """
                MATCH (a)-[r:KNOWS]->(b)
                           -[s:WORKS_AT]->(c)
                DELETE r, s
                RETURN a
                """
        );

        String goal = result.goal();

        String first =
                "cy_delete_edge(amigos, __R)";

        String second =
                "cy_delete_edge(amigos, __S)";

        assertGoalOrder(
                goal,
                first,
                second
        );

        assertEquals(
                List.of("a"),
                result.columnHeaders()
        );
    }

    // =====================================================
    // TEST 5 - RELATIONSHIP BEFORE NODE
    // =====================================================

    /**
     * A mixed DELETE must generate the
     * relationship deletion before node deletion.
     *
     * This supports graph integrity constraints.
     */
    @Test
    void shouldDeleteRelationshipBeforeNode() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)
                      -[r:KNOWS]->
                      (b:Person)
                DELETE a, r
                RETURN b
                """
        );

        String goal = result.goal();

        assertGoalOrder(
                goal,
                "cy_delete_edge(amigos, __R)",
                "cy_delete_node(amigos, __A)"
        );

        assertEquals(
                List.of("b"),
                result.columnHeaders()
        );
    }

    // =====================================================
    // TEST 6 - CREATE FOLLOWED BY NODE DELETE
    // =====================================================

    /**
     * DELETE can operate on a node
     * introduced by standalone CREATE.
     */
    @Test
    void shouldGenerateCreateThenDeleteNode() {

        PrologGenerationResult result = generate(
                """
                CREATE (p:Person {
                    name: 'Temporal'
                })
                DELETE p
                """
        );

        String goal = result.goal();

        assertGoalOrder(
                goal,
                "cy_create_node(",
                "cy_delete_node(amigos, __P)"
        );

        assertFalse(
                result.hasOutputColumns()
        );
    }

    // =====================================================
    // TEST 7 - CREATE FOLLOWED BY EDGE DELETE
    // =====================================================

    /**
     * A relationship created in the same
     * statement must be available to DELETE.
     */
    @Test
    void shouldGenerateCreateThenDeleteRelationship() {

        PrologGenerationResult result = generate(
                """
                CREATE (a:Person)
                       -[r:KNOWS]->
                       (b:Person)
                DELETE r
                RETURN a, b
                """
        );

        String goal = result.goal();

        assertGoalOrder(
                goal,
                "cy_create_edge(",
                "cy_delete_edge(amigos, __R)"
        );

        assertEquals(
                List.of("a", "b"),
                result.columnHeaders()
        );

        assertEquals(
                "__R",
                result.variableMappings().get("r")
        );
    }

    // =====================================================
    // TEST 8 - INCOMING RELATIONSHIP DELETE
    // =====================================================

    /**
     * Relationship direction must not
     * change the deletion predicate.
     *
     * The relationship identifier is
     * sufficient for deletion.
     */
    @Test
    void shouldGenerateIncomingRelationshipDelete() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)
                      <-[r:KNOWS]-
                      (b:Person)
                DELETE r
                """
        );

        assertTrue(
                result.goal().contains(
                        "cy_delete_edge(amigos, __R)"
                )
        );

        assertFalse(
                result.hasOutputColumns()
        );
    }

    // =====================================================
    // TEST 9 - UNDEFINED DELETE VARIABLE
    // =====================================================

    /**
     * A DELETE variable must have been
     * introduced by MATCH or CREATE.
     */
    @Test
    void shouldRejectUndefinedDeleteVariable() {

        assertThrows(
                IllegalArgumentException.class,
                () -> generate(
                        """
                        MATCH (a:Person)
                        DELETE q
                        """
                )
        );
    }

    // =====================================================
    // TEST 10 - DETACH DELETE RELATIONSHIP
    // =====================================================

    /**
     * DETACH DELETE of a relationship still
     * uses cy_delete_edge/2.
     *
     * DETACH is relevant when deleting nodes
     * with attached relationships.
     */
    @Test
    void shouldGenerateDetachDeleteRelationship() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)
                      -[r:KNOWS]->
                      (b:Person)
                DETACH DELETE r
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "cy_delete_edge(amigos, __R)"
                )
        );

        assertFalse(
                goal.contains(
                        "cy_detach_delete_node("
                )
        );
    }
}
