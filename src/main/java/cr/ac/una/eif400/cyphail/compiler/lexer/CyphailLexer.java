package cr.ac.una.eif400.cyphail.compiler.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tokenizes Cyphail source text into an immutable sequence of tokens.
 *
 * <p>The lexer recognizes the keywords, identifiers, integer literals,
 * string literals, delimiters and operators required by the Cyphail
 * Sprint P1 grammar. White space and comments are ignored.</p>
 *
 * <p>This class only performs lexical analysis. Syntactic decisions are
 * delegated to the functional parser combinators.</p>
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
                position = scanInteger(source, position, tokens);
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

    private int scanInteger(
            String source,
            int start,
            List<Token> tokens
    ) {
        int end = start + 1;

        while (end < source.length()
                && Character.isDigit(source.charAt(end))) {
            end++;
        }

        tokens.add(new Token(
                TokenType.INTEGER,
                source.substring(start, end),
                start
        ));

        return end;
    }

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

    private boolean addTwoCharacterToken(
            String source,
            int position,
            List<Token> tokens
    ) {
        if (position + 1 >= source.length()) {
            return false;
        }

        String lexeme = source.substring(position, position + 2);

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

    private int skipLineComment(String source, int start) {
        int position = start + 2;

        while (position < source.length()
                && source.charAt(position) != '\n'
                && source.charAt(position) != '\r') {
            position++;
        }

        return position;
    }

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

    private boolean startsWith(
            String source,
            int position,
            String expected
    ) {
        return source.startsWith(expected, position);
    }

    private boolean isIdentifierStart(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || value == '_';
    }

    private boolean isIdentifierPart(char value) {
        return isIdentifierStart(value)
                || (value >= '0' && value <= '9');
    }
}