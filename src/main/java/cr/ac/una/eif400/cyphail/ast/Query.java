package cr.ac.una.eif400.cyphail.ast;

import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.MatchClause;
import cr.ac.una.eif400.cyphail.ast.clause.ReturnClause;
import cr.ac.una.eif400.cyphail.ast.clause.UpdatingClause;
import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Root AST node representing a Cyphail query.
 *
 * <p>Sprint P1 queries begin with a MATCH clause, followed
 * by optional WHERE, updating clauses and RETURN.</p>
 *
 * <p>Sprint P2 additionally supports standalone CREATE
 * statements without a preceding MATCH clause.</p>
 *
 * <p>The original MATCH accessor and record components
 * are preserved for backward compatibility.</p>
 *
 * <p>When a query starts with CREATE, the match component
 * is null. The optionalMatch() method provides a safe
 * representation of the optional MATCH clause.</p>
 *
 * <p>Examples:</p>
 *
 * <pre>
 * MATCH (p:Person)
 * RETURN p
 *
 * CREATE (p:Person {name: 'Marta'})
 * RETURN p
 *
 * MATCH (p:Person)
 * CREATE (e:Employee)
 * RETURN p, e
 * </pre>
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
 * @param match MATCH clause, or null for standalone CREATE
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

    /**
     * Validates the query structure and stores an
     * immutable copy of the updating clauses.
     *
     * <p>A query without MATCH must begin with CREATE.
     * WHERE is only accepted when MATCH is present.</p>
     */
    public Query {
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

        if (match == null) {
            if (where.isPresent()) {
                throw new IllegalArgumentException(
                        "WHERE requires a MATCH clause"
                );
            }

            if (updates.isEmpty()
                    || !(updates.getFirst() instanceof CreateClause)) {
                throw new IllegalArgumentException(
                        "A query without MATCH must start with CREATE"
                );
            }
        }
    }

    /**
     * Indicates whether the query contains MATCH.
     *
     * @return true when a MATCH clause is present
     */
    public boolean hasMatch() {
        return match != null;
    }

    /**
     * Returns the MATCH clause as an Optional.
     *
     * <p>This method should be used by P2 components
     * that support standalone CREATE statements.</p>
     *
     * @return optional MATCH clause
     */
    public Optional<MatchClause> optionalMatch() {
        return Optional.ofNullable(match);
    }
}