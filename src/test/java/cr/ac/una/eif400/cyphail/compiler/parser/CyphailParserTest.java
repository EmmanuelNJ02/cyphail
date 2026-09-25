package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the concrete Cyphail Sprint P1 parser with the official
 * public test cases C1-C11.
 *
 * <p>C1-C9 exercise the required syntax for node patterns, WHERE,
 * projections, CREATE and DELETE. C10 and C11 are intentionally
 * syntactically valid because their undefined-variable errors belong
 * to semantic analysis rather than parsing.</p>
 *
 * <p>Project: Cyphail - Sprint P1<br>
 * Course: EIF400 - Paradigmas de Programacion<br>
 * Universidad Nacional de Costa Rica<br>
 * Group: 04-10am</p>
 *
 * @author Emmanuel Nunez Jimenez
 * @author Keynell Molina Mora
 * @author Valery Alfaro Morales
 * @author Julissa Solano Valverde
 * @author Roy Arias Mejia
 */
class CyphailParserTest {

    private final CyphailParser parser =
            new CyphailParser();

    @Test
    void parsesCase1PatternAndProjectionAlias() {
        String source = """
                MATCH (m:Movie)
                RETURN m.title,
                       m.year AS year
                """;

        assertParses(source);
    }

    @Test
    void parsesCase2WhereComparisonAndProjectionAlias() {
        String source = """
                MATCH (b:Book)
                WHERE b.pages < 300
                RETURN b.title,
                       b.pages AS totalPages
                """;

        assertParses(source);
    }

    @Test
    void parsesCase3MultipleLabelsAndProperty() {
        String source = """
                MATCH (a:Person:Employee {id: 1})
                WHERE a.age > 30
                RETURN a.name AS name,
                       a.age AS age
                """;

        assertParses(source);
    }

    @Test
    void parsesCase4TwoDisconnectedPatterns() {
        String source = """
                MATCH (m:Movie), (p:Person)
                WHERE m.year > 2000
                RETURN m.title AS title,
                       p.name AS actor
                """;

        assertParses(source);
    }

    @Test
    void parsesCase5ComparisonBetweenProperties() {
        String source = """
                MATCH (m:Movie), (p:Person)
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name
                """;

        assertParses(source);
    }

    @Test
    void parsesCase6PatternsWithProperties() {
        String source = """
                MATCH (m:
                Movie {year: 1999}), (p:Person {age: 40})
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name
                """;

        assertParses(source);
    }

    @Test
    void parsesCase7MatchWhereCreateAndReturn() {
        String source = """
                MATCH (p:Person)
                WHERE p.age > 18
                CREATE (c:Certificate {issuedTo: "adult", year: 2026})
                RETURN p.name AS name,
                       p.age AS age
                """;

        assertParses(source);
    }

    @Test
    void parsesCase8PropertyExpressionInsidePattern() {
        String source = """
                MATCH (p:Person {id: 1}), (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total
                """;

        assertParses(source);
    }

    @Test
    void parsesCase9MatchCreateDeleteAndReturn() {
        String source = """
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name
                """;

        assertParses(source);
    }

    @Test
    void parsesCase10BeforeSemanticUndefinedVariableCheck() {
        String source = """
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE q.age > 60
                RETURN q AS name
                """;

        assertParses(source);
    }

    @Test
    void parsesCase11BeforeSemanticVariableOrderCheck() {
        String source = """
                MATCH (o:Order {personId: p.id, status: "cancelled"}), (p:Person)
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name
                """;

        assertParses(source);
    }

    @Test
    void rejectsQueryThatDoesNotStartWithMatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow(
                        "RETURN m.title"
                )
        );
    }

    private void assertParses(String source) {
        Query query = assertDoesNotThrow(
                () -> parser.parseOrThrow(source)
        );

        assertNotNull(query);
    }
}