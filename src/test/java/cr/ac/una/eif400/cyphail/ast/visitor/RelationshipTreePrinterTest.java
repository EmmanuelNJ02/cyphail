package cr.ac.una.eif400.cyphail.ast.visitor;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Sprint P2 relationship tree visualization.
 *
 * <p>Verifies relationship directions, variables, types,
 * properties, chained paths and comma-separated patterns.</p>
 *
 * <p>Also checks backward compatibility with Sprint P1.</p>
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
 * Relationship AST tree printer unit tests.</p>
 */
class RelationshipTreePrinterTest {

    private final CyphailParser parser = new CyphailParser();
    private final AstTreePrinter printer = new AstTreePrinter();

    private String render(String source) {
        Query query = parser.parseOrThrow(source);
        return printer.print(query);
    }

    @Test
    void printsOutgoingRelationship() {
        String tree = render(
                "MATCH (a:Person)-[r:KNOWS]->(b:Person) RETURN a"
        );

        assertTrue(tree.contains("PatternPath:{"));
        assertTrue(tree.contains("PatternRelationship:{"));
        assertTrue(tree.contains("var: r"));
        assertTrue(tree.contains("types: [KNOWS]"));
        assertTrue(tree.contains("direction: OUT"));
        assertTrue(tree.contains("labels: [Person]"));
    }

    @Test
    void printsIncomingRelationship() {
        String tree = render(
                "MATCH (a)<-[r:KNOWS]-(b) RETURN a"
        );

        assertTrue(tree.contains("PatternRelationship:{"));
        assertTrue(tree.contains("var: r"));
        assertTrue(tree.contains("direction: IN"));
    }

    @Test
    void printsUndirectedRelationship() {
        String tree = render(
                "MATCH (a)-[r:KNOWS]-(b) RETURN a"
        );

        assertTrue(tree.contains("PatternRelationship:{"));
        assertTrue(tree.contains("direction: NONE"));
    }

    @Test
    void printsRelationshipProperties() {
        String tree = render(
                "MATCH (a)-[r:KNOWS {since: 2020}]->(b) RETURN r"
        );

        assertTrue(tree.contains("var: r"));
        assertTrue(tree.contains("types: [KNOWS]"));
        assertTrue(tree.contains("properties: [since: 2020]"));
    }

    @Test
    void printsChainedRelationshipsInSourceOrder() {
        String tree = render(
                "MATCH (a)-[r:KNOWS]->(b)"
                        + "-[s:WORKS_AT]->(c) RETURN a"
        );

        int firstNode = tree.indexOf("var: a");
        int firstRelationship = tree.indexOf("var: r");
        int secondNode = tree.indexOf("var: b");
        int secondRelationship = tree.indexOf("var: s");
        int thirdNode = tree.indexOf("var: c");

        assertTrue(firstNode >= 0);
        assertTrue(firstRelationship > firstNode);
        assertTrue(secondNode > firstRelationship);
        assertTrue(secondRelationship > secondNode);
        assertTrue(thirdNode > secondRelationship);

        assertTrue(tree.contains("types: [KNOWS]"));
        assertTrue(tree.contains("types: [WORKS_AT]"));
    }

    @Test
    void printsCommaSeparatedRelationshipPaths() {
        String tree = render(
                "MATCH (a)-[:KNOWS]->(b), "
                        + "(b)-[:WORKS_AT]->(c) RETURN a"
        );

        assertEquals(
                2,
                tree.split("PatternPath:", -1).length - 1
        );

        assertEquals(
                2,
                tree.split("PatternRelationship:", -1).length - 1
        );

        assertTrue(tree.contains("types: [KNOWS]"));
        assertTrue(tree.contains("types: [WORKS_AT]"));
    }

    @Test
    void printsAnonymousRelationship() {
        String tree = render(
                "MATCH (a)-[:KNOWS]->(b) RETURN a"
        );

        assertTrue(tree.contains("PatternRelationship:{"));
        assertTrue(tree.contains("var: null"));
        assertTrue(tree.contains("types: [KNOWS]"));
        assertTrue(tree.contains("direction: OUT"));
    }

    @Test
    void printsCreateRelationship() {
        String tree = render(
                "MATCH (a), (b) "
                        + "CREATE (a)-[r:KNOWS]->(b) "
                        + "RETURN r"
        );

        assertTrue(tree.contains("Create:{"));
        assertTrue(tree.contains("PatternPath:{"));
        assertTrue(tree.contains("PatternRelationship:{"));
        assertTrue(tree.contains("var: r"));
        assertTrue(tree.contains("direction: OUT"));
    }

    @Test
    void preservesSprintP1NodeOnlyTree() {
        String tree = render(
                "MATCH (a:Person), (b:Company) RETURN a, b"
        );

        assertTrue(tree.contains("Query{"));
        assertTrue(tree.contains("Match:{"));
        assertTrue(tree.contains("PatternNode:{"));
        assertTrue(tree.contains("labels: [Person]"));
        assertTrue(tree.contains("labels: [Company]"));
        assertTrue(tree.contains("Return:{"));

        assertFalse(tree.contains("PatternPath:{"));
        assertFalse(tree.contains("PatternRelationship:{"));
    }

    @Test
    void printsRelationshipQueryWithWhereExpression() {
        String tree = render(
                "MATCH (a)-[r:KNOWS]->(b) "
                        + "WHERE r.since < 2020 "
                        + "RETURN a, b"
        );

        assertTrue(tree.contains("PatternRelationship:{"));
        assertTrue(tree.contains("direction: OUT"));
        assertTrue(tree.contains("Where:{"));
        assertTrue(tree.contains("Expr: (< (. r since) 2020)"));
        assertTrue(tree.contains("Return:{"));
    }
}