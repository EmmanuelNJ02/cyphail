package cr.ac.una.eif400.cyphail.ast.expression;

import cr.ac.una.eif400.cyphail.ast.AstNode;

/**
 * Base type for expressions represented in the Cyphail AST.
 *
 * <p>Expressions include variables, property accesses, literals and
 * operations such as comparisons. Concrete immutable expression nodes
 * will be represented with Java records.</p>
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
public interface Expression extends AstNode {
}