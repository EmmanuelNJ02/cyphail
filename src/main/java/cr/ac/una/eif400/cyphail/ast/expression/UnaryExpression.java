package cr.ac.una.eif400.cyphail.ast.expression;

import java.util.Objects;

/**
 * Immutable AST node representing a unary expression.
 *
 * A unary expression applies an operator to a single
 * operand. Sprint P2 supports the logical NOT operator.
 *
 * Example:
 *
 * NOT e.country = 'CR'
 *
 * The operand represents the complete comparison,
 * allowing NOT to negate its boolean result.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * School: School of Informatics
 * Group Code: 04-10am
 */
public record UnaryExpression(
        UnaryOperator operator,
        Expression operand
) implements Expression {

    public UnaryExpression {
        Objects.requireNonNull(
                operator,
                "Unary operator cannot be null"
        );

        Objects.requireNonNull(
                operand,
                "Unary operand cannot be null"
        );
    }
}
