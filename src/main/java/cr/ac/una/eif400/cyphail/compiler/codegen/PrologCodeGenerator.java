
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
 * A separate visitor is created for every compilation to keep
 * variable mappings and intermediate goals isolated.
 *
 * This initial stage supports MATCH with isolated node patterns,
 * literal property constraints, and explicit RETURN projections.
 * Unsupported P2 features fail explicitly and will be added
 * incrementally rather than producing incorrect Prolog code.
 */
public final class PrologCodeGenerator {

    public PrologGenerationResult generate(Query query) {
        Objects.requireNonNull(query, "Query cannot be null");
        return new QueryVisitor().compile(query);
    }

    private static final class QueryVisitor
            implements AstVisitor<String> {

        private final PrologVariableTable symbols =
                new PrologVariableTable();

        private final List<String> goals =
                new ArrayList<>();

        private final List<PrologGenerationResult.OutputColumn> columns =
                new ArrayList<>();

        private final Set<String> matchedVariables =
                new HashSet<>();

        PrologGenerationResult compile(Query query) {
            String goal = visitQuery(query);

            return new PrologGenerationResult(
                    goal,
                    columns,
                    symbols.variableMappings(),
                    symbols.propertyMappings()
            );
        }

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
                    .map(column -> quoteAtom(column.header()))
                    .collect(Collectors.joining(", "));

            String body = String.join(", ", goals);

            String clause =
                    "(cyphail_query_p2("
                            + outputVariables
                            + ") :- ("
                            + body
                            + "))";

            // Keeps Prolog clause references out of MQI JSON.
            return "findall(Rows, (cy_create_query(__QueryRef, "
                    + clause
                    + "), cy_exec_query(__QueryRef, ["
                    + columnHeaders
                    + "], Rows), "
                    + "cy_erase_query(__QueryRef)), [Rows])";
        }

        @Override
        public String visitMatchClause(MatchClause clause) {
            return visitPattern(clause.pattern());
        }

        @Override
        public String visitPattern(Pattern pattern) {

            for (PatternPath path : pattern.paths()) {

                if (path.hasRelationships()) {
                    throw unsupported(
                            "Relationship patterns"
                    );
                }

                visitNodePattern(path.firstNode());
            }

            return String.join(", ", goals);
        }

        @Override
        public String visitNodePattern(NodePattern node) {

            String nodeId = node.variable()
                    .map(name -> {
                        matchedVariables.add(name);
                        return symbols.resolveVariable(name);
                    })
                    .orElseGet(
                            symbols::newTemporaryVariable
                    );

            goals.add(
                    "user_amigos_node(" + nodeId + ")"
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
            }

            return nodeId;
        }

        @Override
        public String visitReturnClause(
                ReturnClause clause
        ) {

            if (clause.distinct()) {
                throw unsupported("RETURN DISTINCT");
            }

            return visitProjection(
                    clause.projection()
            );
        }

        @Override
        public String visitProjection(
                Projection projection
        ) {

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

        @Override
        public String visitProjectionItem(
                ProjectionItem item
        ) {

            Expression expression =
                    item.expression();

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

                source = owner.name()
                        + "."
                        + property.property();

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

        @Override
        public String visitPropertyExpression(
                PropertyExpression expression
        ) {

            if (!(expression.target()
                    instanceof VariableExpression owner)) {

                throw unsupported(
                        "Nested property expressions"
                );
            }

            ensureMatched(owner.name());

            String nodeId =
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
                            + nodeId
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

        @Override
        public String visitVariableExpression(
                VariableExpression expression
        ) {

            return symbols.resolveVariable(
                    expression.name()
            );
        }

        private void ensureMatched(
                String variableName
        ) {

            if (!matchedVariables.contains(variableName)) {

                throw new IllegalArgumentException(
                        "RETURN references an unmatched variable: "
                                + variableName
                );
            }
        }

        private static String literalTerm(
                Expression expression
        ) {

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

        private static String quoteAtom(
                String value
        ) {

            return "'"
                    + value.replace("\\", "\\\\")
                            .replace("'", "''")
                    + "'";
        }

        private static UnsupportedOperationException unsupported(
                String feature
        ) {

            return new UnsupportedOperationException(
                    "P2 CodeGen support is pending for: "
                            + feature
            );
        }

        @Override
        public String visitWhereClause(WhereClause clause) {
            throw unsupported("WHERE");
        }

        @Override
        public String visitCreateClause(CreateClause clause) {
            throw unsupported("CREATE");
        }

        @Override
        public String visitDeleteClause(DeleteClause clause) {
            throw unsupported("DELETE");
        }

        @Override
        public String visitPatternProperty(PatternProperty property) {
            throw unsupported("PatternProperty visitor");
        }

        @Override
        public String visitBinaryExpression(BinaryExpression expression) {
            throw unsupported("Binary expressions");
        }

        @Override
        public String visitIntegerLiteralExpression(
                IntegerLiteralExpression expression
        ) {
            return literalTerm(expression);
        }

        @Override
        public String visitStringLiteralExpression(
                StringLiteralExpression expression
        ) {
            return literalTerm(expression);
        }

        @Override
        public String visitBooleanLiteralExpression(
                BooleanLiteralExpression expression
        ) {
            throw unsupported("Boolean literals");
        }

        @Override
        public String visitNullLiteralExpression(
                NullLiteralExpression expression
        ) {
            throw unsupported("Null literals");
        }
    }
}
