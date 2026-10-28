
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.expression.*;
import cr.ac.una.eif400.cyphail.ast.pattern.*;

import java.util.ArrayList;
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
 * Generates SWI-Prolog goals for CREATE patterns.
 *
 * This helper shares symbols and bound variables
 * with the main QueryVisitor.
 *
 * Graph mutations use the predicates provided by
 * graph_store.pl in the P2 engine.
 */
public final class PrologCreateGoalBuilder {

    /**
     * Compiles CREATE paths in source order.
     *
     * The variable and property sets are updated
     * so subsequent CREATE clauses and RETURN
     * can reuse established bindings.
     *
     * @param clause CREATE clause
     * @param symbols shared Prolog variable table
     * @param availableVariables variables already bound
     * @param boundProperties properties already retrieved
     * @return ordered Prolog creation goals
     */
    public List<String> generate(
            CreateClause clause,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> boundProperties
    ) {

        Objects.requireNonNull(
                clause,
                "CREATE clause cannot be null"
        );

        Objects.requireNonNull(
                symbols,
                "Symbol table cannot be null"
        );

        Objects.requireNonNull(
                availableVariables,
                "Variable set cannot be null"
        );

        Objects.requireNonNull(
                boundProperties,
                "Bound properties cannot be null"
        );

        List<String> goals = new ArrayList<>();

        for (PatternPath path : clause.pattern().paths()) {

            String left = createOrReuseNode(
                    path.firstNode(),
                    symbols,
                    availableVariables,
                    boundProperties,
                    goals
            );

            for (RelationshipStep step : path.steps()) {

                String right = createOrReuseNode(
                        step.rightNode(),
                        symbols,
                        availableVariables,
                        boundProperties,
                        goals
                );

                createRelationship(
                        step.relationship(),
                        left,
                        right,
                        symbols,
                        availableVariables,
                        boundProperties,
                        goals
                );

                left = right;
            }
        }

        return List.copyOf(goals);
    }

    /**
     * Creates a node once.
     *
     * If the variable already exists because of
     * MATCH or an earlier CREATE, its existing
     * identifier is reused.
     */
    private String createOrReuseNode(
            NodePattern node,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> boundProperties,
            List<String> goals
    ) {

        if (node.variable().isPresent()) {

            String name =
                    node.variable().orElseThrow();

            if (availableVariables.contains(name)) {

                if (!node.labels().isEmpty()
                        || !node.properties().isEmpty()) {

                    throw new UnsupportedOperationException(
                            "CREATE cannot redeclare labels "
                                    + "or properties of an "
                                    + "already bound node: "
                                    + name
                    );
                }

                return symbols.resolveVariable(name);
            }
        }

        String nodeId = node.variable()
                .map(symbols::resolveVariable)
                .orElseGet(
                        symbols::newTemporaryVariable
                );

        List<String> properties = compileProperties(
                node.properties(),
                symbols,
                availableVariables,
                boundProperties,
                goals
        );

        String labels = node.labels().stream()
                .map(
                        PrologCreateGoalBuilder::quoteAtom
                )
                .collect(
                        Collectors.joining(
                                ", ",
                                "[",
                                "]"
                        )
                );

        goals.add(
                "cy_create_node(amigos, "
                        + labels
                        + ", "
                        + listTerm(properties)
                        + ", "
                        + nodeId
                        + ")"
        );

        node.variable().ifPresent(
                availableVariables::add
        );

        return nodeId;
    }

    /**
     * Creates a relationship after both
     * endpoint nodes are available.
     *
     * Direction IN reverses the endpoints.
     * Direction OUT uses left to right.
     *
     * Direction NONE is stored left to right
     * because the underlying graph store
     * represents directed relationships.
     */
    private void createRelationship(
            RelationshipPattern relationship,
            String left,
            String right,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> boundProperties,
            List<String> goals
    ) {

        if (relationship.types().size() != 1) {

            throw new UnsupportedOperationException(
                    "CREATE relationships require "
                            + "exactly one type"
            );
        }

        relationship.variable().ifPresent(name -> {

            if (availableVariables.contains(name)) {

                throw new IllegalArgumentException(
                        "Relationship variable "
                                + "already bound: "
                                + name
                );
            }
        });

        List<String> properties = compileProperties(
                relationship.properties(),
                symbols,
                availableVariables,
                boundProperties,
                goals
        );

        String edgeId = relationship.variable()
                .map(symbols::resolveVariable)
                .orElseGet(
                        symbols::newTemporaryVariable
                );

        String from =
                relationship.direction()
                        == RelationshipDirection.IN
                        ? right
                        : left;

        String to =
                relationship.direction()
                        == RelationshipDirection.IN
                        ? left
                        : right;

        goals.add(
                "cy_create_edge(amigos, "
                        + from
                        + ", "
                        + to
                        + ", "
                        + quoteAtom(
                                relationship.types().getFirst()
                        )
                        + ", "
                        + listTerm(properties)
                        + ", "
                        + edgeId
                        + ")"
        );

        relationship.variable().ifPresent(
                availableVariables::add
        );
    }

    /**
     * Compiles property assignments.
     *
     * Examples:
     *
     * name: 'Lucia'
     * age: 21
     * issuedTo: a.name
     *
     * Each result has the Prolog representation:
     *
     * 'property'-Value
     */
    private List<String> compileProperties(
            List<PatternProperty> properties,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> boundProperties,
            List<String> goals
    ) {

        List<String> result = new ArrayList<>();

        for (PatternProperty property : properties) {

            String value = compileValue(
                    property.value(),
                    symbols,
                    availableVariables,
                    boundProperties,
                    goals
            );

            result.add(
                    quoteAtom(property.name())
                            + "-"
                            + value
            );
        }

        return result;
    }

    /**
     * Resolves literal values and references
     * to previously bound variables.
     *
     * Property lookups are reused when they
     * have already been performed by MATCH,
     * WHERE or another CREATE expression.
     */
    private String compileValue(
            Expression expression,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> boundProperties,
            List<String> goals
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

        if (expression
                instanceof BooleanLiteralExpression value) {

            return Boolean.toString(value.value());
        }

        if (expression
                instanceof VariableExpression variable) {

            requireAvailable(
                    variable.name(),
                    availableVariables
            );

            return symbols.resolveVariable(
                    variable.name()
            );
        }

        if (expression instanceof PropertyExpression property
                && property.target()
                        instanceof VariableExpression owner) {

            requireAvailable(
                    owner.name(),
                    availableVariables
            );

            String key =
                    owner.name()
                            + "."
                            + property.property();

            String propertyValue =
                    symbols.resolveProperty(
                            owner.name(),
                            property.property()
                    );

            if (boundProperties.add(key)) {

                goals.add(
                        "user_amigos_prop("
                                + symbols.resolveVariable(
                                        owner.name()
                                )
                                + ", "
                                + quoteAtom(
                                        property.property()
                                )
                                + ", "
                                + propertyValue
                                + ")"
                );
            }

            return propertyValue;
        }

        throw new UnsupportedOperationException(
                "CREATE property expression "
                        + "is not supported: "
                        + expression.getClass()
                                .getSimpleName()
        );
    }

    /**
     * Validates references to source variables.
     */
    private static void requireAvailable(
            String name,
            Set<String> available
    ) {

        if (!available.contains(name)) {

            throw new IllegalArgumentException(
                    "CREATE references an "
                            + "unbound variable: "
                            + name
            );
        }
    }

    /**
     * Converts a collection into a Prolog list.
     *
     * Empty collection -> []
     * Nonempty collection -> [A, B, C]
     */
    private static String listTerm(
            List<String> values
    ) {

        if (values.isEmpty()) {
            return "[]";
        }

        return "["
                + String.join(", ", values)
                + "]";
    }

    /**
     * Escapes a value as a SWI-Prolog atom.
     */
    private static String quoteAtom(
            String text
    ) {

        Objects.requireNonNull(
                text,
                "Prolog atom cannot be null"
        );

        return "'"
                + text.replace("\\", "\\\\")
                        .replace("'", "''")
                + "'";
    }
}
