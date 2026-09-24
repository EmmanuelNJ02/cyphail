package cr.ac.una.eif400.cyphail.ast.expression;

import java.util.Objects;

/**
 * Immutable AST node that represents a string literal.
 *
 * <p>Examples are "adult", "cancelled" or "retired".</p>
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
 * @param value string value without surrounding quotes
 */
public record StringLiteralExpression(String value)
        implements LiteralExpression {

    public StringLiteralExpression {
        Objects.requireNonNull(value, "String literal cannot be null");
    }
}