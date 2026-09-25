package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.AstNode;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable AST node that represents RETURN projection modifiers.
 *
 * <p>The Sprint P1 grammar allows optional ORDER BY, SKIP and LIMIT
 * modifiers after the projection items.</p>
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
 * @param orderBy ordered sort items
 * @param skip optional SKIP expression
 * @param limit optional LIMIT expression
 */
public record ProjectionModifiers(
        List<SortItem> orderBy,
        Optional<Expression> skip,
        Optional<Expression> limit
) implements AstNode {

    public ProjectionModifiers {
        Objects.requireNonNull(
                orderBy,
                "ORDER BY items cannot be null"
        );

        Objects.requireNonNull(
                skip,
                "SKIP expression cannot be null"
        );

        Objects.requireNonNull(
                limit,
                "LIMIT expression cannot be null"
        );

        orderBy = List.copyOf(orderBy);
    }
}