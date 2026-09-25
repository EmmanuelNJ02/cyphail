package cr.ac.una.eif400.cyphail.compiler.lexer;

import java.util.Objects;

/**
 * Represents one immutable token produced by the Cyphail lexer.
 *
 * <p>Each token stores its type, the original lexeme found in the
 * source query and the zero-based position where the token begins.</p>
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
public record Token(
        TokenType type,
        String lexeme,
        int position
) {

    /**
     * Validates the immutable token data.
     */
    public Token {
        Objects.requireNonNull(type, "Token type cannot be null");
        Objects.requireNonNull(lexeme, "Token lexeme cannot be null");

        if (position < 0) {
            throw new IllegalArgumentException(
                    "Token position cannot be negative"
            );
        }
    }

    /**
     * Indicates whether this token has the expected type.
     *
     * @param expectedType token type to compare
     * @return true when the token has the expected type
     */
    public boolean is(TokenType expectedType) {
        return type == expectedType;
    }

    /**
     * Creates the token that marks the end of the input.
     *
     * @param position position immediately after the source input
     * @return EOF token
     */
    public static Token eof(int position) {
        return new Token(TokenType.EOF, "", position);
    }
}