
package cr.ac.una.eif400.cyphail.compiler.codegen;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
 * Immutable result produced by the Prolog code generator.
 *
 * Stores the generated Prolog goal, ordered output
 * columns, and symbol mappings used during
 * Cyphail-to-Prolog transpilation.
 *
 * The goal can later be submitted to the MQI broker.
 * Column metadata allows the frontend to preserve
 * the original RETURN order and aliases.
 */
public record PrologGenerationResult(
        String goal,
        List<OutputColumn> columns,
        Map<String, String> variableMappings,
        Map<String, String> propertyMappings
) {

    /**
     * Creates an immutable snapshot of the generated
     * goal and its associated metadata.
     */
    public PrologGenerationResult {

        Objects.requireNonNull(
                goal,
                "Generated Prolog goal cannot be null"
        );

        if (goal.isBlank()) {
            throw new IllegalArgumentException(
                    "Generated Prolog goal cannot be blank"
            );
        }

        Objects.requireNonNull(
                columns,
                "Output columns cannot be null"
        );

        Objects.requireNonNull(
                variableMappings,
                "Variable mappings cannot be null"
        );

        Objects.requireNonNull(
                propertyMappings,
                "Property mappings cannot be null"
        );

        columns = List.copyOf(columns);

        variableMappings = Collections.unmodifiableMap(
                new LinkedHashMap<>(variableMappings)
        );

        propertyMappings = Collections.unmodifiableMap(
                new LinkedHashMap<>(propertyMappings)
        );
    }

    /**
     * Returns output column headers in their
     * original RETURN order.
     */
    public List<String> columnHeaders() {

        return columns.stream()
                .map(OutputColumn::header)
                .toList();
    }

    /**
     * Indicates whether the statement has
     * projected output columns.
     */
    public boolean hasOutputColumns() {

        return !columns.isEmpty();
    }

    /**
     * Describes one projected Cyphail value.
     *
     * @param sourceExpression original source reference,
     *                         such as a.nombre
     * @param header output column name or alias
     * @param prologVariable generated Prolog variable
     *                       containing the projected value
     */
    public record OutputColumn(
            String sourceExpression,
            String header,
            String prologVariable
    ) {

        /**
         * Ensures each projected column has the
         * metadata needed by the execution pipeline.
         */
        public OutputColumn {

            validate(
                    sourceExpression,
                    "Source expression"
            );

            validate(
                    header,
                    "Column header"
            );

            validate(
                    prologVariable,
                    "Prolog variable"
            );
        }

        /**
         * Rejects null or blank column metadata.
         */
        private static void validate(
                String value,
                String description
        ) {

            Objects.requireNonNull(
                    value,
                    description + " cannot be null"
            );

            if (value.isBlank()) {
                throw new IllegalArgumentException(
                        description + " cannot be blank"
                );
            }
        }
    }
}
