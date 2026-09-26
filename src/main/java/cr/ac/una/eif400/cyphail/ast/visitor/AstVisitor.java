package cr.ac.una.eif400.cyphail.ast.visitor;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.clause.MatchClause;
import cr.ac.una.eif400.cyphail.ast.clause.Projection;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionItem;
import cr.ac.una.eif400.cyphail.ast.clause.ReturnClause;
import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.BooleanLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.IntegerLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.NullLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;
import cr.ac.una.eif400.cyphail.ast.expression.StringLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.NodePattern;
import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;

/**
 * Visitor contract for the Cyphail Sprint P1 AST.
 *
 * <p>The visitor separates operations over the AST from the immutable
 * AST node model. Concrete visitors can traverse the query structure
 * without adding presentation logic to the AST records themselves.</p>
 *
 * <p>The Sprint P1 tree printer uses this contract to produce the
 * logical representation requested by the .tree command.</p>
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
 * @param <R> result type produced by the visitor
 */
public interface AstVisitor<R> {

    R visitQuery(Query query);

    R visitMatchClause(MatchClause clause);

    R visitWhereClause(WhereClause clause);

    R visitCreateClause(CreateClause clause);

    R visitDeleteClause(DeleteClause clause);

    R visitReturnClause(ReturnClause clause);

    R visitPattern(Pattern pattern);

    R visitNodePattern(NodePattern node);

    R visitPatternProperty(PatternProperty property);

    R visitProjection(Projection projection);

    R visitProjectionItem(ProjectionItem item);

    R visitBinaryExpression(BinaryExpression expression);

    R visitPropertyExpression(PropertyExpression expression);

    R visitVariableExpression(VariableExpression expression);

    R visitIntegerLiteralExpression(
            IntegerLiteralExpression expression
    );

    R visitStringLiteralExpression(
            StringLiteralExpression expression
    );

    R visitBooleanLiteralExpression(
            BooleanLiteralExpression expression
    );

    R visitNullLiteralExpression(
            NullLiteralExpression expression
    );
}