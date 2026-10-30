
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import org.junit.jupiter.api.Test;

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
 * Regression tests for MATCH pattern properties
 * that reference previously declared variables.
 *
 * These tests preserve Sprint P1 compatibility
 * while validating the Sprint P2 Prolog CodeGen.
 */
class PrologCodeGeneratorMatchPropertyReferenceTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    private PrologGenerationResult generate(String source) {

        Query query = parser.parseOrThrow(source);

        return generator.generate(query);
    }

    private static int occurrences(
            String text,
            String fragment
    ) {

        int count = 0;
        int index = 0;

        while ((index = text.indexOf(fragment, index)) >= 0) {

            count++;

            index += fragment.length();
        }

        return count;
    }

    @Test
    void shouldCompileReferenceToPreviouslyDeclaredProperty() {

        PrologGenerationResult result = assertDoesNotThrow(
                () -> generate("""
                        MATCH (p:Person {id: 1}),
                              (o:Order {personId: p.id})
                        RETURN p.name, o.total
                        """)
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__P, 'id', __P_ID)"
                )
        );

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__O, 'personId',"
                )
        );

        assertTrue(
                goal.contains(
                        "cy_eq(__P_ID, 1)"
                )
        );

        assertTrue(
                goal.contains(
                        "cy_eq(__O_PERSONID, __P_ID)"
                )
        );

        assertEquals(
                1,
                occurrences(
                        goal,
                        "user_amigos_prop(__P, 'id',"
                )
        );
    }

    @Test
    void shouldGeneratePropertyLookupWhenReferenceIsNotBoundYet() {

        PrologGenerationResult result = assertDoesNotThrow(
                () -> generate("""
                        MATCH (p:Person),
                              (o:Order {personId: p.id})
                        RETURN o
                        """)
        );

        String goal = result.goal();

        String lookup =
                "user_amigos_prop(__P, 'id', __P_ID)";

        String comparison =
                "cy_eq(__O_PERSONID, __P_ID)";

        assertTrue(goal.contains(lookup));

        assertTrue(goal.contains(comparison));

        assertTrue(
                goal.indexOf(lookup)
                        < goal.indexOf(comparison)
        );

        assertEquals(
                1,
                occurrences(goal, lookup)
        );
    }

    @Test
    void shouldPreserveLiteralPatternProperties() {

        PrologGenerationResult result = assertDoesNotThrow(
                () -> generate("""
                        MATCH (p:Product {
                            code: 'PROD-99',
                            precio: 19.99,
                            cantidad: 5
                        })
                        RETURN p
                        """)
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "cy_eq(__P_CODE, 'PROD-99')"
                )
        );

        assertTrue(
                goal.contains(
                        "cy_eq(__P_PRECIO, 19.99)"
                )
        );

        assertTrue(
                goal.contains(
                        "cy_eq(__P_CANTIDAD, 5)"
                )
        );
    }

    @Test
    void shouldRejectPropertyReferenceBeforeVariableDeclaration() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generate("""
                        MATCH (o:Order {personId: p.id}),
                              (p:Person)
                        RETURN o
                        """)
        );

        assertTrue(
                exception.getMessage().contains(
                        "unmatched variable: p"
                )
        );
    }
}
