package cr.ac.una.eif400.cyphail.compiler.parser.combinator;

import java.util.Objects;

/**
 * Represents a failed result produced by a parser combinator.
 *
 * <p>The failure contains a human-readable reason and the immutable
 * parser input where parsing could not continue.</p>
 *
 * <p>Project: Cyphail - Sprint P1<br>
 * Course: EIF400 - Paradigmas de Programacion<br>
 * Universidad Nacional de Costa Rica<br>
 * Group: 04-10am</p>
 *
 * @param reason reason why parsing failed
 * @param rest parser input at the point of failure
 * @param <T> expected result type of the parser
 *
 * @author Emmanuel Nunez Jimenez
 * @author Keynell Molina Mora
 * @author Valery Alfaro Morales
 * @author Julissa Solano Valverde
 * @author Roy Arias Mejia
 */
public record ParseFailure<T>(
        String reason,
        ParserInput rest
) implements ParseResult<T> {

    /**
     * Validates the failed parse result.
     */
    public ParseFailure {
        Objects.requireNonNull(
                reason,
                "Failure reason cannot be null"
        );

        Objects.requireNonNull(
                rest,
                "Parser input cannot be null"
        );

        if (reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Failure reason cannot be blank"
            );
        }
    }

    /**
     * A ParseFailure never represents a successful parse.
     *
     * @return false
     */
    @Override
    public boolean isSuccess() {
        return false;
    }
}