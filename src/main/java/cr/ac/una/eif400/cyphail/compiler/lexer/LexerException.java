package cr.ac.una.eif400.cyphail.compiler.lexer;

import java.util.Objects;

/**
 * Represents an error detected while tokenizing Cyphail source text.
 *
 * <p>The exception keeps the source position where the lexical error
 * was detected so that the frontend can later report useful diagnostics
 * to the user.</p>
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
public final class LexerException extends RuntimeException {

    private final int position;

    /**
     * Creates a lexical error.
     *
     * @param message description of the error
     * @param position zero-based position where the error was detected
     */
    public LexerException(String message, int position) {
        super(Objects.requireNonNull(message, "Message cannot be null")
                + " at position " + position);

        if (position < 0) {
            throw new IllegalArgumentException(
                    "Lexer error position cannot be negative"
            );
        }

        this.position = position;
    }

    /**
     * Returns the source position associated with this lexical error.
     *
     * @return zero-based source position
     */
    public int position() {
        return position;
    }
}