package cr.ac.una.eif400.cyphail.ast;

import cr.ac.una.eif400.cyphail.ast.clause.MatchClause;
import cr.ac.una.eif400.cyphail.ast.clause.ReturnClause;
import cr.ac.una.eif400.cyphail.ast.clause.UpdatingClause;
import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Root AST node that represents a Cyphail query for Sprint P1.
 *
 * <p>The P1 reference queries start with MATCH, may contain WHERE,
 * may contain updating clauses such as CREATE or DELETE, and may
 * finish with RETURN.</p>
 *
 * <p>The structure intentionally mirrors the logical AST sections
 * used by the Sprint P1 tree representation:</p>
 *
 * <pre>
 * Query
 *   Match
 *   Where
 *   Updates
 *   Return
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
 * @param match mandatory MATCH clause
 * @param where optional WHERE clause
 * @param updates updating clauses in source order
 * @param returnClause optional RETURN clause
 */
public record Query(
        MatchClause match,
        Optional<WhereClause> where,
        List<UpdatingClause> updates,
        Optional<ReturnClause> returnClause
) implements AstNode {

    public Query {
        Objects.requireNonNull(
                match,
                "MATCH clause cannot be null"
        );

        Objects.requireNonNull(
                where,
                "WHERE clause container cannot be null"
        );

        Objects.requireNonNull(
                updates,
                "Updating clauses cannot be null"
        );

        Objects.requireNonNull(
                returnClause,
                "RETURN clause container cannot be null"
        );

        updates = List.copyOf(updates);
    }
}