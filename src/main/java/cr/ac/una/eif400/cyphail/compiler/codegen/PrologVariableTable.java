
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
     * Returns a stable Prolog variable for a
     * named Cyphail variable.
     */
    public String resolveVariable(String name) {

        validateIdentifier(name, "Cyphail variable");

        if (name.startsWith("__")) {
            throw new IllegalArgumentException(
                    "Cyphail variable names cannot start with '__': "
                            + name
            );
        }

        return variables.computeIfAbsent(
                name,
                key -> allocate(
                        "__" + key.toUpperCase(Locale.ROOT)
                )
        );
    }

    /**
     * Returns a stable Prolog variable for a
     * property belonging to a named variable.
     */
    public String resolveProperty(
            String variableName,
            String propertyName
    ) {

        String owner = resolveVariable(variableName);

        validateIdentifier(propertyName, "Property");

        String reference =
                variableName + "." + propertyName;

        return properties.computeIfAbsent(
                reference,
                key -> allocate(
                        owner + "_"
                                + propertyName.toUpperCase(
                                        Locale.ROOT
                                )
                )
        );
    }

    /**
     * Allocates a unique temporary Prolog variable
     * for anonymous nodes or relationships.
     */
    public String newTemporaryVariable() {

        temporaryCounter++;

        return allocate(
                "__TMP_" + temporaryCounter
        );
    }

    /**
     * Returns a read-only snapshot of the
     * original variable mappings.
     */
    public Map<String, String> variableMappings() {

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(variables)
        );
    }

    /**
     * Returns a read-only snapshot of the
     * property reference mappings.
     */
    public Map<String, String> propertyMappings() {

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(properties)
        );
    }

    /**
     * Reserves a unique Prolog variable name,
     * adding a numeric suffix if necessary.
     */
    private String allocate(String base) {

        String candidate = base;
        int suffix = 2;

        while (!allocatedNames.add(candidate)) {
            candidate = base + "_" + suffix++;
        }

        return candidate;
    }

    /**
     * Validates a supported source identifier.
     */
    private static void validateIdentifier(
            String name,
            String description
    ) {

        Objects.requireNonNull(
                name,
                description + " cannot be null"
        );

        if (!name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                    description
                            + " must be a valid identifier: "
                            + name
            );
        }
    }
}
