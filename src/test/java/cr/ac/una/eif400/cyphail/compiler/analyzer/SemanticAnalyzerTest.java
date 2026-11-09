package cr.ac.una.eif400.cyphail.compiler.analyzer;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Cyphail semantic analyzer.
 *
 * <p>Sprint P1 tests validate declaration order,
 * property-expression references and undefined variables.</p>
 *
 * <p>Sprint P2 tests validate:</p>
 * <ul>
 *     <li>Duplicate labels inside the same node pattern.</li>
 *     <li>Different labels on a reused node variable.</li>
 *     <li>Node-variable reuse across relationship patterns.</li>
 *     <li>Compatibility with the original semantic rules.</li>
 * </ul>
 *
 * <p>Node variables may be reused to represent joins.
 * Different labels on a reused variable are permitted
 * as additional constraints on the same node.</p>
 *
 * <p>This class tests static semantic acceptance.
 * Runtime enforcement of label constraints must
 * also be verified through SWI-Prolog/MQI tests.</p>
 *
 * <p>Project: Cyphail - Sprint P2</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica</p>
 * <p>Escuela de Informatica</p>
 * <p>Group Code: 04-10am</p>
 *
 * <p>Authors:</p>
 * <ul>
 *     <li>Emmanuel Nunez Jimenez</li>
 *     <li>Valery Alfaro Morales</li>
 *     <li>Roy Arias Mejia</li>
 *     <li>Keynell Molina Mora</li>
 *     <li>Julissa Solano Valverde</li>
 * </ul>
 */
class SemanticAnalyzerTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final SemanticAnalyzer analyzer =
            new SemanticAnalyzer();

    // ============================================
    // TEST 1 - SIMPLE VALID QUERY
    // ============================================

    @Test
    void shouldAcceptSimpleValidQuery() {

        Query query = parser.parseOrThrow("""
                MATCH (m:Movie)
                RETURN m.title,
                       m.year AS year
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertTrue(result.errors().isEmpty());
    }

    // ============================================
    // TEST 2 - PREVIOUSLY DECLARED VARIABLE
    // ============================================

    @Test
    void shouldAcceptPropertyReferenceToPreviouslyDeclaredVariable() {

        Query query = parser.parseOrThrow("""
                MATCH (p:Person {id: 1}),
                      (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    // ============================================
    // TEST 3 - MATCH CREATE DELETE
    // ============================================

    @Test
    void shouldAcceptMatchCreateDeleteQuery() {

        Query query = parser.parseOrThrow("""
                MATCH (p:Person),
                      (o:Order {personId: p.id, status: "cancelled"})
                WHERE p.age > 60
                CREATE (a:Archive {
                    id: o.id,
                    name: "retired",
                    year: 2026
                })
                DELETE o
                RETURN p.name AS name
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    // ============================================
    // TEST 4 - UNDEFINED VARIABLE
    // ============================================

    @Test
    void shouldReportUndefinedVariableInWhereAndReturn() {

        Query query = parser.parseOrThrow("""
                MATCH (p:Person),
                      (o:Order {personId: p.id, status: "cancelled"})
                WHERE q.age > 60
                RETURN q AS name
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(result.hasErrors());
        assertFalse(result.isValid());

        assertEquals(
                List.of(
                        "Undefined variable 'q' in WHERE clause.",
                        "Undefined variable 'q' in RETURN clause."
                ),
                result.errors()
        );
    }

    // ============================================
    // TEST 5 - DECLARATION ORDER
    // ============================================

    @Test
    void shouldReportVariableUsedBeforeDeclaration() {

        Query query = parser.parseOrThrow("""
                MATCH (o:Order {
                    personId: p.id,
                    status: "cancelled"
                }),
                (p:Person)
                WHERE p.age > 60
                CREATE (a:Archive {
                    id: o.id,
                    name: "retired",
                    year: 2026
                })
                DELETE o
                RETURN p.name AS name
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(result.hasErrors());

        assertEquals(
                List.of(
                        "Undefined variable 'p' in MATCH pattern."
                ),
                result.errors()
        );
    }

    // ============================================
    // TEST 6 - DUPLICATE NODE LABELS
    // ============================================

    /**
     * Sprint P2:
     *
     * Rejects repeated labels inside the same
     * node pattern.
     *
     * Example:
     * MATCH (e:Company:Company)
     * RETURN e
     */
    @Test
    void shouldRejectDuplicateLabelsInMatchPattern() {

        Query query = parser.parseOrThrow("""
                MATCH (e:Company:Company)
                RETURN e
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertFalse(result.isValid());
        assertTrue(result.hasErrors());

        assertEquals(
                List.of(
                        "Node pattern contains duplicate labels in MATCH pattern."
                ),
                result.errors()
        );
    }

    // ============================================
    // TEST 7 - DIFFERENT LABELS ON SHARED VARIABLE
    // ============================================

    /**
     * Sprint P2:
     *
     * Verifies that a node variable can appear
     * in different patterns with different labels.
     *
     * The variable e represents the same node.
     *
     * Empresa and Ciudad are treated as additional
     * constraints on that node.
     *
     * Static semantic analysis must accept the query.
     *
     * Actual matching of both labels must also be
     * checked separately in the Prolog engine.
     */
    @Test
    void shouldAllowDifferentLabelsOnReusedNodeVariable() {

        Query query = parser.parseOrThrow("""
                MATCH (e:Empresa),
                      (e:Ciudad)
                RETURN e
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(
                result.isValid(),
                "Different labels on a reused node variable "
                        + "should be accepted as additional constraints."
        );

        assertFalse(result.hasErrors());

        assertTrue(result.errors().isEmpty());
    }

    // ============================================
    // TEST 8 - SHARED NODE VARIABLE ACROSS PATTERNS
    // ============================================

    /**
     * Sprint P2:
     *
     * Verifies that separate relationship patterns
     * may share a node variable.
     *
     * This follows complementary test case 8b
     * provided by the professor.
     *
     * The variable e identifies the same Empresa
     * in both relationship patterns.
     *
     * The relationship patterns are separate,
     * preserving the scope of Sprint P2.
     */
    @Test
    void shouldAllowSharedNodeVariableAcrossRelationshipPatterns() {

        Query query = parser.parseOrThrow("""
                MATCH (p:Persona)-[:TRABAJA_EN]->(e:Empresa),
                      (c:Persona)-[:TRABAJA_EN]->(e)
                WHERE p.nombre <> c.nombre
                RETURN p.nombre, c.nombre, e.nombre
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(
                result.isValid(),
                "A node variable shared between relationship "
                        + "patterns must be valid."
        );

        assertFalse(result.hasErrors());

        assertTrue(result.errors().isEmpty());
    }
}
