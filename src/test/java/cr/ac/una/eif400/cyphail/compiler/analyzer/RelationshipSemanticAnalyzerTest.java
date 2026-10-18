package cr.ac.una.eif400.cyphail.compiler.analyzer;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for relationship semantic validation in Sprint P2.
 *
 * <p>Tests relationship variable uniqueness, node/relationship
 * variable conflicts, reserved identifiers, valid node joins,
 * variable references and relationship properties.</p>
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
 * Relationship-aware semantic analyzer unit tests.</p>
 */
class RelationshipSemanticAnalyzerTest {

    private final CyphailParser parser = new CyphailParser();
    private final SemanticAnalyzer analyzer = new SemanticAnalyzer();

    private SemanticAnalysisResult analyze(String source) {
        Query query = parser.parseOrThrow(source);
        return analyzer.analyze(query);
    }

    @Test
    void acceptsRepeatedNodeVariablesForJoins() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[r:KNOWS]->(b), "
                        + "(b)-[s:WORKS_AT]->(c) "
                        + "RETURN a, b, c, r, s"
        );

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void rejectsRepeatedRelationshipVariableAcrossPaths() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[r:KNOWS]->(b), "
                        + "(c)-[r:WORKS_AT]->(d) "
                        + "RETURN a"
        );

        assertFalse(result.isValid());

        assertEquals(
                List.of(
                        "Relationship variable 'r' "
                                + "is declared more than once in the query."
                ),
                result.errors()
        );
    }

    @Test
    void rejectsRepeatedRelationshipVariableInOnePath() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[r:KNOWS]->(b)"
                        + "-[r:WORKS_AT]->(c) RETURN a"
        );

        assertTrue(result.hasErrors());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Relationship variable 'r' "
                                        + "is declared more than once"
                        )
                )
        );
    }

    @Test
    void rejectsNodeVariableUsedAsRelationship() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[a:KNOWS]->(b) RETURN a"
        );

        assertFalse(result.isValid());

        assertTrue(
                result.errors().contains(
                        "Variable 'a' cannot be used as both "
                                + "a node and a relationship."
                )
        );
    }

    @Test
    void rejectsRelationshipVariableUsedAsNode() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[r:KNOWS]->(r) RETURN a"
        );

        assertFalse(result.isValid());

        assertTrue(
                result.errors().contains(
                        "Variable 'r' cannot be used as both "
                                + "a node and a relationship."
                )
        );
    }

    @Test
    void rejectsNodeVariableWithReservedPrefix() {
        SemanticAnalysisResult result = analyze(
                "MATCH (__person:Person) RETURN __person"
        );

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Variable '__person' "
                                        + "cannot start with '__'"
                        )
                )
        );
    }

    @Test
    void rejectsRelationshipVariableWithReservedPrefix() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[__relation:KNOWS]->(b) RETURN a"
        );

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Variable '__relation' "
                                        + "cannot start with '__'"
                        )
                )
        );
    }

    @Test
    void acceptsVariablesWithSingleUnderscore() {
        SemanticAnalysisResult result = analyze(
                "MATCH (_a)-[_r:KNOWS]->(_b) RETURN _a, _r"
        );

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void acceptsRelationshipVariableInWhereAndReturn() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[r:KNOWS]->(b) "
                        + "WHERE r.since < 2020 "
                        + "RETURN r, a, b"
        );

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void acceptsRelationshipVariableInDelete() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[r:KNOWS]->(b) "
                        + "DELETE r RETURN a"
        );

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void acceptsRelationshipDeclaredInCreate() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a), (b) "
                        + "CREATE (a)-[r:KNOWS]->(b) "
                        + "RETURN r"
        );

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void rejectsUndefinedVariableInRelationshipProperties() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[r:KNOWS "
                        + "{since: unknown.year}]->(b) "
                        + "RETURN a"
        );

        assertFalse(result.isValid());

        assertTrue(
                result.errors().contains(
                        "Undefined variable 'unknown' "
                                + "in MATCH pattern."
                )
        );
    }

    @Test
    void rejectsRepeatedRelationshipAcrossMatchAndCreate() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[r:KNOWS]->(b) "
                        + "CREATE (b)-[r:WORKS_AT]->(c) "
                        + "RETURN a"
        );

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Relationship variable 'r' "
                                        + "is declared more than once"
                        )
                )
        );
    }

    @Test
    void rejectsUndefinedRelationshipVariableInReturn() {
        SemanticAnalysisResult result = analyze(
                "MATCH (a)-[:KNOWS]->(b) RETURN r"
        );

        assertFalse(result.isValid());

        assertEquals(
                List.of(
                        "Undefined variable 'r' in RETURN clause."
                ),
                result.errors()
        );
    }
}