
package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryOperator;
import cr.ac.una.eif400.cyphail.ast.expression.BooleanLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;

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
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * Tests for logical WHERE expressions.
 *
 * Verifies:
 * - AND and OR parsing.
 * - Operator precedence.
 * - Parenthesized expressions.
 * - Chained logical expressions.
 * - Boolean literals.
 * - Relationship property conditions.
 * - Compatibility with existing queries.
 * - Rejection of incomplete expressions.
 *
 * Expected precedence:
 * 1. Parenthesized expressions.
 * 2. Comparisons.
 * 3. AND.
 * 4. OR.
 */
class CyphailParserWhereLogicalTest {

    private final CyphailParser parser =
            new CyphailParser();

    /**
     * Parses a WHERE expression inside a complete query.
     */
    private Expression parseWhere(String condition) {

        Query query = parser.parseOrThrow(
                "MATCH (a:Person), (b:Person) "
                        + "WHERE "
                        + condition
                        + " RETURN a"
        );

        assertTrue(query.where().isPresent());

        return query.where()
                .orElseThrow()
                .expression();
    }

    /**
     * Asserts that an expression has a binary operator.
     */
    private static BinaryExpression assertOperator(
            Expression expression,
            BinaryOperator expected
    ) {

        BinaryExpression binary =
                assertInstanceOf(
                        BinaryExpression.class,
                        expression
                );

        assertEquals(
                expected,
                binary.operator()
        );

        return binary;
    }

    @Test
    void shouldParseAndBetweenComparisons() {

        Expression expression = parseWhere(
                "a.age > 18 AND b.age < 50"
        );

        BinaryExpression and =
                assertOperator(
                        expression,
                        BinaryOperator.AND
                );

        assertOperator(
                and.left(),
                BinaryOperator.GREATER_THAN
        );

        assertOperator(
                and.right(),
                BinaryOperator.LESS_THAN
        );
    }

    @Test
    void shouldParseOrBetweenComparisons() {

        Expression expression = parseWhere(
                "a.age < 18 OR b.age > 50"
        );

        BinaryExpression or =
                assertOperator(
                        expression,
                        BinaryOperator.OR
                );

        assertOperator(
                or.left(),
                BinaryOperator.LESS_THAN
        );

        assertOperator(
                or.right(),
                BinaryOperator.GREATER_THAN
        );
    }

    @Test
    void shouldGiveAndHigherPrecedenceThanOr() {

        Expression expression = parseWhere(
                "a.age > 18 OR b.age < 50 "
                        + "AND a.name = 'Juan'"
        );

        BinaryExpression or =
                assertOperator(
                        expression,
                        BinaryOperator.OR
                );

        assertOperator(
                or.left(),
                BinaryOperator.GREATER_THAN
        );

        BinaryExpression and =
                assertOperator(
                        or.right(),
                        BinaryOperator.AND
                );

        assertOperator(
                and.left(),
                BinaryOperator.LESS_THAN
        );

        assertOperator(
                and.right(),
                BinaryOperator.EQUAL
        );
    }

    @Test
    void shouldGroupAndBeforeOr() {

        Expression expression = parseWhere(
                "a.age > 18 AND b.age < 50 "
                        + "OR a.age = 10"
        );

        BinaryExpression or =
                assertOperator(
                        expression,
                        BinaryOperator.OR
                );

        BinaryExpression and =
                assertOperator(
                        or.left(),
                        BinaryOperator.AND
                );

        assertOperator(
                and.left(),
                BinaryOperator.GREATER_THAN
        );

        assertOperator(
                and.right(),
                BinaryOperator.LESS_THAN
        );

        assertOperator(
                or.right(),
                BinaryOperator.EQUAL
        );
    }

    @Test
    void shouldRespectParenthesizedOrExpression() {

        Expression expression = parseWhere(
                "(a.age > 18 OR b.age < 50) "
                        + "AND a.name = 'Juan'"
        );

        BinaryExpression and =
                assertOperator(
                        expression,
                        BinaryOperator.AND
                );

        BinaryExpression or =
                assertOperator(
                        and.left(),
                        BinaryOperator.OR
                );

        assertOperator(
                or.left(),
                BinaryOperator.GREATER_THAN
        );

        assertOperator(
                or.right(),
                BinaryOperator.LESS_THAN
        );

        assertOperator(
                and.right(),
                BinaryOperator.EQUAL
        );
    }

    @Test
    void shouldParseChainedAndExpressions() {

        Expression expression = parseWhere(
                "a.age > 18 AND b.age < 50 "
                        + "AND a.name <> b.name"
        );

        BinaryExpression outer =
                assertOperator(
                        expression,
                        BinaryOperator.AND
                );

        BinaryExpression inner =
                assertOperator(
                        outer.left(),
                        BinaryOperator.AND
                );

        assertOperator(
                inner.left(),
                BinaryOperator.GREATER_THAN
        );

        assertOperator(
                inner.right(),
                BinaryOperator.LESS_THAN
        );

        assertOperator(
                outer.right(),
                BinaryOperator.NOT_EQUAL
        );
    }

    @Test
    void shouldParseChainedOrExpressions() {

        Expression expression = parseWhere(
                "a.age < 18 OR a.age > 65 "
                        + "OR b.age = 30"
        );

        BinaryExpression outer =
                assertOperator(
                        expression,
                        BinaryOperator.OR
                );

        BinaryExpression inner =
                assertOperator(
                        outer.left(),
                        BinaryOperator.OR
                );

        assertOperator(
                inner.left(),
                BinaryOperator.LESS_THAN
        );

        assertOperator(
                inner.right(),
                BinaryOperator.GREATER_THAN
        );

        assertOperator(
                outer.right(),
                BinaryOperator.EQUAL
        );
    }

    @Test
    void shouldParseBooleanLiteralConditions() {

        Expression expression = parseWhere(
                "TRUE OR FALSE"
        );

        BinaryExpression or =
                assertOperator(
                        expression,
                        BinaryOperator.OR
                );

        BooleanLiteralExpression left =
                assertInstanceOf(
                        BooleanLiteralExpression.class,
                        or.left()
                );

        BooleanLiteralExpression right =
                assertInstanceOf(
                        BooleanLiteralExpression.class,
                        or.right()
                );

        assertTrue(left.value());
        assertFalse(right.value());
    }

    @Test
    void shouldParseLogicalWhereWithRelationship() {

        Query query = parser.parseOrThrow(
                """
                MATCH (a:Person)-[r:Amigo]->(b:Person)
                WHERE a.age > 18 AND r.since >= 2000
                RETURN a, r, b
                """
        );

        assertTrue(query.where().isPresent());

        BinaryExpression and =
                assertOperator(
                        query.where()
                                .orElseThrow()
                                .expression(),
                        BinaryOperator.AND
                );

        assertOperator(
                and.left(),
                BinaryOperator.GREATER_THAN
        );

        assertOperator(
                and.right(),
                BinaryOperator.GREATER_THAN_OR_EQUAL
        );
    }

    @Test
    void shouldPreserveSimpleWhereComparisons() {

        Expression expression = parseWhere(
                "a.age >= 21"
        );

        assertOperator(
                expression,
                BinaryOperator.GREATER_THAN_OR_EQUAL
        );
    }

    @Test
    void shouldPreserveQueriesWithoutWhere() {

        Query query = parser.parseOrThrow(
                "MATCH (a:Person) RETURN a"
        );

        assertTrue(query.where().isEmpty());

        assertTrue(query.returnClause().isPresent());
    }

    @Test
    void shouldRejectIncompleteLogicalExpression() {

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "MATCH (a:Person) "
                                + "WHERE a.age > 18 AND "
                                + "RETURN a"
                )
        );
    }

    @Test
    void shouldRejectUnclosedParentheses() {

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "MATCH (a:Person) "
                                + "WHERE (a.age > 18 OR a.age < 10 "
                                + "RETURN a"
                )
        );
    }
}
