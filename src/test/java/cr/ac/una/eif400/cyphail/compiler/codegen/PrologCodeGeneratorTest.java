
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
 * Unit tests for the Prolog code generator.
 *
 * Tests parse real Cyphail source statements and verify
 * that the AST visitor generates the expected Prolog goals,
 * variable mappings, column headers and RETURN projections.
 *
 * Coverage includes:
 * - MATCH with isolated nodes.
 * - Node labels and properties.
 * - Outgoing relationships.
 * - Incoming relationships.
 * - Undirected relationships.
 * - Anonymous relationships.
 * - Chained relationships.
 * - Comma-separated pattern paths.
 * - Relationship property constraints.
 * - Relationship property projections.
 * - Node and relationship identifier projections.
 * - RETURN aliases.
 * - Unsupported P2 features.
 *
 * Integration with the running Prolog engine and MQI
 * is tested separately.
 */
class PrologCodeGeneratorTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Parses a Cyphail statement and generates
     * its corresponding Prolog result.
     */
    private PrologGenerationResult generate(
            String source
    ) {

        Query query =
                parser.parseOrThrow(source);

        return generator.generate(query);
    }

    /**
     * Verifies that a node MATCH statement produces
     * the expected node, label and property goals.
     */
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
                result.goal().contains(
                        "cy_create_query"
                )
        );

        assertTrue(
                result.goal().contains(
                        "cy_exec_query"
                )
        );

        assertTrue(
                result.goal().contains(
                        "cy_erase_query"
                )
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

    /**
     * Verifies that RETURN can project
     * a matched node identifier.
     */
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

        assertTrue(
                result.hasOutputColumns()
        );
    }

    /**
     * Verifies that RETURN preserves
     * projection order and aliases.
     */
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

    /**
     * Verifies the generation of equality
     * constraints for node properties.
     */
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

    /**
     * Verifies that multiple node labels
     * produce separate Prolog goals.
     */
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

    /**
     * Verifies that each compilation maintains
     * its own independent variable mappings.
     */
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
                second.variableMappings()
                        .containsKey("a")
        );
    }

    /**
     * Verifies outgoing relationship generation.
     *
     * OUT means that the relationship goes
     * from the left node to the right node.
     */
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
                result.variableMappings()
                        .get("r")
        );

        assertTrue(
                result.goal().contains(
                        "cyphail_query_p2(__A, __R, __B)"
                )
        );
    }

    /**
     * Verifies incoming relationship generation.
     *
     * IN means that the relationship goes
     * from the right node to the left node.
     */
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

    /**
     * Verifies that an undirected relationship
     * generates both endpoint orientations.
     */
    @Test
    void shouldGenerateUndirectedRelationship() {

        PrologGenerationResult result = generate(
                """
                MATCH (a:Persona)-[r:CONOCE]-(b:Persona)
                RETURN a, r, b
                """
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_edge(__R, 'CONOCE', __A, __B)"
                )
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_edge(__R, 'CONOCE', __B, __A)"
                )
        );

        assertTrue(
                result.goal().contains(
                        "dif(__A, __B)"
                )
        );
    }

    /**
     * Verifies that anonymous relationships
     * receive temporary Prolog variables.
     */
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
                result.variableMappings()
                        .containsKey("r")
        );
    }

    /**
     * Verifies that consecutive relationships
     * are generated in their original order.
     */
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

        assertTrue(
                goal.contains(
                        "user_amigos_edge(__R, 'CONOCE', __A, __B)"
                )
        );

        assertTrue(
                goal.contains(
                        "user_amigos_edge(__S, 'TRABAJA_EN', __B, __C)"
                )
        );

        assertTrue(
                goal.indexOf(
                        "user_amigos_edge(__R, 'CONOCE', __A, __B)"
                ) < goal.indexOf(
                        "user_amigos_edge(__S, 'TRABAJA_EN', __B, __C)"
                )
        );

        assertEquals(
                List.of("a", "r", "b", "s", "c"),
                result.columnHeaders()
        );
    }

    /**
     * Verifies relationships distributed across
     * comma-separated MATCH paths.
     */
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

    /**
     * Verifies literal property constraints
     * declared inside relationship patterns.
     */
    @Test
    void shouldGenerateRelationshipPropertyConstraint() {

        PrologGenerationResult result = generate(
                """
                MATCH (a)-[r:CONOCE {since: 2020}]->(b)
                RETURN r
                """
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_edge(__R, 'CONOCE', __A, __B)"
                )
        );

        assertTrue(
                result.goal().contains(
                        "user_amigos_prop(__R, 'since', __R_SINCE)"
                )
        );

        assertTrue(
                result.goal().contains(
                        "cy_eq(__R_SINCE, 2020)"
                )
        );

        assertEquals(
                "__R_SINCE",
                result.propertyMappings()
                        .get("r.since")
        );
    }

    /**
     * Verifies that RETURN can project a
     * relationship property with an alias.
     */
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

    /**
     * Verifies mixed node and relationship
     * projections within the same RETURN.
     */
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
                List.of(
                        "origen",
                        "relacion",
                        "destino"
                ),
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

    /**
     * WHERE generation remains unsupported
     * until the next CodeGen implementation.
     */
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

    /**
     * Standalone CREATE generation is not yet
     * implemented by the current generator.
     */
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

    /**
     * SET generation remains unsupported
     * until mutation support is implemented.
     */
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

    /**
     * A RETURN variable must be introduced
     * by the MATCH pattern.
     */
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

    /**
     * Generating a null query must fail
     * with a clear validation error.
     */
    @Test
    void shouldRejectNullQuery() {

        assertThrows(
                NullPointerException.class,
                () -> generator.generate(null)
        );
    }
}
