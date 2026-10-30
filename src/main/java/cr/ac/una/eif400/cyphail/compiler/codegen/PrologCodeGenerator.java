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
 * - SET node properties.
 * - SET relationship properties.
 * - Multiple SET assignments.
 * - CREATE followed by SET.
 * - RETURN after SET.
 * - Updated property bindings.
 * - REMOVE node properties.
 * - REMOVE relationship properties.
 * - Multiple REMOVE properties.
 * - Property binding invalidation after REMOVE.
 * - DELETE node variables.
 * - DELETE relationship variables.
 * - DELETE multiple graph elements.
 * - DETACH DELETE node variables.
 * - DELETE after MATCH or CREATE.
 *
 * DELETE goal execution requires the corresponding
 * predicates to be implemented in SWI-Prolog.
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

        // =====================================================
        // BUILDERS AND SYMBOL TABLE
        // =====================================================

        private final PrologVariableTable symbols =
                new PrologVariableTable();

        private final PrologRelationshipGoalBuilder
                relationshipBuilder =
                new PrologRelationshipGoalBuilder();

        private final PrologWhereGoalBuilder whereBuilder =
                new PrologWhereGoalBuilder();

        private final PrologCreateGoalBuilder createBuilder =
                new PrologCreateGoalBuilder();

        private final PrologSetGoalBuilder setBuilder =
                new PrologSetGoalBuilder();

        /**
         * Sprint P2 REMOVE:
         *
         * Uses the dedicated REMOVE goal builder
         * to translate property removals into
         * SWI-Prolog graph mutation predicates.
         */
        private final PrologRemoveGoalBuilder removeBuilder =
                new PrologRemoveGoalBuilder();

        /**
         * Sprint P2 DELETE:
         *
         * Generates Prolog goals for deleting
         * nodes and relationships.
         *
         * Also supports DETACH DELETE.
         */
        private final PrologDeleteGoalBuilder deleteBuilder =
                new PrologDeleteGoalBuilder();

        // =====================================================
        // QUERY STATE
        // =====================================================

        private final List<String> goals =
                new ArrayList<>();

        private final List<PrologGenerationResult.OutputColumn>
                columns =
                new ArrayList<>();

        /**
         * Available Cyphail variables.
         *
         * Includes variables introduced by MATCH
         * and variables introduced by CREATE.
         */
        private final Set<String> matchedVariables =
                new HashSet<>();

        /**
         * Stores the names of variables representing
         * relationships.
         *
         * This is required by SET, REMOVE and DELETE
         * to distinguish between node and
         * relationship mutations.
         */
        private final Set<String> relationshipVariables =
                new HashSet<>();

        /**
         * Properties whose values have already
         * been bound by earlier Prolog goals.
         *
         * SET and REMOVE invalidate the binding
         * for each modified property.
         */
        private final Set<String> boundProperties =
                new HashSet<>();

        // =====================================================
        // MAIN COMPILATION
        // =====================================================

        /**
         * Compiles a query and constructs its
         * output metadata.
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
         * 3. CREATE, SET, REMOVE, DELETE and updates.
         * 4. RETURN, when present.
         *
         * A standalone CREATE does not require MATCH.
         *
         * Updating statements can execute without
         * a RETURN clause.
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

            // -----------------------------------------
            // MATCH
            // -----------------------------------------

            if (query.hasMatch()) {

                visitMatchClause(
                        query.match()
                );
            }

            // -----------------------------------------
            // WHERE
            // -----------------------------------------

            query.where().ifPresent(
                    this::visitWhereClause
            );

            // -----------------------------------------
            // UPDATING CLAUSES
            // -----------------------------------------

            for (UpdatingClause update : query.updates()) {

                if (update instanceof CreateClause create) {

                    visitCreateClause(create);

                } else if (update instanceof SetClause set) {

                    visitSetClause(set);

                } else if (update instanceof RemoveClause remove) {

                    visitRemoveClause(remove);

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

            // -----------------------------------------
            // RETURN
            // -----------------------------------------

            query.returnClause().ifPresent(
                    this::visitReturnClause
            );

            if (goals.isEmpty()) {

                throw new IllegalStateException(
                        "No executable Prolog goals generated"
                );
            }

            // -----------------------------------------
            // OUTPUT VARIABLES
            // -----------------------------------------

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

            /**
             * Prolog predicates without arguments
             * must be represented as atoms.
             *
             * This is necessary for CREATE,
             * SET, REMOVE and DELETE statements
             * without RETURN.
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

            /**
             * Query lifecycle:
             *
             * 1. Register the generated temporary clause.
             * 2. Execute the clause and obtain its tabular result.
             * 3. Always erase the temporary clause after execution,
             *    including on failure or an exception.
             *
             * setup_call_cleanup/3 is provided by SWI-Prolog.
             * The outer findall/3 preserves the original MQI
             * result format expected by the Java response handler.
             */
            return "findall(Rows, ("
                    + "setup_call_cleanup("
                    + "cy_create_query(__QueryRef, "
                    + clause
                    + "), "
                    + "cy_exec_query(__QueryRef, ["
                    + columnHeaders
                    + "], Rows), "
                    + "cy_erase_query(__QueryRef)"
                    + ")"
                    + "), [Rows])";
        }

        // =====================================================
        // MATCH
        // =====================================================

        /**
         * Compiles a MATCH clause.
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
         * Supports:
         * - Isolated nodes.
         * - Directed relationships.
         * - Undirected relationships.
         * - Chained relationships.
         * - Multiple pattern paths.
         */
        @Override
        public String visitPattern(Pattern pattern) {

            Objects.requireNonNull(
                    pattern,
                    "Pattern cannot be null"
            );

            // Track physical edge IDs throughout this MATCH clause.
            // Distinct relationship occurrences must not share an edge.
            List<String> usedRelationshipIds = new ArrayList<>();

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

                    /*
                     * Sprint P2: forbid reusing one physical edge
                     * for different relationship occurrences.
                     * Covers named, anonymous and chained patterns.
                     */
                    String currentRelationshipId = generated.variable();

                    for (String previousRelationshipId : usedRelationshipIds) {
                        goals.add(
                                "cy_neq("
                                        + previousRelationshipId
                                        + ", "
                                        + currentRelationshipId
                                        + ")"
                        );
                    }

                    usedRelationshipIds.add(currentRelationshipId);

                    /**
                     * Register variables introduced
                     * by MATCH relationships.
                     *
                     * These variables are used later
                     * by SET, REMOVE and DELETE.
                     */
                    step.relationship()
                            .variable()
                            .ifPresent(name -> {

                                matchedVariables.add(name);

                                relationshipVariables.add(name);

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

        // =====================================================
        // MATCH NODE PATTERNS
        // =====================================================

        /**
         * Generates MATCH node enumeration,
         * label constraints and property constraints.
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

            // -----------------------------------------
            // NODE ENUMERATION
            // -----------------------------------------

            goals.add(
                    "user_amigos_node("
                            + nodeId
                            + ")"
            );

            // -----------------------------------------
            // LABEL CONSTRAINTS
            // -----------------------------------------

            for (String label : node.labels()) {

                goals.add(
                        "user_amigos_label("
                                + nodeId
                                + ", "
                                + quoteAtom(label)
                                + ")"
                );
            }

            // -----------------------------------------
            // PROPERTY CONSTRAINTS
            // -----------------------------------------

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

                /*
                 * Pattern properties can reference a property
                 * of an earlier MATCH variable, such as p.id.
                 * Keep literal handling unchanged.
                 */
                String expectedValue;

                if (property.value()
                        instanceof PropertyExpression reference) {

                    expectedValue =
                            visitPropertyExpression(reference);

                } else {

                    expectedValue = literalTerm(
                            property.value()
                    );
                }

                goals.add(
                        "cy_eq("
                                + propertyValue
                                + ", "
                                + expectedValue
                                + ")"
                );

                /**
                 * Record the property binding
                 * to avoid unnecessary duplicate
                 * lookups during RETURN.
                 */
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

        // =====================================================
        // WHERE
        // =====================================================

        /**
         * Compiles WHERE expressions.
         *
         * The WHERE builder handles:
         *
         * - Property comparisons.
         * - AND expressions.
         * - OR expressions.
         * - Parenthesized expressions.
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

            /**
             * Track the property bindings that
             * are guaranteed after WHERE.
             */
            boundProperties.addAll(
                    guaranteedWhereProperties(
                            clause.expression()
                    )
            );

            return whereGoal;
        }

        /**
         * Determines which property bindings
         * are guaranteed by a WHERE expression.
         *
         * AND:
         * Union of both branches.
         *
         * OR:
         * Intersection of both branches.
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
         * Collects property references inside
         * expressions.
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
         * Creates a consistent property key.
         */
        private static String propertyKey(
                String variable,
                String property
        ) {

            return variable
                    + "."
                    + property;
        }

        // =====================================================
        // CREATE
        // =====================================================

        /**
         * Compiles CREATE patterns.
         *
         * The existing CREATE builder is preserved
         * to avoid changing the implementation
         * already tested with SWI-Prolog and MQI.
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

            /**
             * Register relationship variables
             * introduced by CREATE.
             *
             * This permits queries such as:
             *
             * CREATE (a:Student)
             *        -[r:attends]->
             *        (b:Course)
             * SET r.grade = 9.5
             * RETURN r.grade
             */
            for (PatternPath path : clause.pattern().paths()) {

                for (RelationshipStep step : path.steps()) {

                    step.relationship()
                            .variable()
                            .ifPresent(
                                    relationshipVariables::add
                            );
                }
            }

            return String.join(
                    ", ",
                    createGoals
            );
        }

        // =====================================================
        // SET
        // =====================================================

        /**
         * Compiles SET assignments.
         *
         * Supports:
         *
         * SET p.precio = 19.99
         *
         * SET p.precio = 19.99,
         *     p.estado = 'activo'
         *
         * SET r.grade = 9.5
         *
         * CREATE (...) SET p.precio = 19.99
         *
         * Uses the existing graph mutation
         * predicates implemented in Prolog.
         */
        private String visitSetClause(SetClause clause) {

            Objects.requireNonNull(
                    clause,
                    "SET clause cannot be null"
            );

            /**
             * The builder uses the same variable
             * table as MATCH, WHERE and CREATE.
             *
             * It also receives the relationship
             * variable set, allowing it to choose
             * the correct mutation predicate.
             */
            List<String> setGoals =
                    setBuilder.generate(
                            clause,
                            symbols,
                            matchedVariables,
                            relationshipVariables,
                            boundProperties,
                            (variable, property) ->
                                    symbols.invalidateProperty(
                                            variable,
                                            property
                                    )
                    );

            /**
             * Preserve the original order
             * of SET assignments.
             */
            goals.addAll(
                    setGoals
            );

            return String.join(
                    ", ",
                    setGoals
            );
        }

        // =====================================================
        // REMOVE
        // =====================================================

        /**
         * Compiles REMOVE property operations.
         *
         * Supported examples:
         *
         * MATCH (p:Product)
         * REMOVE p.discount
         * RETURN p.id
         *
         * MATCH (p:Product)
         * REMOVE p.discount, p.oldPrice
         * RETURN p.id
         *
         * MATCH (a)-[r:KNOWS]->(b)
         * REMOVE r.since
         *
         * The builder chooses the correct mutation
         * predicate depending on whether the
         * variable represents a node or relationship.
         *
         * Property mappings are invalidated after
         * each removal, avoiding reuse of stale
         * Prolog property bindings.
         */
        private String visitRemoveClause(RemoveClause clause) {

            Objects.requireNonNull(
                    clause,
                    "REMOVE clause cannot be null"
            );

            List<String> removeGoals =
                    removeBuilder.generate(
                            clause,
                            symbols,
                            matchedVariables,
                            relationshipVariables,
                            boundProperties,
                            (variable, property) ->
                                    symbols.invalidateProperty(
                                            variable,
                                            property
                                    )
                    );

            /**
             * Preserve the original order of
             * REMOVE property operations.
             */
            goals.addAll(
                    removeGoals
            );

            return String.join(
                    ", ",
                    removeGoals
            );
        }

        // =====================================================
        // DELETE
        // =====================================================

        /**
         * Compiles DELETE and DETACH DELETE.
         *
         * Examples:
         *
         * MATCH (a)-[r:KNOWS]->(b)
         * DELETE r
         *
         * MATCH (p:Person)
         * DELETE p
         *
         * MATCH (p:Person)
         * DETACH DELETE p
         *
         * MATCH (a)-[r:KNOWS]->(b)
         * DELETE r, a
         *
         * CREATE (a:Person)-[r:KNOWS]->(b:Person)
         * DELETE r
         * RETURN a
         *
         * The builder distinguishes node and
         * relationship variables.
         *
         * The Prolog engine is responsible for
         * enforcing graph integrity constraints.
         */
        @Override
        public String visitDeleteClause(DeleteClause clause) {

            Objects.requireNonNull(
                    clause,
                    "DELETE clause cannot be null"
            );

            List<String> deleteGoals =
                    deleteBuilder.generate(
                            clause,
                            symbols,
                            matchedVariables,
                            relationshipVariables
                    );

            /*
             * Append DELETE goals in the correct
             * compilation order.
             *
             * Previous MATCH, WHERE, CREATE, SET
             * and REMOVE goals remain unchanged.
             */
            goals.addAll(
                    deleteGoals
            );

            return String.join(
                    ", ",
                    deleteGoals
            );
        }

        // =====================================================
        // RETURN
        // =====================================================

        /**
         * Compiles RETURN.
         *
         * Preserves the original P2
         * projection behavior.
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
         * Pattern variables return their
         * unique graph identifiers.
         *
         * Property expressions return
         * their corresponding property values.
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

            // -----------------------------------------
            // RETURN VARIABLE
            // -----------------------------------------

            if (expression instanceof VariableExpression variable) {

                source = variable.name();

                ensureMatched(source);

                prologVariable =
                        visitVariableExpression(
                                variable
                        );

            // -----------------------------------------
            // RETURN PROPERTY
            // -----------------------------------------

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

                /**
                 * If SET or REMOVE modified this property,
                 * the refreshed mapping is used.
                 */
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

        // =====================================================
        // PROPERTY EXPRESSIONS
        // =====================================================

        /**
         * Generates a property lookup only when
         * the current property value has not
         * already been bound.
         *
         * After SET or REMOVE:
         *
         * 1. The previous property binding is removed.
         * 2. PrologVariableTable provides a fresh
         *    variable for the property.
         * 3. Future accesses use the new binding.
         *
         * Reading a property that no longer exists
         * still depends on the graph engine's
         * missing-property semantics.
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

            /**
             * boundProperties.add(key) returns
             * true when this property needs
             * a new lookup.
             *
             * For a property modified by SET
             * or REMOVE, its old binding has
             * already been invalidated.
             */
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

        // =====================================================
        // VARIABLE EXPRESSIONS
        // =====================================================

        /**
         * Resolves a Cyphail variable to
         * its corresponding Prolog variable.
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
         * Validates that a variable has been
         * introduced by MATCH or CREATE.
         */
        private void ensureMatched(String variableName) {

            if (!matchedVariables.contains(variableName)) {

                throw new IllegalArgumentException(
                        "RETURN references an unmatched variable: "
                                + variableName
                );
            }
        }

        // =====================================================
        // LITERAL VALUES
        // =====================================================

        /**
         * Converts a supported pattern literal
         * into an executable Prolog term.
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
         * Escapes a Java string to a
         * quoted SWI-Prolog atom.
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

        // =====================================================
        // UNSUPPORTED FEATURES
        // =====================================================

        /**
         * Creates descriptive exceptions for
         * functionality still pending.
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
         * Pattern properties are compiled
         * by their corresponding goal builders.
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

        // =====================================================
        // INTEGER LITERALS
        // =====================================================

        @Override
        public String visitIntegerLiteralExpression(
                IntegerLiteralExpression expression
        ) {

            return literalTerm(
                    expression
            );
        }

        // =====================================================
        // STRING LITERALS
        // =====================================================

        @Override
        public String visitStringLiteralExpression(
                StringLiteralExpression expression
        ) {

            return literalTerm(
                    expression
            );
        }

        // =====================================================
        // BOOLEAN LITERALS
        // =====================================================

        @Override
        public String visitBooleanLiteralExpression(
                BooleanLiteralExpression expression
        ) {

            return expression.value()
                    ? "true"
                    : "false";
        }

        // =====================================================
        // NULL LITERALS
        // =====================================================

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
