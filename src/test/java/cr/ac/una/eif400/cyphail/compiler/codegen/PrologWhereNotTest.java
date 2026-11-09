package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;
import cr.ac.una.eif400.cyphail.ast.expression.*;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for logical NOT generation in WHERE clauses.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * Group Code: 04-10am
 *
 * Covers NOT expressions, nested logical conditions
 * and the structures required by review cases C6 and C10.
 *
 * These tests inspect generated Prolog goals.
 * Real SWI-Prolog/MQI execution is tested separately.
 */
class PrologWhereNotTest {

    private final PrologWhereGoalBuilder builder =
            new PrologWhereGoalBuilder();

    private static PropertyExpression property(
            String variable,
            String name
    ) {
        return new PropertyExpression(
                new VariableExpression(variable),
                name
        );
    }

    private static BinaryExpression binary(
            Expression left,
            BinaryOperator operator,
            Expression right
    ) {
        return new BinaryExpression(
                left,
                operator,
                right
        );
    }

    private static UnaryExpression not(
            Expression operand
    ) {
        return new UnaryExpression(
                UnaryOperator.NOT,
                operand
        );
    }

    private String generate(
            Expression expression,
            String... matchedVariables
    ) {
        return builder.generate(
                new WhereClause(expression),
                new PrologVariableTable(),
                Set.of(matchedVariables)
        );
    }

    @Test
    void shouldGenerateNotForEqualityComparison() {

        Expression condition = not(
                binary(
                        property("e", "country"),
                        BinaryOperator.EQUAL,
                        new StringLiteralExpression("CR")
                )
        );

        String goal = generate(condition, "e");

        assertEquals(
                "\\+ ((user_amigos_prop(__E, 'country', __E_COUNTRY), "
                        + "cy_eq(__E_COUNTRY, 'CR')))",
                goal
        );
    }

    @Test
    void shouldGenerateCaseSixWithOrAndNot() {

        Expression sinceCondition = binary(
                property("t", "since"),
                BinaryOperator.GREATER_THAN_OR_EQUAL,
                new IntegerLiteralExpression(2018)
        );

        Expression countryCondition = not(
                binary(
                        property("e", "country"),
                        BinaryOperator.EQUAL,
                        new StringLiteralExpression("CR")
                )
        );

        Expression condition = binary(
                sinceCondition,
                BinaryOperator.OR,
                countryCondition
        );

        String goal = generate(
                condition,
                "t",
                "e"
        );

        assertTrue(
                goal.contains("once(((")
        );

        assertTrue(
                goal.contains("(__T_SINCE >= 2018)")
        );

        assertTrue(
                goal.contains("\\+ (")
        );

        assertTrue(
                goal.contains("cy_eq(__E_COUNTRY, 'CR')")
        );

        assertTrue(
                goal.contains(" ; ")
        );
    }

    @Test
    void shouldGenerateCaseTenWithAndOrNot() {

        Expression sinceCondition = binary(
                property("t", "since"),
                BinaryOperator.GREATER_THAN_OR_EQUAL,
                new IntegerLiteralExpression(2018)
        );

        Expression percentageCondition = binary(
                property("s", "percentage"),
                BinaryOperator.GREATER_THAN,
                new FloatLiteralExpression(25.5)
        );

        Expression orCondition = binary(
                sinceCondition,
                BinaryOperator.OR,
                percentageCondition
        );

        Expression notCondition = not(
                binary(
                        property("k", "since"),
                        BinaryOperator.EQUAL,
                        new IntegerLiteralExpression(2020)
                )
        );

        Expression completeCondition = binary(
                orCondition,
                BinaryOperator.AND,
                notCondition
        );

        String goal = generate(
                completeCondition,
                "t",
                "s",
                "k"
        );

        assertTrue(
                goal.contains("once(((")
        );

        assertTrue(
                goal.contains("(__T_SINCE >= 2018)")
        );

        assertTrue(
                goal.contains("(__S_PERCENTAGE > 25.5)")
        );

        assertTrue(
                goal.contains("\\+ (")
        );

        assertTrue(
                goal.contains("cy_eq(__K_SINCE, 2020)")
        );
    }

    @Test
    void shouldNegateParenthesizedOrCondition() {

        Expression first = binary(
                property("a", "age"),
                BinaryOperator.GREATER_THAN,
                new IntegerLiteralExpression(18)
        );

        Expression second = binary(
                property("b", "age"),
                BinaryOperator.LESS_THAN,
                new IntegerLiteralExpression(30)
        );

        Expression condition = not(
                binary(
                        first,
                        BinaryOperator.OR,
                        second
                )
        );

        String goal = generate(
                condition,
                "a",
                "b"
        );

        assertTrue(
                goal.startsWith("\\+ (once(((")
        );

        assertTrue(
                goal.contains("(__A_AGE > 18)")
        );

        assertTrue(
                goal.contains("(__B_AGE < 30)")
        );

        assertTrue(
                goal.contains(" ; ")
        );
    }

    @Test
    void shouldGenerateDoubleNegation() {

        Expression equality = binary(
                property("a", "active"),
                BinaryOperator.EQUAL,
                new BooleanLiteralExpression(true)
        );

        Expression condition = not(
                not(equality)
        );

        String goal = generate(
                condition,
                "a"
        );

        assertTrue(
                goal.startsWith("\\+ (\\+ (")
        );

        assertTrue(
                goal.contains("cy_eq(__A_ACTIVE, true)")
        );
    }

    @Test
    void shouldNegateBooleanLiteralConditions() {

        assertEquals(
                "\\+ (fail)",
                generate(
                        not(
                                new BooleanLiteralExpression(false)
                        )
                )
        );

        assertEquals(
                "\\+ (true)",
                generate(
                        not(
                                new BooleanLiteralExpression(true)
                        )
                )
        );
    }

    @Test
    void shouldRejectUnmatchedVariableInsideNot() {

        Expression condition = not(
                binary(
                        property("missing", "age"),
                        BinaryOperator.EQUAL,
                        new IntegerLiteralExpression(18)
                )
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> generate(condition, "a")
                );

        assertTrue(
                exception.getMessage().contains(
                        "unmatched variable"
                )
        );
    }
}
