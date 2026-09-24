package cr.ac.una.eif400.cyphail.ast.expression;

import java.util.Objects;

/**
 * Immutable AST node that represents a binary expression.
 *
 * <p>A binary expression combines a left expression, an operator and
 * a right expression. Examples include b.pages < 300,
 * m.year > 2000 and m.year <> p.age.</p>
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
 *
 * @param left left operand
 * @param operator binary operator
 * @param right right operand
 */
public record BinaryExpression(
        Expression left,
        BinaryOperator operator,
        Expression right
) implements Expression {

    public BinaryExpression {
        Objects.requireNonNull(left, "Left expression cannot be null");
        Objects.requireNonNull(operator, "Binary operator cannot be null");
        Objects.requireNonNull(right, "Right expression cannot be null");
    }
}