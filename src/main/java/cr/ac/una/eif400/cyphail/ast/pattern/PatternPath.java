package cr.ac.una.eif400.cyphail.ast.pattern;

import cr.ac.una.eif400.cyphail.ast.AstNode;

import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node representing a path in a Cyphail pattern.
 *
 * <p>A pattern path starts with a node and contains zero or more
 * relationship steps. Each step includes a relationship and the
 * next node in the path.</p>
 *
 * <p>Example:</p>
 * <pre>
 * (a:Person)-[r:KNOWS]->(b:Person)-[s:WORKS_AT]->(c:Company)
 * </pre>
 *
 * <p>The corresponding AST path contains:</p>
 * <ul>
 *     <li>First node: a</li>
 *     <li>First step: relationship r followed by node b</li>
 *     <li>Second step: relationship s followed by node c</li>
 * </ul>
 *
 * <p>A path with no relationship steps represents an isolated node.
 * Relationship directions are stored in their respective
 * RelationshipPattern objects.</p>
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
 * <p>Sprint P2 contribution:
 * Pattern path AST model.</p>
 *
 * @param firstNode first node of the pattern path
 * @param steps ordered relationship steps in the path
 */
public record PatternPath(
        NodePattern firstNode,
        List<RelationshipStep> steps
) implements AstNode {

    /**
     * Validates the pattern path and creates an immutable
     * snapshot of its relationship steps.
     */
    public PatternPath {
        Objects.requireNonNull(
                firstNode,
                "First node cannot be null"
        );

        Objects.requireNonNull(
                steps,
                "Relationship steps cannot be null"
        );

        steps = List.copyOf(steps);
    }

    /**
     * Checks whether this path contains at least one relationship.
     *
     * @return true when the path contains a relationship
     */
    public boolean hasRelationships() {
        return !steps.isEmpty();
    }

    /**
     * Returns the number of relationships in this path.
     *
     * @return number of relationship steps
     */
    public int relationshipCount() {
        return steps.size();
    }
}