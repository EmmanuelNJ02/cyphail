package cr.ac.una.eif400.cyphail.compiler.analyzer;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Sprint P1 semantic analyzer.
 *
 * <p>The tests exercise valid and invalid queries based on the
 * official Sprint P1 cases. They verify declaration order,
 * property-expression references and undefined-variable detection.</p>
 *
 * <p>Project: Cyphail</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica - Escuela de Informatica</p>
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

    private final CyphailParser parser = new CyphailParser();
    private final SemanticAnalyzer analyzer = new SemanticAnalyzer();

    @Test
    void shouldAcceptSimpleValidQuery() {
        Query query = parser.parseOrThrow("""
                MATCH (m:Movie)
                RETURN m.title,
                       m.year AS year
                """);

        SemanticAnalysisResult result = analyzer.analyze(query);

        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldAcceptPropertyReferenceToPreviouslyDeclaredVariable() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Person {id: 1}),
                      (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total
                """);

        SemanticAnalysisResult result = analyzer.analyze(query);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

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

        SemanticAnalysisResult result = analyzer.analyze(query);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldReportUndefinedVariableInWhereAndReturn() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Person),
                      (o:Order {personId: p.id, status: "cancelled"})
                WHERE q.age > 60
                RETURN q AS name
                """);

        SemanticAnalysisResult result = analyzer.analyze(query);

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

        SemanticAnalysisResult result = analyzer.analyze(query);

        assertTrue(result.hasErrors());
        assertEquals(
                List.of(
                        "Undefined variable 'p' in MATCH pattern."
                ),
                result.errors()
        );
    }
}