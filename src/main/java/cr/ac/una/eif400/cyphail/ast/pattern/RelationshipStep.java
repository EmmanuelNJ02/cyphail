package cr.ac.una.eif400.cyphail.ast.pattern;

import cr.ac.una.eif400.cyphail.ast.AstNode;

import java.util.Objects;

/**
 * Immutable AST node representing one relationship step
 * within a Cyphail pattern path.
 *
 * <p>A relationship step contains a relationship pattern
 * and the node located to its right in the source query.</p>
 *
 * <p>For example, the pattern:</p>
 *
 * <pre>
 * (a)-[r:KNOWS]->(b)-[s:WORKS_AT]->(c)
 * </pre>
 *
 * <p>Contains two relationship steps:</p>
 * <ul>
 *     <li>Relationship r followed by node b.</li>
 *     <li>Relationship s followed by node c.</li>
 * </ul>
 *
 * <p>The direction stored in each relationship is interpreted
 * relative to the preceding node and the right node.</p>
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
 * Relationship path step AST model.</p>
 *
 * @param relationship relationship represented by this step
 * @param rightNode node appearing after the relationship
 */
public record RelationshipStep(
        RelationshipPattern relationship,
        NodePattern rightNode
) implements AstNode {

    /**
     * Validates the components of the relationship step.
     */
    public RelationshipStep {
        Objects.requireNonNull(
                relationship,
                "Relationship pattern cannot be null"
        );

        Objects.requireNonNull(
                rightNode,
                "Right node cannot be null"
        );
    }
}