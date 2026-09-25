package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.AstNode;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable AST node that represents one ORDER BY sort item.
 *
 * <p>A sort item contains an expression and may optionally specify
 * ASCENDING or DESCENDING.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * ORDER BY p.name
 * ORDER BY p.age ASCENDING
 * ORDER BY p.age DESCENDING
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
 * @param expression expression used for sorting
 * @param direction optional sort direction
 */
public record SortItem(
        Expression expression,
        Optional<SortDirection> direction
) implements AstNode {

    public SortItem {
        Objects.requireNonNull(
                expression,
                "Sort expression cannot be null"
        );

        Objects.requireNonNull(
                direction,
                "Sort direction cannot be null"
        );
    }
}