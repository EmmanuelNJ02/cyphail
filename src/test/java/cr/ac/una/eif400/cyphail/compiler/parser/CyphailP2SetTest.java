package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetProperty;
import cr.ac.una.eif400.cyphail.ast.expression.FloatLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;
import cr.ac.una.eif400.cyphail.ast.visitor.AstTreePrinter;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalysisResult;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalyzer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint P2 tests for SET property assignments.
 *
 * Verifies parsing, AST construction, semantic analysis
 * and tree visualization for node and relationship
 * property assignments.
 *
 * Also checks compatibility with standalone CREATE
 * statements and existing MATCH-based queries.
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
class CyphailP2SetTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final SemanticAnalyzer analyzer =
            new SemanticAnalyzer();

    private final AstTreePrinter treePrinter =
            new AstTreePrinter();

    @Test
    void shouldParseSetNodeProperty() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product {code: "PROD-99"})
                SET p.precio = 19.99
                RETURN p
                """);

        assertEquals(1, query.updates().size());

        SetClause set = assertInstanceOf(
                SetClause.class,
                query.updates().getFirst()
        );

        assertEquals(1, set.items().size());

        SetProperty property = assertInstanceOf(
                SetProperty.class,
                set.items().getFirst()
        );

        assertEquals(
                "precio",
                property.property().property()
        );

        VariableExpression target = assertInstanceOf(
                VariableExpression.class,
                property.property().target()
        );

        assertEquals("p", target.name());

        FloatLiteralExpression value = assertInstanceOf(
                FloatLiteralExpression.class,
                property.value()
        );

        assertEquals(19.99, value.value(), 0.000001);

        assertTrue(query.returnClause().isPresent());
    }

    @Test
    void shouldParseSetRelationshipProperty() {
        Query query = parser.parseOrThrow("""
                MATCH (u:Student {id: "4-666-000"})
                      -[r:attends]->
                      (c:Course {nrc: "50691"})
                SET r.grade = 9.5
                RETURN u.id, r.grade
                """);

        assertNotNull(query.match());

        assertTrue(
                query.match()
                        .pattern()
                        .paths()
                        .getFirst()
                        .hasRelationships()
        );

        SetClause set = assertInstanceOf(
                SetClause.class,
                query.updates().getFirst()
        );

        SetProperty property = assertInstanceOf(
                SetProperty.class,
                set.items().getFirst()
        );

        assertEquals(
                "grade",
                property.property().property()
        );

        VariableExpression target = assertInstanceOf(
                VariableExpression.class,
                property.property().target()
        );

        assertEquals("r", target.name());

        FloatLiteralExpression value = assertInstanceOf(
                FloatLiteralExpression.class,
                property.value()
        );

        assertEquals(9.5, value.value(), 0.000001);
    }

    @Test
    void shouldParseSetAfterStandaloneCreate() {
        Query query = parser.parseOrThrow("""
                CREATE (p:Product {code: 'PROD-99'})
                SET p.precio = 19.99
                RETURN p
                """);

        assertFalse(query.hasMatch());
        assertEquals(2, query.updates().size());

        assertInstanceOf(
                CreateClause.class,
                query.updates().get(0)
        );

        assertInstanceOf(
                SetClause.class,
                query.updates().get(1)
        );

        assertTrue(query.returnClause().isPresent());
    }

    @Test
    void shouldDisplaySetNodePropertyInTree() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                SET p.precio = 19.99
                RETURN p
                """);

        String tree = treePrinter.print(query);

        assertTrue(tree.contains("Set:{"));
        assertTrue(tree.contains("(. p precio)"));
        assertTrue(tree.contains("19.99"));
        assertTrue(tree.contains("Return:"));
    }

    @Test
    void shouldDisplaySetRelationshipPropertyInTree() {
        Query query = parser.parseOrThrow("""
                MATCH (u:Student)-[r:attends]->(c:Course)
                SET r.grade = 9.5
                RETURN r.grade
                """);

        String tree = treePrinter.print(query);

        assertTrue(
                tree.contains("PatternRelationship")
        );

        assertTrue(tree.contains("Set:{"));
        assertTrue(tree.contains("(. r grade)"));
        assertTrue(tree.contains("9.5"));
    }

    @Test
    void shouldAcceptSemanticSetNodeProperty() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product {code: "PROD-99"})
                SET p.precio = 19.99
                RETURN p
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(
                result.isValid(),
                () -> "Semantic errors: " + result.errors()
        );

        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldAcceptSemanticSetRelationshipProperty() {
        Query query = parser.parseOrThrow("""
                MATCH (u:Student {id: "4-666-000"})
                      -[r:attends]->
                      (c:Course {nrc: "50691"})
                SET r.grade = 9.5
                RETURN u.id, r.grade
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(
                result.isValid(),
                () -> "Semantic errors: " + result.errors()
        );
    }

    @Test
    void shouldAcceptSetAfterStandaloneCreate() {
        Query query = parser.parseOrThrow("""
                CREATE (p:Product {code: 'PROD-99'})
                SET p.precio = 19.99
                RETURN p.precio
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(
                result.isValid(),
                () -> "Semantic errors: " + result.errors()
        );
    }

    @Test
    void shouldRejectUndefinedSetTarget() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                SET q.precio = 19.99
                RETURN p
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Undefined variable 'q' in SET target"
                        )
                )
        );
    }

    @Test
    void shouldRejectUndefinedSetValueVariable() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                SET p.precio = q.precio
                RETURN p
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Undefined variable 'q' in SET value"
                        )
                )
        );
    }

    @Test
    void shouldRejectReservedSetTargetVariable() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                SET __product.precio = 19.99
                RETURN p
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Variable '__product' cannot start with '__'"
                        )
                )
        );
    }

    @Test
    void shouldRejectSetWithoutMatchOrCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "SET p.precio = 19.99"
                )
        );
    }

    @Test
    void shouldRejectSetWithoutAssignmentValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "MATCH (p:Product) SET p.precio ="
                )
        );
    }

    @Test
    void shouldPreserveMatchCreateAndSetCompatibility() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                CREATE (q:Product {precio: 10.5})
                SET q.precio = 19.99
                RETURN p, q
                """);

        assertTrue(query.hasMatch());

        assertEquals(2, query.updates().size());

        assertInstanceOf(
                CreateClause.class,
                query.updates().get(0)
        );

        assertInstanceOf(
                SetClause.class,
                query.updates().get(1)
        );

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(
                result.isValid(),
                () -> "Semantic errors: " + result.errors()
        );
    }
}