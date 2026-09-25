package cr.ac.una.eif400.cyphail.compiler.parser.combinator;

import java.util.Objects;

/**
 * Represents a successful result produced by a parser combinator.
 *
 * <p>The result contains the value produced by the parser and the
 * remaining immutable input that has not been consumed.</p>
 *
 * <p>Project: Cyphail - Sprint P1<br>
 * Course: EIF400 - Paradigmas de Programacion<br>
 * Universidad Nacional de Costa Rica<br>
 * Group: 04-10am</p>
 *
 * @param value parsed value
 * @param rest remaining parser input
 * @param <T> type of value produced by the parser
 *
 * @author Emmanuel Nunez Jimenez
 * @author Keynell Molina Mora
 * @author Valery Alfaro Morales
 * @author Julissa Solano Valverde
 * @author Roy Arias Mejia
 */
public record ParseSuccess<T>(
        T value,
        ParserInput rest
) implements ParseResult<T> {

    /**
     * Validates the successful parse result.
     */
    public ParseSuccess {
        Objects.requireNonNull(
                value,
                "Parsed value cannot be null"
        );

        Objects.requireNonNull(
                rest,
                "Remaining parser input cannot be null"
        );
    }

    /**
     * A ParseSuccess always represents a successful parse.
     *
     * @return true
     */
    @Override
    public boolean isSuccess() {
        return true;
    }
}