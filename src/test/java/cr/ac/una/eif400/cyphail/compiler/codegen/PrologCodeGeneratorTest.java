
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import org.junit.jupiter.api.Test;

import java.util.List;
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
 * Unit tests for the initial Prolog code generator.
 *
 * Tests parse real Cyphail source statements and verify
 * that the AST visitor produces the expected Prolog
 * goals, symbol mappings, and RETURN metadata.
 *
 * Unsupported P2 features must fail explicitly until
 * their corresponding code generation is implemented.
 */
class PrologCodeGeneratorTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    private PrologGenerationResult generate(
            String source
    ) {
        Query query = parser.parseOrThrow(source);
        return generator.generate(query);
    }

    @Test
    void shouldGeneratePrologForNodeAndProperty() {

        PrologGenerationResult result = generate(
                "MATCH (a:Persona) RETURN a.nombre"
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_node(__A)"
                )
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_label(__A, 'Persona')"
                )
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_prop(__A, 'nombre', __A_NOMBRE)"
                )
        );

        assertTrue(
                result.goal().contains("cy_create_query")
        );

        assertTrue(
                result.goal().contains("cy_exec_query")
        );

        assertTrue(
                result.goal().contains("cy_erase_query")
        );

        assertEquals(
                List.of("a.nombre"),
                result.columnHeaders()
        );

        assertEquals(
                Map.of("a", "__A"),
                result.variableMappings()
        );
    }

    @Test
    void shouldReturnNodeIdentifierForPatternVariable() {

        PrologGenerationResult result = generate(
                "MATCH (a:Persona) RETURN a"
        );

        assertEquals(
                List.of("a"),
                result.columnHeaders()
        );

        assertEquals(
                "__A",
                result.columns()
                        .getFirst()
                        .prologVariable()
        );

        assertTrue(
                result.goal().contains(
                        "cyphail_query_p2(__A)"
                )
        );

        assertTrue(result.hasOutputColumns());
    }

    @Test
    void shouldPreserveProjectionOrderAndAliases() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Persona), (b:Empresa)
                RETURN b.nombre AS empresa,
                       a.nombre AS persona
                """
        );

        assertEquals(
                List.of("empresa", "persona"),
                result.columnHeaders()
        );

        assertEquals(
                "__B_NOMBRE",
                result.columns()
                        .get(0)
                        .prologVariable()
        );

        assertEquals(
                "__A_NOMBRE",
                result.columns()
                        .get(1)
                        .prologVariable()
        );

        assertTrue(
                result.goal().contains(
                        "cyphail_query_p2(__B_NOMBRE, __A_NOMBRE)"
                )
        );
    }

    @Test
    void shouldGenerateLiteralPropertyConstraint() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Persona {nombre: 'Marta'})
                RETURN a.nombre
                """
        );

        assertTrue(
                result.goal().contains(
                        "cy_eq(__A_NOMBRE, 'Marta')"
                )
        );

        assertEquals(
                "__A_NOMBRE",
                result.propertyMappings()
                        .get("a.nombre")
        );
    }

    @Test
    void shouldGenerateAllNodeLabels() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Persona:Empleado)
                RETURN a
                """
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_label(__A, 'Persona')"
                )
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_label(__A, 'Empleado')"
                )
        );
    }

    @Test
    void shouldKeepQueryVariableMappingsIndependent() {

        PrologGenerationResult first = generate(
                "MATCH (a:Persona) RETURN a"
        );

        PrologGenerationResult second = generate(
                "MATCH (b:Empresa) RETURN b"
        );

        assertEquals(
                Map.of("a", "__A"),
                first.variableMappings()
        );

        assertEquals(
                Map.of("b", "__B"),
                second.variableMappings()
        );

        assertFalse(
                second.variableMappings().containsKey("a")
        );
    }

    @Test
    void shouldRejectRelationshipPatternsUntilSupported() {

        Query query = parser.parseOrThrow(
                """
                MATCH (a:Persona)-[:TRABAJA_EN]->(b:Empresa)
                RETURN a.nombre
                """
        );

        UnsupportedOperationException exception =
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> generator.generate(query)
                );

        assertTrue(
                exception.getMessage().contains(
                        "Relationship patterns"
                )
        );
    }

    @Test
    void shouldRejectWhereUntilSupported() {

        Query query = parser.parseOrThrow(
                """
                MATCH (a:Persona)
                WHERE a.edad > 18
                RETURN a.nombre
                """
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> generator.generate(query)
        );
    }

    @Test
    void shouldRejectStandaloneCreateUntilSupported() {

        Query query = parser.parseOrThrow(
                """
                CREATE (a:Persona {nombre: 'Marta'})
                RETURN a
                """
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> generator.generate(query)
        );
    }

    @Test
    void shouldRejectSetUntilSupported() {

        Query query = parser.parseOrThrow(
                """
                MATCH (a:Persona)
                SET a.edad = 25
                RETURN a
                """
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> generator.generate(query)
        );
    }

    @Test
    void shouldRejectUnmatchedReturnVariable() {

        Query query = parser.parseOrThrow(
                "MATCH (a:Persona) RETURN b"
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> generator.generate(query)
                );

        assertTrue(
                exception.getMessage().contains(
                        "unmatched variable"
                )
        );
    }

    @Test
    void shouldRejectNullQuery() {

        assertThrows(
                NullPointerException.class,
                () -> generator.generate(null)
        );
    }
}
