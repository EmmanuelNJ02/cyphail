package cr.ac.una.eif400.cyphail.compiler.analyzer;

import java.util.List;
import java.util.Objects;

/**
 * Immutable result produced by the Sprint P1 semantic analyzer.
 *
 * <p>The result contains every semantic error detected while the AST
 * is traversed. Collecting all errors allows the analyzer to report
 * more than one undefined-variable reference in the same query instead
 * of stopping after the first problem.</p>
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
 * @param errors semantic errors in detection order
 */
public record SemanticAnalysisResult(
        List<String> errors
) {

    public SemanticAnalysisResult {
        Objects.requireNonNull(
                errors,
                "Semantic errors cannot be null"
        );

        errors = List.copyOf(errors);
    }

    /**
     * Indicates whether the analyzed query has no semantic errors.
     *
     * @return true when the analysis completed without errors
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Indicates whether at least one semantic error was detected.
     *
     * @return true when the result contains semantic errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}