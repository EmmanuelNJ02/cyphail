package cr.ac.una.eif400.cyphail.ast.expression;

import java.util.Objects;

/**
 * Immutable AST node that represents a Cyphail variable.
 *
 * <p>Examples of variables are m, p, o or q in Cyphail queries.</p>
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
 * @param name variable identifier
 */
public record VariableExpression(String name) implements Expression {

    public VariableExpression {
        Objects.requireNonNull(name, "Variable name cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Variable name cannot be blank"
            );
        }
    }
}