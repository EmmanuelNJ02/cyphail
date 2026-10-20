package cr.ac.una.eif400.cyphail.compiler.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for numeric literal tokenization in Sprint P2.
 *
 * Decimal values must be recognized as single tokens
 * without breaking existing integer or property access
 * tokenization.
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
class CyphailLexerP2Test {

    private final CyphailLexer lexer = new CyphailLexer();

    @Test
    void shouldRecognizeDecimalLiteralAsSingleToken() {
        List<Token> tokens = lexer.tokenize(
                "SET p.precio = 19.99"
        );

        List<String> types = tokens.stream()
                .map(token -> token.type().name())
                .toList();

        assertEquals(
                List.of(
                        "SET",
                        "IDENTIFIER",
                        "DOT",
                        "IDENTIFIER",
                        "EQUAL",
                        "FLOAT",
                        "EOF"
                ),
                types
        );

        assertEquals("19.99", tokens.get(5).lexeme());
    }

    @Test
    void shouldRecognizeDecimalInRelationshipAssignment() {
        List<Token> tokens = lexer.tokenize(
                "SET r.grade = 9.5"
        );

        List<Token> numericTokens = tokens.stream()
                .filter(token ->
                        Character.isDigit(
                                token.lexeme().isEmpty()
                                        ? ' '
                                        : token.lexeme().charAt(0)
                        )
                )
                .toList();

        assertEquals(1, numericTokens.size());
        assertEquals("9.5", numericTokens.getFirst().lexeme());
        assertEquals(
                "FLOAT",
                numericTokens.getFirst().type().name()
        );
    }

    @Test
    void shouldPreserveIntegerAndPropertyAccessTokens() {
        List<Token> tokens = lexer.tokenize(
                "WHERE p.edad > 18"
        );

        List<TokenType> types = tokens.stream()
                .map(Token::type)
                .toList();

        assertEquals(
                List.of(
                        TokenType.WHERE,
                        TokenType.IDENTIFIER,
                        TokenType.DOT,
                        TokenType.IDENTIFIER,
                        TokenType.GREATER_THAN,
                        TokenType.INTEGER,
                        TokenType.EOF
                ),
                types
        );
    }
}