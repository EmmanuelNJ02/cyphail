package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.expression.Expression;
import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;

import java.util.Objects;

/**
 * Immutable AST node that represents a SET property assignment.
 *
 * <p>Example:</p>
 * <pre>
 * SET p.age = 30
 * </pre>
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
 * @param property property being assigned
 * @param value assigned expression
 */
public record SetProperty(
        PropertyExpression property,
        Expression value
) implements SetItem {

    public SetProperty {
        Objects.requireNonNull(
                property,
                "SET property cannot be null"
        );

        Objects.requireNonNull(
                value,
                "SET property value cannot be null"
        );
    }
}