package cr.ac.una.eif400.cyphail.compiler.analyzer;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests semantic analysis of standalone CREATE queries.
 *
 * Sprint P2 allows CREATE without a preceding MATCH.
 * The semantic analyzer must register declared variables
 * and preserve the existing relationship validation rules.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * Group Code: 04-10am
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
class SemanticAnalyzerP2CreateTest {

    private final CyphailParser parser =
            new CyphailParser();

    private final SemanticAnalyzer analyzer =
            new SemanticAnalyzer();

    @Test
    void shouldAcceptStandaloneCreateNode() {
        Query query = parser.parseOrThrow(
                "CREATE (a:Persona {nombre: 'Marta'}) RETURN a"
        );

        SemanticAnalysisResult result = analyzer.analyze(query);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldAcceptStandaloneCreateRelationship() {
        Query query = parser.parseOrThrow("""
                CREATE (a:Persona {nombre: 'Marta'})
                       -[r:TRABAJA_EN {rol: 'Dev'}]->
                       (e:Empresa {nombre: 'UCR'})
                RETURN a, r, e
                """);

        SemanticAnalysisResult result = analyzer.analyze(query);

        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void shouldRejectReservedVariableInStandaloneCreate() {
        Query query = parser.parseOrThrow(
                "CREATE (__person:Persona) RETURN __person"
        );

        SemanticAnalysisResult result = analyzer.analyze(query);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().stream().anyMatch(
                        error -> error.contains(
                                "Variable '__person' cannot start with '__'"
                        )
                )
        );
    }

    @Test
    void shouldRejectNodeRelationshipVariableConflict() {
        Query query = parser.parseOrThrow(
                "CREATE (a:Persona)-[a:CONOCE]->(b:Persona) RETURN b"
        );

        SemanticAnalysisResult result = analyzer.analyze(query);

        assertFalse(result.isValid());

        assertTrue(
                result.errors().contains(
                        "Variable 'a' cannot be used as both "
                                + "a node and a relationship."
                )
        );
    }

    @Test
    void shouldRejectRepeatedRelationshipVariable() {
        Query query = parser.parseOrThrow("""
                CREATE (a:Persona)-[r:CONOCE]->(b:Persona),
                       (b)-[r:TRABAJA_CON]->(c:Persona)
                RETURN a
                """);

        SemanticAnalysisResult result = analyzer.analyze(query);

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
}