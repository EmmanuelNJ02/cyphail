
package cr.ac.una.eif400.cyphail.frontend.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * National University of Costa Rica
 * School of Informatics
 *
 * Course: EIF400 - Programming Paradigms
 * Project: Cyphail
 * Sprint: P2
 * Group Code: 04-10am
 *
 * Class: TreeCommandHandlerTest
 *
 * Description:
 *
 * Unit and regression tests for the .tree
 * command handler in the Cyphail REPL.
 *
 * These tests preserve Sprint P1 behavior
 * while verifying the additional functionality
 * introduced in Sprint P2.
 *
 * The test suite covers:
 *
 * 1. Activation of multiline tree capture.
 * 2. Multiline AST visualization.
 * 3. Undefined-variable semantic errors.
 * 4. Variables used before declaration.
 * 5. Inline AST visualization.
 * 6. Syntax error reporting and recovery.
 * 7. Inline queries terminated with semicolons.
 * 8. Multiline queries terminated with semicolons.
 * 9. Relationship patterns and properties.
 * 10. CREATE statements without RETURN.
 * 11. Syntax errors with statement terminators.
 *
 * AST visualization must not execute queries
 * or modify graph data.
 *
 * Team Members:
 *
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
class TreeCommandHandlerTest {

    // ============================================
    // TEST OUTPUT CAPTURE
    // ============================================

    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    // ============================================
    // TEST SETUP
    // ============================================

    /**
     * Redirects standard output before each test.
     */
    @BeforeEach
    void setUp() {
        originalOut = System.out;
        output = new ByteArrayOutputStream();

        System.setOut(
                new PrintStream(output)
        );
    }

    // ============================================
    // TEST CLEANUP
    // ============================================

    /**
     * Restores standard output after each test.
     */
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // ============================================
    // ORIGINAL SPRINT P1 REGRESSION TESTS
    // ============================================

    /**
     * Verifies that .tree starts multiline
     * query capture mode.
     *
     * The .exit command must remain available.
     */
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

    /**
     * Verifies multiline AST rendering without
     * requiring a terminating semicolon.
     *
     * This preserves the original P1 behavior.
     */
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

    /**
     * Verifies semantic error reporting for
     * undefined variables in WHERE and RETURN.
     *
     * Preserves the official Sprint P1 case 10.
     */
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

    /**
     * Verifies semantic validation when
     * a variable is referenced before
     * its declaration.
     *
     * Preserves the official Sprint P1 case 11.
     */
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

    /**
     * Verifies inline AST visualization
     * without a terminating semicolon.
     */
    @Test
    void shouldPrintTreeForInlineQuery() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        assertTrue(
                handler.supports(
                        ".tree MATCH (m:Movie) "
                                + "RETURN m.title, "
                                + "m.year AS year"
                )
        );

        handler.handle(
                ".tree MATCH (m:Movie) "
                        + "RETURN m.title, "
                        + "m.year AS year"
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
                        "{(. m title)}"
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

    /**
     * Verifies that malformed inline queries
     * produce a syntax error and reset
     * the capture state.
     */
    @Test
    void shouldReportSyntaxErrorAndResetAfterInlineQuery() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        handler.handle(
                ".tree MATCH (m:Movie RETURN m.title"
        );

        String result = output.toString();

        assertTrue(
                result.contains("Syntax error:")
        );

        assertTrue(
                result.contains(
                        "Expected RIGHT_PAREN"
                )
        );

        assertFalse(
                handler.isAwaitingQuery()
        );
    }

    // ============================================
    // NEW SPRINT P2 REGRESSION TESTS
    // ============================================

    /**
     * Verifies that an inline .tree query
     * accepts a trailing semicolon.
     *
     * Also verifies Sprint P2 relationship
     * patterns, directions, and properties.
     *
     * This prevents regression of the original
     * "Unexpected character ';'" error.
     */
    @Test
    void shouldPrintTreeForInlineQueryWithSemicolon() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        String command =
                ".tree MATCH (a:Persona)"
                        + "-[r:TRABAJA_EN {rol: 'Dev'}]->"
                        + "(e:Empresa) "
                        + "RETURN a.nombre, r.rol, e.nombre;";

        assertTrue(
                handler.supports(command)
        );

        handler.handle(command);

        String result = output.toString();

        assertTrue(
                result.contains("Query{")
        );

        assertTrue(
                result.contains("Match:{")
        );

        assertTrue(
                result.contains("PatternRelationship:{")
        );

        assertTrue(
                result.contains("types: [TRABAJA_EN]")
        );

        assertTrue(
                result.contains("direction: OUT")
        );

        assertTrue(
                result.contains("rol: \"Dev\"")
        );

        assertTrue(
                result.contains("{(. a nombre)}")
        );

        assertTrue(
                result.contains("{(. r rol)}")
        );

        assertTrue(
                result.contains("{(. e nombre)}")
        );

        assertFalse(
                result.contains("Syntax error:")
        );

        assertFalse(
                handler.isAwaitingQuery()
        );
    }

    /**
     * Verifies that multiline .tree input
     * accepts a semicolon at the end
     * of the RETURN clause.
     *
     * The handler must wait until the
     * complete query is available.
     */
    @Test
    void shouldPrintTreeForMultilineQueryWithSemicolon() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        handler.handle(".tree");

        assertTrue(
                handler.isAwaitingQuery()
        );

        handler.handle(
                "MATCH (p:Persona)"
        );

        assertTrue(
                handler.isAwaitingQuery()
        );

        handler.handle(
                "RETURN p.nombre;"
        );

        String result = output.toString();

        assertTrue(
                result.contains("Query{")
        );

        assertTrue(
                result.contains("Match:{")
        );

        assertTrue(
                result.contains("labels: [Persona]")
        );

        assertTrue(
                result.contains("{(. p nombre)}")
        );

        assertFalse(
                result.contains("Syntax error:")
        );

        assertFalse(
                handler.isAwaitingQuery()
        );
    }

    /**
     * Verifies that a CREATE statement
     * without RETURN is supported
     * in inline .tree mode.
     *
     * Sprint P2 allows update statements
     * to terminate with a semicolon.
     */
    @Test
    void shouldPrintTreeForInlineCreateWithoutReturn() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        handler.handle(
                ".tree CREATE "
                        + "(p:Person {name: 'Alice'});"
        );

        String result = output.toString();

        assertTrue(
                result.contains("Query{")
        );

        assertTrue(
                result.contains("Updates:[")
        );

        assertFalse(
                result.contains("Syntax error:")
        );

        assertFalse(
                handler.isAwaitingQuery()
        );
    }

    /**
     * Verifies that an update statement
     * without RETURN is not rendered
     * before its terminating semicolon.
     *
     * The semicolon is then supplied
     * as an additional input line.
     */
    @Test
    void shouldWaitForSemicolonBeforeRenderingUpdateWithoutReturn() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        handler.handle(".tree");

        handler.handle(
                "CREATE (p:Person {name: 'Alice'})"
        );

        assertTrue(
                handler.isAwaitingQuery()
        );

        assertFalse(
                output.toString().contains("Query{")
        );

        handler.handle(";");

        String result = output.toString();

        assertTrue(
                result.contains("Query{")
        );

        assertTrue(
                result.contains("Updates:[")
        );

        assertFalse(
                result.contains("Syntax error:")
        );

        assertFalse(
                handler.isAwaitingQuery()
        );
    }

    /**
     * Verifies that a malformed inline
     * query still reports a syntax error
     * when terminated by a semicolon.
     *
     * Removing the optional semicolon
     * must not hide actual syntax errors.
     */
    @Test
    void shouldReportSyntaxErrorForSemicolonTerminatedInlineQuery() {
        TreeCommandHandler handler =
                new TreeCommandHandler();

        handler.handle(
                ".tree MATCH (m:Movie RETURN m.title;"
        );

        String result = output.toString();

        assertTrue(
                result.contains("Syntax error:")
        );

        assertTrue(
                result.contains("Expected RIGHT_PAREN")
        );

        assertFalse(
                result.contains("Query{")
        );

        assertFalse(
                handler.isAwaitingQuery()
        );
    }
}
