package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.expression.Expression;

import java.util.Objects;

/**
 * Immutable AST node that represents a WHERE clause.
 *
 * <p>The clause stores the expression used to filter a Cyphail query.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * WHERE b.pages &lt; 300
 * WHERE a.age &gt; 30
 * WHERE m.year &lt;&gt; p.age
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
 * @param expression filtering expression
 */
public record WhereClause(
        Expression expression
) implements Clause {

    public WhereClause {
        Objects.requireNonNull(
                expression,
                "WHERE expression cannot be null"
        );
    }
}