
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
 * Julissa Solano Valverde
 * Valery Alfaro Morales
 * Keynell Molina Mora
 * Roy Arias Mejia
 *
 * Generates executable SWI-Prolog goals from Cyphail ASTs.
 *
 * Supported:
 * - MATCH with nodes and relationship paths.
 * - IN, OUT and NONE relationship directions.
 * - Node and relationship properties.
 * - WHERE comparisons and logical expressions.
 * - RETURN variables, properties and aliases.
 * - Standalone CREATE.
 * - MATCH followed by CREATE.
 * - CREATE nodes and relationships.
 * - Multiple CREATE clauses.
 * - CREATE statements without RETURN.
 *
 * SET, REMOVE and DELETE code generation will be
 * integrated in subsequent Sprint P2 steps.
 */
public final class PrologCodeGenerator {

    /**
     * Generates a complete executable Prolog goal.
     *
     * A fresh visitor is created for every query.
     */
    public PrologGenerationResult generate(Query query) {

        Objects.requireNonNull(
                query,
                "Query cannot be null"
        );

        return new QueryVisitor().compile(query);
    }

    /**
     * Stateful AST visitor for one query.
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

        private final PrologCreateGoalBuilder createBuilder =
                new PrologCreateGoalBuilder();

        private final List<String> goals =
                new ArrayList<>();

        private final List<PrologGenerationResult.OutputColumn>
                columns =
                new ArrayList<>();

        /**
         * Available source variables.
         *
         * Includes variables introduced by MATCH
         * and subsequently created by CREATE.
         */
        private final Set<String> matchedVariables =
                new HashSet<>();

        /**
         * Properties whose values have already been
         * bound by earlier Prolog goals.
         */
        private final Set<String> boundProperties =
                new HashSet<>();

        /**
         * Compiles a query and its output metadata.
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
         * Compiles the query in source order:
         *
         * 1. MATCH, when present.
         * 2. WHERE, when present.
         * 3. Updating clauses, in source order.
         * 4. RETURN, when present.
         *
         * A standalone CREATE does not require MATCH.
         * Updating statements can execute without RETURN.
         *
         * The existing query registration and execution
         * lifecycle is preserved.
         */
        @Override
        public String visitQuery(Query query) {

            Objects.requireNonNull(
                    query,
                    "Query cannot be null"
            );

            if (!query.hasMatch()
                    && query.updates().isEmpty()) {

                throw unsupported(
                        "Query without MATCH or CREATE"
                );
            }

            if (query.returnClause().isEmpty()
                    && query.updates().isEmpty()) {

                throw unsupported(
                        "Queries without RETURN"
                );
            }

            /*
             * MATCH is optional in P2 for queries
             * beginning with standalone CREATE.
             */
            if (query.hasMatch()) {

                visitMatchClause(
                        query.match()
                );
            }

            /*
             * WHERE is evaluated after MATCH and
             * before updating clauses.
             */
            query.where().ifPresent(
                    this::visitWhereClause
            );

            /*
             * Execute all updating clauses in
             * their original source order.
             */
            for (UpdatingClause update : query.updates()) {

                if (update instanceof CreateClause create) {

                    visitCreateClause(create);

                } else if (update instanceof SetClause) {

                    throw unsupported("SET");

                } else if (update instanceof RemoveClause) {

                    throw unsupported("REMOVE");

                } else if (update instanceof DeleteClause delete) {

                    visitDeleteClause(delete);

                } else {

                    throw unsupported(
                            "Updating clause: "
                                    + update.getClass()
                                            .getSimpleName()
                    );
                }
            }

            /*
             * RETURN is optional for CREATE and
             * other updating statements.
             */
            query.returnClause().ifPresent(
                    this::visitReturnClause
            );

            if (goals.isEmpty()) {

                throw new IllegalStateException(
                        "No executable Prolog goals generated"
                );
            }

            String outputVariables = columns.stream()
                    .map(
                            PrologGenerationResult.OutputColumn
                                    ::prologVariable
                    )
                    .collect(
                            Collectors.joining(", ")
                    );

            String columnHeaders = columns.stream()
                    .map(
                            column -> quoteAtom(
                                    column.header()
                            )
                    )
                    .collect(
                            Collectors.joining(", ")
                    );

            String body = String.join(
                    ", ",
                    goals
            );

            /*
             * Prolog predicates without arguments are
             * written as atoms rather than name().
             *
             * This allows CREATE without RETURN.
             */
            String queryHead = columns.isEmpty()
                    ? "cyphail_query_p2"
                    : "cyphail_query_p2("
                            + outputVariables
                            + ")";

            String clause =
                    "("
                            + queryHead
                            + " :- ("
                            + body
                            + "))";

            /*
             * The query lifecycle remains compatible
             * with Roy's SWI-Prolog engine.
             *
             * cy_create_query registers the clause.
             * cy_exec_query executes its body.
             * cy_erase_query removes the clause.
             */
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
         * Compiles MATCH patterns.
         */
        @Override
        public String visitMatchClause(MatchClause clause) {

            Objects.requireNonNull(
                    clause,
                    "MATCH clause cannot be null"
            );

            return visitPattern(
                    clause.pattern()
            );
        }

        /**
         * Generates Prolog goals for every MATCH path.
         *
         * Supports isolated nodes, directed and
         * undirected relationships and chained paths.
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

                    goals.addAll(
                            generated.goals()
                    );

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

            return String.join(
                    ", ",
                    goals
            );
        }

        /**
         * Generates MATCH node enumeration,
         * labels and property constraints.
         *
         * The original P2 MATCH semantics
         * are preserved.
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

                        return symbols.resolveVariable(
                                name
                        );
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
                                + quoteAtom(
                                        property.name()
                                )
                                + ", "
                                + propertyValue
                                + ")"
                );

                goals.add(
                        "cy_eq("
                                + propertyValue
                                + ", "
                                + literalTerm(
                                        property.value()
                                )
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
         * Compiles WHERE conditions.
         *
         * The existing WHERE goal builder handles
         * comparisons, AND and OR.
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

            /*
             * Track properties that are guaranteed
             * to be bound after the condition.
             */
            boundProperties.addAll(
                    guaranteedWhereProperties(
                            clause.expression()
                    )
            );

            return whereGoal;
        }

        /**
         * Determines property bindings guaranteed
         * after a successful WHERE expression.
         *
         * AND: union of both branches.
         * OR: intersection of both branches.
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

            Set<String> result =
                    new HashSet<>();

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
         * Collects property references within
         * comparisons and arithmetic expressions.
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
         * Creates a consistent property mapping key.
         */
        private static String propertyKey(
                String variable,
                String property
        ) {

            return variable
                    + "."
                    + property;
        }

        /**
         * Compiles a complete CREATE clause.
         *
         * Uses the new helper responsible for
         * graph creation operations.
         *
         * The helper shares the same symbols,
         * available variables and property bindings
         * already established by MATCH and WHERE.
         */
        @Override
        public String visitCreateClause(CreateClause clause) {

            Objects.requireNonNull(
                    clause,
                    "CREATE clause cannot be null"
            );

            List<String> createGoals =
                    createBuilder.generate(
                            clause,
                            symbols,
                            matchedVariables,
                            boundProperties
                    );

            goals.addAll(
                    createGoals
            );

            return String.join(
                    ", ",
                    createGoals
            );
        }

        /**
         * Compiles RETURN and preserves the
         * existing P2 projection semantics.
         */
        @Override
        public String visitReturnClause(ReturnClause clause) {

            Objects.requireNonNull(
                    clause,
                    "RETURN clause cannot be null"
            );

            if (clause.distinct()) {

                throw unsupported(
                        "RETURN DISTINCT"
                );
            }

            return visitProjection(
                    clause.projection()
            );
        }

        /**
         * Compiles explicit RETURN items.
         *
         * Projection modifiers unsupported by the
         * current CodeGen fail explicitly.
         */
        @Override
        public String visitProjection(Projection projection) {

            Objects.requireNonNull(
                    projection,
                    "Projection cannot be null"
            );

            if (projection.wildcard()) {

                throw unsupported(
                        "RETURN *"
                );
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
                    .collect(
                            Collectors.joining(", ")
                    );
        }

        /**
         * Compiles an individual RETURN item.
         *
         * A pattern variable returns its ID.
         * A property expression returns its value.
         */
        @Override
        public String visitProjectionItem(ProjectionItem item) {

            Objects.requireNonNull(
                    item,
                    "Projection item cannot be null"
            );

            Expression expression =
                    item.expression();

            String source;
            String prologVariable;

            if (expression instanceof VariableExpression variable) {

                source = variable.name();

                ensureMatched(source);

                prologVariable =
                        visitVariableExpression(
                                variable
                        );

            } else if (
                    expression instanceof PropertyExpression property
                            && property.target()
                            instanceof VariableExpression owner
            ) {

                source = propertyKey(
                        owner.name(),
                        property.property()
                );

                ensureMatched(
                        owner.name()
                );

                prologVariable =
                        visitPropertyExpression(
                                property
                        );

            } else {

                throw unsupported(
                        "RETURN expression: "
                                + expression
                                        .getClass()
                                        .getSimpleName()
                );
            }

            String header =
                    item.alias().orElse(source);

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
         * Generates a property lookup only when
         * that lookup has not already been guaranteed
         * by an earlier MATCH or WHERE goal.
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

            ensureMatched(
                    owner.name()
            );

            String elementId =
                    symbols.resolveVariable(
                            owner.name()
                    );

            String propertyValue =
                    symbols.resolveProperty(
                            owner.name(),
                            expression.property()
                    );

            String key = propertyKey(
                    owner.name(),
                    expression.property()
            );

            if (boundProperties.add(key)) {

                goals.add(
                        "user_amigos_prop("
                                + elementId
                                + ", "
                                + quoteAtom(
                                        expression.property()
                                )
                                + ", "
                                + propertyValue
                                + ")"
                );
            }

            return propertyValue;
        }

        /**
         * Resolves a Cyphail variable to its
         * corresponding Prolog variable.
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
         * Validates that a source variable
         * is available for projection.
         *
         * The available set includes both MATCH
         * variables and CREATE variables.
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
         * Converts a supported pattern literal
         * to an executable Prolog term.
         */
        private static String literalTerm(
                Expression expression
        ) {

            Objects.requireNonNull(
                    expression,
                    "Literal expression cannot be null"
            );

            if (expression instanceof StringLiteralExpression value) {

                return quoteAtom(
                        value.value()
                );
            }

            if (expression instanceof IntegerLiteralExpression value) {

                return Long.toString(
                        value.value()
                );
            }

            if (expression instanceof FloatLiteralExpression value) {

                return Double.toString(
                        value.value()
                );
            }

            throw unsupported(
                    "Pattern property expression: "
                            + expression
                                    .getClass()
                                    .getSimpleName()
            );
        }

        /**
         * Escapes an atom used by SWI-Prolog.
         */
        private static String quoteAtom(
                String value
        ) {

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
         * Creates a descriptive exception
         * for functionality still pending.
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
         * DELETE will be implemented after CREATE.
         */
        @Override
        public String visitDeleteClause(DeleteClause clause) {

            throw unsupported(
                    "DELETE"
            );
        }

        /**
         * Pattern properties are compiled within
         * the corresponding node or relationship
         * goal builder.
         */
        @Override
        public String visitPatternProperty(
                PatternProperty property
        ) {

            throw unsupported(
                    "PatternProperty visitor"
            );
        }

        /**
         * WHERE binary expressions are compiled
         * by PrologWhereGoalBuilder.
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
         * Generates integer literal terms.
         */
        @Override
        public String visitIntegerLiteralExpression(
                IntegerLiteralExpression expression
        ) {

            return literalTerm(
                    expression
            );
        }

        /**
         * Generates string literal terms.
         */
        @Override
        public String visitStringLiteralExpression(
                StringLiteralExpression expression
        ) {

            return literalTerm(
                    expression
            );
        }

        /**
         * Generates boolean literal terms.
         */
        @Override
        public String visitBooleanLiteralExpression(
                BooleanLiteralExpression expression
        ) {

            return expression.value()
                    ? "true"
                    : "false";
        }

        /**
         * NULL requires explicit runtime semantics.
         */
        @Override
        public String visitNullLiteralExpression(
                NullLiteralExpression expression
        ) {

            throw unsupported(
                    "NULL literals"
            );
        }
    }
}
