package cr.ac.una.eif400.cyphail.ast.pattern;

import cr.ac.una.eif400.cyphail.ast.AstNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable AST node that represents a relationship in a Cyphail pattern.
 *
 * <p>A relationship may have an optional variable, zero or more types,
 * zero or more properties and a direction.</p>
 *
 * <p>Examples:</p>
 * <pre>
 * -[:KNOWS]->
 * -[r:KNOWS]->
 * -[r:WORKS_AT {role: 'Dev'}]->
 * &lt;-[r:KNOWS]-
 * -[r:KNOWS]-
 * </pre>
 *
 * <p>The direction is interpreted relative to the left and right
 * nodes of the relationship pattern:</p>
 *
 * <ul>
 *     <li>OUT: from the left node to the right node.</li>
 *     <li>IN: from the right node to the left node.</li>
 *     <li>NONE: no direction is specified.</li>
 * </ul>
 *
 * <p>Project: Cyphail - Sprint P2</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica</p>
 * <p>Escuela de Informatica</p>
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
 * <p>Sprint P2 contribution:
 * Relationship pattern AST model.</p>
 *
 * @param variable optional relationship variable
 * @param types relationship types in source order
 * @param properties relationship properties in source order
 * @param direction relationship direction
 */
public record RelationshipPattern(
        Optional<String> variable,
        List<String> types,
        List<PatternProperty> properties,
        RelationshipDirection direction
) implements AstNode {

    public RelationshipPattern {
        Objects.requireNonNull(
                variable,
                "Relationship variable cannot be null"
        );

        Objects.requireNonNull(
                types,
                "Relationship types cannot be null"
        );

        Objects.requireNonNull(
                properties,
                "Relationship properties cannot be null"
        );

        Objects.requireNonNull(
                direction,
                "Relationship direction cannot be null"
        );

        types = List.copyOf(types);
        properties = List.copyOf(properties);

        variable.ifPresent(name -> {
            if (name.isBlank()) {
                throw new IllegalArgumentException(
                        "Relationship variable cannot be blank"
                );
            }
        });

        if (types.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "Relationship types cannot contain blank values"
            );
        }
    }
}