package cr.ac.una.eif400.cyphail.frontend;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for multiline Cyphail statement buffering.
 *
 * Verifies statement termination, multiple statements,
 * string literals, comments and buffer state management.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * Group Code: 04-10am
 *
 * Authors:
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
class CyphailStatementBufferTest {

    @Test
    void shouldAccumulateMultilineStatementUntilSemicolon() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        assertTrue(
                buffer.appendLine("MATCH (p:Persona)").isEmpty()
        );

        assertTrue(
                buffer.appendLine("WHERE p.edad > 18").isEmpty()
        );

        assertTrue(buffer.hasPendingStatement());

        List<String> statements =
                buffer.appendLine("RETURN p.nombre;");

        assertEquals(1, statements.size());

        assertEquals(
                "MATCH (p:Persona)\n"
                        + "WHERE p.edad > 18\n"
                        + "RETURN p.nombre",
                statements.get(0)
        );

        assertFalse(buffer.hasPendingStatement());
        assertEquals("", buffer.pendingSource());
    }

    @Test
    void shouldProcessMultipleStatementsOnOneLine() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        List<String> statements = buffer.appendLine(
                "MATCH (a) RETURN a; MATCH (b) RETURN b;"
        );

        assertEquals(2, statements.size());
        assertEquals("MATCH (a) RETURN a", statements.get(0));
        assertEquals("MATCH (b) RETURN b", statements.get(1));

        assertFalse(buffer.hasPendingStatement());
    }

    @Test
    void shouldPreserveUnfinishedStatementAfterCompletedOne() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        List<String> first = buffer.appendLine(
                "MATCH (a) RETURN a; MATCH (b)"
        );

        assertEquals(
                List.of("MATCH (a) RETURN a"),
                first
        );

        assertTrue(buffer.hasPendingStatement());

        List<String> second =
                buffer.appendLine("RETURN b;");

        assertEquals(
                List.of("MATCH (b)\nRETURN b"),
                second
        );

        assertFalse(buffer.hasPendingStatement());
    }

    @Test
    void shouldIgnoreSemicolonsInsideSingleQuotedStrings() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        List<String> statements = buffer.appendLine(
                "MATCH (p {name: 'Ana; Maria'}) RETURN p;"
        );

        assertEquals(
                List.of("MATCH (p {name: 'Ana; Maria'}) RETURN p"),
                statements
        );
    }

    @Test
    void shouldIgnoreSemicolonsInsideDoubleQuotedStrings() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        List<String> statements = buffer.appendLine(
                "MATCH (p {name: \"Ana; Maria\"}) RETURN p;"
        );

        assertEquals(
                List.of("MATCH (p {name: \"Ana; Maria\"}) RETURN p"),
                statements
        );
    }

    @Test
    void shouldHandleEscapedDoubleQuotes() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        List<String> statements = buffer.appendLine(
                "RETURN \"a\\\";b\";"
        );

        assertEquals(
                List.of("RETURN \"a\\\";b\""),
                statements
        );

        assertFalse(buffer.hasPendingStatement());
    }

    @Test
    void shouldIgnoreSemicolonsInLineComments() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        assertTrue(
                buffer.appendLine(
                        "MATCH (a) // ; ignored"
                ).isEmpty()
        );

        List<String> statements =
                buffer.appendLine("RETURN a;");

        assertEquals(1, statements.size());

        assertEquals(
                "MATCH (a) // ; ignored\nRETURN a",
                statements.get(0)
        );
    }

    @Test
    void shouldIgnoreSemicolonsInBlockComments() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        assertTrue(
                buffer.appendLine(
                        "MATCH (a) /* ; ignored"
                ).isEmpty()
        );

        List<String> statements =
                buffer.appendLine(
                        "still ; ignored */ RETURN a;"
                );

        assertEquals(1, statements.size());

        assertEquals(
                "MATCH (a) /* ; ignored\n"
                        + "still ; ignored */ RETURN a",
                statements.get(0)
        );
    }

    @Test
    void shouldIgnoreEmptyStatements() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        List<String> statements =
                buffer.appendLine("; ; ;");

        assertTrue(statements.isEmpty());
        assertFalse(buffer.hasPendingStatement());
    }

    @Test
    void shouldResetPendingStatementAndState() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        buffer.appendLine("RETURN \"unfinished;");

        assertTrue(buffer.hasPendingStatement());

        buffer.reset();

        assertFalse(buffer.hasPendingStatement());
        assertEquals("", buffer.pendingSource());

        List<String> statements =
                buffer.appendLine("MATCH (a) RETURN a;");

        assertEquals(
                List.of("MATCH (a) RETURN a"),
                statements
        );
    }

    @Test
    void shouldPreserveBlankLinesInsideStatements() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        buffer.appendLine("MATCH (a)");
        buffer.appendLine("");

        List<String> statements =
                buffer.appendLine("RETURN a;");

        assertEquals(
                List.of("MATCH (a)\n\nRETURN a"),
                statements
        );
    }

    @Test
    void shouldRejectNullInput() {
        CyphailStatementBuffer buffer =
                new CyphailStatementBuffer();

        assertThrows(
                NullPointerException.class,
                () -> buffer.appendLine(null)
        );
    }
}