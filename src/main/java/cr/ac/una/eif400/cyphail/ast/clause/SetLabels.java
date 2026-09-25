package cr.ac.una.eif400.cyphail.ast.clause;

import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;

import java.util.List;
import java.util.Objects;

/**
 * Immutable AST node that represents adding one or more labels
 * to a Cyphail variable using SET.
 *
 * <p>Examples:</p>
 * <pre>
 * SET p:Employee
 * SET p:Employee:Manager
 * </pre>
 *
 * <p>Project: Cyphail</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica - Escuela de Informatica</p>
 * <p>Group Code: 04-10am</p>
 *
 * <p>Authors:</p>
 * <ul>
 *     <li>Emmanuel Nunez Jimenez</li>
 *     <li>Valery Alfaro Morales</li>
 *     <li>Roy Arias Mejia</li>
 *     <li>Keynell Molina Mora</li>
 *     <li>Julissa Solano Valverde</li>
 * </ul>
 *
 * @param variable variable receiving the labels
 * @param labels labels added to the variable
 */
public record SetLabels(
        VariableExpression variable,
        List<String> labels
) implements SetItem {

    public SetLabels {
        Objects.requireNonNull(
                variable,
                "SET variable cannot be null"
        );

        Objects.requireNonNull(
                labels,
                "SET labels cannot be null"
        );

        labels = List.copyOf(labels);

        if (labels.isEmpty()) {
            throw new IllegalArgumentException(
                    "SET labels cannot be empty"
            );
        }

        if (labels.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "SET labels cannot contain blank values"
            );
        }
    }
}