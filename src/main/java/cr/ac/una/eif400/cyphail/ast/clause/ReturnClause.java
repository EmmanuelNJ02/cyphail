package cr.ac.una.eif400.cyphail.ast.clause;

import java.util.Objects;

/**
 * Immutable AST node that represents a RETURN clause.
 *
 * <p>The clause may optionally use DISTINCT and contains
 * the projection produced by RETURN.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * RETURN m.title, m.year AS year
 * RETURN DISTINCT p.name AS name
 * RETURN *
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
 * @param distinct whether DISTINCT is present
 * @param projection RETURN projection
 */
public record ReturnClause(
        boolean distinct,
        Projection projection
) implements Clause {

    public ReturnClause {
        Objects.requireNonNull(
                projection,
                "RETURN projection cannot be null"
        );
    }
}