package cr.ac.una.eif400.cyphail.compiler.parser.combinator;

import cr.ac.una.eif400.cyphail.compiler.lexer.Token;
import cr.ac.una.eif400.cyphail.compiler.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Collection of primitive functional parser combinators for Cyphail.
 *
 * <p>These combinators operate over immutable ParserInput values and
 * return ParseResult values without mutating the original input.</p>
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
public final class Parsers {

    private Parsers() {
    }

    /**
     * Creates a parser that succeeds without consuming input.
     *
     * @param value value returned by the parser
     * @param <T> value type
     * @return parser that always succeeds
     */
    public static <T> Parser<T> pure(T value) {
        Objects.requireNonNull(value, "Value cannot be null");

        return input -> new ParseSuccess<>(
                value,
                input
        );
    }

    /**
     * Creates a parser that always fails without consuming input.
     *
     * @param reason failure reason
     * @param <T> expected result type
     * @return parser that always fails
     */
    public static <T> Parser<T> fail(String reason) {
        Objects.requireNonNull(reason, "Reason cannot be null");

        return input -> new ParseFailure<>(
                reason,
                input
        );
    }

    /**
     * Creates a parser that accepts the current token when it satisfies
     * the supplied predicate.
     *
     * @param predicate condition applied to the current token
     * @param expectation description used when parsing fails
     * @return token parser
     */
    public static Parser<Token> satisfy(
            Predicate<Token> predicate,
            String expectation
    ) {
        Objects.requireNonNull(
                predicate,
                "Predicate cannot be null"
        );

        Objects.requireNonNull(
                expectation,
                "Expectation cannot be null"
        );

        return input -> {
            Token current = input.current();

            if (predicate.test(current)) {
                return new ParseSuccess<>(
                        current,
                        input.advance()
                );
            }

            return new ParseFailure<>(
                    "Expected " + expectation
                            + " but found " + current.type(),
                    input
            );
        };
    }

    /**
     * Creates a parser for one exact token type.
     *
     * @param expectedType required token type
     * @return token parser
     */
    public static Parser<Token> token(
            TokenType expectedType
    ) {
        Objects.requireNonNull(
                expectedType,
                "Expected token type cannot be null"
        );

        return satisfy(
                current -> current.type() == expectedType,
                expectedType.name()
        );
    }

    /**
     * Creates a parser for an identifier token.
     *
     * @return identifier parser
     */
    public static Parser<Token> identifier() {
        return token(TokenType.IDENTIFIER);
    }

    /**
     * Tries the first parser and, if it fails, tries the second parser
     * from the original input.
     *
     * @param first first parser alternative
     * @param second second parser alternative
     * @param <T> produced value type
     * @return alternative parser
     */
    public static <T> Parser<T> or(
            Parser<T> first,
            Parser<T> second
    ) {
        Objects.requireNonNull(
                first,
                "First parser cannot be null"
        );

        Objects.requireNonNull(
                second,
                "Second parser cannot be null"
        );

        return input -> {
            ParseResult<T> firstResult = first.parse(input);

            if (firstResult.isSuccess()) {
                return firstResult;
            }

            return second.parse(input);
        };
    }

    /**
     * Converts a parser into an optional parser.
     *
     * <p>If the parser fails, this combinator succeeds with an empty
     * Optional and leaves the input untouched.</p>
     *
     * @param parser parser to make optional
     * @param <T> produced value type
     * @return optional parser
     */
    public static <T> Parser<Optional<T>> optional(
            Parser<T> parser
    ) {
        Objects.requireNonNull(
                parser,
                "Parser cannot be null"
        );

        return input -> {
            ParseResult<T> result = parser.parse(input);

            if (result instanceof ParseSuccess<T> success) {
                return new ParseSuccess<>(
                        Optional.of(success.value()),
                        success.rest()
                );
            }

            return new ParseSuccess<>(
                    Optional.empty(),
                    input
            );
        };
    }

    /**
     * Repeats a parser zero or more times.
     *
     * <p>Parsing stops when the supplied parser fails. The failure used
     * to stop repetition is not propagated.</p>
     *
     * @param parser parser to repeat
     * @param <T> produced value type
     * @return parser producing all successfully parsed values
     */
    public static <T> Parser<List<T>> many(
            Parser<T> parser
    ) {
        Objects.requireNonNull(
                parser,
                "Parser cannot be null"
        );

        return input -> {
            List<T> values = new ArrayList<>();
            ParserInput rest = input;

            while (true) {
                ParseResult<T> result = parser.parse(rest);

                if (result instanceof ParseFailure<?>) {
                    return new ParseSuccess<>(
                            List.copyOf(values),
                            rest
                    );
                }

                @SuppressWarnings("unchecked")
                ParseSuccess<T> success =
                        (ParseSuccess<T>) result;

                if (success.rest().position()
                        == rest.position()) {
                    throw new IllegalStateException(
                            "Repeated parser did not consume input"
                    );
                }

                values.add(success.value());
                rest = success.rest();
            }
        };
    }

    /**
     * Repeats a parser one or more times.
     *
     * @param parser parser to repeat
     * @param <T> produced value type
     * @return parser producing one or more values
     */
    public static <T> Parser<List<T>> manyOne(
            Parser<T> parser
    ) {
        Objects.requireNonNull(
                parser,
                "Parser cannot be null"
        );

        return parser.flatMap(
                first -> many(parser).map(rest -> {
                    List<T> values = new ArrayList<>();
                    values.add(first);
                    values.addAll(rest);
                    return List.copyOf(values);
                })
        );
    }
}