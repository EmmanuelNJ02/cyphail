package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.AstNode;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable AST node that represents one RETURN projection item.
 *
 * <p>A projection item contains an expression and may optionally
 * define an alias using AS.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * m.title
 * m.year AS year
 * p.name AS name
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
 * @param expression projected expression
 * @param alias optional projection alias
 */
public record ProjectionItem(
        Expression expression,
        Optional<String> alias
) implements AstNode {

    public ProjectionItem {
        Objects.requireNonNull(
                expression,
                "Projection expression cannot be null"
        );

        Objects.requireNonNull(
                alias,
                "Projection alias cannot be null"
        );

        alias.ifPresent(name -> {
            if (name.isBlank()) {
                throw new IllegalArgumentException(
                        "Projection alias cannot be blank"
                );
            }
        });
    }
}