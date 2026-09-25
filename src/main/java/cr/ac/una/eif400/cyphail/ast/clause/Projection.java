package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.AstNode;

import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node that represents the projection of a RETURN clause.
 *
 * <p>A projection contains the projected items, may include the
 * wildcard *, and stores its ORDER BY, SKIP and LIMIT modifiers.</p>
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
 * @param wildcard whether * is projected
 * @param items explicit projection items in source order
 * @param modifiers projection modifiers
 */
public record Projection(
        boolean wildcard,
        List<ProjectionItem> items,
        ProjectionModifiers modifiers
) implements AstNode {

    public Projection {
        Objects.requireNonNull(
                items,
                "Projection items cannot be null"
        );

        Objects.requireNonNull(
                modifiers,
                "Projection modifiers cannot be null"
        );

        items = List.copyOf(items);

        if (!wildcard && items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Projection must contain at least one item"
            );
        }
    }
}