
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
 * Regression tests for CREATE code generation.
 *
 * Covered cases:
 * - Standalone CREATE with RETURN.
 * - Standalone CREATE without RETURN.
 * - MATCH followed by CREATE.
 * - CREATE with two nodes and a relationship.
 * - Multiple CREATE clauses.
 *
 * These tests define the required behavior before
 * CREATE code generation is implemented.
 */
class PrologCodeGeneratorCreateTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Parses a Cyphail statement and generates Prolog.
     */
    private PrologGenerationResult generate(
            String source
    ) {

        Query query = parser.parseOrThrow(source);

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
     * Counts exact occurrences of text.
     */
    private static int occurrences(
            String source,
            String fragment
    ) {

        int count = 0;
        int position = 0;

        while ((position = source.indexOf(
                fragment,
                position
        )) >= 0) {

            count++;

            position += fragment.length();
        }

        return count;
    }

    /**
     * A standalone CREATE statement must create
     * a new node and expose its variable to RETURN.
     */
    @Test
    void shouldGenerateStandaloneCreateNodeWithReturn() {

        PrologGenerationResult result = generate(
                """
                CREATE (p:Person {
                    name: 'Lucia',
                    age: 21
                })
                RETURN p
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains("cy_create_node("),
                "CREATE must call the Prolog node creation predicate."
        );

        assertTrue(
                goal.contains("'Person'"),
                "The node label must be preserved."
        );

        assertTrue(
                goal.contains("'name'"),
                "The name property must be preserved."
        );

        assertTrue(
                goal.contains("'Lucia'"),
                "The name value must be preserved."
        );

        assertTrue(
                goal.contains("21"),
                "The age value must be preserved."
        );

        assertEquals(
                List.of("p"),
                result.columnHeaders()
        );

        assertEquals(
                "__P",
                result.variableMappings().get("p")
        );
    }

    /**
     * CREATE without RETURN is a valid P2 statement.
     *
     * The command must still execute the creation
     * operation even without output columns.
     */
    @Test
    void shouldGenerateStandaloneCreateWithoutReturn() {

        PrologGenerationResult result = generate(
                """
                CREATE (p:Person {
                    name: 'Lucia'
                })
                """
        );

        assertTrue(
                result.goal().contains("cy_create_node("),
                "CREATE without RETURN must generate an executable operation."
        );

        assertFalse(
                result.hasOutputColumns(),
                "A query without RETURN must not invent output columns."
        );
    }

    /**
     * MATCH variables can be referenced by a
     * following CREATE clause.
     *
     * The source property must be retrieved
     * before the new node is created.
     */
    @Test
    void shouldGenerateCreateAfterMatch() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person {
                    name: 'Juan Perez'
                })
                CREATE (c:Certificate {
                    issuedTo: a.name,
                    year: 2026
                })
                RETURN c
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains("user_amigos_node("),
                "MATCH must execute before CREATE."
        );

        assertTrue(
                goal.contains("cy_create_node("),
                "The new node must be created."
        );

        assertTrue(
                goal.contains("__A_NAME"),
                "The matched node property must be reusable."
        );

        assertTrue(
                goal.contains("'Certificate'"),
                "The created node label must be preserved."
        );

        assertEquals(
                List.of("c"),
                result.columnHeaders()
        );

        assertTrue(
                goal.indexOf("user_amigos_node(")
                        < goal.indexOf("cy_create_node("),
                "MATCH must precede CREATE."
        );
    }

    /**
     * Creating a relationship between two new nodes
     * requires two node creation operations and
     * one edge creation operation.
     */
    @Test
    void shouldGenerateCreateRelationship() {

        PrologGenerationResult result = generate(
                """
                CREATE (a:Person {name: 'Ana'})
                       -[r:Amigo {since: 2026}]->
                       (b:Person {name: 'Luis'})
                RETURN a, r, b
                """
        );

        String goal = result.goal();

        assertEquals(
                2,
                occurrences(goal, "cy_create_node("),
                "Two nodes must be created."
        );

        assertEquals(
                1,
                occurrences(goal, "cy_create_edge("),
                "One relationship must be created."
        );

        assertTrue(
                goal.contains("'Amigo'"),
                "The relationship type must be preserved."
        );

        assertTrue(
                goal.contains("'since'"),
                "Relationship properties must be preserved."
        );

        assertEquals(
                List.of("a", "r", "b"),
                result.columnHeaders()
        );

        int firstNode =
                goal.indexOf("cy_create_node(");

        int edge =
                goal.indexOf("cy_create_edge(");

        assertTrue(
                firstNode >= 0 && edge > firstNode,
                "Nodes must be created before the relationship."
        );
    }

    /**
     * Consecutive CREATE clauses must preserve
     * their source execution order.
     */
    @Test
    void shouldGenerateMultipleCreateClauses() {

        PrologGenerationResult result = generate(
                """
                CREATE (a:Person {name: 'Ana'})
                CREATE (b:Person {name: 'Luis'})
                RETURN a, b
                """
        );

        String goal = result.goal();

        assertEquals(
                2,
                occurrences(goal, "cy_create_node("),
                "Both CREATE clauses must execute."
        );

        assertEquals(
                List.of("a", "b"),
                result.columnHeaders()
        );

        assertNotEquals(
                result.variableMappings().get("a"),
                result.variableMappings().get("b"),
                "Each source variable must have a distinct Prolog symbol."
        );
    }
}
