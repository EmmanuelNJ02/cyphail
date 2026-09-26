package cr.ac.una.eif400.cyphail.ast.visitor;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Sprint P1 AST tree printer.
 *
 * <p>The tests verify the logical representation required by the
 * .tree command, including query sections, node patterns, properties,
 * updating clauses and preorder expressions.</p>
 *
 * <p>Project: Cyphail</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica - Escuela de Informatica</p>
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
 */
class AstTreePrinterTest {

    private final CyphailParser parser = new CyphailParser();
    private final AstTreePrinter printer = new AstTreePrinter();

    @Test
    void shouldPrintOfficialTreeExampleWithPreorderExpression() {
        Query query = parser.parseOrThrow("""
                MATCH (m:Movie)
                WHERE m.year > 1990
                RETURN m.title AS title,
                       m.year AS year
                """);

        String tree = printer.print(query);

        assertTrue(tree.contains("Query{"));
        assertTrue(tree.contains("Match:{"));
        assertTrue(tree.contains("var: m"));
        assertTrue(tree.contains("labels: [Movie]"));
        assertTrue(tree.contains("Where:{"));

        assertTrue(
                tree.contains(
                        "Expr: (> (. m year) 1990)"
                )
        );

        assertTrue(tree.contains("Updates:[]"));
        assertTrue(tree.contains("Return:{"));

        assertTrue(
                tree.contains(
                        "{as (. m title) title}"
                )
        );

        assertTrue(
                tree.contains(
                        "{as (. m year) year}"
                )
        );

        assertTrue(tree.contains("Modifiers:[]"));
    }

    @Test
    void shouldPrintPatternPropertiesAndPropertyExpression() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Person {id: 1}),
                      (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total
                """);

        String tree = printer.print(query);

        assertTrue(tree.contains("var: p"));
        assertTrue(tree.contains("labels: [Person]"));
        assertTrue(tree.contains("properties: [id: 1]"));

        assertTrue(tree.contains("var: o"));
        assertTrue(tree.contains("labels: [Order]"));

        assertTrue(
                tree.contains(
                        "properties: [personId: (. p id)]"
                )
        );

        assertTrue(
                tree.contains(
                        "{as (. p name) name}"
                )
        );

        assertTrue(
                tree.contains(
                        "{as (. o total) total}"
                )
        );
    }

    @Test
    void shouldPrintCreateAndDeleteUpdates() {
        Query query = parser.parseOrThrow("""
                MATCH (p:Person),
                      (o:Order {personId: p.id, status: "cancelled"})
                WHERE p.age > 60
                CREATE (a:Archive {
                    id: o.id,
                    name: "retired",
                    year: 2026
                })
                DELETE o
                RETURN p.name AS name
                """);

        String tree = printer.print(query);

        assertTrue(tree.contains("Updates:["));
        assertTrue(tree.contains("Create:{"));
        assertTrue(tree.contains("var: a"));
        assertTrue(tree.contains("labels: [Archive]"));

        assertTrue(
                tree.contains(
                        "id: (. o id)"
                )
        );

        assertTrue(
                tree.contains(
                        "name: \"retired\""
                )
        );

        assertTrue(tree.contains("year: 2026"));
        assertTrue(tree.contains("Delete:{"));
        assertTrue(tree.contains("Detach: false"));
        assertTrue(tree.contains("Expressions: [o]"));
    }

    @Test
    void shouldPrintQueryWithoutWhereAsNull() {
        Query query = parser.parseOrThrow("""
                MATCH (m:Movie)
                RETURN m.title
                """);

        String tree = printer.print(query);

        assertTrue(tree.contains("Where: null"));
        assertTrue(tree.contains("Updates:[]"));
        assertTrue(tree.contains("{(. m title)}"));
    }
}