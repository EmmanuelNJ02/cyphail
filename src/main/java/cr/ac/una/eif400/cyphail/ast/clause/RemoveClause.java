package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;

import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node representing a REMOVE clause.
 *
 * <p>Sprint P2 supports removing properties from
 * node and relationship variables.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * REMOVE p.discount
 * REMOVE r.grade
 * REMOVE p.discount, p.oldPrice
 * </pre>
 *
 * <p>The properties are stored in their original
 * source order as an immutable list.</p>
 *
 * <p>Project: Cyphail - Sprint P2</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica</p>
 * <p>Escuela de Informatica</p>
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
 * @param properties properties to remove
 */
public record RemoveClause(
        List<PropertyExpression> properties
) implements UpdatingClause {

    /**
     * Validates and preserves the immutable properties.
     */
    public RemoveClause {
        Objects.requireNonNull(
                properties,
                "REMOVE properties cannot be null"
        );

        properties = List.copyOf(properties);

        if (properties.isEmpty()) {
            throw new IllegalArgumentException(
                    "REMOVE must contain at least one property"
            );
        }
    }
}