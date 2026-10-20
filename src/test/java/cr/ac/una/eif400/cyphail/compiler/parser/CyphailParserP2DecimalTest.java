package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.FloatLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.IntegerLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;
import cr.ac.una.eif400.cyphail.ast.visitor.AstTreePrinter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests decimal literal parsing and AST representation
 * for Cyphail Sprint P2.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * Group Code: 04-10am
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
class CyphailParserP2DecimalTest {

    private final CyphailParser parser = new CyphailParser();

    @Test
    void shouldParseDecimalInWhereClause() {
        Query query = parser.parseOrThrow(
                "MATCH (p:Product) "
                        + "WHERE p.precio > 19.99 "
                        + "RETURN p.precio"
        );

        BinaryExpression condition = assertInstanceOf(
                BinaryExpression.class,
                query.where().orElseThrow().expression()
        );

        FloatLiteralExpression decimal = assertInstanceOf(
                FloatLiteralExpression.class,
                condition.right()
        );

        assertEquals(19.99, decimal.value());
    }

    @Test
    void shouldParseDecimalInNodeProperty() {
        Query query = parser.parseOrThrow(
                "MATCH (p:Product {precio: 9.5}) RETURN p"
        );

        PatternProperty property = query.match()
                .pattern()
                .nodes()
                .getFirst()
                .properties()
                .getFirst();

        assertEquals("precio", property.name());

        FloatLiteralExpression decimal = assertInstanceOf(
                FloatLiteralExpression.class,
                property.value()
        );

        assertEquals(9.5, decimal.value());
    }

    @Test
    void shouldDisplayDecimalInAstTree() {
        Query query = parser.parseOrThrow(
                "MATCH (p:Product) "
                        + "WHERE p.precio > 19.99 "
                        + "RETURN p"
        );

        String tree = new AstTreePrinter().print(query);

        assertTrue(
                tree.contains("19.99"),
                "The AST tree must display the decimal literal"
        );
    }

    @Test
    void shouldPreserveIntegerParsing() {
        Query query = parser.parseOrThrow(
                "MATCH (p:Person) "
                        + "WHERE p.edad > 18 "
                        + "RETURN p.edad"
        );

        BinaryExpression condition = assertInstanceOf(
                BinaryExpression.class,
                query.where().orElseThrow().expression()
        );

        IntegerLiteralExpression integer = assertInstanceOf(
                IntegerLiteralExpression.class,
                condition.right()
        );

        assertEquals(18L, integer.value());
    }
}