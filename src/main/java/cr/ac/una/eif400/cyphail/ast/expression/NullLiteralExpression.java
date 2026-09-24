package cr.ac.una.eif400.cyphail.ast.expression;

/**
 * Immutable AST node that represents the Cyphail NULL literal.
 *
 * <p>This node has no associated value because the node itself represents
 * the presence of NULL in a Cyphail expression.</p>
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
 */
public record NullLiteralExpression()
        implements LiteralExpression {
}