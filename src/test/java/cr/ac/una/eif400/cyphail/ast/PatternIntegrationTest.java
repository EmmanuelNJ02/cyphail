package cr.ac.una.eif400.cyphail.ast;

import cr.ac.una.eif400.cyphail.ast.pattern.NodePattern;
import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternPath;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipDirection;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipPattern;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipStep;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Sprint P2 Pattern AST model.
 *
 * <p>Verifies backward compatibility with Sprint P1, relationship
 * paths, ordered node traversal, comma-separated patterns,
 * immutability and invalid pattern structures.</p>
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
 * Pattern AST integration and backward compatibility tests.</p>
 */
class PatternIntegrationTest {

    @Test
    void preservesSprintP1NodeBasedConstructor() {
        NodePattern first = node("a");
        NodePattern second = node("b");

        Pattern pattern = new Pattern(
                List.of(first, second)
        );

        assertEquals(
                List.of(first, second),
                pattern.nodes()
        );

        assertEquals(2, pattern.paths().size());

        assertEquals(
                first,
                pattern.paths().get(0).firstNode()
        );

        assertEquals(
                second,
                pattern.paths().get(1).firstNode()
        );

        assertFalse(pattern.hasRelationships());
        assertEquals(0, pattern.relationshipCount());
    }

    @Test
    void supportsSingleDirectedRelationship() {
        NodePattern first = node("a");
        NodePattern second = node("b");

        RelationshipStep step = new RelationshipStep(
                relationship("r", RelationshipDirection.OUT),
                second
        );

        PatternPath path = new PatternPath(
                first,
                List.of(step)
        );

        Pattern pattern = Pattern.fromPaths(
                List.of(path)
        );

        assertEquals(
                List.of(first, second),
                pattern.nodes()
        );

        assertEquals(List.of(path), pattern.paths());
        assertTrue(pattern.hasRelationships());
        assertEquals(1, pattern.relationshipCount());

        assertEquals(
                RelationshipDirection.OUT,
                pattern.paths().get(0)
                        .steps().get(0)
                        .relationship().direction()
        );
    }

    @Test
    void supportsMultipleRelationshipsInSourceOrder() {
        NodePattern first = node("a");
        NodePattern second = node("b");
        NodePattern third = node("c");

        RelationshipStep firstStep = new RelationshipStep(
                relationship("r", RelationshipDirection.OUT),
                second
        );

        RelationshipStep secondStep = new RelationshipStep(
                relationship("s", RelationshipDirection.IN),
                third
        );

        PatternPath path = new PatternPath(
                first,
                List.of(firstStep, secondStep)
        );

        Pattern pattern = Pattern.fromPaths(
                List.of(path)
        );

        assertEquals(
                List.of(first, second, third),
                pattern.nodes()
        );

        assertEquals(1, pattern.paths().size());
        assertEquals(2, pattern.relationshipCount());

        assertEquals(
                firstStep,
                pattern.paths().get(0).steps().get(0)
        );

        assertEquals(
                secondStep,
                pattern.paths().get(0).steps().get(1)
        );
    }

    @Test
    void supportsCommaSeparatedRelationshipPaths() {
        NodePattern a = node("a");
        NodePattern b = node("b");
        NodePattern c = node("c");
        NodePattern d = node("d");

        PatternPath firstPath = new PatternPath(
                a,
                List.of(
                        new RelationshipStep(
                                relationship(
                                        "r",
                                        RelationshipDirection.OUT
                                ),
                                b
                        )
                )
        );

        PatternPath secondPath = new PatternPath(
                c,
                List.of(
                        new RelationshipStep(
                                relationship(
                                        "s",
                                        RelationshipDirection.NONE
                                ),
                                d
                        )
                )
        );

        Pattern pattern = Pattern.fromPaths(
                List.of(firstPath, secondPath)
        );

        assertEquals(
                List.of(firstPath, secondPath),
                pattern.paths()
        );

        assertEquals(
                List.of(a, b, c, d),
                pattern.nodes()
        );

        assertEquals(2, pattern.relationshipCount());
    }

    @Test
    void preservesRepeatedNodeVariablesForJoins() {
        NodePattern a = node("a");
        NodePattern b = node("b");
        NodePattern bAgain = node("b");
        NodePattern c = node("c");

        PatternPath firstPath = new PatternPath(
                a,
                List.of(
                        new RelationshipStep(
                                relationship(
                                        "r",
                                        RelationshipDirection.OUT
                                ),
                                b
                        )
                )
        );

        PatternPath secondPath = new PatternPath(
                bAgain,
                List.of(
                        new RelationshipStep(
                                relationship(
                                        "s",
                                        RelationshipDirection.OUT
                                ),
                                c
                        )
                )
        );

        Pattern pattern = Pattern.fromPaths(
                List.of(firstPath, secondPath)
        );

        assertEquals(4, pattern.nodes().size());

        assertEquals(
                Optional.of("b"),
                pattern.nodes().get(1).variable()
        );

        assertEquals(
                Optional.of("b"),
                pattern.nodes().get(2).variable()
        );

        assertEquals(2, pattern.relationshipCount());
    }

    @Test
    void copiesPathsAndPreventsModification() {
        List<PatternPath> source = new ArrayList<>();

        source.add(
                new PatternPath(
                        node("a"),
                        List.of()
                )
        );

        Pattern pattern = Pattern.fromPaths(source);

        source.add(
                new PatternPath(
                        node("b"),
                        List.of()
                )
        );

        assertEquals(1, pattern.paths().size());
        assertEquals(1, pattern.nodes().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> pattern.paths().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> pattern.nodes().clear()
        );
    }

    @Test
    void rejectsEmptyOrNullPaths() {
        assertThrows(
                NullPointerException.class,
                () -> Pattern.fromPaths(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Pattern.fromPaths(List.of())
        );

        List<PatternPath> paths = new ArrayList<>();
        paths.add(null);

        assertThrows(
                NullPointerException.class,
                () -> Pattern.fromPaths(paths)
        );
    }

    @Test
    void rejectsInconsistentNodesAndPaths() {
        PatternPath path = new PatternPath(
                node("a"),
                List.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Pattern(
                        List.of(node("b")),
                        List.of(path)
                )
        );
    }

    private NodePattern node(String variable) {
        return new NodePattern(
                Optional.of(variable),
                List.of(),
                List.of()
        );
    }

    private RelationshipPattern relationship(
            String variable,
            RelationshipDirection direction
    ) {
        return new RelationshipPattern(
                Optional.of(variable),
                List.of("KNOWS"),
                List.of(),
                direction
        );
    }
}