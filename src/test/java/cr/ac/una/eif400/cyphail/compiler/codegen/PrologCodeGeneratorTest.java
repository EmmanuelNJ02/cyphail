
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
 * Unit and regression tests for PrologCodeGenerator.
 *
 * Verifies:
 * - MATCH with isolated nodes.
 * - Node labels and properties.
 * - Relationship directions OUT, IN and NONE.
 * - Anonymous and chained relationships.
 * - Comma-separated relationship paths.
 * - Relationship properties and identifiers.
 * - RETURN projections and aliases.
 * - WHERE comparisons and property references.
 * - WHERE execution order.
 * - Shared property lookup behavior.
 * - Unsupported features and invalid references.
 *
 * Real SWI-Prolog and MQI execution is tested
 * separately using integration tests.
 */
class PrologCodeGeneratorTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Parses and compiles a Cyphail query.
     */
    private PrologGenerationResult generate(
            String source
    ) {

        Query query = parser.parseOrThrow(source);

        return generator.generate(query);
    }

    /**
     * Counts exact substring occurrences.
     */
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

    // =====================================================
    // MATCH AND NODE PROPERTIES
    // =====================================================

    @Test
    void shouldGeneratePrologForNodeAndProperty() {

        PrologGenerationResult result = generate(
                "MATCH (a:Persona) RETURN a.nombre"
        );

        String goal = result.goal();

        assertTrue(
                goal.contains("user_amigos_node(__A)")
        );

        assertTrue(
                goal.contains(
                        "user_amigos_label(__A, 'Persona')"
                )
        );

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__A, 'nombre', __A_NOMBRE)"
                )
        );

        assertTrue(goal.contains("cy_create_query"));
        assertTrue(goal.contains("cy_exec_query"));
        assertTrue(goal.contains("cy_erase_query"));

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
                result.propertyMappings().get("a.nombre")
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

    // =====================================================
    // RELATIONSHIP PATTERNS
    // =====================================================

    @Test
    void shouldGenerateOutgoingRelationship() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Persona)-[r:CONOCE]->(b:Persona)
                RETURN a, r, b
                """
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_edge(__R, 'CONOCE', __A, __B)"
                )
        );

        assertEquals(
                List.of("a", "r", "b"),
                result.columnHeaders()
        );

        assertEquals(
                "__R",
                result.variableMappings().get("r")
        );

        assertTrue(
                result.goal().contains(
                        "cyphail_query_p2(__A, __R, __B)"
                )
        );
    }

    @Test
    void shouldGenerateIncomingRelationship() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Persona)<-[r:CONOCE]-(b:Persona)
                RETURN r
                """
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_edge(__R, 'CONOCE', __B, __A)"
                )
        );

        assertEquals(
                List.of("r"),
                result.columnHeaders()
        );

        assertEquals(
                "__R",
                result.columns()
                        .getFirst()
                        .prologVariable()
        );
    }

    @Test
    void shouldGenerateUndirectedRelationship() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Persona)-[r:CONOCE]-(b:Persona)
                RETURN a, r, b
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "user_amigos_edge(__R, 'CONOCE', __A, __B)"
                )
        );

        assertTrue(
                goal.contains(
                        "user_amigos_edge(__R, 'CONOCE', __B, __A)"
                )
        );

        assertTrue(
                goal.contains("dif(__A, __B)")
        );
    }

    @Test
    void shouldGenerateAnonymousRelationship() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Persona)-[:CONOCE]->(b:Persona)
                RETURN a, b
                """
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_edge(__TMP_1, 'CONOCE', __A, __B)"
                )
        );

        assertEquals(
                List.of("a", "b"),
                result.columnHeaders()
        );

        assertFalse(
                result.variableMappings().containsKey("r")
        );
    }

    @Test
    void shouldGenerateChainedRelationships() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Persona)-[r:CONOCE]->(b:Persona)
                      -[s:TRABAJA_EN]->(c:Empresa)
                RETURN a, r, b, s, c
                """
        );

        String goal = result.goal();

        String firstEdge =
                "user_amigos_edge(__R, 'CONOCE', __A, __B)";

        String secondEdge =
                "user_amigos_edge(__S, 'TRABAJA_EN', __B, __C)";

        assertTrue(goal.contains(firstEdge));
        assertTrue(goal.contains(secondEdge));

        assertTrue(
                goal.indexOf(firstEdge)
                        < goal.indexOf(secondEdge)
        );

        assertEquals(
                List.of("a", "r", "b", "s", "c"),
                result.columnHeaders()
        );
    }

    @Test
    void shouldGenerateCommaSeparatedRelationshipPaths() {

        PrologGenerationResult result = generate(
                """
                MATCH (a)-[r:CONOCE]->(b),
                      (b)-[s:TRABAJA_EN]->(c)
                RETURN a, b, c
                """
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_edge(__R, 'CONOCE', __A, __B)"
                )
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_edge(__S, 'TRABAJA_EN', __B, __C)"
                )
        );

        assertEquals(
                List.of("a", "b", "c"),
                result.columnHeaders()
        );

        assertEquals(
                5,
                result.variableMappings().size()
        );
    }

    @Test
    void shouldGenerateRelationshipPropertyConstraint() {

        PrologGenerationResult result = generate(
                """
                MATCH (a)-[r:CONOCE {since: 2020}]->(b)
                RETURN r
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "user_amigos_edge(__R, 'CONOCE', __A, __B)"
                )
        );

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__R, 'since', __R_SINCE)"
                )
        );

        assertTrue(
                goal.contains(
                        "cy_eq(__R_SINCE, 2020)"
                )
        );

        assertEquals(
                "__R_SINCE",
                result.propertyMappings().get("r.since")
        );
    }

    @Test
    void shouldReturnRelationshipPropertyWithAlias() {

        PrologGenerationResult result = generate(
                """
                MATCH (a)-[r:CONOCE]->(b)
                RETURN r.since AS desde
                """
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_prop(__R, 'since', __R_SINCE)"
                )
        );

        assertEquals(
                List.of("desde"),
                result.columnHeaders()
        );

        assertEquals(
                "r.since",
                result.columns()
                        .getFirst()
                        .sourceExpression()
        );

        assertEquals(
                "__R_SINCE",
                result.columns()
                        .getFirst()
                        .prologVariable()
        );
    }

    @Test
    void shouldReturnNodeAndRelationshipValuesTogether() {

        PrologGenerationResult result = generate(
                """
                MATCH (a)-[r:CONOCE]->(b)
                RETURN a.nombre AS origen,
                       r AS relacion,
                       b.nombre AS destino
                """
        );

        assertEquals(
                List.of("origen", "relacion", "destino"),
                result.columnHeaders()
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_prop(__A, 'nombre', __A_NOMBRE)"
                )
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_prop(__B, 'nombre', __B_NOMBRE)"
                )
        );

        assertTrue(
                result.goal().contains(
                        "cyphail_query_p2(__A_NOMBRE, __R, __B_NOMBRE)"
                )
        );
    }

    // =====================================================
    // WHERE CODE GENERATION
    // =====================================================

    @Test
    void shouldGenerateWhereGreaterThanForNodeProperty() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)
                WHERE a.age > 18
                RETURN a.name
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__A, 'age', __A_AGE)"
                )
        );

        assertTrue(
                goal.contains("number(__A_AGE)")
        );

        assertTrue(
                goal.contains("(__A_AGE > 18)")
        );

        assertEquals(
                List.of("a.name"),
                result.columnHeaders()
        );

        assertEquals(
                "__A_AGE",
                result.propertyMappings().get("a.age")
        );
    }

    @Test
    void shouldGenerateWhereEqualityWithString() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)
                WHERE a.name = 'Juan Perez'
                RETURN a
                """
        );

        assertTrue(
                result.goal().contains(
                        "cy_eq(__A_NAME, 'Juan Perez')"
                )
        );

        assertEquals(
                List.of("a"),
                result.columnHeaders()
        );
    }

    @Test
    void shouldGenerateWhereInequalityBetweenProperties() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person), (b:Person)
                WHERE a.name <> b.name
                RETURN a.name, b.name
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__A, 'name', __A_NAME)"
                )
        );

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__B, 'name', __B_NAME)"
                )
        );

        assertTrue(
                goal.contains(
                        "cy_neq(__A_NAME, __B_NAME)"
                )
        );
    }

    @Test
    void shouldGenerateWhereForRelationshipProperty() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)-[r:KNOWS]->(b:Person)
                WHERE r.since < 2000
                RETURN r
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "user_amigos_edge(__R, 'KNOWS', __A, __B)"
                )
        );

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__R, 'since', __R_SINCE)"
                )
        );

        assertTrue(
                goal.contains(
                        "(__R_SINCE < 2000)"
                )
        );

        assertEquals(
                "__R_SINCE",
                result.propertyMappings().get("r.since")
        );
    }

    @Test
    void shouldGenerateWhereWithDecimalComparison() {

        PrologGenerationResult result = generate(
                """
                MATCH (p:Product)
                WHERE p.price >= 19.99
                RETURN p
                """
        );

        String goal = result.goal();

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__P, 'price', __P_PRICE)"
                )
        );

        assertTrue(
                goal.contains(
                        "(__P_PRICE >= 19.99)"
                )
        );

        assertEquals(
                List.of("p"),
                result.columnHeaders()
        );
    }

    @Test
    void shouldPlaceWhereAfterMatchAndBeforeReturn() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)
                WHERE a.age > 18
                RETURN a.name
                """
        );

        String goal = result.goal();

        int matchPosition = goal.indexOf(
                "user_amigos_node(__A)"
        );

        int wherePosition = goal.indexOf(
                "(__A_AGE > 18)"
        );

        int returnPosition = goal.indexOf(
                "user_amigos_prop(__A, 'name', __A_NAME)"
        );

        assertTrue(matchPosition >= 0);
        assertTrue(wherePosition >= 0);
        assertTrue(returnPosition >= 0);

        assertTrue(
                matchPosition < wherePosition,
                "MATCH must precede WHERE."
        );

        assertTrue(
                wherePosition < returnPosition,
                "WHERE must precede RETURN property retrieval."
        );
    }

    @Test
    void shouldRejectUnmatchedWhereVariable() {

        Query query = parser.parseOrThrow(
                """
                MATCH (a:Person)
                WHERE missing.age > 18
                RETURN a
                """
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

    /**
     * Regression test:
     *
     * A property referenced in both WHERE and RETURN
     * should be retrieved once per matched binding.
     *
     * Repeating an identical property goal can multiply
     * query solutions when the graph contains duplicate
     * property facts.
     */
    @Test
    void shouldAvoidDuplicatePropertyLookupBetweenWhereAndReturn() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Person)
                WHERE a.age > 18
                RETURN a.age
                """
        );

        String lookup =
                "user_amigos_prop(__A, 'age', __A_AGE)";

        assertEquals(
                1,
                occurrences(result.goal(), lookup),
                "The same property lookup should not be "
                        + "generated twice."
        );
    }

    // =====================================================
    // UNSUPPORTED FEATURES AND VALIDATION
    // =====================================================

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
