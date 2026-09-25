package cr.ac.una.eif400.cyphail.compiler.lexer;

/**
 * Defines the token categories recognized by the Cyphail lexer.
 *
 * <p>The token types are used by the lexer to transform Cyphail source
 * text into a token stream that can later be consumed by the functional
 * parser combinators.</p>
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
public enum TokenType {

    // Keywords
    MATCH,
    WHERE,
    CREATE,
    SET,
    DETACH,
    DELETE,
    REMOVE,
    RETURN,
    DISTINCT,
    AS,
    ORDER,
    BY,
    ASCENDING,
    DESCENDING,
    SKIP,
    LIMIT,
    OR,
    AND,
    NOT,
    IS,
    NULL,
    TRUE,
    FALSE,

    // Values
    IDENTIFIER,
    INTEGER,
    STRING,

    // Delimiters
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACKET,
    RIGHT_BRACKET,
    LEFT_BRACE,
    RIGHT_BRACE,
    COMMA,
    COLON,
    DOT,
    PIPE,

    // Operators
    STAR,
    PLUS,
    MINUS,
    SLASH,
    PERCENT,
    EQUAL,
    NOT_EQUAL,
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN_OR_EQUAL,
    ARROW_RIGHT,
    ARROW_LEFT,
    RANGE,

    // End of input
    EOF
}