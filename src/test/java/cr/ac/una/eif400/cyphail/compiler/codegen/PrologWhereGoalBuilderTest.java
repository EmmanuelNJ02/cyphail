
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;
import cr.ac.una.eif400.cyphail.ast.expression.*;

import org.junit.jupiter.api.Test;

import java.util.Set;

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
 * Unit tests for PrologWhereGoalBuilder.
 *
 * Tests verify the generation of Prolog goals for:
 * - Equality and inequality.
 * - Numeric comparisons.
 * - Node and relationship properties.
 * - String, integer, decimal and boolean literals.
 * - Logical AND and OR.
 * - Arithmetic expressions.
 * - Variable mappings.
 * - Unsupported expressions and invalid inputs.
 *
 * These tests operate on the AST directly.
 * Parsing and real MQI execution are tested separately.
 */
class PrologWhereGoalBuilderTest {

    private final PrologWhereGoalBuilder builder =
            new PrologWhereGoalBuilder();

    /**
     * Creates a property access expression.
     */
    private static PropertyExpression property(
            String variable,
            String name
    ) {

        return new PropertyExpression(
                new VariableExpression(variable),
                name
        );
    }

    /**
     * Creates a WHERE clause containing
     * one binary expression.
     */
    private static WhereClause comparison(
            Expression left,
            BinaryOperator operator,
            Expression right
    ) {

        return new WhereClause(
                new BinaryExpression(
                        left,
                        operator,
                        right
                )
        );
    }

    /**
     * Generates a WHERE goal using
     * a fresh symbol table.
     */
    private String generate(
            WhereClause clause,
            String... matched
    ) {

        return builder.generate(
                clause,
                new PrologVariableTable(),
                Set.of(matched)
        );
    }

    @Test
    void shouldGenerateGreaterThanCondition() {

        String goal = generate(
                comparison(
                        property("a", "age"),
                        BinaryOperator.GREATER_THAN,
                        new IntegerLiteralExpression(18)
                ),
                "a"
        );

        assertEquals(
                "(user_amigos_prop(__A, 'age', __A_AGE), "
                        + "number(__A_AGE), "
                        + "number(18), "
                        + "(__A_AGE > 18))",
                goal
        );
    }

    @Test
    void shouldGenerateEqualityWithStringLiteral() {

        String goal = generate(
                comparison(
                        property("a", "name"),
                        BinaryOperator.EQUAL,
                        new StringLiteralExpression("Juan Perez")
                ),
                "a"
        );

        assertEquals(
                "(user_amigos_prop(__A, 'name', __A_NAME), "
                        + "cy_eq(__A_NAME, 'Juan Perez'))",
                goal
        );
    }

    @Test
    void shouldGenerateNotEqualBetweenProperties() {

        String goal = generate(
                comparison(
                        property("a", "name"),
                        BinaryOperator.NOT_EQUAL,
                        property("b", "name")
                ),
                "a",
                "b"
        );

        assertEquals(
                "(user_amigos_prop(__A, 'name', __A_NAME), "
                        + "user_amigos_prop(__B, 'name', __B_NAME), "
                        + "cy_neq(__A_NAME, __B_NAME))",
                goal
        );
    }

    @Test
    void shouldGenerateLessThanCondition() {

        String goal = generate(
                comparison(
                        property("r", "since"),
                        BinaryOperator.LESS_THAN,
                        new IntegerLiteralExpression(2000)
                ),
                "r"
        );

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__R, 'since', __R_SINCE)"
                )
        );

        assertTrue(
                goal.contains(
                        "(__R_SINCE < 2000)"
                )
        );
    }

    @Test
    void shouldGenerateLessThanOrEqualCondition() {

        String goal = generate(
                comparison(
                        property("a", "age"),
                        BinaryOperator.LESS_THAN_OR_EQUAL,
                        new IntegerLiteralExpression(30)
                ),
                "a"
        );

        assertTrue(
                goal.contains(
                        "(__A_AGE =< 30)"
                )
        );

        assertTrue(
                goal.contains(
                        "number(__A_AGE)"
                )
        );
    }

    @Test
    void shouldGenerateGreaterThanOrEqualCondition() {

        String goal = generate(
                comparison(
                        property("a", "age"),
                        BinaryOperator.GREATER_THAN_OR_EQUAL,
                        new IntegerLiteralExpression(21)
                ),
                "a"
        );

        assertTrue(
                goal.contains(
                        "(__A_AGE >= 21)"
                )
        );
    }

    @Test
    void shouldSupportDecimalLiteral() {

        String goal = generate(
                comparison(
                        property("p", "price"),
                        BinaryOperator.GREATER_THAN,
                        new FloatLiteralExpression(19.99)
                ),
                "p"
        );

        assertTrue(
                goal.contains(
                        "(__P_PRICE > 19.99)"
                )
        );
    }

    @Test
    void shouldGenerateLogicalAnd() {

        Expression ageCondition =
                new BinaryExpression(
                        property("a", "age"),
                        BinaryOperator.GREATER_THAN,
                        new IntegerLiteralExpression(18)
                );

        Expression nameCondition =
                new BinaryExpression(
                        property("a", "name"),
                        BinaryOperator.EQUAL,
                        new StringLiteralExpression("Juan Perez")
                );

        String goal = generate(
                new WhereClause(
                        new BinaryExpression(
                                ageCondition,
                                BinaryOperator.AND,
                                nameCondition
                        )
                ),
                "a"
        );

        assertTrue(
                goal.contains(
                        "(__A_AGE > 18)"
                )
        );

        assertTrue(
                goal.contains(
                        "cy_eq(__A_NAME, 'Juan Perez')"
                )
        );

        assertTrue(
                goal.contains(
                        "), ("
                )
        );
    }

    @Test
    void shouldGenerateLogicalOr() {

        Expression first =
                new BinaryExpression(
                        property("a", "age"),
                        BinaryOperator.EQUAL,
                        new IntegerLiteralExpression(18)
                );

        Expression second =
                new BinaryExpression(
                        property("a", "age"),
                        BinaryOperator.EQUAL,
                        new IntegerLiteralExpression(30)
                );

        String goal = generate(
                new WhereClause(
                        new BinaryExpression(
                                first,
                                BinaryOperator.OR,
                                second
                        )
                ),
                "a"
        );

        assertTrue(
                goal.contains(
                        "cy_eq(__A_AGE, 18)"
                )
        );

        assertTrue(
                goal.contains(
                        "cy_eq(__A_AGE, 30)"
                )
        );

        assertTrue(
                goal.contains(" ; ")
        );
    }

    @Test
    void shouldSupportBooleanConditions() {

        assertEquals(
                "true",
                generate(
                        new WhereClause(
                                new BooleanLiteralExpression(true)
                        )
                )
        );

        assertEquals(
                "fail",
                generate(
                        new WhereClause(
                                new BooleanLiteralExpression(false)
                        )
                )
        );
    }

    @Test
    void shouldGenerateArithmeticComparison() {

        Expression sum =
                new BinaryExpression(
                        property("a", "age"),
                        BinaryOperator.ADD,
                        new IntegerLiteralExpression(1)
                );

        String goal = generate(
                comparison(
                        sum,
                        BinaryOperator.GREATER_THAN,
                        new IntegerLiteralExpression(18)
                ),
                "a"
        );

        assertTrue(
                goal.contains(
                        "__TMP_1 is (__A_AGE + 1)"
                )
        );

        assertTrue(
                goal.contains(
                        "(__TMP_1 > 18)"
                )
        );
    }

    @Test
    void shouldGenerateRelationshipPropertyComparison() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        String goal = builder.generate(
                comparison(
                        property("r", "grade"),
                        BinaryOperator.GREATER_THAN_OR_EQUAL,
                        new FloatLiteralExpression(9.5)
                ),
                symbols,
                Set.of("r")
        );

        assertTrue(
                goal.contains(
                        "user_amigos_prop(__R, 'grade', __R_GRADE)"
                )
        );

        assertTrue(
                goal.contains(
                        "(__R_GRADE >= 9.5)"
                )
        );

        assertEquals(
                "__R_GRADE",
                symbols.propertyMappings().get("r.grade")
        );
    }

    @Test
    void shouldEscapeSingleQuotesInStrings() {

        String goal = generate(
                comparison(
                        property("a", "name"),
                        BinaryOperator.EQUAL,
                        new StringLiteralExpression("O'Brien")
                ),
                "a"
        );

        assertTrue(
                goal.contains(
                        "cy_eq(__A_NAME, 'O''Brien')"
                )
        );
    }

    @Test
    void shouldCompareMatchedVariables() {

        String goal = generate(
                comparison(
                        new VariableExpression("a"),
                        BinaryOperator.NOT_EQUAL,
                        new VariableExpression("b")
                ),
                "a",
                "b"
        );

        assertEquals(
                "(cy_neq(__A, __B))",
                goal
        );
    }

    @Test
    void shouldRejectUnmatchedVariable() {

        WhereClause clause = comparison(
                property("missing", "age"),
                BinaryOperator.GREATER_THAN,
                new IntegerLiteralExpression(18)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> generate(clause, "a")
                );

        assertTrue(
                exception.getMessage().contains(
                        "unmatched variable"
                )
        );
    }

    @Test
    void shouldRejectNestedPropertyAccess() {

        PropertyExpression nested =
                new PropertyExpression(
                        property("a", "address"),
                        "city"
                );

        WhereClause clause = comparison(
                nested,
                BinaryOperator.EQUAL,
                new StringLiteralExpression("Heredia")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> generate(clause, "a")
        );
    }

    @Test
    void shouldRejectUnsupportedExpressions() {

        WhereClause nullComparison = comparison(
                property("a", "age"),
                BinaryOperator.EQUAL,
                new NullLiteralExpression()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> generate(nullComparison, "a")
        );

        WhereClause bareVariable =
                new WhereClause(
                        new VariableExpression("a")
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> generate(bareVariable, "a")
        );
    }

    @Test
    void shouldValidateNullArguments() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        WhereClause clause = comparison(
                property("a", "age"),
                BinaryOperator.EQUAL,
                new IntegerLiteralExpression(20)
        );

        assertThrows(
                NullPointerException.class,
                () -> builder.generate(
                        null,
                        symbols,
                        Set.of("a")
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> builder.generate(
                        clause,
                        null,
                        Set.of("a")
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> builder.generate(
                        clause,
                        symbols,
                        null
                )
        );
    }
}
