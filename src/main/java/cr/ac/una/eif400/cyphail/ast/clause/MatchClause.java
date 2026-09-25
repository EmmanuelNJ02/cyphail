package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;

import java.util.Objects;

/**
 * Immutable AST node that represents a MATCH clause.
 *
 * <p>The clause contains the pattern matched by the query.
 * WHERE is represented separately in the abstract syntax tree.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * MATCH (m:Movie)
 * MATCH (m:Movie), (p:Person)
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
 * @param pattern pattern matched by the query
 */
public record MatchClause(
        Pattern pattern
) implements Clause {

    public MatchClause {
        Objects.requireNonNull(pattern, "MATCH pattern cannot be null");
    }
}