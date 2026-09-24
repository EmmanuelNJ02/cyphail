package cr.ac.una.eif400.cyphail.ast.pattern;

import cr.ac.una.eif400.cyphail.ast.AstNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable AST node that represents a Cyphail node pattern.
 *
 * <p>A node pattern can contain an optional variable, zero or more labels
 * and zero or more properties.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * (m:Movie)
 * (a:Person:Employee {id: 1})
 * (o:Order {personId: p.id, status: "cancelled"})
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
 * @param variable optional node variable
 * @param labels node labels
 * @param properties node properties in source order
 */
public record NodePattern(
        Optional<String> variable,
        List<String> labels,
        List<PatternProperty> properties
) implements AstNode {

    public NodePattern {
        Objects.requireNonNull(variable, "Variable cannot be null");
        Objects.requireNonNull(labels, "Labels cannot be null");
        Objects.requireNonNull(properties, "Properties cannot be null");

        labels = List.copyOf(labels);
        properties = List.copyOf(properties);

        variable.ifPresent(name -> {
            if (name.isBlank()) {
                throw new IllegalArgumentException(
                        "Variable name cannot be blank"
                );
            }
        });

        if (labels.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "Labels cannot contain blank values"
            );
        }
    }
}