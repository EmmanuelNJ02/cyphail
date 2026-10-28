
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.expression.Expression;
import cr.ac.una.eif400.cyphail.ast.expression.FloatLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.IntegerLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.StringLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipDirection;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
 * Generates SWI-Prolog goals for Cyphail relationship patterns.
 *
 * Supports outgoing, incoming and undirected relationships,
 * anonymous relationships, relationship types and literal
 * property constraints.
 *
 * This class generates individual relationship goals.
 * The main PrologCodeGenerator will integrate these goals
 * into complete MATCH queries in a subsequent stage.
 */
public final class PrologRelationshipGoalBuilder {

    /**
     * Immutable generated result for one relationship.
     *
     * @param variable Prolog variable containing the relationship ID
     * @param goals ordered Prolog goals for this relationship
     */
    public record GeneratedRelationship(
            String variable,
            List<String> goals
    ) {

        public GeneratedRelationship {
            Objects.requireNonNull(
                    variable,
                    "Relationship variable cannot be null"
            );

            if (variable.isBlank()) {
                throw new IllegalArgumentException(
                        "Relationship variable cannot be blank"
                );
            }

            goals = List.copyOf(
                    Objects.requireNonNull(
                            goals,
                            "Relationship goals cannot be null"
                    )
            );
        }
    }

    /**
     * Generates goals for a relationship between two nodes.
     *
     * @param relationship relationship AST
     * @param leftNodeId Prolog variable for the left node
     * @param rightNodeId Prolog variable for the right node
     * @param symbols variable mapping table for the query
     * @return generated relationship identifier and ordered goals
     */
    public GeneratedRelationship generate(
            RelationshipPattern relationship,
            String leftNodeId,
            String rightNodeId,
            PrologVariableTable symbols
    ) {

        Objects.requireNonNull(
                relationship,
                "Relationship pattern cannot be null"
        );

        Objects.requireNonNull(
                symbols,
                "Symbol table cannot be null"
        );

        validateNodeVariable(leftNodeId);
        validateNodeVariable(rightNodeId);

        String relationshipId = relationship.variable()
                .map(symbols::resolveVariable)
                .orElseGet(symbols::newTemporaryVariable);

        List<String> goals = new ArrayList<>();

        String relationshipType = resolveRelationshipType(
                relationship,
                symbols,
                goals
        );

        goals.add(
                directionGoal(
                        relationship.direction(),
                        relationshipId,
                        relationshipType,
                        leftNodeId,
                        rightNodeId
                )
        );

        for (PatternProperty property :
                relationship.properties()) {

            String propertyVariable = relationship.variable()
                    .map(name -> symbols.resolveProperty(
                            name,
                            property.name()
                    ))
                    .orElseGet(symbols::newTemporaryVariable);

            goals.add(
                    "user_amigos_prop("
                            + relationshipId
                            + ", "
                            + quoteAtom(property.name())
                            + ", "
                            + propertyVariable
                            + ")"
            );

            goals.add(
                    "cy_eq("
                            + propertyVariable
                            + ", "
                            + literalTerm(property.value())
                            + ")"
            );
        }

        return new GeneratedRelationship(
                relationshipId,
                goals
        );
    }

    /**
     * Resolves a single type, multiple types,
     * or an unrestricted relationship type.
     */
    private static String resolveRelationshipType(
            RelationshipPattern relationship,
            PrologVariableTable symbols,
            List<String> goals
    ) {

        List<String> types = relationship.types();

        if (types.size() == 1) {
            return quoteAtom(types.getFirst());
        }

        String typeVariable =
                symbols.newTemporaryVariable();

        if (!types.isEmpty()) {

            String allowedTypes = types.stream()
                    .map(PrologRelationshipGoalBuilder::quoteAtom)
                    .collect(Collectors.joining(", "));

            goals.add(
                    "member("
                            + typeVariable
                            + ", ["
                            + allowedTypes
                            + "])"
            );
        }

        return typeVariable;
    }

    /**
     * Creates the relationship goal according
     * to its direction in the Cyphail source.
     */
    private static String directionGoal(
            RelationshipDirection direction,
            String relationshipId,
            String relationshipType,
            String leftNodeId,
            String rightNodeId
    ) {

        String outgoing = edgeGoal(
                relationshipId,
                relationshipType,
                leftNodeId,
                rightNodeId
        );

        String incoming = edgeGoal(
                relationshipId,
                relationshipType,
                rightNodeId,
                leftNodeId
        );

        return switch (direction) {

            case OUT -> outgoing;

            case IN -> incoming;

            case NONE -> "(("
                    + outgoing
                    + ") ; (dif("
                    + leftNodeId
                    + ", "
                    + rightNodeId
                    + "), "
                    + incoming
                    + "))";
        };
    }

    /**
     * Builds a call to the professor-compatible
     * graph relationship predicate.
     */
    private static String edgeGoal(
            String relationshipId,
            String relationshipType,
            String source,
            String destination
    ) {

        return "user_amigos_edge("
                + relationshipId
                + ", "
                + relationshipType
                + ", "
                + source
                + ", "
                + destination
                + ")";
    }

    /**
     * Converts supported Cyphail literals
     * into SWI-Prolog terms.
     */
    private static String literalTerm(
            Expression expression
    ) {

        if (expression instanceof StringLiteralExpression value) {
            return quoteAtom(value.value());
        }

        if (expression instanceof IntegerLiteralExpression value) {
            return Long.toString(value.value());
        }

        if (expression instanceof FloatLiteralExpression value) {
            return Double.toString(value.value());
        }

        throw new UnsupportedOperationException(
                "Relationship property expression is not yet "
                        + "supported by CodeGen: "
                        + expression.getClass().getSimpleName()
        );
    }

    /**
     * Quotes a string as a SWI-Prolog atom.
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
     * Validates the generated Prolog variables
     * identifying relationship endpoints.
     */
    private static void validateNodeVariable(
            String variable
    ) {

        Objects.requireNonNull(
                variable,
                "Node variable cannot be null"
        );

        if (!variable.matches("_[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(
                    "Invalid Prolog node variable: "
                            + variable
            );
        }
    }
}
