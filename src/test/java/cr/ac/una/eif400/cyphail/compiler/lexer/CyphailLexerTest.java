package cr.ac.una.eif400.cyphail.compiler.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Cyphail lexer used during Sprint P1.
 *
 * <p>The tests cover representative lexical elements from the official
 * Sprint P1 cases, including MATCH, WHERE, CREATE, DELETE, RETURN,
 * properties, aliases, literals and comparison operators.</p>
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
class CyphailLexerTest {

    private final CyphailLexer lexer = new CyphailLexer();

    @Test
    void tokenizesMatchWhereReturnQuery() {
        String source = """
                MATCH (m:Movie)
                WHERE m.year > 1990
                RETURN m.title AS title
                """;

        List<TokenType> types = lexer.tokenize(source)
                .stream()
                .map(Token::type)
                .toList();

        assertEquals(
                List.of(
                        TokenType.MATCH,
                        TokenType.LEFT_PAREN,
                        TokenType.IDENTIFIER,
                        TokenType.COLON,
                        TokenType.IDENTIFIER,
                        TokenType.RIGHT_PAREN,
                        TokenType.WHERE,
                        TokenType.IDENTIFIER,
                        TokenType.DOT,
                        TokenType.IDENTIFIER,
                        TokenType.GREATER_THAN,
                        TokenType.INTEGER,
                        TokenType.RETURN,
                        TokenType.IDENTIFIER,
                        TokenType.DOT,
                        TokenType.IDENTIFIER,
                        TokenType.AS,
                        TokenType.IDENTIFIER,
                        TokenType.EOF
                ),
                types
        );
    }

    @Test
    void recognizesCreateDeletePropertiesAndStringLiteral() {
        String source = """
                MATCH (p:Person)
                CREATE (a:Archive {name: "retired", year: 2026})
                DELETE p
                RETURN p.name AS name
                """;

        List<Token> tokens = lexer.tokenize(source);

        assertTrue(tokens.stream().anyMatch(
                token -> token.type() == TokenType.CREATE
        ));

        assertTrue(tokens.stream().anyMatch(
                token -> token.type() == TokenType.DELETE
        ));

        assertTrue(tokens.stream().anyMatch(
                token -> token.type() == TokenType.STRING
                        && token.lexeme().equals("\"retired\"")
        ));

        assertTrue(tokens.stream().anyMatch(
                token -> token.type() == TokenType.INTEGER
                        && token.lexeme().equals("2026")
        ));

        assertEquals(
                TokenType.EOF,
                tokens.getLast().type()
        );
    }

    @Test
    void recognizesSprintOneComparisonOperators() {
        List<Token> lessThan = lexer.tokenize(
                "MATCH (p:Person) WHERE p.age < 30 RETURN p.name"
        );

        List<Token> greaterThan = lexer.tokenize(
                "MATCH (p:Person) WHERE p.age > 30 RETURN p.name"
        );

        List<Token> notEqual = lexer.tokenize(
                "MATCH (p:Person) WHERE p.age <> 30 RETURN p.name"
        );

        assertTrue(lessThan.stream().anyMatch(
                token -> token.type() == TokenType.LESS_THAN
        ));

        assertTrue(greaterThan.stream().anyMatch(
                token -> token.type() == TokenType.GREATER_THAN
        ));

        assertTrue(notEqual.stream().anyMatch(
                token -> token.type() == TokenType.NOT_EQUAL
        ));
    }

    @Test
    void ignoresWhitespaceAndComments() {
        String source = """
                // Cyphail query
                MATCH (p:Person)
                /* filter */
                WHERE p.age > 18
                RETURN p.name
                """;

        List<Token> tokens = lexer.tokenize(source);

        assertEquals(TokenType.MATCH, tokens.getFirst().type());
        assertEquals(TokenType.EOF, tokens.getLast().type());
    }

    @Test
    void rejectsUnexpectedCharacter() {
        LexerException exception = assertThrows(
                LexerException.class,
                () -> lexer.tokenize(
                        "MATCH (p:Person) @ RETURN p.name"
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Unexpected character '@'"
                )
        );
    }

    @Test
    void rejectsUnterminatedStringLiteral() {
        LexerException exception = assertThrows(
                LexerException.class,
                () -> lexer.tokenize(
                        "MATCH (p:Person {name: \"Ana}) RETURN p.name"
                )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Unterminated string literal"
                )
        );
    }
}