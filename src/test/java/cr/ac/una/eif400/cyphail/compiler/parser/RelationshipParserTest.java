package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.expression.IntegerLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternPath;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipDirection;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipPattern;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Sprint P2 relationship parsing.
 *
 * <p>Verifies relationship directions, variables, types,
 * properties, chained relationships, comma-separated paths
 * and backward compatibility with Sprint P1.</p>
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
 * Functional relationship parser unit tests.</p>
 */
class RelationshipParserTest {

    private final CyphailParser parser = new CyphailParser();

    @Test
    void parsesOutgoingRelationship() {
        Query query = parser.parseOrThrow(
                "MATCH (a:Person)-[r:KNOWS]->(b:Person) RETURN a, b"
        );

        Pattern pattern = query.match().pattern();

        assertEquals(1, pattern.relationshipCount());
        assertEquals(2, pattern.nodes().size());

        RelationshipPattern relationship =
                pattern.paths().get(0)
                        .steps().get(0).relationship();

        assertEquals(
                RelationshipDirection.OUT,
                relationship.direction()
        );

        assertEquals(
                Optional.of("r"),
                relationship.variable()
        );

        assertEquals(
                List.of("KNOWS"),
                relationship.types()
        );
    }

    @Test
    void parsesIncomingRelationship() {
        Query query = parser.parseOrThrow(
                "MATCH (a)<-[r:KNOWS]-(b) RETURN a"
        );

        RelationshipPattern relationship =
                query.match().pattern()
                        .paths().get(0)
                        .steps().get(0).relationship();

        assertEquals(
                RelationshipDirection.IN,
                relationship.direction()
        );

        assertEquals(
                Optional.of("r"),
                relationship.variable()
        );
    }

    @Test
    void parsesUndirectedRelationship() {
        Query query = parser.parseOrThrow(
                "MATCH (a)-[r:KNOWS]-(b) RETURN a"
        );

        RelationshipPattern relationship =
                query.match().pattern()
                        .paths().get(0)
                        .steps().get(0).relationship();

        assertEquals(
                RelationshipDirection.NONE,
                relationship.direction()
        );
    }

    @Test
    void parsesRelationshipProperties() {
        Query query = parser.parseOrThrow(
                "MATCH (a)-[r:KNOWS {since: 2020}]->(b) RETURN r"
        );

        RelationshipPattern relationship =
                query.match().pattern()
                        .paths().get(0)
                        .steps().get(0).relationship();

        assertEquals(1, relationship.properties().size());

        PatternProperty property =
                relationship.properties().get(0);

        assertEquals("since", property.name());

        IntegerLiteralExpression value =
                assertInstanceOf(
                        IntegerLiteralExpression.class,
                        property.value()
                );

        assertEquals(2020L, value.value());
    }

    @Test
    void parsesAnonymousRelationship() {
        Query query = parser.parseOrThrow(
                "MATCH (a)-[:KNOWS]->(b) RETURN a"
        );

        RelationshipPattern relationship =
                query.match().pattern()
                        .paths().get(0)
                        .steps().get(0).relationship();

        assertTrue(relationship.variable().isEmpty());

        assertEquals(
                List.of("KNOWS"),
                relationship.types()
        );

        assertEquals(
                RelationshipDirection.OUT,
                relationship.direction()
        );
    }

    @Test
    void parsesChainedRelationships() {
        Query query = parser.parseOrThrow(
                "MATCH (a)-[r:KNOWS]->(b)-[s:WORKS_AT]->(c) RETURN a"
        );

        Pattern pattern = query.match().pattern();

        assertEquals(1, pattern.paths().size());
        assertEquals(3, pattern.nodes().size());
        assertEquals(2, pattern.relationshipCount());

        PatternPath path = pattern.paths().get(0);

        assertEquals(
                Optional.of("a"),
                path.firstNode().variable()
        );

        assertEquals(
                Optional.of("b"),
                path.steps().get(0).rightNode().variable()
        );

        assertEquals(
                Optional.of("c"),
                path.steps().get(1).rightNode().variable()
        );

        assertEquals(
                List.of("KNOWS"),
                path.steps().get(0).relationship().types()
        );

        assertEquals(
                List.of("WORKS_AT"),
                path.steps().get(1).relationship().types()
        );
    }

    @Test
    void parsesCommaSeparatedRelationshipPaths() {
        Query query = parser.parseOrThrow(
                "MATCH (a)-[:KNOWS]->(b), "
                        + "(b)-[:WORKS_AT]->(c) "
                        + "RETURN a, c"
        );

        Pattern pattern = query.match().pattern();

        assertEquals(2, pattern.paths().size());
        assertEquals(4, pattern.nodes().size());
        assertEquals(2, pattern.relationshipCount());

        assertEquals(
                Optional.of("b"),
                pattern.paths().get(0)
                        .steps().get(0).rightNode().variable()
        );

        assertEquals(
                Optional.of("b"),
                pattern.paths().get(1)
                        .firstNode().variable()
        );
    }

    @Test
    void rejectsIncompleteRelationshipPattern() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "MATCH (a)-[r:KNOWS]-> RETURN a"
                )
        );
    }

    @Test
    void preservesSprintP1NodeOnlyPatterns() {
        Query query = parser.parseOrThrow(
                "MATCH (a:Person), (b:Company) RETURN a, b"
        );

        Pattern pattern = query.match().pattern();

        assertEquals(2, pattern.nodes().size());
        assertEquals(2, pattern.paths().size());
        assertEquals(0, pattern.relationshipCount());

        assertFalse(pattern.hasRelationships());
    }
}