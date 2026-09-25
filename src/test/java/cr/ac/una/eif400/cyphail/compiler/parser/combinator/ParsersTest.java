package cr.ac.una.eif400.cyphail.compiler.parser.combinator;

import cr.ac.una.eif400.cyphail.compiler.lexer.Token;
import cr.ac.una.eif400.cyphail.compiler.lexer.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the functional parser combinators used by Cyphail.
 *
 * <p>The tests verify primitive token parsing, alternatives, optional
 * parsing, repetition and functional composition through map and
 * flatMap.</p>
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
class ParsersTest {

    @Test
    void tokenConsumesExpectedToken() {
        ParserInput input = ParserInput.from(
                List.of(
                        new Token(TokenType.MATCH, "MATCH", 0),
                        Token.eof(5)
                )
        );

        ParseResult<Token> result =
                Parsers.token(TokenType.MATCH).parse(input);

        ParseSuccess<Token> success =
                assertInstanceOf(ParseSuccess.class, result);

        assertEquals(TokenType.MATCH, success.value().type());
        assertEquals(1, success.rest().position());
        assertTrue(success.rest().isAtEnd());
    }

    @Test
    void tokenFailsWithoutConsumingUnexpectedToken() {
        ParserInput input = ParserInput.from(
                List.of(
                        new Token(TokenType.RETURN, "RETURN", 0),
                        Token.eof(6)
                )
        );

        ParseResult<Token> result =
                Parsers.token(TokenType.MATCH).parse(input);

        ParseFailure<Token> failure =
                assertInstanceOf(ParseFailure.class, result);

        assertFalse(failure.isSuccess());
        assertEquals(0, failure.rest().position());
        assertEquals(TokenType.RETURN, failure.rest().current().type());
    }

    @Test
    void orUsesSecondParserFromOriginalInput() {
        ParserInput input = ParserInput.from(
                List.of(
                        new Token(TokenType.IDENTIFIER, "person", 0),
                        Token.eof(6)
                )
        );

        Parser<Token> parser = Parsers.or(
                Parsers.token(TokenType.MATCH),
                Parsers.identifier()
        );

        ParseResult<Token> result = parser.parse(input);

        ParseSuccess<Token> success =
                assertInstanceOf(ParseSuccess.class, result);

        assertEquals("person", success.value().lexeme());
        assertEquals(1, success.rest().position());
    }

    @Test
    void optionalLeavesInputUntouchedWhenParserFails() {
        ParserInput input = ParserInput.from(
                List.of(
                        new Token(TokenType.RETURN, "RETURN", 0),
                        Token.eof(6)
                )
        );

        ParseResult<Optional<Token>> result =
                Parsers.optional(
                        Parsers.identifier()
                ).parse(input);

        ParseSuccess<Optional<Token>> success =
                assertInstanceOf(ParseSuccess.class, result);

        assertTrue(success.value().isEmpty());
        assertEquals(0, success.rest().position());
    }

    @Test
    void manyCollectsRepeatedValues() {
        ParserInput input = ParserInput.from(
                List.of(
                        new Token(TokenType.IDENTIFIER, "first", 0),
                        new Token(TokenType.IDENTIFIER, "second", 6),
                        Token.eof(12)
                )
        );

        ParseResult<List<Token>> result =
                Parsers.many(
                        Parsers.identifier()
                ).parse(input);

        ParseSuccess<List<Token>> success =
                assertInstanceOf(ParseSuccess.class, result);

        assertEquals(2, success.value().size());
        assertEquals("first", success.value().get(0).lexeme());
        assertEquals("second", success.value().get(1).lexeme());
        assertTrue(success.rest().isAtEnd());
    }

    @Test
    void manyOneRequiresAtLeastOneValue() {
        ParserInput input = ParserInput.from(
                List.of(
                        Token.eof(0)
                )
        );

        ParseResult<List<Token>> result =
                Parsers.manyOne(
                        Parsers.identifier()
                ).parse(input);

        ParseFailure<List<Token>> failure =
                assertInstanceOf(ParseFailure.class, result);

        assertFalse(failure.isSuccess());
        assertEquals(0, failure.rest().position());
    }

    @Test
    void mapTransformsSuccessfulValue() {
        ParserInput input = ParserInput.from(
                List.of(
                        new Token(TokenType.IDENTIFIER, "movie", 0),
                        Token.eof(5)
                )
        );

        Parser<String> parser =
                Parsers.identifier().map(Token::lexeme);

        ParseResult<String> result = parser.parse(input);

        ParseSuccess<String> success =
                assertInstanceOf(ParseSuccess.class, result);

        assertEquals("movie", success.value());
        assertTrue(success.rest().isAtEnd());
    }

    @Test
    void flatMapChainsParsersUsingRemainingInput() {
        ParserInput input = ParserInput.from(
                List.of(
                        new Token(TokenType.MATCH, "MATCH", 0),
                        new Token(TokenType.IDENTIFIER, "person", 6),
                        Token.eof(12)
                )
        );

        Parser<String> parser =
                Parsers.token(TokenType.MATCH)
                        .flatMap(match ->
                                Parsers.identifier()
                                        .map(Token::lexeme)
                        );

        ParseResult<String> result = parser.parse(input);

        ParseSuccess<String> success =
                assertInstanceOf(ParseSuccess.class, result);

        assertEquals("person", success.value());
        assertEquals(2, success.rest().position());
        assertTrue(success.rest().isAtEnd());
    }
}