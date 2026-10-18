package cr.ac.una.eif400.cyphail.ast.pattern;

import cr.ac.una.eif400.cyphail.ast.AstNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node representing a collection of Cyphail patterns.
 *
 * <p>Sprint P1 supports isolated node patterns separated by commas.
 * Sprint P2 extends this model to support relationship paths while
 * preserving the existing node-based API.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * MATCH (m:Movie)
 * MATCH (m:Movie), (p:Person)
 * MATCH (a)-[r:KNOWS]->(b)
 * MATCH (a)-[r:KNOWS]->(b)-[s:WORKS_AT]->(c)
 * MATCH (a)-[:KNOWS]->(b), (b)-[:KNOWS]->(c)
 * </pre>
 *
 * <p>The paths collection preserves the order of comma-separated
 * patterns and the relationship steps within each path.</p>
 *
 * <p>The nodes collection provides a flattened view of all nodes
 * in source order, preserving compatibility with Sprint P1.</p>
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
 * Relationship path integration with backward compatibility.</p>
 *
 * @param nodes flattened node patterns in source order
 * @param paths ordered pattern paths
 */
public record Pattern(
        List<NodePattern> nodes,
        List<PatternPath> paths
) implements AstNode {

    /**
     * Creates a pattern from isolated nodes.
     *
     * <p>This constructor preserves the original Sprint P1 API.</p>
     *
     * @param nodes node patterns in source order
     */
    public Pattern(List<NodePattern> nodes) {
        this(
                nodes,
                createIsolatedPaths(nodes)
        );
    }

    /**
     * Validates the pattern and stores immutable collections.
     */
    public Pattern {
        Objects.requireNonNull(
                nodes,
                "Pattern nodes cannot be null"
        );

        Objects.requireNonNull(
                paths,
                "Pattern paths cannot be null"
        );

        nodes = List.copyOf(nodes);
        paths = List.copyOf(paths);

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "A pattern must contain at least one node"
            );
        }

        if (paths.isEmpty()) {
            throw new IllegalArgumentException(
                    "A pattern must contain at least one path"
            );
        }

        List<NodePattern> pathNodes = flattenNodes(paths);

        if (!pathNodes.equals(nodes)) {
            throw new IllegalArgumentException(
                    "Pattern nodes must match path nodes in source order"
            );
        }
    }

    /**
     * Creates a pattern from one or more relationship paths.
     *
     * <p>The flattened node collection is derived automatically
     * from the provided paths.</p>
     *
     * @param paths pattern paths in source order
     * @return immutable pattern containing nodes and paths
     */
    public static Pattern fromPaths(
            List<PatternPath> paths
    ) {
        Objects.requireNonNull(
                paths,
                "Pattern paths cannot be null"
        );

        List<PatternPath> snapshot = List.copyOf(paths);

        return new Pattern(
                flattenNodes(snapshot),
                snapshot
        );
    }

    /**
     * Returns whether this pattern contains any relationships.
     *
     * @return true when at least one path contains a relationship
     */
    public boolean hasRelationships() {
        return paths.stream()
                .anyMatch(PatternPath::hasRelationships);
    }

    /**
     * Returns the total number of relationships in the pattern.
     *
     * @return number of relationship steps across all paths
     */
    public int relationshipCount() {
        return paths.stream()
                .mapToInt(PatternPath::relationshipCount)
                .sum();
    }

    /**
     * Converts Sprint P1 node patterns into isolated paths.
     *
     * @param nodes existing node patterns
     * @return ordered isolated paths
     */
    private static List<PatternPath> createIsolatedPaths(
            List<NodePattern> nodes
    ) {
        Objects.requireNonNull(
                nodes,
                "Pattern nodes cannot be null"
        );

        return nodes.stream()
                .map(node -> new PatternPath(
                        node,
                        List.of()
                ))
                .toList();
    }

    /**
     * Collects every node from the supplied paths in source order.
     *
     * @param paths paths to flatten
     * @return immutable list of node patterns
     */
    private static List<NodePattern> flattenNodes(
            List<PatternPath> paths
    ) {
        List<NodePattern> result = new ArrayList<>();

        for (PatternPath path : paths) {
            result.add(path.firstNode());

            for (RelationshipStep step : path.steps()) {
                result.add(step.rightNode());
            }
        }

        return List.copyOf(result);
    }
}