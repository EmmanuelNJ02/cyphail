package cr.ac.una.eif400.cyphail.ast;

import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipDirection;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipPattern;
import cr.ac.una.eif400.cyphail.ast.expression.IntegerLiteralExpression;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Sprint P2 relationship AST model.
 *
 * <p>Verifies relationship directions, optional variables,
 * types, properties, immutability and invalid values.</p>
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
 * Relationship AST unit tests.</p>
 */
class RelationshipPatternTest {

    @Test
    void supportsAllRelationshipDirections() {
        assertArrayEquals(
                new RelationshipDirection[]{
                        RelationshipDirection.OUT,
                        RelationshipDirection.IN,
                        RelationshipDirection.NONE
                },
                RelationshipDirection.values()
        );
    }

    @Test
    void storesRelationshipVariableTypeAndDirection() {
        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.of("r"),
                        List.of("TRABAJA_EN"),
                        List.of(),
                        RelationshipDirection.OUT
                );

        assertEquals(
                Optional.of("r"),
                relationship.variable()
        );

        assertEquals(
                List.of("TRABAJA_EN"),
                relationship.types()
        );

        assertTrue(relationship.properties().isEmpty());

        assertEquals(
                RelationshipDirection.OUT,
                relationship.direction()
        );
    }

    @Test
    void supportsAnonymousUndirectedRelationship() {
        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        RelationshipDirection.NONE
                );

        assertTrue(relationship.variable().isEmpty());
        assertTrue(relationship.types().isEmpty());
        assertTrue(relationship.properties().isEmpty());

        assertEquals(
                RelationshipDirection.NONE,
                relationship.direction()
        );
    }

    @Test
    void storesRelationshipProperties() {
        PatternProperty property =
                new PatternProperty(
                        "desde",
                        new IntegerLiteralExpression(2020)
                );

        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.of("r"),
                        List.of("CONOCE"),
                        List.of(property),
                        RelationshipDirection.IN
                );

        assertEquals(
                List.of(property),
                relationship.properties()
        );

        assertEquals(
                RelationshipDirection.IN,
                relationship.direction()
        );
    }

    @Test
    void copiesListsAndPreventsExternalModification() {
        List<String> types = new ArrayList<>();
        types.add("CONOCE");

        List<PatternProperty> properties =
                new ArrayList<>();

        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.of("r"),
                        types,
                        properties,
                        RelationshipDirection.OUT
                );

        types.add("TRABAJA_EN");

        properties.add(
                new PatternProperty(
                        "desde",
                        new IntegerLiteralExpression(2020)
                )
        );

        assertEquals(
                List.of("CONOCE"),
                relationship.types()
        );

        assertTrue(relationship.properties().isEmpty());

        assertThrows(
                UnsupportedOperationException.class,
                () -> relationship.types().add("OTRO")
        );
    }

    @Test
    void rejectsBlankRelationshipVariable() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RelationshipPattern(
                        Optional.of(" "),
                        List.of("CONOCE"),
                        List.of(),
                        RelationshipDirection.OUT
                )
        );
    }

    @Test
    void rejectsBlankRelationshipType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RelationshipPattern(
                        Optional.of("r"),
                        List.of(" "),
                        List.of(),
                        RelationshipDirection.OUT
                )
        );
    }

    @Test
    void rejectsNullComponents() {
        assertThrows(
                NullPointerException.class,
                () -> new RelationshipPattern(
                        null,
                        List.of(),
                        List.of(),
                        RelationshipDirection.NONE
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new RelationshipPattern(
                        Optional.empty(),
                        null,
                        List.of(),
                        RelationshipDirection.NONE
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new RelationshipPattern(
                        Optional.empty(),
                        List.of(),
                        null,
                        RelationshipDirection.NONE
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new RelationshipPattern(
                        Optional.empty(),
                        List.of(),
                        List.of(),
                        null
                )
        );
    }
}