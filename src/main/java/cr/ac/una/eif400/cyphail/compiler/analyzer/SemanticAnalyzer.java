package cr.ac.una.eif400.cyphail.compiler.analyzer;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionItem;
import cr.ac.una.eif400.cyphail.ast.clause.RemoveClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetAllProperties;
import cr.ac.una.eif400.cyphail.ast.clause.SetClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetItem;
import cr.ac.una.eif400.cyphail.ast.clause.SetLabels;
import cr.ac.una.eif400.cyphail.ast.clause.SetProperty;
import cr.ac.una.eif400.cyphail.ast.clause.UpdatingClause;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;
import cr.ac.una.eif400.cyphail.ast.expression.LiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.NodePattern;
import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternPath;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipPattern;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipStep;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Semantic analyzer for the Cyphail language.
 *
 * <p>Sprint P1 validates variable declarations and references
 * according to the order in which query clauses appear.</p>
 *
 * <p>Sprint P2 extends the analysis with relationship
 * variables, additional static semantic restrictions,
 * standalone CREATE statements without MATCH,
 * SET assignments and REMOVE property clauses.</p>
 *
 * <p>Sprint P2 rules:</p>
 * <ul>
 *     <li>Relationship variables cannot be declared twice
 *         within the same query.</li>
 *     <li>Node and relationship variables cannot share
 *         the same name.</li>
 *     <li>Variables cannot begin with two underscores.</li>
 *     <li>Node variables may be reused for joins.</li>
 *     <li>Relationship properties may reference previously
 *         declared variables.</li>
 *     <li>Standalone CREATE statements do not require
 *         a preceding MATCH clause.</li>
 *     <li>SET assignments must reference previously
 *         declared variables.</li>
 *     <li>REMOVE properties must reference previously
 *         declared node or relationship variables.</li>
 * </ul>
 *
 * <p>The analyzer processes MATCH when present, followed
 * by WHERE, updating clauses and RETURN in source order.</p>
 *
 * <p>Pattern paths are traversed from their first node
 * through each relationship step.</p>
 *
 * <p>The symbol table includes both node and relationship
 * variables, while separate sets preserve their roles.</p>
 *
 * <p>Property names, node labels and relationship types
 * are not treated as variable declarations.</p>
 *
 * <p>Project: Cyphail - Sprint P2</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica</p>
 * <p>Escuela de Informatica</p>
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
 * <p>Sprint P2 contributions:
 * Relationship-aware static semantic validation,
 * standalone CREATE statements, SET analysis
 * and REMOVE analysis.</p>
 */
public final class SemanticAnalyzer {

    /**
     * Analyzes a parsed Cyphail query.
     *
     * <p>A fresh analysis state is created for every query
     * to prevent variable declarations from leaking between
     * separate invocations.</p>
     *
     * <p>Sprint P2 allows queries without MATCH when they
     * begin with a standalone CREATE clause.</p>
     *
     * @param query query AST
     * @return semantic analysis result
     */
    public SemanticAnalysisResult analyze(Query query) {
        Objects.requireNonNull(
                query,
                "Query cannot be null"
        );

        AnalysisState state = new AnalysisState();

        /*
         * Sprint P2:
         * MATCH is optional for standalone CREATE queries.
         *
         * If MATCH exists, analyze its pattern normally.
         * Otherwise, continue with the updating clauses.
         *
         * This preserves the original P1 behavior.
         */
        query.optionalMatch().ifPresent(
                match -> analyzePattern(
                        match.pattern(),
                        state,
                        "MATCH pattern"
                )
        );

        query.where().ifPresent(
                where -> analyzeExpression(
                        where.expression(),
                        state,
                        "WHERE clause"
                )
        );

        for (UpdatingClause update : query.updates()) {
            analyzeUpdatingClause(
                    update,
                    state
            );
        }

        query.returnClause().ifPresent(
                returnClause -> {
                    for (ProjectionItem item
                            : returnClause.projection().items()) {

                        analyzeExpression(
                                item.expression(),
                                state,
                                "RETURN clause"
                        );
                    }
                }
        );

        return new SemanticAnalysisResult(
                state.errors
        );
    }

    /**
     * Analyzes an updating clause in source order.
     *
     * <p>CREATE declares node and relationship variables.
     * SET and REMOVE reference previously declared variables.
     * DELETE validates its target expressions.</p>
     *
     * @param update updating clause
     * @param state query analysis state
     */
    private void analyzeUpdatingClause(
            UpdatingClause update,
            AnalysisState state
    ) {
        if (update instanceof CreateClause createClause) {
            analyzePattern(
                    createClause.pattern(),
                    state,
                    "CREATE pattern"
            );
            return;
        }

        if (update instanceof SetClause setClause) {
            for (SetItem item : setClause.items()) {
                analyzeSetItem(
                        item,
                        state
                );
            }
            return;
        }

        if (update instanceof RemoveClause removeClause) {
            for (PropertyExpression property
                    : removeClause.properties()) {

                analyzeExpression(
                        property,
                        state,
                        "REMOVE clause"
                );
            }
            return;
        }

        if (update instanceof DeleteClause deleteClause) {
            for (Expression expression
                    : deleteClause.expressions()) {

                analyzeExpression(
                        expression,
                        state,
                        "DELETE clause"
                );
            }
        }
    }

    /**
     * Validates a SET item according to its AST type.
     *
     * <p>Property assignments validate both the target
     * and assigned value. The other existing SET item
     * types are also preserved and analyzed.</p>
     *
     * @param item SET item
     * @param state query analysis state
     */
    private void analyzeSetItem(
            SetItem item,
            AnalysisState state
    ) {
        if (item instanceof SetProperty property) {
            analyzeExpression(
                    property.property(),
                    state,
                    "SET target"
            );

            analyzeExpression(
                    property.value(),
                    state,
                    "SET value"
            );
            return;
        }

        if (item instanceof SetAllProperties allProperties) {
            analyzeExpression(
                    allProperties.variable(),
                    state,
                    "SET target"
            );

            analyzeExpression(
                    allProperties.value(),
                    state,
                    "SET value"
            );
            return;
        }

        if (item instanceof SetLabels labels) {
            analyzeExpression(
                    labels.variable(),
                    state,
                    "SET target"
            );
            return;
        }

        throw new IllegalArgumentException(
                "Unsupported SET item type: "
                        + item.getClass().getName()
        );
    }

    /**
     * Analyzes all pattern paths in their original order.
     *
     * @param pattern pattern AST
     * @param state query analysis state
     * @param context clause description
     */
    private void analyzePattern(
            Pattern pattern,
            AnalysisState state,
            String context
    ) {
        for (PatternPath path : pattern.paths()) {
            analyzeNode(
                    path.firstNode(),
                    state,
                    context
            );

            for (RelationshipStep step : path.steps()) {
                analyzeRelationship(
                        step.relationship(),
                        state,
                        context
                );

                analyzeNode(
                        step.rightNode(),
                        state,
                        context
                );
            }
        }
    }

    /**
     * Registers a node variable and validates its properties.
     *
     * <p>A node variable may appear multiple times in a query,
     * allowing patterns to join through the same variable.</p>
     *
     * @param node node pattern
     * @param state query analysis state
     * @param context clause description
     */
    private void analyzeNode(
            NodePattern node,
            AnalysisState state,
            String context
    ) {
        node.variable().ifPresent(
                name -> {
                    validateReservedVariable(
                            name,
                            state,
                            context
                    );

                    if (state.relationshipVariables.contains(name)) {
                        reportVariableRoleConflict(
                                name,
                                state
                        );
                    }

                    state.nodeVariables.add(name);
                    state.symbols.define(name);
                }
        );

        for (PatternProperty property : node.properties()) {
            analyzeExpression(
                    property.value(),
                    state,
                    context
            );
        }
    }

    /**
     * Registers a relationship variable and validates
     * its properties.
     *
     * <p>Unlike node variables, relationship variables
     * must be unique within the complete query.</p>
     *
     * @param relationship relationship pattern
     * @param state query analysis state
     * @param context clause description
     */
    private void analyzeRelationship(
            RelationshipPattern relationship,
            AnalysisState state,
            String context
    ) {
        relationship.variable().ifPresent(
                name -> {
                    validateReservedVariable(
                            name,
                            state,
                            context
                    );

                    if (state.nodeVariables.contains(name)) {
                        reportVariableRoleConflict(
                                name,
                                state
                        );
                    }

                    if (!state.relationshipVariables.add(name)) {
                        state.errors.add(
                                "Relationship variable '"
                                        + name
                                        + "' is declared more than once "
                                        + "in the query."
                        );
                    }

                    state.symbols.define(name);
                }
        );

        for (PatternProperty property
                : relationship.properties()) {

            analyzeExpression(
                    property.value(),
                    state,
                    context
            );
        }
    }

    /**
     * Reports a name used for both a node and a relationship.
     *
     * @param name conflicting variable name
     * @param state query analysis state
     */
    private void reportVariableRoleConflict(
            String name,
            AnalysisState state
    ) {
        state.errors.add(
                "Variable '"
                        + name
                        + "' cannot be used as both "
                        + "a node and a relationship."
        );
    }

    /**
     * Checks the reserved double-underscore prefix.
     *
     * <p>The prefix is reserved for generated Prolog
     * identifiers used by the Cyphail transpiler.</p>
     *
     * @param name variable name
     * @param state query analysis state
     * @param context clause description
     */
    private void validateReservedVariable(
            String name,
            AnalysisState state,
            String context
    ) {
        if (name.startsWith("__")
                && state.reportedReservedVariables.add(name)) {

            state.errors.add(
                    "Variable '"
                            + name
                            + "' cannot start with '__' in "
                            + context
                            + "."
            );
        }
    }

    /**
     * Analyzes variable references inside an expression.
     *
     * <p>Property names are not variable references.
     * Only the target expression is analyzed.</p>
     *
     * @param expression expression AST
     * @param state query analysis state
     * @param context clause description
     */
    private void analyzeExpression(
            Expression expression,
            AnalysisState state,
            String context
    ) {
        if (expression instanceof VariableExpression variable) {
            validateVariable(
                    variable.name(),
                    state,
                    context
            );
            return;
        }

        if (expression instanceof PropertyExpression property) {
            analyzeExpression(
                    property.target(),
                    state,
                    context
            );
            return;
        }

        if (expression instanceof BinaryExpression binary) {
            analyzeExpression(
                    binary.left(),
                    state,
                    context
            );

            analyzeExpression(
                    binary.right(),
                    state,
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

    /**
     * Validates a variable reference against the symbol table.
     *
     * @param name referenced variable
     * @param state query analysis state
     * @param context clause description
     */
    private void validateVariable(
            String name,
            AnalysisState state,
            String context
    ) {
        validateReservedVariable(
                name,
                state,
                context
        );

        if (!state.symbols.isDefined(name)) {
            state.errors.add(
                    "Undefined variable '"
                            + name
                            + "' in "
                            + context
                            + "."
            );
        }
    }

    /**
     * Stores the mutable analysis data for one query only.
     *
     * <p>Using a local state instance keeps the analyzer
     * reusable without sharing variable declarations
     * between queries.</p>
     */
    private static final class AnalysisState {

        private final SymbolTable symbols =
                new SymbolTable();

        private final Set<String> nodeVariables =
                new LinkedHashSet<>();

        private final Set<String> relationshipVariables =
                new LinkedHashSet<>();

        private final Set<String> reportedReservedVariables =
                new LinkedHashSet<>();

        private final List<String> errors =
                new ArrayList<>();
    }
}