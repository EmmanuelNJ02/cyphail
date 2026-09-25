package cr.ac.una.eif400.cyphail.compiler.parser.combinator;

import cr.ac.una.eif400.cyphail.compiler.lexer.Token;
import cr.ac.una.eif400.cyphail.compiler.lexer.TokenType;

import java.util.List;
import java.util.Objects;

/**
 * Represents the immutable input consumed by the functional parser combinators.
 *
 * <p>The input stores the complete token sequence and the current position.
 * Advancing the parser creates a new ParserInput instead of modifying the
 * existing value.</p>
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
public record ParserInput(
        List<Token> tokens,
        int position
) {

    /**
     * Validates and normalizes the parser input.
     */
    public ParserInput {
        Objects.requireNonNull(tokens, "Tokens cannot be null");

        tokens = List.copyOf(tokens);

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException(
                    "Parser input requires at least one token"
            );
        }

        if (position < 0 || position >= tokens.size()) {
            throw new IllegalArgumentException(
                    "Parser input position is out of range"
            );
        }
    }

    /**
     * Creates parser input positioned at the first token.
     *
     * @param tokens token sequence to parse
     * @return parser input at position zero
     */
    public static ParserInput from(List<Token> tokens) {
        return new ParserInput(tokens, 0);
    }

    /**
     * Returns the token at the current position.
     *
     * @return current token
     */
    public Token current() {
        return tokens.get(position);
    }

    /**
     * Returns a new parser input advanced by one token.
     *
     * <p>If the current token is EOF, the same input is returned.</p>
     *
     * @return advanced parser input
     */
    public ParserInput advance() {
        if (isAtEnd()) {
            return this;
        }

        return new ParserInput(tokens, position + 1);
    }

    /**
     * Indicates whether the current token marks the end of input.
     *
     * @return true when the current token is EOF
     */
    public boolean isAtEnd() {
        return current().type() == TokenType.EOF;
    }

    /**
     * Returns the number of tokens still available from the current position.
     *
     * @return remaining token count
     */
    public int remaining() {
        return tokens.size() - position;
    }
}