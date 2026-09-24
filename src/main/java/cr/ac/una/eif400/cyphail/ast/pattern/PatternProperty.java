package cr.ac.una.eif400.cyphail.ast.pattern;

import cr.ac.una.eif400.cyphail.ast.AstNode;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;

import java.util.Objects;

/**
 * Immutable AST node that represents one property inside a node pattern.
 *
 * <p>Examples are id: 1, personId: p.id or status: "cancelled".</p>
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
 * @param name property name
 * @param value property expression
 */
public record PatternProperty(
        String name,
        Expression value
) implements AstNode {

    public PatternProperty {
        Objects.requireNonNull(name, "Property name cannot be null");
        Objects.requireNonNull(value, "Property value cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Property name cannot be blank"
            );
        }
    }
}