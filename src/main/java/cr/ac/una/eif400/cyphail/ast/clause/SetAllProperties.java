package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.expression.Expression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;

import java.util.Objects;

/**
 * Immutable AST node that represents assigning an expression
 * to a complete Cyphail variable using SET.
 *
 * <p>Example:</p>
 * <pre>
 * SET p = value
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
 * @param variable variable being assigned
 * @param value assigned expression
 */
public record SetAllProperties(
        VariableExpression variable,
        Expression value
) implements SetItem {

    public SetAllProperties {
        Objects.requireNonNull(
                variable,
                "SET variable cannot be null"
        );

        Objects.requireNonNull(
                value,
                "SET value cannot be null"
        );
    }
}