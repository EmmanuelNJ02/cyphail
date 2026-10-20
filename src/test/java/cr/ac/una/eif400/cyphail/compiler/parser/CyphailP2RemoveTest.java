package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.RemoveClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetClause;
import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;
import cr.ac.una.eif400.cyphail.ast.visitor.AstTreePrinter;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalysisResult;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalyzer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint P2 tests for REMOVE property clauses.
 *
 * Verifies parsing, AST construction, semantic validation
 * and .tree visualization for node and relationship
 * property removal.
 *
 * Also verifies compatibility with CREATE, SET and
 * the previously supported query grammar.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * Group Code: 04-10am
 */
class CyphailP2RemoveTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final SemanticAnalyzer analyzer =
            new SemanticAnalyzer();

    private final AstTreePrinter treePrinter =
            new AstTreePrinter();

    @Test
    void shouldParseOfficialRemoveNodeProperty() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product {code: "PROD-99"})
                REMOVE p.discount
                RETURN p.id
                """);

        assertTrue(query.hasMatch());
        assertEquals(1, query.updates().size());

        RemoveClause remove = assertInstanceOf(
                RemoveClause.class,
                query.updates().getFirst()
        );

        assertEquals(1, remove.properties().size());

        PropertyExpression property =
                remove.properties().getFirst();

        assertEquals("discount", property.property());

        VariableExpression target = assertInstanceOf(
                VariableExpression.class,
                property.target()
        );

        assertEquals("p", target.name());
        assertTrue(query.returnClause().isPresent());
    }

    @Test
    void shouldParseRemoveRelationshipProperty() {
        Query query = parser.parseOrThrow("""
                MATCH (u:Student {id: "4-666-000"})
                      -[r:attends]->
                      (c:Course {nrc: "50691"})
                REMOVE r.grade
                RETURN u.id, r.grade
                """);

        assertTrue(
                query.match()
                        .pattern()
                        .paths()
                        .getFirst()
                        .hasRelationships()
        );

        RemoveClause remove = assertInstanceOf(
                RemoveClause.class,
                query.updates().getFirst()
        );

        PropertyExpression property =
                remove.properties().getFirst();

        assertEquals("grade", property.property());

        VariableExpression variable = assertInstanceOf(
                VariableExpression.class,
                property.target()
        );

        assertEquals("r", variable.name());
    }

    @Test
    void shouldParseMultipleRemovePropertiesInOrder() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                REMOVE p.discount, p.oldPrice, p.expired
                RETURN p.id
                """);

        RemoveClause remove = assertInstanceOf(
                RemoveClause.class,
                query.updates().getFirst()
        );

        List<String> propertyNames =
                remove.properties()
                        .stream()
                        .map(PropertyExpression::property)
                        .toList();

        assertEquals(
                List.of("discount", "oldPrice", "expired"),
                propertyNames
        );
    }

    @Test
    void shouldParseRemoveFromDifferentVariables() {
        Query query = parser.parseOrThrow("""
                MATCH (a:Person)-[r:KNOWS]->(b:Person)
                REMOVE a.temporary, r.oldFlag, b.expired
                RETURN a.id, b.id
                """);

        RemoveClause remove = assertInstanceOf(
                RemoveClause.class,
                query.updates().getFirst()
        );

        assertEquals(3, remove.properties().size());

        List<String> variables =
                remove.properties()
                        .stream()
                        .map(PropertyExpression::target)
                        .map(target -> assertInstanceOf(
                                VariableExpression.class,
                                target
                        ))
                        .map(VariableExpression::name)
                        .toList();

        assertEquals(
                List.of("a", "r", "b"),
                variables
        );
    }

    @Test
    void shouldParseRemoveAfterStandaloneCreate() {
        Query query = parser.parseOrThrow("""
                CREATE (p:Product {code: "PROD-99"})
                REMOVE p.discount
                RETURN p.id
                """);

        assertFalse(query.hasMatch());
        assertEquals(2, query.updates().size());

        assertInstanceOf(
                CreateClause.class,
                query.updates().get(0)
        );

        assertInstanceOf(
                RemoveClause.class,
                query.updates().get(1)
        );
    }

    @Test
    void shouldPreserveSetAndRemoveOrder() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                SET p.precio = 19.99
                REMOVE p.discount
                RETURN p.id
                """);

        assertEquals(2, query.updates().size());

        assertInstanceOf(
                SetClause.class,
                query.updates().get(0)
        );

        assertInstanceOf(
                RemoveClause.class,
                query.updates().get(1)
        );
    }

    @Test
    void shouldDisplayRemoveNodePropertyInTree() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product {code: "PROD-99"})
                REMOVE p.discount
                RETURN p.id
                """);

        String tree = treePrinter.print(query);

        assertTrue(tree.contains("Remove:{"));
        assertTrue(tree.contains("Properties:"));
        assertTrue(tree.contains("(. p discount)"));
        assertTrue(tree.contains("(. p id)"));
        assertTrue(tree.contains("Return:"));
    }

    @Test
    void shouldDisplayRemoveRelationshipPropertyInTree() {
        Query query = parser.parseOrThrow("""
                MATCH (u:Student)-[r:attends]->(c:Course)
                REMOVE r.grade
                RETURN u.id
                """);

        String tree = treePrinter.print(query);

        assertTrue(
                tree.contains("PatternRelationship")
        );

        assertTrue(tree.contains("Remove:{"));
        assertTrue(tree.contains("(. r grade)"));
    }

    @Test
    void shouldDisplayMultipleRemovePropertiesInTree() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                REMOVE p.discount, p.oldPrice
                RETURN p
                """);

        String tree = treePrinter.print(query);

        assertTrue(tree.contains("Remove:{"));

        assertTrue(
                tree.contains(
                        "Properties: [(. p discount), (. p oldPrice)]"
                )
        );
    }

    @Test
    void shouldAcceptSemanticRemoveNodeProperty() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product {code: "PROD-99"})
                REMOVE p.discount
                RETURN p.id
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
    void shouldAcceptSemanticRemoveRelationshipProperty() {
        Query query = parser.parseOrThrow("""
                MATCH (u:Student {id: "4-666-000"})
                      -[r:attends]->
                      (c:Course {nrc: "50691"})
                REMOVE r.grade
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
    void shouldAcceptSemanticRemoveAfterStandaloneCreate() {
        Query query = parser.parseOrThrow("""
                CREATE (p:Product {code: "PROD-99"})
                REMOVE p.discount
                RETURN p.id
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(
                result.isValid(),
                () -> "Semantic errors: " + result.errors()
        );
    }

    @Test
    void shouldAcceptSemanticSetThenRemove() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                SET p.precio = 19.99
                REMOVE p.discount
                RETURN p.id
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertTrue(
                result.isValid(),
                () -> "Semantic errors: " + result.errors()
        );

        String tree = treePrinter.print(query);

        assertTrue(tree.contains("Set:{"));
        assertTrue(tree.contains("Remove:{"));

        assertTrue(
                tree.indexOf("Set:{")
                        < tree.indexOf("Remove:{")
        );
    }

    @Test
    void shouldRejectUndefinedRemoveVariable() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                REMOVE q.discount
                RETURN p
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Undefined variable 'q' in REMOVE clause"
                        )
                )
        );
    }

    @Test
    void shouldRejectUndefinedVariableInMultipleRemove() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                REMOVE p.discount, q.oldPrice
                RETURN p
                """);

        SemanticAnalysisResult result =
                analyzer.analyze(query);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Undefined variable 'q' in REMOVE clause"
                        )
                )
        );
    }

    @Test
    void shouldRejectReservedRemoveVariable() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Product)
                REMOVE __product.discount
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
    void shouldRejectRemoveWithoutMatchOrCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "REMOVE p.discount"
                )
        );
    }

    @Test
    void shouldRejectRemoveWithoutPropertyName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "MATCH (p:Product) REMOVE p."
                )
        );
    }

    @Test
    void shouldRejectRemoveOfBareVariable() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "MATCH (p:Product) REMOVE p"
                )
        );
    }

    @Test
    void shouldRejectTrailingCommaInRemove() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "MATCH (p:Product) REMOVE p.discount,"
                )
        );
    }
}