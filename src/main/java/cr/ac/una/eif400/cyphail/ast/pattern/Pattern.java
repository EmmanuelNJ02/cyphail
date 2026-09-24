package cr.ac.una.eif400.cyphail.ast.pattern;

import cr.ac.una.eif400.cyphail.ast.AstNode;

import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node that represents the pattern section of a Cyphail MATCH.
 *
 * <p>For Sprint P1, the official test cases use node patterns only.
 * A pattern may contain one or more node patterns separated by commas.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * MATCH (m:Movie)
 * MATCH (m:Movie), (p:Person)
 * MATCH (p:Person {id: 1}), (o:Order {personId: p.id})
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
 * @param nodes node patterns in source order
 */
public record Pattern(
        List<NodePattern> nodes
) implements AstNode {

    public Pattern {
        Objects.requireNonNull(nodes, "Pattern nodes cannot be null");

        nodes = List.copyOf(nodes);

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "A pattern must contain at least one node"
            );
        }
    }
}