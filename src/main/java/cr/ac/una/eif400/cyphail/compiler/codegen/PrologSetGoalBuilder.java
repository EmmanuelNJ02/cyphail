
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.clause.SetClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetItem;
import cr.ac.una.eif400.cyphail.ast.clause.SetProperty;
import cr.ac.una.eif400.cyphail.ast.expression.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

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
 * Roy Arias Mejia
 * Keynell Molina Mora
 *
 * Generates SWI-Prolog goals for SET clauses.
 *
 * Supports:
 * - Node property assignments.
 * - Relationship property assignments.
 * - Integer, floating-point, string and boolean values.
 * - Property references as assignment values.
 * - Variable references as assignment values.
 * - Multiple assignments in source order.
 * - Notifications for property-binding invalidation.
 *
 * Uses the graph update predicates provided by
 * graph_store.pl.
 */
public final class PrologSetGoalBuilder {

    /**
     * Generates SET goals in their original source order.
     *
     * The caller provides the variables introduced by
     * MATCH and CREATE and identifies relationship
     * variables separately from node variables.
     *
     * After each assignment, onPropertyUpdated is called
     * so the shared symbol table can assign a fresh
     * Prolog variable to future reads of that property.
     *
     * @param clause SET clause to compile
     * @param symbols shared Prolog variable table
     * @param availableVariables already bound variables
     * @param relationshipVariables relationship variable names
     * @param boundProperties previously resolved properties
     * @param onPropertyUpdated callback for property invalidation
     * @return immutable ordered list of Prolog goals
     */
    public List<String> generate(
            SetClause clause,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> relationshipVariables,
            Set<String> boundProperties,
            BiConsumer<String, String> onPropertyUpdated
    ) {

        Objects.requireNonNull(
                clause,
                "SET clause cannot be null"
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
                onPropertyUpdated,
                "Property update callback cannot be null"
        );

        List<String> goals = new ArrayList<>();

        for (SetItem item : clause.items()) {

            if (!(item instanceof SetProperty assignment)) {

                throw new UnsupportedOperationException(
                        "SET item is not supported by P2 CodeGen: "
                                + item.getClass().getSimpleName()
                );
            }

            compileAssignment(
                    assignment,
                    symbols,
                    availableVariables,
                    relationshipVariables,
                    boundProperties,
                    onPropertyUpdated,
                    goals
            );
        }

        return List.copyOf(goals);
    }

    /**
     * Compiles one SET assignment.
     *
     * For example:
     *
     * SET p.precio = 19.99
     *
     * becomes:
     *
     * cy_set_node_property(
     *     amigos, __P, 'precio', 19.99
     * )
     */
    private void compileAssignment(
            SetProperty assignment,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> relationshipVariables,
            Set<String> boundProperties,
            BiConsumer<String, String> onPropertyUpdated,
            List<String> goals
    ) {

        PropertyExpression target =
                assignment.property();

        if (!(target.target()
                instanceof VariableExpression owner)) {

            throw new UnsupportedOperationException(
                    "SET requires a direct property target"
            );
        }

        String ownerName = owner.name();

        requireAvailable(
                ownerName,
                availableVariables
        );

        String propertyName =
                target.property();

        String ownerId =
                symbols.resolveVariable(ownerName);

        /*
         * The right-hand expression must be resolved
         * before the mutation is performed.
         *
         * This also permits statements such as:
         *
         * SET a.edad = b.age
         */
        String value = compileValue(
                assignment.value(),
                symbols,
                availableVariables,
                boundProperties,
                goals
        );

        /*
         * Relationship properties and node properties
         * are stored using separate engine predicates.
         */
        String predicate =
                relationshipVariables.contains(ownerName)
                        ? "cy_set_edge_property"
                        : "cy_set_node_property";

        String updateGoal =
                predicate
                        + "(amigos, "
                        + ownerId
                        + ", "
                        + quoteAtom(propertyName)
                        + ", "
                        + value
                        + ")";

        goals.add(updateGoal);

        /*
         * Any previous binding for the modified
         * property refers to its old value.
         *
         * Future reads must use a fresh Prolog
         * variable rather than unifying with
         * the original property value.
         */
        boundProperties.remove(
                propertyKey(ownerName, propertyName)
        );

        onPropertyUpdated.accept(
                ownerName,
                propertyName
        );
    }

    /**
     * Compiles a SET assignment value.
     *
     * Supported examples:
     *
     * 19
     * 19.99
     * 'activo'
     * true
     * b.age
     * b
     */
    private String compileValue(
            Expression expression,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> boundProperties,
            List<String> goals
    ) {

        Objects.requireNonNull(
                expression,
                "SET value cannot be null"
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

        if (expression
                instanceof BooleanLiteralExpression value) {

            return Boolean.toString(value.value());
        }

        if (expression
                instanceof NullLiteralExpression) {

            return "null";
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

        if (expression
                instanceof PropertyExpression property) {

            return compilePropertyReference(
                    property,
                    symbols,
                    availableVariables,
                    boundProperties,
                    goals
            );
        }

        throw new UnsupportedOperationException(
                "SET value expression is not supported: "
                        + expression.getClass().getSimpleName()
        );
    }

    /**
     * Resolves a property referenced on the right
     * side of an assignment.
     *
     * Example:
     *
     * SET a.edad = b.age
     *
     * If b.age has not already been bound, this
     * adds:
     *
     * user_amigos_prop(
     *     __B, 'age', __B_AGE
     * )
     *
     * The resulting variable is then used as
     * the SET assignment value.
     */
    private String compilePropertyReference(
            PropertyExpression property,
            PrologVariableTable symbols,
            Set<String> availableVariables,
            Set<String> boundProperties,
            List<String> goals
    ) {

        if (!(property.target()
                instanceof VariableExpression owner)) {

            throw new UnsupportedOperationException(
                    "Nested SET property references "
                            + "are not supported"
            );
        }

        String ownerName =
                owner.name();

        requireAvailable(
                ownerName,
                availableVariables
        );

        String propertyName =
                property.property();

        String key =
                propertyKey(ownerName, propertyName);

        String ownerId =
                symbols.resolveVariable(ownerName);

        String value =
                symbols.resolveProperty(
                        ownerName,
                        propertyName
                );

        /*
         * Avoid repeating a property lookup if
         * the current binding is already available.
         *
         * A preceding SET assignment invalidates
         * the old binding through its callback.
         */
        if (boundProperties.add(key)) {

            goals.add(
                    "user_amigos_prop("
                            + ownerId
                            + ", "
                            + quoteAtom(propertyName)
                            + ", "
                            + value
                            + ")"
            );
        }

        return value;
    }

    /**
     * Rejects undefined source variables.
     */
    private static void requireAvailable(
            String name,
            Set<String> availableVariables
    ) {

        if (!availableVariables.contains(name)) {

            throw new IllegalArgumentException(
                    "SET references an undefined variable: "
                            + name
            );
        }
    }

    /**
     * Generates consistent property-binding keys.
     */
    private static String propertyKey(
            String owner,
            String property
    ) {

        return owner + "." + property;
    }

    /**
     * Converts a Java string to a quoted Prolog atom.
     *
     * Escapes backslashes and apostrophes
     * to preserve valid SWI-Prolog syntax.
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
