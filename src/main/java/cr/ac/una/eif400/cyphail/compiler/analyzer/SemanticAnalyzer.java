package cr.ac.una.eif400.cyphail.compiler.analyzer;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionItem;
import cr.ac.una.eif400.cyphail.ast.clause.UpdatingClause;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;
import cr.ac.una.eif400.cyphail.ast.expression.LiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.NodePattern;
import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Semantic analyzer for the Cyphail Sprint P1 AST.
 *
 * <p>The analyzer traverses a query in source order and maintains a
 * symbol table containing the variables that have already been
 * declared. Variable references are validated against that table.</p>
 *
 * <p>The traversal order is:</p>
 *
 * <pre>
 * MATCH
 * WHERE
 * updating clauses
 * RETURN
 * </pre>
 *
 * <p>This order allows the analyzer to detect references to variables
 * that are used before they are declared, including variables used
 * inside node-pattern properties.</p>
 *
 * <p>Property names are not treated as variables. For example, in
 * p.id only p is validated against the symbol table; id is the
 * property name.</p>
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
public final class SemanticAnalyzer {

    /**
     * Analyzes a parsed Cyphail query.
     *
     * @param query query AST
     * @return semantic analysis result
     */
    public SemanticAnalysisResult analyze(Query query) {
        Objects.requireNonNull(
                query,
                "Query cannot be null"
        );

        SymbolTable symbols = new SymbolTable();
        List<String> errors = new ArrayList<>();

        analyzePattern(
                query.match().pattern(),
                symbols,
                errors,
                "MATCH pattern"
        );

        query.where().ifPresent(
                where -> analyzeExpression(
                        where.expression(),
                        symbols,
                        errors,
                        "WHERE clause"
                )
        );

        for (UpdatingClause update : query.updates()) {
            analyzeUpdatingClause(
                    update,
                    symbols,
                    errors
            );
        }

        query.returnClause().ifPresent(
                returnClause -> {
                    for (ProjectionItem item
                            : returnClause.projection().items()) {

                        analyzeExpression(
                                item.expression(),
                                symbols,
                                errors,
                                "RETURN clause"
                        );
                    }
                }
        );

        return new SemanticAnalysisResult(errors);
    }

    private void analyzeUpdatingClause(
            UpdatingClause update,
            SymbolTable symbols,
            List<String> errors
    ) {
        if (update instanceof CreateClause createClause) {
            analyzePattern(
                    createClause.pattern(),
                    symbols,
                    errors,
                    "CREATE pattern"
            );
            return;
        }

        if (update instanceof DeleteClause deleteClause) {
            for (Expression expression
                    : deleteClause.expressions()) {

                analyzeExpression(
                        expression,
                        symbols,
                        errors,
                        "DELETE clause"
                );
            }
        }
    }

    private void analyzePattern(
            Pattern pattern,
            SymbolTable symbols,
            List<String> errors,
            String context
    ) {
        for (NodePattern node : pattern.nodes()) {

            node.variable().ifPresent(symbols::define);

            for (PatternProperty property
                    : node.properties()) {

                analyzeExpression(
                        property.value(),
                        symbols,
                        errors,
                        context
                );
            }
        }
    }

    private void analyzeExpression(
            Expression expression,
            SymbolTable symbols,
            List<String> errors,
            String context
    ) {
        if (expression instanceof VariableExpression variable) {
            validateVariable(
                    variable.name(),
                    symbols,
                    errors,
                    context
            );
            return;
        }

        if (expression instanceof PropertyExpression property) {
            analyzeExpression(
                    property.target(),
                    symbols,
                    errors,
                    context
            );
            return;
        }

        if (expression instanceof BinaryExpression binary) {
            analyzeExpression(
                    binary.left(),
                    symbols,
                    errors,
                    context
            );

            analyzeExpression(
                    binary.right(),
                    symbols,
                    errors,
                    context
            );
            return;
        }

        if (expression instanceof LiteralExpression) {
            return;
        }

        throw new IllegalArgumentException(
                "Unsupported expression type: "
                        + expression.getClass().getName()
        );
    }

    private void validateVariable(
            String name,
            SymbolTable symbols,
            List<String> errors,
            String context
    ) {
        if (!symbols.isDefined(name)) {
            errors.add(
                    "Undefined variable '"
                            + name
                            + "' in "
                            + context
                            + "."
            );
        }
    }
}