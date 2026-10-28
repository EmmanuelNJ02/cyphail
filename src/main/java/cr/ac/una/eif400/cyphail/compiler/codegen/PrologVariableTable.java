
package cr.ac.una.eif400.cyphail.compiler.codegen;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
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
 * Maps Cyphail variables and property references
 * to unique Prolog variables.
 *
 * A new instance must be created for each query
 * to avoid sharing bindings between statements.
 *
 * Sprint P2 SET support:
 * Property mappings can be refreshed after an update
 * without modifying previously generated Prolog goals.
 */
public final class PrologVariableTable {

    private final Map<String, String> variables =
            new LinkedHashMap<>();

    private final Map<String, String> properties =
            new LinkedHashMap<>();

    private final Set<String> allocatedNames =
            new HashSet<>();

    private int temporaryCounter;

    /**
     * Returns a stable Prolog variable for
     * a named Cyphail variable.
     *
     * Example:
     *
     * a -> __A
     */
    public String resolveVariable(String name) {

        validateIdentifier(
                name,
                "Cyphail variable"
        );

        if (name.startsWith("__")) {

            throw new IllegalArgumentException(
                    "Cyphail variable names cannot "
                            + "start with '__': "
                            + name
            );
        }

        return variables.computeIfAbsent(
                name,
                key -> allocate(
                        "__"
                                + key.toUpperCase(
                                        Locale.ROOT
                                )
                )
        );
    }

    /**
     * Returns the current Prolog variable
     * corresponding to a property.
     *
     * When the property has not been resolved
     * before, a new mapping is allocated.
     *
     * Subsequent reads reuse the same mapping
     * until invalidateProperty is called.
     *
     * Example:
     *
     * a.nombre -> __A_NOMBRE
     */
    public String resolveProperty(
            String variableName,
            String propertyName
    ) {

        String owner =
                resolveVariable(variableName);

        validateIdentifier(
                propertyName,
                "Property"
        );

        String reference =
                variableName
                        + "."
                        + propertyName;

        return properties.computeIfAbsent(
                reference,
                key -> allocate(
                        owner
                                + "_"
                                + propertyName.toUpperCase(
                                        Locale.ROOT
                                )
                )
        );
    }

    /**
     * Invalidates a previous property mapping
     * after a SET mutation.
     *
     * A new Prolog variable is allocated so
     * future property reads can obtain the
     * updated value.
     *
     * The previously generated goals keep
     * referencing their original variables.
     *
     * Example:
     *
     * Before SET:
     * a.edad -> __A_EDAD
     *
     * After SET:
     * a.edad -> __A_EDAD_2
     *
     * The node mapping remains unchanged:
     * a -> __A
     *
     * @param variableName owner variable
     * @param propertyName updated property
     * @return fresh Prolog variable
     */
    public String invalidateProperty(
            String variableName,
            String propertyName
    ) {

        String owner =
                resolveVariable(variableName);

        validateIdentifier(
                propertyName,
                "Property"
        );

        String reference =
                variableName
                        + "."
                        + propertyName;

        /*
         * allocate() never reuses a previously
         * reserved Prolog variable name.
         *
         * Therefore the old property binding
         * remains independent of the new one.
         */
        String freshVariable = allocate(
                owner
                        + "_"
                        + propertyName.toUpperCase(
                                Locale.ROOT
                        )
        );

        /*
         * Replace only the current property
         * mapping, not the owner node mapping.
         */
        properties.put(
                reference,
                freshVariable
        );

        return freshVariable;
    }

    /**
     * Allocates a unique temporary Prolog variable
     * for anonymous nodes, relationships or
     * intermediate expressions.
     */
    public String newTemporaryVariable() {

        temporaryCounter++;

        return allocate(
                "__TMP_" + temporaryCounter
        );
    }

    /**
     * Returns a read-only snapshot of the
     * original Cyphail variable mappings.
     */
    public Map<String, String> variableMappings() {

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        variables
                )
        );
    }

    /**
     * Returns a read-only snapshot of the
     * current property mappings.
     *
     * After SET, the most recent binding is
     * returned for each property.
     */
    public Map<String, String> propertyMappings() {

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(
                        properties
                )
        );
    }

    /**
     * Reserves a unique Prolog variable name.
     *
     * A numeric suffix is added whenever
     * the requested name has already been used.
     */
    private String allocate(String base) {

        String candidate = base;

        int suffix = 2;

        while (!allocatedNames.add(candidate)) {

            candidate =
                    base + "_" + suffix++;

        }

        return candidate;
    }

    /**
     * Validates a supported Cyphail identifier.
     *
     * Identifiers must begin with a letter
     * or underscore and may subsequently contain
     * letters, digits or underscores.
     */
    private static void validateIdentifier(
            String name,
            String description
    ) {

        Objects.requireNonNull(
                name,
                description + " cannot be null"
        );

        if (!name.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {

            throw new IllegalArgumentException(
                    description
                            + " must be a valid identifier: "
                            + name
            );
        }
    }
}
