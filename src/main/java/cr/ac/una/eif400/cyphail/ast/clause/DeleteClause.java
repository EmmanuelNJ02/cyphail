package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.expression.Expression;

import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node that represents a DELETE clause.
 *
 * <p>The clause may optionally use DETACH and contains one or more
 * expressions identifying the values to delete.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * DELETE o
 * DETACH DELETE p
 * DELETE p, o
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
 * @param detach whether DETACH is present
 * @param expressions expressions to delete
 */
public record DeleteClause(
        boolean detach,
        List<Expression> expressions
) implements UpdatingClause {

    public DeleteClause {
        Objects.requireNonNull(
                expressions,
                "DELETE expressions cannot be null"
        );

        expressions = List.copyOf(expressions);

        if (expressions.isEmpty()) {
            throw new IllegalArgumentException(
                    "DELETE must contain at least one expression"
            );
        }
    }
}