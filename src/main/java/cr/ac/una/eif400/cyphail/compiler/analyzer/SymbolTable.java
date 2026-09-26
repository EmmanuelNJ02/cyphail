package cr.ac.una.eif400.cyphail.compiler.analyzer;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Symbol table used by the Sprint P1 semantic analyzer.
 *
 * <p>The table stores the variables that have been declared while the
 * query AST is traversed. Variables are kept in declaration order so
 * the analyzer can validate references according to the order in which
 * patterns and clauses appear in the source query.</p>
 *
 * <p>Sprint P1 uses a single query scope, because the official cases do
 * not include nested MATCH clauses.</p>
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
 */
public final class SymbolTable {

    private final Set<String> variables = new LinkedHashSet<>();

    /**
     * Defines a variable in the current query scope.
     *
     * @param name variable name
     * @return true when the variable was added, false when it was
     *         already defined
     */
    public boolean define(String name) {
        validateName(name);
        return variables.add(name);
    }

    /**
     * Checks whether a variable has already been defined.
     *
     * @param name variable name
     * @return true when the variable exists in the symbol table
     */
    public boolean isDefined(String name) {
        validateName(name);
        return variables.contains(name);
    }

    /**
     * Returns a read-only snapshot of the variables currently defined.
     *
     * @return variables in declaration order
     */
    public Set<String> variables() {
        return Collections.unmodifiableSet(
                new LinkedHashSet<>(variables)
        );
    }

    private void validateName(String name) {
        Objects.requireNonNull(
                name,
                "Variable name cannot be null"
        );

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Variable name cannot be blank"
            );
        }
    }
}