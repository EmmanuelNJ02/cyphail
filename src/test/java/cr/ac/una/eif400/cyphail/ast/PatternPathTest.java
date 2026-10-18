package cr.ac.una.eif400.cyphail.ast;

import cr.ac.una.eif400.cyphail.ast.pattern.NodePattern;
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
 * Unit tests for the Sprint P2 relationship path AST model.
 *
 * <p>Verifies relationship steps, ordered paths, isolated nodes,
 * immutability and null validation.</p>
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
 * Relationship step and pattern path unit tests.</p>
 */
class PatternPathTest {

    @Test
    void storesRelationshipStepComponents() {
        NodePattern rightNode = node("b");

        RelationshipPattern relationship =
                relationship(
                        "r",
                        RelationshipDirection.OUT
                );

        RelationshipStep step =
                new RelationshipStep(
                        relationship,
                        rightNode
                );

        assertEquals(relationship, step.relationship());
        assertEquals(rightNode, step.rightNode());
    }

    @Test
    void rejectsNullRelationshipStepComponents() {
        NodePattern rightNode = node("b");

        RelationshipPattern relationship =
                relationship(
                        "r",
                        RelationshipDirection.OUT
                );

        assertThrows(
                NullPointerException.class,
                () -> new RelationshipStep(
                        null,
                        rightNode
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new RelationshipStep(
                        relationship,
                        null
                )
        );
    }

    @Test
    void supportsIsolatedNodePath() {
        NodePattern firstNode = node("a");

        PatternPath path =
                new PatternPath(
                        firstNode,
                        List.of()
                );

        assertEquals(firstNode, path.firstNode());
        assertTrue(path.steps().isEmpty());
        assertFalse(path.hasRelationships());
        assertEquals(0, path.relationshipCount());
    }

    @Test
    void supportsMultipleOrderedRelationshipSteps() {
        NodePattern firstNode = node("a");
        NodePattern secondNode = node("b");
        NodePattern thirdNode = node("c");

        RelationshipStep firstStep =
                new RelationshipStep(
                        relationship(
                                "r",
                                RelationshipDirection.OUT
                        ),
                        secondNode
                );

        RelationshipStep secondStep =
                new RelationshipStep(
                        relationship(
                                "s",
                                RelationshipDirection.IN
                        ),
                        thirdNode
                );

        PatternPath path =
                new PatternPath(
                        firstNode,
                        List.of(firstStep, secondStep)
                );

        assertEquals(firstNode, path.firstNode());
        assertTrue(path.hasRelationships());
        assertEquals(2, path.relationshipCount());

        assertEquals(firstStep, path.steps().get(0));
        assertEquals(secondStep, path.steps().get(1));

        assertEquals(
                RelationshipDirection.OUT,
                path.steps().get(0)
                        .relationship().direction()
        );

        assertEquals(
                RelationshipDirection.IN,
                path.steps().get(1)
                        .relationship().direction()
        );

        assertEquals(
                secondNode,
                path.steps().get(0).rightNode()
        );

        assertEquals(
                thirdNode,
                path.steps().get(1).rightNode()
        );
    }

    @Test
    void copiesRelationshipStepsAndPreventsModification() {
        List<RelationshipStep> steps =
                new ArrayList<>();

        RelationshipStep firstStep =
                new RelationshipStep(
                        relationship(
                                "r",
                                RelationshipDirection.NONE
                        ),
                        node("b")
                );

        steps.add(firstStep);

        PatternPath path =
                new PatternPath(
                        node("a"),
                        steps
                );

        steps.add(
                new RelationshipStep(
                        relationship(
                                "s",
                                RelationshipDirection.OUT
                        ),
                        node("c")
                )
        );

        assertEquals(1, path.relationshipCount());
        assertEquals(firstStep, path.steps().get(0));

        assertThrows(
                UnsupportedOperationException.class,
                () -> path.steps().clear()
        );
    }

    @Test
    void rejectsNullPatternPathComponents() {
        NodePattern firstNode = node("a");

        assertThrows(
                NullPointerException.class,
                () -> new PatternPath(
                        null,
                        List.of()
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new PatternPath(
                        firstNode,
                        null
                )
        );
    }

    @Test
    void rejectsNullRelationshipStepsInsidePath() {
        List<RelationshipStep> steps =
                new ArrayList<>();

        steps.add(null);

        assertThrows(
                NullPointerException.class,
                () -> new PatternPath(
                        node("a"),
                        steps
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