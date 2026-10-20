package cr.ac.una.eif400.cyphail.compiler.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tokenizes Cyphail source text into an immutable sequence of tokens.
 *
 * The lexer recognizes keywords, identifiers, integer and decimal
 * literals, string literals, delimiters and operators.
 * White space and comments are ignored.
 *
 * Sprint P2 extends numeric tokenization to support decimal literals
 * without changing existing integer or property access behavior.
 *
 * This class only performs lexical analysis. Syntactic decisions
 * are delegated to the functional parser combinators.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * School: School of Informatics
 * Group Code: 04-10am
 *
 * @author Emmanuel Nunez Jimenez
 * @author Keynell Molina Mora
 * @author Valery Alfaro Morales
 * @author Julissa Solano Valverde
 * @author Roy Arias Mejia
 */
public final class CyphailLexer {

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("MATCH", TokenType.MATCH),
            Map.entry("WHERE", TokenType.WHERE),
            Map.entry("CREATE", TokenType.CREATE),
            Map.entry("SET", TokenType.SET),
            Map.entry("DETACH", TokenType.DETACH),
            Map.entry("DELETE", TokenType.DELETE),
            Map.entry("REMOVE", TokenType.REMOVE),
            Map.entry("RETURN", TokenType.RETURN),
            Map.entry("DISTINCT", TokenType.DISTINCT),
            Map.entry("AS", TokenType.AS),
            Map.entry("ORDER", TokenType.ORDER),
            Map.entry("BY", TokenType.BY),
            Map.entry("ASCENDING", TokenType.ASCENDING),
            Map.entry("DESCENDING", TokenType.DESCENDING),
            Map.entry("SKIP", TokenType.SKIP),
            Map.entry("LIMIT", TokenType.LIMIT),
            Map.entry("OR", TokenType.OR),
            Map.entry("AND", TokenType.AND),
            Map.entry("NOT", TokenType.NOT),
            Map.entry("IS", TokenType.IS),
            Map.entry("NULL", TokenType.NULL),
            Map.entry("TRUE", TokenType.TRUE),
            Map.entry("FALSE", TokenType.FALSE)
    );

    /**
     * Converts Cyphail source text into tokens.
     *
     * @param source Cyphail query source
     * @return immutable list of tokens ending in EOF
     * @throws LexerException when an invalid character or malformed
     *                        literal is found
     */
    public List<Token> tokenize(String source) {
        Objects.requireNonNull(source, "Source cannot be null");

        List<Token> tokens = new ArrayList<>();
        int position = 0;

        while (position < source.length()) {
            char current = source.charAt(position);

            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }

            if (startsWith(source, position, "//")) {
                position = skipLineComment(source, position);
                continue;
            }

            if (startsWith(source, position, "/*")) {
                position = skipBlockComment(source, position);
                continue;
            }

            if (isIdentifierStart(current)) {
                position = scanIdentifier(source, position, tokens);
                continue;
            }

            if (Character.isDigit(current)) {
                position = scanNumber(source, position, tokens);
                continue;
            }

            if (current == '"' || current == '\'') {
                position = scanString(source, position, tokens);
                continue;
            }

            int nextPosition = scanOperatorOrDelimiter(
                    source,
                    position,
                    tokens
            );

            if (nextPosition == position) {
                throw new LexerException(
                        "Unexpected character '" + current + "'",
                        position
                );
            }

            position = nextPosition;
        }

        tokens.add(Token.eof(source.length()));
        return List.copyOf(tokens);
    }

    /**
     * Scans a keyword or an identifier.
     *
     * @param source complete source text
     * @param start starting position
     * @param tokens destination token list
     * @return position after the identifier
     */
    private int scanIdentifier(
            String source,
            int start,
            List<Token> tokens
    ) {
        int end = start + 1;

        while (end < source.length()
                && isIdentifierPart(source.charAt(end))) {
            end++;
        }

        String lexeme = source.substring(start, end);
        TokenType type = KEYWORDS.getOrDefault(
                lexeme,
                TokenType.IDENTIFIER
        );

        tokens.add(new Token(type, lexeme, start));
        return end;
    }

    /**
     * Scans an integer or a decimal literal.
     *
     * A decimal literal consists of digits, a decimal point,
     * and at least one digit after the point.
     *
     * Examples:
     * 18    -> INTEGER
     * 19.99 -> FLOAT
     * 9.5   -> FLOAT
     *
     * The RANGE operator is preserved:
     * 1..3 -> INTEGER, RANGE, INTEGER
     *
     * @param source complete source text
     * @param start starting position
     * @param tokens destination token list
     * @return position after the numeric literal
     */
    private int scanNumber(
            String source,
            int start,
            List<Token> tokens
    ) {
        int end = start;

        while (end < source.length()
                && Character.isDigit(source.charAt(end))) {
            end++;
        }

        TokenType type = TokenType.INTEGER;

        if (end + 1 < source.length()
                && source.charAt(end) == '.'
                && Character.isDigit(source.charAt(end + 1))) {

            type = TokenType.FLOAT;
            end++;

            while (end < source.length()
                    && Character.isDigit(source.charAt(end))) {
                end++;
            }
        }

        tokens.add(new Token(
                type,
                source.substring(start, end),
                start
        ));

        return end;
    }

    /**
     * Scans a quoted string literal.
     *
     * @param source complete source text
     * @param start starting position
     * @param tokens destination token list
     * @return position after the string literal
     */
    private int scanString(
            String source,
            int start,
            List<Token> tokens
    ) {
        char quote = source.charAt(start);
        int position = start + 1;

        while (position < source.length()) {
            char current = source.charAt(position);

            if (current == quote) {
                int end = position + 1;

                tokens.add(new Token(
                        TokenType.STRING,
                        source.substring(start, end),
                        start
                ));

                return end;
            }

            if (quote == '"' && current == '\\') {
                if (position + 1 >= source.length()) {
                    throw new LexerException(
                            "Unterminated string literal",
                            start
                    );
                }

                position += 2;
                continue;
            }

            position++;
        }

        throw new LexerException(
                "Unterminated string literal",
                start
        );
    }

    /**
     * Scans an operator or delimiter.
     *
     * @param source complete source text
     * @param position current position
     * @param tokens destination token list
     * @return position after the token, or the same position
     *         when no valid token is recognized
     */
    private int scanOperatorOrDelimiter(
            String source,
            int position,
            List<Token> tokens
    ) {
        if (addTwoCharacterToken(source, position, tokens)) {
            return position + 2;
        }

        TokenType type = switch (source.charAt(position)) {
            case '(' -> TokenType.LEFT_PAREN;
            case ')' -> TokenType.RIGHT_PAREN;
            case '[' -> TokenType.LEFT_BRACKET;
            case ']' -> TokenType.RIGHT_BRACKET;
            case '{' -> TokenType.LEFT_BRACE;
            case '}' -> TokenType.RIGHT_BRACE;
            case ',' -> TokenType.COMMA;
            case ':' -> TokenType.COLON;
            case '.' -> TokenType.DOT;
            case '|' -> TokenType.PIPE;
            case '*' -> TokenType.STAR;
            case '+' -> TokenType.PLUS;
            case '-' -> TokenType.MINUS;
            case '/' -> TokenType.SLASH;
            case '%' -> TokenType.PERCENT;
            case '=' -> TokenType.EQUAL;
            case '<' -> TokenType.LESS_THAN;
            case '>' -> TokenType.GREATER_THAN;
            default -> null;
        };

        if (type == null) {
            return position;
        }

        tokens.add(new Token(
                type,
                String.valueOf(source.charAt(position)),
                position
        ));

        return position + 1;
    }

    /**
     * Recognizes operators consisting of two characters.
     *
     * @param source complete source text
     * @param position current position
     * @param tokens destination token list
     * @return true when a two-character token is recognized
     */
    private boolean addTwoCharacterToken(
            String source,
            int position,
            List<Token> tokens
    ) {
        if (position + 1 >= source.length()) {
            return false;
        }

        String lexeme = source.substring(
                position,
                position + 2
        );

        TokenType type = switch (lexeme) {
            case "<>" -> TokenType.NOT_EQUAL;
            case "<=" -> TokenType.LESS_THAN_OR_EQUAL;
            case ">=" -> TokenType.GREATER_THAN_OR_EQUAL;
            case "->" -> TokenType.ARROW_RIGHT;
            case "<-" -> TokenType.ARROW_LEFT;
            case ".." -> TokenType.RANGE;
            default -> null;
        };

        if (type == null) {
            return false;
        }

        tokens.add(new Token(type, lexeme, position));
        return true;
    }

    /**
     * Skips a single-line comment.
     *
     * @param source complete source text
     * @param start comment starting position
     * @return position after the comment
     */
    private int skipLineComment(String source, int start) {
        int position = start + 2;

        while (position < source.length()
                && source.charAt(position) != '\n'
                && source.charAt(position) != '\r') {
            position++;
        }

        return position;
    }

    /**
     * Skips a block comment.
     *
     * @param source complete source text
     * @param start comment starting position
     * @return position after the comment
     */
    private int skipBlockComment(String source, int start) {
        int position = start + 2;

        while (position + 1 < source.length()) {
            if (startsWith(source, position, "*/")) {
                return position + 2;
            }

            position++;
        }

        throw new LexerException(
                "Unterminated block comment",
                start
        );
    }

    /**
     * Checks whether the source contains the expected text.
     *
     * @param source complete source text
     * @param position current position
     * @param expected expected text
     * @return true when the expected text matches
     */
    private boolean startsWith(
            String source,
            int position,
            String expected
    ) {
        return source.startsWith(expected, position);
    }

    /**
     * Determines whether a character can start an identifier.
     *
     * @param value character to check
     * @return true for valid identifier-start characters
     */
    private boolean isIdentifierStart(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || value == '_';
    }

    /**
     * Determines whether a character can appear in an identifier.
     *
     * @param value character to check
     * @return true for valid identifier characters
     */
    private boolean isIdentifierPart(char value) {
        return isIdentifierStart(value)
                || (value >= '0' && value <= '9');
    }
}