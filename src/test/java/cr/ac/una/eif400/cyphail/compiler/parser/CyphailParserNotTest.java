package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryOperator;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;
import cr.ac.una.eif400.cyphail.ast.expression.UnaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.UnaryOperator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parser regression tests for logical NOT expressions.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * Group Code: 04-10am
 *
 * These tests cover the logical structures required
 * by review cases C6 and C10.
 */
class CyphailParserNotTest {

    private final CyphailParser parser =
            new CyphailParser();

    private Expression parseWhere(String condition) {

        Query query = parser.parseOrThrow(
                "MATCH (a:Person), (b:Person) "
                        + "WHERE "
                        + condition
                        + " RETURN a"
        );

        return query.where()
                .orElseThrow()
                .expression();
    }

    private static BinaryExpression assertBinary(
            Expression expression,
            BinaryOperator expectedOperator
    ) {

        BinaryExpression binary =
                assertInstanceOf(
                        BinaryExpression.class,
                        expression
                );

        assertEquals(
                expectedOperator,
                binary.operator()
        );

        return binary;
    }

    private static UnaryExpression assertNot(
            Expression expression
    ) {

        UnaryExpression unary =
                assertInstanceOf(
                        UnaryExpression.class,
                        expression
                );

        assertEquals(
                UnaryOperator.NOT,
                unary.operator()
        );

        return unary;
    }

    @Test
    void shouldParseCaseSixWithOrAndNot() {

        Query query = parser.parseOrThrow(
                """
                MATCH (p:Person)-[t:WORKS_AT {role: 'Dev'}]->(e:Company)
                WHERE t.since >= 2018 OR NOT e.country = 'CR'
                RETURN p.name, e.name, t.role, t.since
                """
        );

        Expression expression = query.where()
                .orElseThrow()
                .expression();

        BinaryExpression or = assertBinary(
                expression,
                BinaryOperator.OR
        );

        assertBinary(
                or.left(),
                BinaryOperator.GREATER_THAN_OR_EQUAL
        );

        UnaryExpression not = assertNot(
                or.right()
        );

        assertBinary(
                not.operand(),
                BinaryOperator.EQUAL
        );
    }

    @Test
    void shouldParseCaseTenWithAndOrAndNot() {

        Query query = parser.parseOrThrow(
                """
                MATCH (p:Person {active: true})-[t:WORKS_AT {role: 'Dev'}]->(e:Company),
                      (q:Person)-[s:PARTNER_OF]->(e),
                      (p)-[k:KNOWS]-(q)
                WHERE (t.since >= 2018 OR s.percentage > 25.5)
                      AND NOT k.since = 2020
                RETURN p.name, q.name, e.name, t.role, s.percentage, k.since
                """
        );

        Expression expression = query.where()
                .orElseThrow()
                .expression();

        BinaryExpression and = assertBinary(
                expression,
                BinaryOperator.AND
        );

        BinaryExpression or = assertBinary(
                and.left(),
                BinaryOperator.OR
        );

        assertBinary(
                or.left(),
                BinaryOperator.GREATER_THAN_OR_EQUAL
        );

        assertBinary(
                or.right(),
                BinaryOperator.GREATER_THAN
        );

        UnaryExpression not = assertNot(
                and.right()
        );

        assertBinary(
                not.operand(),
                BinaryOperator.EQUAL
        );
    }

    @Test
    void shouldNegateCompleteParenthesizedExpression() {

        Expression expression = parseWhere(
                "NOT (a.age > 18 OR b.age < 30)"
        );

        UnaryExpression not = assertNot(
                expression
        );

        BinaryExpression or = assertBinary(
                not.operand(),
                BinaryOperator.OR
        );

        assertBinary(
                or.left(),
                BinaryOperator.GREATER_THAN
        );

        assertBinary(
                or.right(),
                BinaryOperator.LESS_THAN
        );
    }

    @Test
    void shouldParseRepeatedNotOperators() {

        Expression expression = parseWhere(
                "NOT NOT a.active = true"
        );

        UnaryExpression outerNot = assertNot(
                expression
        );

        UnaryExpression innerNot = assertNot(
                outerNot.operand()
        );

        assertBinary(
                innerNot.operand(),
                BinaryOperator.EQUAL
        );
    }

    @Test
    void shouldRejectNotWithoutOperand() {

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "MATCH (a:Person) "
                                + "WHERE NOT RETURN a"
                )
        );
    }
}
