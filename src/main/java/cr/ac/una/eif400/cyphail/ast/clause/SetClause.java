package cr.ac.una.eif400.cyphail.ast.clause;

import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node that represents a SET clause.
 *
 * <p>A SET clause contains one or more set items in the same
 * order in which they appear in the source query.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * SET p.age = 30
 * SET p.age = 30, p:Employee
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
 * @param items SET items in source order
 */
public record SetClause(
        List<SetItem> items
) implements UpdatingClause {

    public SetClause {
        Objects.requireNonNull(
                items,
                "SET items cannot be null"
        );

        items = List.copyOf(items);

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "SET must contain at least one item"
            );
        }
    }
}