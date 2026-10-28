
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.clause.RemoveClause;
import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 *
 * Generates SWI-Prolog goals for REMOVE clauses.
 *
 * Supported operations:
 *
 * REMOVE p.discount
 * REMOVE p.discount, p.oldPrice
 * REMOVE r.grade
 *
 * REMOVE deletes properties, not graph elements.
 *
 * The generated goals use the graph mutation
 * predicates already available in graph_store.pl.
 */
public final class PrologRemoveGoalBuilder {

    /**
     * Generates REMOVE goals in source order.
     *
     * Node properties use:
     *
     * cy_remove_node_property/3
     *
     * Relationship properties use:
     *
     * cy_remove_edge_property/3
     *
     * After every removal, the previous property
     * binding is invalidated to avoid reusing
     * an outdated Prolog variable.
     *
     * @param clause REMOVE clause
     * @param symbols shared variable table
     * @param availableVariables variables from MATCH/CREATE
     * @param relationshipVariables relationship variable names
     * @param boundProperties properties already resolved
     * @param onPropertyRemoved property invalidation callback
     * @return immutable list of generated Prolog goals
     */
    public List<String> generate(
            RemoveClause clause,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> relationshipVariables,
            Set<String> boundProperties,
            BiConsumer<String, String> onPropertyRemoved
    ) {

        Objects.requireNonNull(
                clause,
                "REMOVE clause cannot be null"
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

        Objects.requireNonNull(
                boundProperties,
                "Bound properties cannot be null"
        );

        Objects.requireNonNull(
                onPropertyRemoved,
                "Property removal callback cannot be null"
        );

        List<String> goals = new ArrayList<>();

        for (PropertyExpression property : clause.properties()) {

            Objects.requireNonNull(
                    property,
                    "REMOVE property cannot be null"
            );

            if (!(property.target()
                    instanceof VariableExpression owner)) {

                throw new UnsupportedOperationException(
                        "REMOVE requires a direct property target"
                );
            }

            String ownerName = owner.name();

            if (!availableVariables.contains(ownerName)) {

                throw new IllegalArgumentException(
                        "REMOVE references an undefined variable: "
                                + ownerName
                );
            }

            String propertyName = property.property();

            String elementId =
                    symbols.resolveVariable(ownerName);

            /*
             * Select the appropriate mutation predicate
             * according to the variable type.
             */
            String predicate =
                    relationshipVariables.contains(ownerName)
                            ? "cy_remove_edge_property"
                            : "cy_remove_node_property";

            String goal =
                    predicate
                            + "(amigos, "
                            + elementId
                            + ", "
                            + quoteAtom(propertyName)
                            + ")";

            goals.add(goal);

            /*
             * Remove the old property binding.
             *
             * Any preceding MATCH/WHERE/property access
             * may have resolved this property's old value.
             *
             * Future accesses must not accidentally reuse
             * that previous Prolog variable.
             */
            boundProperties.remove(
                    propertyKey(ownerName, propertyName)
            );

            /*
             * Ask the shared symbol table to assign a
             * fresh Prolog variable for future reads.
             */
            onPropertyRemoved.accept(
                    ownerName,
                    propertyName
            );
        }

        return List.copyOf(goals);
    }

    /**
     * Creates a consistent property-binding key.
     *
     * Example:
     *
     * p.discount
     */
    private static String propertyKey(
            String owner,
            String property
    ) {

        return owner + "." + property;
    }

    /**
     * Quotes a Prolog atom safely for
     * generated source code.
     *
     * Apostrophes and backslashes are escaped
     * following the same strategy used by SET.
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
}
