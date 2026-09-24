package cr.ac.una.eif400.cyphail.ast.expression;

import java.util.Objects;

/**
 * Immutable AST node that represents a property access expression.
 *
 * <p>Examples are m.title, p.age, p.id or o.total.</p>
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
 * @param target expression on which the property is accessed
 * @param property property name
 */
public record PropertyExpression(
        Expression target,
        String property
) implements Expression {

    public PropertyExpression {
        Objects.requireNonNull(target, "Property target cannot be null");
        Objects.requireNonNull(property, "Property name cannot be null");

        if (property.isBlank()) {
            throw new IllegalArgumentException(
                    "Property name cannot be blank"
            );
        }
    }
}