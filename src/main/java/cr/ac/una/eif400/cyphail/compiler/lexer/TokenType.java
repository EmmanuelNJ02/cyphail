package cr.ac.una.eif400.cyphail.compiler.lexer;

/**
 * Defines the token categories recognized by the Cyphail lexer.
 *
 * The token types represent keywords, identifiers, numeric
 * literals, string literals, delimiters and operators.
 *
 * Sprint P2 introduces FLOAT to support decimal values
 * such as 19.99 and 9.5 while preserving INTEGER tokens.
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
    FLOAT,
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