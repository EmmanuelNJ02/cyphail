
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
 * Transpiles Cyphail query ASTs to executable SWI-Prolog goals.
 *
 * A separate visitor is created for every compilation to keep
 * variable mappings and intermediate goals isolated.
 *
 * Supported features:
 * - MATCH with isolated node patterns.
 * - MATCH with relationship paths.
 * - Outgoing relationships (OUT).
 * - Incoming relationships (IN).
 * - Undirected relationships (NONE).
 * - Chained relationship patterns.
 * - Comma-separated pattern paths.
 * - Node labels and literal property constraints.
 * - Relationship types and literal property constraints.
 * - Named and anonymous nodes and relationships.
 * - Explicit RETURN projections.
 * - RETURN aliases.
 * - RETURN of node and relationship identifiers.
 * - RETURN of node and relationship properties.
 *
 * Other Sprint P2 features are implemented incrementally.
 * Unsupported operations fail explicitly rather than
 * generating incorrect Prolog code.
 */
public final class PrologCodeGenerator {

    /**
     * Generates a Prolog goal from a Cyphail query AST.
     *
     * Each invocation creates a fresh visitor, ensuring that
     * variables and goals from separate queries do not mix.
     *
     * @param query Cyphail query AST
     * @return immutable Prolog generation result
     */
    public PrologGenerationResult generate(Query query) {

        Objects.requireNonNull(
                query,
                "Query cannot be null"
        );

        return new QueryVisitor().compile(query);
    }

    /**
     * Stateful AST visitor responsible for compiling
     * one complete Cyphail query.
     */
    private static final class QueryVisitor
            implements AstVisitor<String> {

        private final PrologVariableTable symbols =
                new PrologVariableTable();

        private final PrologRelationshipGoalBuilder
                relationshipBuilder =
                new PrologRelationshipGoalBuilder();

        private final List<String> goals =
                new ArrayList<>();

        private final List<PrologGenerationResult.OutputColumn>
                columns =
                new ArrayList<>();

        private final Set<String> matchedVariables =
                new HashSet<>();

        /**
         * Compiles one query and returns its Prolog goal,
         * columns and variable mappings.
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
         * Generates the complete query lifecycle:
         *
         * 1. Register a temporary Prolog query.
         * 2. Execute it.
         * 3. Obtain formatted rows.
         * 4. Remove the temporary query.
         *
         * The surrounding findall/3 keeps the internal
         * Prolog clause reference out of the MQI result.
         */
        @Override
        public String visitQuery(Query query) {

            if (!query.hasMatch()) {
                throw unsupported("Standalone CREATE");
            }

            if (query.where().isPresent()) {
                throw unsupported("WHERE");
            }

            if (!query.updates().isEmpty()) {
                throw unsupported("Updating clauses");
            }

            if (query.returnClause().isEmpty()) {
                throw unsupported("Queries without RETURN");
            }

            visitMatchClause(query.match());

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
         * Visits the MATCH clause and processes
         * all pattern paths.
         */
        @Override
        public String visitMatchClause(
                MatchClause clause
        ) {

            Objects.requireNonNull(
                    clause,
                    "MATCH clause cannot be null"
            );

            return visitPattern(
                    clause.pattern()
            );
        }

        /**
         * Generates node and relationship goals
         * for all paths in the MATCH pattern.
         *
         * Each path begins with a node and may
         * contain zero or more relationship steps.
         *
         * Relationship goals are delegated to
         * PrologRelationshipGoalBuilder.
         */
        @Override
        public String visitPattern(
                Pattern pattern
        ) {

            Objects.requireNonNull(
                    pattern,
                    "Pattern cannot be null"
            );

            for (PatternPath path : pattern.paths()) {

                String leftNodeId =
                        visitNodePattern(
                                path.firstNode()
                        );

                for (RelationshipStep step :
                        path.steps()) {

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
                            .ifPresent(
                                    matchedVariables::add
                            );

                    leftNodeId = rightNodeId;
                }
            }

            return String.join(", ", goals);
        }

        /**
         * Generates goals for a node pattern.
         *
         * A node can be named or anonymous and can
         * contain labels and property restrictions.
         *
         * The resulting variable identifies
         * the node in Prolog.
         */
        @Override
        public String visitNodePattern(
                NodePattern node
        ) {

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

            for (String label :
                    node.labels()) {

                goals.add(
                        "user_amigos_label("
                                + nodeId
                                + ", "
                                + quoteAtom(label)
                                + ")"
                );
            }

            for (PatternProperty property :
                    node.properties()) {

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
            }

            return nodeId;
        }

        /**
         * Processes a RETURN clause.
         *
         * Supports explicit projections and aliases.
         * DISTINCT is reserved for a later stage.
         */
        @Override
        public String visitReturnClause(
                ReturnClause clause
        ) {

            Objects.requireNonNull(
                    clause,
                    "RETURN clause cannot be null"
            );

            if (clause.distinct()) {
                throw unsupported("RETURN DISTINCT");
            }

            return visitProjection(
                    clause.projection()
            );
        }

        /**
         * Generates the ordered list of variables
         * that must appear in the result table.
         */
        @Override
        public String visitProjection(
                Projection projection
        ) {

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
                    .collect(
                            Collectors.joining(", ")
                    );
        }

        /**
         * Generates the Prolog variable and column
         * metadata for a RETURN projection item.
         *
         * Supports:
         * - RETURN a
         * - RETURN r
         * - RETURN a.name
         * - RETURN r.since
         * - RETURN a.name AS personName
         * - RETURN r AS relationshipId
         */
        @Override
        public String visitProjectionItem(
                ProjectionItem item
        ) {

            Objects.requireNonNull(
                    item,
                    "Projection item cannot be null"
            );

            Expression expression =
                    item.expression();

            String source;
            String prologVariable;

            if (expression
                    instanceof VariableExpression variable) {

                source = variable.name();

                ensureMatched(source);

                prologVariable =
                        visitVariableExpression(
                                variable
                        );

            } else if (
                    expression
                            instanceof PropertyExpression property
                            && property.target()
                            instanceof VariableExpression owner
            ) {

                source = owner.name()
                        + "."
                        + property.property();

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
         * Generates a property access goal.
         *
         * This predicate supports both node
         * properties and relationship properties
         * because the Prolog engine exposes them
         * through user_amigos_prop/3.
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

            return propertyValue;
        }

        /**
         * Resolves a Cyphail variable to its
         * corresponding Prolog variable.
         *
         * Both nodes and relationships use
         * the same mapping table.
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
         * Ensures that a RETURN expression
         * references a variable introduced
         * by the MATCH pattern.
         *
         * Named relationship variables are
         * registered while visiting paths.
         */
        private void ensureMatched(
                String variableName
        ) {

            if (!matchedVariables.contains(
                    variableName
            )) {

                throw new IllegalArgumentException(
                        "RETURN references an unmatched variable: "
                                + variableName
                );
            }
        }

        /**
         * Converts a supported literal expression
         * to an equivalent SWI-Prolog term.
         */
        private static String literalTerm(
                Expression expression
        ) {

            Objects.requireNonNull(
                    expression,
                    "Literal expression cannot be null"
            );

            if (expression
                    instanceof StringLiteralExpression value) {

                return quoteAtom(
                        value.value()
                );
            }

            if (expression
                    instanceof IntegerLiteralExpression value) {

                return Long.toString(
                        value.value()
                );
            }

            if (expression
                    instanceof FloatLiteralExpression value) {

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
         * Escapes and quotes a value as
         * a SWI-Prolog atom.
         */
        private static String quoteAtom(
                String value
        ) {

            Objects.requireNonNull(
                    value,
                    "Prolog atom cannot be null"
            );

            return "'"
                    + value.replace(
                            "\\",
                            "\\\\"
                    ).replace(
                            "'",
                            "''"
                    )
                    + "'";
        }

        /**
         * Constructs a consistent exception
         * for functionality not yet supported
         * by the current CodeGen stage.
         */
        private static UnsupportedOperationException
        unsupported(String feature) {

            return new UnsupportedOperationException(
                    "P2 CodeGen support is pending for: "
                            + feature
            );
        }

        /**
         * WHERE code generation will be
         * integrated in a subsequent stage.
         */
        @Override
        public String visitWhereClause(
                WhereClause clause
        ) {

            throw unsupported("WHERE");
        }

        /**
         * CREATE code generation will be
         * integrated in a subsequent stage.
         */
        @Override
        public String visitCreateClause(
                CreateClause clause
        ) {

            throw unsupported("CREATE");
        }

        /**
         * DELETE code generation will be
         * integrated in a subsequent stage.
         */
        @Override
        public String visitDeleteClause(
                DeleteClause clause
        ) {

            throw unsupported("DELETE");
        }

        /**
         * Pattern properties are handled by
         * the node and relationship generators.
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
         * Binary expressions will be handled
         * when WHERE support is integrated.
         */
        @Override
        public String visitBinaryExpression(
                BinaryExpression expression
        ) {

            throw unsupported(
                    "Binary expressions"
            );
        }

        /**
         * Generates an integer literal term.
         */
        @Override
        public String visitIntegerLiteralExpression(
                IntegerLiteralExpression expression
        ) {

            return literalTerm(expression);
        }

        /**
         * Generates a string literal term.
         */
        @Override
        public String visitStringLiteralExpression(
                StringLiteralExpression expression
        ) {

            return literalTerm(expression);
        }

        /**
         * Boolean literal support is pending.
         */
        @Override
        public String visitBooleanLiteralExpression(
                BooleanLiteralExpression expression
        ) {

            throw unsupported(
                    "Boolean literals"
            );
        }

        /**
         * NULL literal support is pending.
         */
        @Override
        public String visitNullLiteralExpression(
                NullLiteralExpression expression
        ) {

            throw unsupported(
                    "Null literals"
            );
        }
    }
}
