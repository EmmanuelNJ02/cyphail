package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;

import java.util.Objects;

/**
 * Immutable AST node that represents a CREATE clause.
 *
 * <p>The clause stores the pattern that should be created.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * CREATE (c:Certificate {issuedTo: "adult", year: 2026})
 * CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
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
 * @param pattern pattern created by the clause
 */
public record CreateClause(
        Pattern pattern
) implements UpdatingClause {

    public CreateClause {
        Objects.requireNonNull(
                pattern,
                "CREATE pattern cannot be null"
        );
    }
}