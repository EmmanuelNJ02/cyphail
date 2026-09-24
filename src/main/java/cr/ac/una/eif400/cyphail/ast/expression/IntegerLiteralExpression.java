package cr.ac.una.eif400.cyphail.ast.expression;

/**
 * Immutable AST node that represents an integer literal.
 *
 * <p>Examples are 1, 300, 1999 or 2026.</p>
 *
 * <p>Project: Cyphail</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica - Escuela de Informatica</p>
 * <p>Group Code: 04-10am</p>
 *
 * <p>Authors:</p>
 * <ul>
 *     <li>Emmanuel Nunez Jimenez</li>
 *     <li>Valery Alfaro Morales</li>
 *     <li>Roy Arias Mejia</li>
 *     <li>Keynell Molina Mora</li>
 *     <li>Julissa Solano Valverde</li>
 * </ul>
 *
 * @param value integer value
 */
public record IntegerLiteralExpression(long value)
        implements LiteralExpression {
}