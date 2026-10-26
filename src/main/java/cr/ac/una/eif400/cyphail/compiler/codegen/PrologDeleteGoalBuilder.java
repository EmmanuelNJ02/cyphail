
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 * Group Code: 04-10am
 *
 * Generates SWI-Prolog goals for DELETE clauses.
 *
 * Supported goal generation:
 *
 * - DELETE of relationship variables.
 * - DELETE of node variables.
 * - DELETE of multiple variables.
 * - DETACH DELETE of node variables.
 * - DELETE after MATCH or CREATE.
 *
 * Relationship deletions are generated before
 * node deletions when both appear in the same
 * DELETE clause.
 *
 * This preserves the graph integrity constraint
 * that a normal node deletion cannot remove a
 * node while attached relationships still exist.
 *
 * The SWI-Prolog engine must implement:
 *
 * cy_delete_edge/2
 * cy_delete_node/2
 * cy_detach_delete_node/2
 *
 * Support for original sample graph elements
 * is handled by the Prolog engine.
 */
public final class PrologDeleteGoalBuilder {

    /**
     * Generates ordered Prolog goals for DELETE.
     *
     * @param clause DELETE clause from the AST
     * @param symbols shared Prolog symbol table
     * @param availableVariables variables introduced
     *                           by MATCH or CREATE
     * @param relationshipVariables names identifying
     *                              relationship variables
     * @return immutable list of generated Prolog goals
     */
    public List<String> generate(
            DeleteClause clause,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> relationshipVariables
    ) {

        Objects.requireNonNull(
                clause,
                "DELETE clause cannot be null"
        );

        Objects.requireNonNull(
                symbols,
                "Symbol table cannot be null"
        );

        Objects.requireNonNull(
                availableVariables,
                "Available variables cannot be null"
        );

        Objects.requireNonNull(
                relationshipVariables,
                "Relationship variables cannot be null"
        );

        /*
         * LinkedHashSet preserves insertion order
         * and avoids deleting the same variable
         * more than once in one DELETE clause.
         */
        Set<String> edgeGoals =
                new LinkedHashSet<>();

        Set<String> nodeGoals =
                new LinkedHashSet<>();

        for (Expression expression :
                clause.expressions()) {

            /*
             * DELETE targets must be graph
             * element variables, not properties
             * or literal expressions.
             */
            if (!(expression
                    instanceof VariableExpression variable)) {

                throw new UnsupportedOperationException(
                        "DELETE requires a node or "
                                + "relationship variable"
                );
            }

            String variableName =
                    variable.name();

            if (!availableVariables.contains(
                    variableName
            )) {

                throw new IllegalArgumentException(
                        "DELETE references an undefined variable: "
                                + variableName
                );
            }

            String elementId =
                    symbols.resolveVariable(
                            variableName
                    );

            /*
             * Relationship variables use the
             * relationship deletion predicate.
             *
             * DETACH does not change the
             * deletion operation for a
             * relationship itself.
             */
            if (relationshipVariables.contains(
                    variableName
            )) {

                edgeGoals.add(
                        "cy_delete_edge(amigos, "
                                + elementId
                                + ")"
                );

                continue;
            }

            /*
             * Node deletion depends on whether
             * DETACH was requested.
             *
             * Normal DELETE requires the node
             * to have no attached relationships.
             *
             * DETACH DELETE must remove attached
             * relationships before the node.
             */
            String predicate =
                    clause.detach()
                            ? "cy_detach_delete_node"
                            : "cy_delete_node";

            nodeGoals.add(
                    predicate
                            + "(amigos, "
                            + elementId
                            + ")"
            );
        }

        /*
         * Relationships are deleted first when
         * a clause targets both relationships
         * and nodes.
         *
         * Examples:
         *
         * DELETE a, r
         *
         * Generated execution order:
         *
         * cy_delete_edge(amigos, __R)
         * cy_delete_node(amigos, __A)
         *
         * Relative order is preserved inside
         * each category.
         */
        List<String> goals =
                new ArrayList<>(
                        edgeGoals.size()
                                + nodeGoals.size()
                );

        goals.addAll(edgeGoals);
        goals.addAll(nodeGoals);

        return List.copyOf(goals);
    }
}
