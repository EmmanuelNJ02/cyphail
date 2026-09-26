package cr.ac.una.eif400.cyphail.frontend.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Sprint P1 .tree command handler.
 *
 * <p>The tests verify tree capture mode, multiline query processing,
 * AST rendering and semantic error reporting for undefined variables.</p>
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
class TreeCommandHandlerTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        output = new ByteArrayOutputStream();

        System.setOut(
                new PrintStream(output)
        );
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void shouldActivateTreeCaptureMode() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        assertTrue(
                handler.supports(".tree")
        );

        handler.handle(".tree");

        assertTrue(
                handler.isAwaitingQuery()
        );

        assertTrue(
                handler.supports(
                        "MATCH (m:Movie)"
                )
        );

        assertFalse(
                handler.supports(".exit")
        );
    }

    @Test
    void shouldPrintTreeForMultilineQuery() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        handler.handle(".tree");

        handler.handle(
                "MATCH (m:Movie)"
        );

        handler.handle(
                "WHERE m.year > 1990"
        );

        handler.handle(
                "RETURN m.title AS title,"
        );

        handler.handle(
                "m.year AS year"
        );

        String result = output.toString();

        assertTrue(
                result.contains("Query{")
        );

        assertTrue(
                result.contains("Match:{")
        );

        assertTrue(
                result.contains(
                        "Expr: (> (. m year) 1990)"
                )
        );

        assertTrue(
                result.contains(
                        "{as (. m title) title}"
                )
        );

        assertTrue(
                result.contains(
                        "{as (. m year) year}"
                )
        );

        assertFalse(
                handler.isAwaitingQuery()
        );
    }

    @Test
    void shouldReportUndefinedVariableFromOfficialCase10() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        handler.handle(".tree");

        handler.handle(
                "MATCH (p:Person), "
                        + "(o:Order {personId: p.id, "
                        + "status: \"cancelled\"})"
        );

        handler.handle(
                "WHERE q.age > 60"
        );

        handler.handle(
                "RETURN q AS name"
        );

        String result = output.toString();

        assertTrue(
                result.contains(
                        "Undefined variable 'q' "
                                + "in WHERE clause."
                )
        );

        assertTrue(
                result.contains(
                        "Undefined variable 'q' "
                                + "in RETURN clause."
                )
        );

        assertFalse(
                result.contains("Query{")
        );

        assertFalse(
                handler.isAwaitingQuery()
        );
    }

    @Test
    void shouldReportVariableUsedBeforeDeclarationFromOfficialCase11() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        handler.handle(".tree");

        handler.handle(
                "MATCH (o:Order {personId: p.id, "
                        + "status: \"cancelled\"}), "
                        + "(p:Person)"
        );

        handler.handle(
                "WHERE p.age > 60"
        );

        handler.handle(
                "CREATE (a:Archive {id: o.id, "
                        + "name: \"retired\", year: 2026})"
        );

        handler.handle(
                "DELETE o"
        );

        handler.handle(
                "RETURN p.name AS name"
        );

        String result = output.toString();

        assertTrue(
                result.contains(
                        "Undefined variable 'p' "
                                + "in MATCH pattern."
                )
        );

        assertFalse(
                result.contains("Query{")
        );

        assertFalse(
                handler.isAwaitingQuery()
        );
    }
}
