
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.*;
import cr.ac.una.eif400.cyphail.ast.expression.*;
import cr.ac.una.eif400.cyphail.ast.pattern.*;
import cr.ac.una.eif400.cyphail.ast.visitor.AstVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * Transpiles Cyphail query ASTs into executable
 * SWI-Prolog goals.
 *
 * Supported features:
 * - MATCH with isolated nodes and relationship paths.
 * - OUT, IN and NONE relationship directions.
 * - Chained and comma-separated patterns.
 * - Node and relationship property constraints.
 * - WHERE comparisons and supported AST expressions.
 * - RETURN variables, properties and aliases.
 * - Node and relationship identifiers.
 *
 * Property bindings already guaranteed by MATCH or
 * WHERE are reused by RETURN to avoid duplicate
 * Prolog property lookups.
 *
 * Unsupported Sprint P2 features fail explicitly.
 */
public final class PrologCodeGenerator {

    /**
     * Compiles a query using a fresh AST visitor.
     *
     * @param query parsed Cyphail query
     * @return generated goal and output metadata
     */
    public PrologGenerationResult generate(Query query) {

        Objects.requireNonNull(
                query,
                "Query cannot be null"
        );

        return new QueryVisitor().compile(query);
    }

    /**
     * Stateful visitor for one query compilation.
     */
    private static final class QueryVisitor
            implements AstVisitor<String> {

        private final PrologVariableTable symbols =
                new PrologVariableTable();

        private final PrologRelationshipGoalBuilder
                relationshipBuilder =
                new PrologRelationshipGoalBuilder();

        private final PrologWhereGoalBuilder whereBuilder =
                new PrologWhereGoalBuilder();

        private final List<String> goals =
                new ArrayList<>();

        private final List<PrologGenerationResult.OutputColumn>
                columns =
                new ArrayList<>();

        private final Set<String> matchedVariables =
                new HashSet<>();

        /**
         * Tracks properties guaranteed to be bound
         * before processing the next RETURN item.
         *
         * Keys have the form "variable.property".
         */
        private final Set<String> boundProperties =
                new HashSet<>();

        /**
         * Generates one query and its output metadata.
         */
        PrologGenerationResult compile(Query query) {

            String goal = visitQuery(query);

            return new PrologGenerationResult(
                    goal,
                    columns,
                    symbols.variableMappings(),
                    symbols.propertyMappings()
            );
        }

        /**
         * Compiles the query in source execution order:
         *
         * MATCH -> WHERE -> RETURN.
         *
         * The generated Prolog goal registers, executes
         * and removes its temporary query definition.
         */
        @Override
        public String visitQuery(Query query) {

            Objects.requireNonNull(
                    query,
                    "Query cannot be null"
            );

            if (!query.hasMatch()) {
                throw unsupported("Standalone CREATE");
            }

            if (!query.updates().isEmpty()) {
                throw unsupported("Updating clauses");
            }

            if (query.returnClause().isEmpty()) {
                throw unsupported("Queries without RETURN");
            }

            visitMatchClause(query.match());

            query.where().ifPresent(
                    this::visitWhereClause
            );

            visitReturnClause(
                    query.returnClause().orElseThrow()
            );

            String outputVariables = columns.stream()
                    .map(
                            PrologGenerationResult.OutputColumn
                                    ::prologVariable
                    )
                    .collect(Collectors.joining(", "));

            String columnHeaders = columns.stream()
                    .map(
                            column -> quoteAtom(
                                    column.header()
                            )
                    )
                    .collect(Collectors.joining(", "));

            String body = String.join(", ", goals);

            String clause =
                    "(cyphail_query_p2("
                            + outputVariables
                            + ") :- ("
                            + body
                            + "))";

            return "findall(Rows, ("
                    + "cy_create_query(__QueryRef, "
                    + clause
                    + "), "
                    + "cy_exec_query(__QueryRef, ["
                    + columnHeaders
                    + "], Rows), "
                    + "cy_erase_query(__QueryRef)"
                    + "), [Rows])";
        }

        /**
         * Visits the MATCH clause.
         */
        @Override
        public String visitMatchClause(MatchClause clause) {

            Objects.requireNonNull(
                    clause,
                    "MATCH clause cannot be null"
            );

            return visitPattern(clause.pattern());
        }

        /**
         * Generates goals for every path in MATCH.
         *
         * Named relationship property bindings are
         * registered after their pattern is generated.
         */
        @Override
        public String visitPattern(Pattern pattern) {

            Objects.requireNonNull(
                    pattern,
                    "Pattern cannot be null"
            );

            for (PatternPath path : pattern.paths()) {

                String leftNodeId =
                        visitNodePattern(
                                path.firstNode()
                        );

                for (RelationshipStep step : path.steps()) {

                    String rightNodeId =
                            visitNodePattern(
                                    step.rightNode()
                            );

                    PrologRelationshipGoalBuilder
                            .GeneratedRelationship generated =
                            relationshipBuilder.generate(
                                    step.relationship(),
                                    leftNodeId,
                                    rightNodeId,
                                    symbols
                            );

                    goals.addAll(generated.goals());

                    step.relationship()
                            .variable()
                            .ifPresent(name -> {

                                matchedVariables.add(name);

                                for (PatternProperty property :
                                        step.relationship()
                                                .properties()) {

                                    boundProperties.add(
                                            propertyKey(
                                                    name,
                                                    property.name()
                                            )
                                    );
                                }
                            });

                    leftNodeId = rightNodeId;
                }
            }

            return String.join(", ", goals);
        }

        /**
         * Generates node enumeration, labels and
         * literal property constraints.
         *
         * Named property bindings are recorded for
         * reuse by WHERE-related output projections.
         */
        @Override
        public String visitNodePattern(NodePattern node) {

            Objects.requireNonNull(
                    node,
                    "Node pattern cannot be null"
            );

            String nodeId = node.variable()
                    .map(name -> {

                        matchedVariables.add(name);

                        return symbols.resolveVariable(name);
                    })
                    .orElseGet(
                            symbols::newTemporaryVariable
                    );

            goals.add(
                    "user_amigos_node("
                            + nodeId
                            + ")"
            );

            for (String label : node.labels()) {

                goals.add(
                        "user_amigos_label("
                                + nodeId
                                + ", "
                                + quoteAtom(label)
                                + ")"
                );
            }

            for (PatternProperty property : node.properties()) {

                String propertyValue =
                        node.variable().isPresent()
                                ? symbols.resolveProperty(
                                        node.variable()
                                                .orElseThrow(),
                                        property.name()
                                )
                                : symbols.newTemporaryVariable();

                goals.add(
                        "user_amigos_prop("
                                + nodeId
                                + ", "
                                + quoteAtom(property.name())
                                + ", "
                                + propertyValue
                                + ")"
                );

                goals.add(
                        "cy_eq("
                                + propertyValue
                                + ", "
                                + literalTerm(property.value())
                                + ")"
                );

                node.variable().ifPresent(name ->
                        boundProperties.add(
                                propertyKey(
                                        name,
                                        property.name()
                                )
                        )
                );
            }

            return nodeId;
        }

        /**
         * Adds the WHERE goal after MATCH.
         *
         * Records properties that are guaranteed to
         * have been retrieved when WHERE succeeds.
         *
         * For OR, a property is considered guaranteed
         * only when both branches retrieve it.
         */
        @Override
        public String visitWhereClause(WhereClause clause) {

            Objects.requireNonNull(
                    clause,
                    "WHERE clause cannot be null"
            );

            String whereGoal = whereBuilder.generate(
                    clause,
                    symbols,
                    matchedVariables
            );

            goals.add(whereGoal);

            boundProperties.addAll(
                    guaranteedWhereProperties(
                            clause.expression()
                    )
            );

            return whereGoal;
        }

        /**
         * Determines which properties must be bound
         * whenever a WHERE expression succeeds.
         *
         * AND: bindings from both conditions.
         * OR: only bindings common to both branches.
         * Comparison: bindings used by both operands.
         */
        private static Set<String> guaranteedWhereProperties(
                Expression expression
        ) {

            if (!(expression instanceof BinaryExpression binary)) {
                return Set.of();
            }

            if (binary.operator() == BinaryOperator.AND) {

                Set<String> result = new HashSet<>(
                        guaranteedWhereProperties(
                                binary.left()
                        )
                );

                result.addAll(
                        guaranteedWhereProperties(
                                binary.right()
                        )
                );

                return result;
            }

            if (binary.operator() == BinaryOperator.OR) {

                Set<String> result = new HashSet<>(
                        guaranteedWhereProperties(
                                binary.left()
                        )
                );

                result.retainAll(
                        guaranteedWhereProperties(
                                binary.right()
                        )
                );

                return result;
            }

            Set<String> result = new HashSet<>();

            collectPropertyReferences(
                    binary.left(),
                    result
            );

            collectPropertyReferences(
                    binary.right(),
                    result
            );

            return result;
        }

        /**
         * Collects property references used inside
         * a comparison or arithmetic expression.
         */
        private static void collectPropertyReferences(
                Expression expression,
                Set<String> result
        ) {

            if (expression instanceof PropertyExpression property) {

                if (property.target()
                        instanceof VariableExpression owner) {

                    result.add(
                            propertyKey(
                                    owner.name(),
                                    property.property()
                            )
                    );
                }

                return;
            }

            if (expression instanceof BinaryExpression binary) {

                collectPropertyReferences(
                        binary.left(),
                        result
                );

                collectPropertyReferences(
                        binary.right(),
                        result
                );
            }
        }

        /**
         * Creates a consistent property binding key.
         */
        private static String propertyKey(
                String variable,
                String property
        ) {

            return variable + "." + property;
        }

        /**
         * Compiles RETURN and rejects unsupported modifiers.
         */
        @Override
        public String visitReturnClause(ReturnClause clause) {

            Objects.requireNonNull(
                    clause,
                    "RETURN clause cannot be null"
            );

            if (clause.distinct()) {
                throw unsupported("RETURN DISTINCT");
            }

            return visitProjection(clause.projection());
        }

        /**
         * Compiles explicit RETURN items in order.
         */
        @Override
        public String visitProjection(Projection projection) {

            Objects.requireNonNull(
                    projection,
                    "Projection cannot be null"
            );

            if (projection.wildcard()) {
                throw unsupported("RETURN *");
            }

            ProjectionModifiers modifiers =
                    projection.modifiers();

            if (!modifiers.orderBy().isEmpty()
                    || modifiers.skip().isPresent()
                    || modifiers.limit().isPresent()) {

                throw unsupported(
                        "ORDER BY, SKIP or LIMIT"
                );
            }

            projection.items().forEach(
                    this::visitProjectionItem
            );

            return columns.stream()
                    .map(
                            PrologGenerationResult.OutputColumn
                                    ::prologVariable
                    )
                    .collect(Collectors.joining(", "));
        }

        /**
         * Generates a RETURN item and its column metadata.
         */
        @Override
        public String visitProjectionItem(ProjectionItem item) {

            Objects.requireNonNull(
                    item,
                    "Projection item cannot be null"
            );

            Expression expression = item.expression();

            String source;
            String prologVariable;

            if (expression instanceof VariableExpression variable) {

                source = variable.name();

                ensureMatched(source);

                prologVariable =
                        visitVariableExpression(variable);

            } else if (
                    expression instanceof PropertyExpression property
                            && property.target()
                            instanceof VariableExpression owner
            ) {

                source = propertyKey(
                        owner.name(),
                        property.property()
                );

                ensureMatched(owner.name());

                prologVariable =
                        visitPropertyExpression(property);

            } else {

                throw unsupported(
                        "RETURN expression: "
                                + expression
                                        .getClass()
                                        .getSimpleName()
                );
            }

            String header = item.alias().orElse(source);

            columns.add(
                    new PrologGenerationResult.OutputColumn(
                            source,
                            header,
                            prologVariable
                    )
            );

            return prologVariable;
        }

        /**
         * Retrieves a property only if it has not
         * already been guaranteed by an earlier goal.
         *
         * All occurrences use the same Prolog variable
         * through the shared symbol table.
         */
        @Override
        public String visitPropertyExpression(
                PropertyExpression expression
        ) {

            Objects.requireNonNull(
                    expression,
                    "Property expression cannot be null"
            );

            if (!(expression.target()
                    instanceof VariableExpression owner)) {

                throw unsupported(
                        "Nested property expressions"
                );
            }

            ensureMatched(owner.name());

            String elementId =
                    symbols.resolveVariable(owner.name());

            String propertyValue =
                    symbols.resolveProperty(
                            owner.name(),
                            expression.property()
                    );

            String key = propertyKey(
                    owner.name(),
                    expression.property()
            );

            // Avoid repeating a guaranteed property lookup.
            if (boundProperties.add(key)) {

                goals.add(
                        "user_amigos_prop("
                                + elementId
                                + ", "
                                + quoteAtom(expression.property())
                                + ", "
                                + propertyValue
                                + ")"
                );
            }

            return propertyValue;
        }

        /**
         * Resolves a named Cyphail variable to Prolog.
         */
        @Override
        public String visitVariableExpression(
                VariableExpression expression
        ) {

            Objects.requireNonNull(
                    expression,
                    "Variable expression cannot be null"
            );

            return symbols.resolveVariable(
                    expression.name()
            );
        }

        /**
         * Checks that a RETURN variable belongs to MATCH.
         */
        private void ensureMatched(String variableName) {

            if (!matchedVariables.contains(variableName)) {

                throw new IllegalArgumentException(
                        "RETURN references an unmatched variable: "
                                + variableName
                );
            }
        }

        /**
         * Converts supported literals to Prolog terms.
         */
        private static String literalTerm(Expression expression) {

            Objects.requireNonNull(
                    expression,
                    "Literal expression cannot be null"
            );

            if (expression
                    instanceof StringLiteralExpression value) {

                return quoteAtom(value.value());
            }

            if (expression
                    instanceof IntegerLiteralExpression value) {

                return Long.toString(value.value());
            }

            if (expression
                    instanceof FloatLiteralExpression value) {

                return Double.toString(value.value());
            }

            throw unsupported(
                    "Pattern property expression: "
                            + expression
                                    .getClass()
                                    .getSimpleName()
            );
        }

        /**
         * Escapes a value as a SWI-Prolog atom.
         */
        private static String quoteAtom(String value) {

            Objects.requireNonNull(
                    value,
                    "Prolog atom cannot be null"
            );

            return "'"
                    + value.replace("\\", "\\\\")
                            .replace("'", "''")
                    + "'";
        }

        /**
         * Constructs consistent unsupported feature errors.
         */
        private static UnsupportedOperationException unsupported(
                String feature
        ) {

            return new UnsupportedOperationException(
                    "P2 CodeGen support is pending for: "
                            + feature
            );
        }

        /**
         * CREATE CodeGen remains pending.
         */
        @Override
        public String visitCreateClause(CreateClause clause) {

            throw unsupported("CREATE");
        }

        /**
         * DELETE CodeGen remains pending.
         */
        @Override
        public String visitDeleteClause(DeleteClause clause) {

            throw unsupported("DELETE");
        }

        /**
         * Pattern properties are compiled by the
         * node and relationship goal builders.
         */
        @Override
        public String visitPatternProperty(
                PatternProperty property
        ) {

            throw unsupported("PatternProperty visitor");
        }

        /**
         * Binary WHERE expressions are handled by
         * PrologWhereGoalBuilder.
         */
        @Override
        public String visitBinaryExpression(
                BinaryExpression expression
        ) {

            throw unsupported(
                    "Direct binary expression visitor"
            );
        }

        /**
         * Generates an integer literal.
         */
        @Override
        public String visitIntegerLiteralExpression(
                IntegerLiteralExpression expression
        ) {

            return literalTerm(expression);
        }

        /**
         * Generates a string literal.
         */
        @Override
        public String visitStringLiteralExpression(
                StringLiteralExpression expression
        ) {

            return literalTerm(expression);
        }

        /**
         * Generates a boolean literal.
         */
        @Override
        public String visitBooleanLiteralExpression(
                BooleanLiteralExpression expression
        ) {

            return expression.value() ? "true" : "false";
        }

        /**
         * NULL runtime semantics remain pending.
         */
        @Override
        public String visitNullLiteralExpression(
                NullLiteralExpression expression
        ) {

            throw unsupported("NULL literals");
        }
    }
}
