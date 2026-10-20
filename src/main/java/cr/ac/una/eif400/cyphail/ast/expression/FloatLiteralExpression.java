package cr.ac.una.eif400.cyphail.ast.expression;

/**
 * Immutable AST node representing a floating-point literal.
 *
 * Sprint P2 introduces decimal values used in Cyphail
 * expressions and property assignments.
 *
 * Examples:
 * 19.99
 * 9.5
 * 3.14
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * School: School of Informatics
 * Group Code: 04-10am
 *
 * @param value finite floating-point value
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
public record FloatLiteralExpression(double value)
        implements LiteralExpression {

    /**
     * Validates that the literal contains a finite number.
     */
    public FloatLiteralExpression {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Float literal must be finite"
            );
        }
    }
}