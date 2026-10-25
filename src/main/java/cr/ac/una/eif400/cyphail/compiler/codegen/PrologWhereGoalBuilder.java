
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;
import cr.ac.una.eif400.cyphail.ast.expression.*;

import java.util.ArrayList;
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
 * Authors:
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * Generates executable SWI-Prolog goals for WHERE clauses.
 *
 * Supports:
 * - Equality and inequality comparisons.
 * - Numeric ordering comparisons.
 * - Node and relationship property references.
 * - Variable-to-variable comparisons.
 * - Integer, decimal, string and boolean literals.
 * - Logical AND and OR in the AST.
 * - Arithmetic expressions in the AST.
 *
 * Logical and arithmetic expression parsing may require
 * additional parser support before those expressions can
 * be written directly in Cyphail source statements.
 *
 * Unsupported expressions fail explicitly.
 */
public final class PrologWhereGoalBuilder {

    /**
     * Generates the Prolog goal for a complete WHERE clause.
     *
     * @param clause WHERE clause to compile
     * @param symbols mapping table shared with the query
     * @param matchedVariables variables introduced by MATCH
     * @return executable Prolog goal
     */
    public String generate(
            WhereClause clause,
            PrologVariableTable symbols,
            Set<String> matchedVariables
    ) {

        Objects.requireNonNull(
                clause,
                "WHERE clause cannot be null"
        );

        Objects.requireNonNull(
                symbols,
                "Symbol table cannot be null"
        );

        Objects.requireNonNull(
                matchedVariables,
                "Matched variables cannot be null"
        );

        return compileCondition(
                clause.expression(),
                symbols,
                matchedVariables
        );
    }

    /**
     * Compiles an expression intended to produce
     * a true or false condition.
     */
    private String compileCondition(
            Expression expression,
            PrologVariableTable symbols,
            Set<String> matchedVariables
    ) {

        Objects.requireNonNull(
                expression,
                "WHERE expression cannot be null"
        );

        if (expression instanceof BooleanLiteralExpression literal) {
            return literal.value() ? "true" : "fail";
        }

        if (!(expression instanceof BinaryExpression binary)) {
            throw unsupported(
                    "WHERE condition: "
                            + expression.getClass().getSimpleName()
            );
        }

        return switch (binary.operator()) {

            case AND -> logicalGoal(
                    binary,
                    symbols,
                    matchedVariables,
                    true
            );

            case OR -> logicalGoal(
                    binary,
                    symbols,
                    matchedVariables,
                    false
            );

            case EQUAL,
                 NOT_EQUAL,
                 LESS_THAN,
                 GREATER_THAN,
                 LESS_THAN_OR_EQUAL,
                 GREATER_THAN_OR_EQUAL ->
                    comparisonGoal(
                            binary,
                            symbols,
                            matchedVariables
                    );

            default -> throw unsupported(
                    "Boolean condition operator: "
                            + binary.operator()
            );
        };
    }

    /**
     * Generates conjunction or disjunction goals.
     *
     * Parentheses preserve the original AST grouping.
     * Property accesses remain inside their branches
     * to preserve OR semantics.
     */
    private String logicalGoal(
            BinaryExpression expression,
            PrologVariableTable symbols,
            Set<String> matchedVariables,
            boolean conjunction
    ) {

        String left = compileCondition(
                expression.left(),
                symbols,
                matchedVariables
        );

        String right = compileCondition(
                expression.right(),
                symbols,
                matchedVariables
        );

        String operator = conjunction ? ", " : " ; ";

        return "(("
                + left
                + ")"
                + operator
                + "("
                + right
                + "))";
    }

    /**
     * Generates a comparison and its required
     * property-access goals.
     */
    private String comparisonGoal(
            BinaryExpression expression,
            PrologVariableTable symbols,
            Set<String> matchedVariables
    ) {

        CompiledValue left = compileValue(
                expression.left(),
                symbols,
                matchedVariables
        );

        CompiledValue right = compileValue(
                expression.right(),
                symbols,
                matchedVariables
        );

        List<String> goals = new ArrayList<>();

        goals.addAll(left.goals());
        goals.addAll(right.goals());

        String leftTerm = left.term();
        String rightTerm = right.term();

        String comparison = switch (expression.operator()) {

            case EQUAL ->
                    "cy_eq(" + leftTerm + ", " + rightTerm + ")";

            case NOT_EQUAL ->
                    "cy_neq(" + leftTerm + ", " + rightTerm + ")";

            case LESS_THAN ->
                    numericComparison(leftTerm, "<", rightTerm, goals);

            case GREATER_THAN ->
                    numericComparison(leftTerm, ">", rightTerm, goals);

            case LESS_THAN_OR_EQUAL ->
                    numericComparison(leftTerm, "=<", rightTerm, goals);

            case GREATER_THAN_OR_EQUAL ->
                    numericComparison(leftTerm, ">=", rightTerm, goals);

            default -> throw unsupported(
                    "Comparison operator: "
                            + expression.operator()
            );
        };

        goals.add(comparison);

        return "(" + String.join(", ", goals) + ")";
    }

    /**
     * Adds numeric guards before an ordering comparison.
     *
     * These guards prevent string values from being passed
     * to Prolog arithmetic ordering operators.
     */
    private static String numericComparison(
            String left,
            String operator,
            String right,
            List<String> goals
    ) {

        goals.add("number(" + left + ")");
        goals.add("number(" + right + ")");

        return "(" + left + " " + operator + " " + right + ")";
    }

    /**
     * Compiles a value used by a comparison.
     *
     * A compiled value contains its Prolog term and
     * any prerequisite goals needed to obtain it.
     */
    private CompiledValue compileValue(
            Expression expression,
            PrologVariableTable symbols,
            Set<String> matchedVariables
    ) {

        if (expression instanceof VariableExpression variable) {

            ensureMatched(
                    variable.name(),
                    matchedVariables
            );

            return new CompiledValue(
                    symbols.resolveVariable(variable.name()),
                    List.of()
            );
        }

        if (expression instanceof PropertyExpression property) {

            if (!(property.target()
                    instanceof VariableExpression owner)) {

                throw unsupported(
                        "Nested property access"
                );
            }

            ensureMatched(
                    owner.name(),
                    matchedVariables
            );

            String elementId =
                    symbols.resolveVariable(owner.name());

            String propertyValue =
                    symbols.resolveProperty(
                            owner.name(),
                            property.property()
                    );

            String accessGoal =
                    "user_amigos_prop("
                            + elementId
                            + ", "
                            + quoteAtom(property.property())
                            + ", "
                            + propertyValue
                            + ")";

            return new CompiledValue(
                    propertyValue,
                    List.of(accessGoal)
            );
        }

        if (expression instanceof StringLiteralExpression literal) {
            return new CompiledValue(
                    quoteAtom(literal.value()),
                    List.of()
            );
        }

        if (expression instanceof IntegerLiteralExpression literal) {
            return new CompiledValue(
                    Long.toString(literal.value()),
                    List.of()
            );
        }

        if (expression instanceof FloatLiteralExpression literal) {
            return new CompiledValue(
                    Double.toString(literal.value()),
                    List.of()
            );
        }

        if (expression instanceof BooleanLiteralExpression literal) {
            return new CompiledValue(
                    literal.value() ? "true" : "false",
                    List.of()
            );
        }

        if (expression instanceof BinaryExpression binary) {
            return compileArithmetic(
                    binary,
                    symbols,
                    matchedVariables
            );
        }

        if (expression instanceof NullLiteralExpression) {
            throw unsupported(
                    "NULL comparisons"
            );
        }

        throw unsupported(
                "WHERE value: "
                        + expression.getClass().getSimpleName()
        );
    }

    /**
     * Compiles arithmetic AST expressions into
     * a calculated Prolog temporary variable.
     */
    private CompiledValue compileArithmetic(
            BinaryExpression expression,
            PrologVariableTable symbols,
            Set<String> matchedVariables
    ) {

        String operator = switch (expression.operator()) {

            case ADD -> "+";
            case SUBTRACT -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
            case MODULO -> "mod";

            default -> throw unsupported(
                    "Arithmetic operator: "
                            + expression.operator()
            );
        };

        CompiledValue left = compileValue(
                expression.left(),
                symbols,
                matchedVariables
        );

        CompiledValue right = compileValue(
                expression.right(),
                symbols,
                matchedVariables
        );

        String result =
                symbols.newTemporaryVariable();

        List<String> goals = new ArrayList<>();

        goals.addAll(left.goals());
        goals.addAll(right.goals());

        goals.add(
                "number(" + left.term() + ")"
        );

        goals.add(
                "number(" + right.term() + ")"
        );

        goals.add(
                result
                        + " is ("
                        + left.term()
                        + " "
                        + operator
                        + " "
                        + right.term()
                        + ")"
        );

        return new CompiledValue(
                result,
                goals
        );
    }

    /**
     * Ensures that a referenced variable
     * is defined in the MATCH pattern.
     */
    private static void ensureMatched(
            String name,
            Set<String> matchedVariables
    ) {

        if (!matchedVariables.contains(name)) {

            throw new IllegalArgumentException(
                    "WHERE references an unmatched variable: "
                            + name
            );
        }
    }

    /**
     * Escapes a text value as a SWI-Prolog atom.
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
     * Creates a descriptive exception for
     * unsupported WHERE expressions.
     */
    private static UnsupportedOperationException unsupported(
            String feature
    ) {

        return new UnsupportedOperationException(
                "P2 WHERE CodeGen support is pending for: "
                        + feature
        );
    }

    /**
     * Immutable intermediate representation
     * of one compiled WHERE value.
     */
    private record CompiledValue(
            String term,
            List<String> goals
    ) {

        private CompiledValue {

            Objects.requireNonNull(
                    term,
                    "Compiled term cannot be null"
            );

            Objects.requireNonNull(
                    goals,
                    "Compiled goals cannot be null"
            );

            if (term.isBlank()) {
                throw new IllegalArgumentException(
                        "Compiled term cannot be blank"
                );
            }

            goals = List.copyOf(goals);
        }
    }
}
