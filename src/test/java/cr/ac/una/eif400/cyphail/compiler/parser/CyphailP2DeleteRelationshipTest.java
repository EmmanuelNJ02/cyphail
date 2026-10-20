package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipDirection;
import cr.ac.una.eif400.cyphail.ast.visitor.AstTreePrinter;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalysisResult;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalyzer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint P2 tests for DELETE clauses involving relationships.
 *
 * Verifies the official Sprint P2 DELETE example, relationship
 * variables, parser behavior, semantic validation, .tree
 * visualization and backward compatibility with Sprint P1.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 */
class CyphailP2DeleteRelationshipTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final SemanticAnalyzer analyzer =
            new SemanticAnalyzer();

    private final AstTreePrinter treePrinter =
            new AstTreePrinter();

    private static final String OFFICIAL_EXAMPLE = """
            MATCH (a:Person:Female)-[r:KNOWS]->(b:Person:Male)
            WHERE r.since < 2000
            DELETE r
            """;

    private Query parse(String source) {
        return parser.parseOrThrow(source);
    }

    private SemanticAnalysisResult analyze(String source) {
        return analyzer.analyze(parse(source));
    }

    private void assertSemanticallyValid(String source) {
        SemanticAnalysisResult result = analyze(source);

        assertTrue(
                result.isValid(),
                () -> "Semantic errors: " + result.errors()
        );

        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldParseOfficialP2DeleteRelationshipExample() {
        Query query = parse(OFFICIAL_EXAMPLE);

        assertTrue(query.hasMatch());
        assertTrue(query.where().isPresent());
        assertTrue(query.returnClause().isEmpty());

        assertEquals(1, query.updates().size());

        DeleteClause delete = assertInstanceOf(
                DeleteClause.class,
                query.updates().getFirst()
        );

        assertFalse(delete.detach());
        assertEquals(1, delete.expressions().size());

        VariableExpression target = assertInstanceOf(
                VariableExpression.class,
                delete.expressions().getFirst()
        );

        assertEquals("r", target.name());
    }

    @Test
    void shouldAcceptOfficialP2DeleteRelationshipSemantics() {
        assertSemanticallyValid(OFFICIAL_EXAMPLE);
    }

    @Test
    void shouldDisplayOfficialP2DeleteRelationshipInTree() {
        String tree = treePrinter.print(
                parse(OFFICIAL_EXAMPLE)
        );

        assertTrue(tree.contains("PatternRelationship:{"));
        assertTrue(tree.contains("var: r"));
        assertTrue(tree.contains("types: [KNOWS]"));
        assertTrue(tree.contains("direction: OUT"));

        assertTrue(tree.contains("Where:{"));
        assertTrue(
                tree.contains("Expr: (< (. r since) 2000)")
        );

        assertTrue(tree.contains("Delete:{"));
        assertTrue(tree.contains("Detach: false"));
        assertTrue(tree.contains("Expressions: [r]"));
    }

    @Test
    void shouldParseIncomingRelationshipDelete() {
        Query query = parse("""
                MATCH (a:Person)<-[r:KNOWS]-(b:Person)
                DELETE r
                """);

        assertEquals(
                RelationshipDirection.IN,
                query.match()
                        .pattern()
                        .paths()
                        .getFirst()
                        .steps()
                        .getFirst()
                        .relationship()
                        .direction()
        );

        assertInstanceOf(
                DeleteClause.class,
                query.updates().getFirst()
        );

        assertSemanticallyValid("""
                MATCH (a:Person)<-[r:KNOWS]-(b:Person)
                DELETE r
                """);
    }

    @Test
    void shouldParseUndirectedRelationshipDelete() {
        Query query = parse("""
                MATCH (a:Person)-[r:KNOWS]-(b:Person)
                DELETE r
                """);

        assertEquals(
                RelationshipDirection.NONE,
                query.match()
                        .pattern()
                        .paths()
                        .getFirst()
                        .steps()
                        .getFirst()
                        .relationship()
                        .direction()
        );

        assertSemanticallyValid("""
                MATCH (a:Person)-[r:KNOWS]-(b:Person)
                DELETE r
                """);
    }

    @Test
    void shouldParseMultipleRelationshipDeleteInChain() {
        Query query = parse("""
                MATCH (a)-[r:KNOWS]->(b)-[s:WORKS_AT]->(c)
                DELETE r, s
                RETURN a
                """);

        assertEquals(
                2,
                query.match().pattern().relationshipCount()
        );

        DeleteClause delete = assertInstanceOf(
                DeleteClause.class,
                query.updates().getFirst()
        );

        assertEquals(2, delete.expressions().size());

        List<String> names = delete.expressions()
                .stream()
                .map(expression -> assertInstanceOf(
                        VariableExpression.class,
                        expression
                ))
                .map(VariableExpression::name)
                .toList();

        assertEquals(List.of("r", "s"), names);

        assertTrue(
                analyzer.analyze(query).isValid()
        );
    }

    @Test
    void shouldParseMultipleRelationshipDeleteAcrossPaths() {
        String source = """
                MATCH (a)-[r:KNOWS]->(b),
                      (c)-[s:WORKS_AT]->(d)
                DELETE r, s
                RETURN a, c
                """;

        Query query = parse(source);

        assertEquals(
                2,
                query.match().pattern().paths().size()
        );

        DeleteClause delete = assertInstanceOf(
                DeleteClause.class,
                query.updates().getFirst()
        );

        assertEquals(2, delete.expressions().size());

        assertSemanticallyValid(source);
    }

    @Test
    void shouldParseDetachDeleteRelationship() {
        String source = """
                MATCH (a)-[r:KNOWS]->(b)
                DETACH DELETE r
                """;

        Query query = parse(source);

        DeleteClause delete = assertInstanceOf(
                DeleteClause.class,
                query.updates().getFirst()
        );

        assertTrue(delete.detach());

        String tree = treePrinter.print(query);

        assertTrue(tree.contains("Delete:{"));
        assertTrue(tree.contains("Detach: true"));
        assertTrue(tree.contains("Expressions: [r]"));

        assertSemanticallyValid(source);
    }

    @Test
    void shouldParseDeleteAfterStandaloneCreate() {
        String source = """
                CREATE (a:Person)-[r:KNOWS]->(b:Person)
                DELETE r
                RETURN a
                """;

        Query query = parse(source);

        assertFalse(query.hasMatch());
        assertEquals(2, query.updates().size());

        assertInstanceOf(
                CreateClause.class,
                query.updates().get(0)
        );

        assertInstanceOf(
                DeleteClause.class,
                query.updates().get(1)
        );

        assertSemanticallyValid(source);
    }

    @Test
    void shouldPreserveMatchCreateDeleteCompatibility() {
        String source = """
                MATCH (a:Person), (b:Person)
                CREATE (a)-[r:KNOWS]->(b)
                DELETE r
                RETURN a, b
                """;

        Query query = parse(source);

        assertEquals(2, query.updates().size());

        assertInstanceOf(
                CreateClause.class,
                query.updates().get(0)
        );

        assertInstanceOf(
                DeleteClause.class,
                query.updates().get(1)
        );

        assertSemanticallyValid(source);
    }

    @Test
    void shouldPreserveSprintP1NodeDelete() {
        String source = """
                MATCH (p:Person)
                DELETE p
                """;

        Query query = parse(source);

        DeleteClause delete = assertInstanceOf(
                DeleteClause.class,
                query.updates().getFirst()
        );

        assertEquals(1, delete.expressions().size());

        assertSemanticallyValid(source);

        String tree = treePrinter.print(query);

        assertTrue(tree.contains("Delete:{"));
        assertTrue(tree.contains("Expressions: [p]"));
    }

    @Test
    void shouldRejectUndefinedRelationshipDeleteTarget() {
        String source = """
                MATCH (a)-[r:KNOWS]->(b)
                DELETE q
                """;

        SemanticAnalysisResult result = analyze(source);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Undefined variable 'q' in DELETE clause"
                        )
                )
        );
    }

    @Test
    void shouldRejectUndefinedTargetInMultipleDelete() {
        String source = """
                MATCH (a)-[r:KNOWS]->(b)
                DELETE r, s
                """;

        SemanticAnalysisResult result = analyze(source);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Undefined variable 's' in DELETE clause"
                        )
                )
        );
    }

    @Test
    void shouldRejectReservedRelationshipVariable() {
        String source = """
                MATCH (a)-[__relationship:KNOWS]->(b)
                DELETE __relationship
                """;

        SemanticAnalysisResult result = analyze(source);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Variable '__relationship' cannot start with '__'"
                        )
                )
        );
    }

    @Test
    void shouldRejectNodeRelationshipVariableConflict() {
        String source = """
                MATCH (a:Person)-[a:KNOWS]->(b:Person)
                DELETE a
                """;

        SemanticAnalysisResult result = analyze(source);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "cannot be used as both a node and a relationship"
                        )
                )
        );
    }

    @Test
    void shouldRejectRepeatedRelationshipVariable() {
        String source = """
                MATCH (a)-[r:KNOWS]->(b),
                      (c)-[r:KNOWS]->(d)
                DELETE r
                """;

        SemanticAnalysisResult result = analyze(source);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Relationship variable 'r' is declared more than once"
                        )
                )
        );
    }

    @Test
    void shouldRejectDeleteWithoutMatchOrCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("DELETE r")
        );
    }

    @Test
    void shouldRejectDeleteWithoutTarget() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("""
                        MATCH (a)-[r:KNOWS]->(b)
                        DELETE
                        """)
        );
    }

    @Test
    void shouldRejectTrailingCommaInDelete() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parse("""
                        MATCH (a)-[r:KNOWS]->(b)
                        DELETE r,
                        """)
        );
    }

    @Test
    void shouldDisplayMultipleDeleteTargetsInTree() {
        String source = """
                MATCH (a)-[r:KNOWS]->(b)-[s:WORKS_AT]->(c)
                DELETE r, s
                """;

        String tree = treePrinter.print(parse(source));

        assertTrue(tree.contains("Delete:{"));
        assertTrue(tree.contains("Expressions: [r, s]"));

        assertTrue(
                tree.indexOf("Expressions: [r, s]")
                        > tree.indexOf("PatternRelationship:{")
        );
    }
}