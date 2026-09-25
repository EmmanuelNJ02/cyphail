package cr.ac.una.eif400.cyphail.compiler.parser.combinator;

import java.util.Objects;
import java.util.function.Function;

/**
 * Functional abstraction for a Cyphail parser combinator.
 *
 * <p>A parser receives immutable ParserInput and produces a ParseResult.
 * Parsers can be transformed and sequentially composed without mutating
 * the original input.</p>
 *
 * <p>Project: Cyphail - Sprint P1<br>
 * Course: EIF400 - Paradigmas de Programacion<br>
 * Universidad Nacional de Costa Rica<br>
 * Group: 04-10am</p>
 *
 * @param <T> type of value produced by the parser
 *
 * @author Emmanuel Nunez Jimenez
 * @author Keynell Molina Mora
 * @author Valery Alfaro Morales
 * @author Julissa Solano Valverde
 * @author Roy Arias Mejia
 */
@FunctionalInterface
public interface Parser<T> {

    /**
     * Applies this parser to the supplied immutable input.
     *
     * @param input parser input
     * @return successful or failed parse result
     */
    ParseResult<T> parse(ParserInput input);

    /**
     * Transforms the successful value produced by this parser.
     *
     * <p>A failure is propagated without consuming additional input.</p>
     *
     * @param mapper function applied to a successful value
     * @param <R> resulting value type
     * @return composed parser
     */
    default <R> Parser<R> map(
            Function<? super T, ? extends R> mapper
    ) {
        Objects.requireNonNull(mapper, "Mapper cannot be null");

        return input -> {
            ParseResult<T> result = parse(input);

            if (result instanceof ParseFailure<?> failure) {
                return new ParseFailure<>(
                        failure.reason(),
                        failure.rest()
                );
            }

            @SuppressWarnings("unchecked")
            ParseSuccess<T> success = (ParseSuccess<T>) result;

            R mappedValue = mapper.apply(success.value());

            return new ParseSuccess<>(
                    mappedValue,
                    success.rest()
            );
        };
    }

    /**
     * Sequentially composes this parser with another parser selected
     * from the successful value.
     *
     * <p>This operation corresponds to monadic parser composition:
     * the next parser receives the remaining input produced by the
     * previous successful parser.</p>
     *
     * @param mapper function that selects the next parser
     * @param <R> resulting value type
     * @return sequentially composed parser
     */
    default <R> Parser<R> flatMap(
            Function<? super T, Parser<R>> mapper
    ) {
        Objects.requireNonNull(mapper, "Mapper cannot be null");

        return input -> {
            ParseResult<T> result = parse(input);

            if (result instanceof ParseFailure<?> failure) {
                return new ParseFailure<>(
                        failure.reason(),
                        failure.rest()
                );
            }

            @SuppressWarnings("unchecked")
            ParseSuccess<T> success = (ParseSuccess<T>) result;

            Parser<R> nextParser = Objects.requireNonNull(
                    mapper.apply(success.value()),
                    "Mapped parser cannot be null"
            );

            return nextParser.parse(success.rest());
        };
    }
}