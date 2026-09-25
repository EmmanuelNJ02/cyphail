package cr.ac.una.eif400.cyphail.compiler.parser.combinator;

/**
 * Represents the result produced by a functional parser combinator.
 *
 * <p>A parse result can either be a successful value together with the
 * remaining parser input, or a failure containing a reason and the input
 * position where parsing could not continue.</p>
 *
 * <p>Project: Cyphail - Sprint P1<br>
 * Course: EIF400 - Paradigmas de Programacion<br>
 * Universidad Nacional de Costa Rica<br>
 * Group: 04-10am</p>
 *
 * @param <T> type of value produced by the parser
 *
 * @author Emmanuel Nunez Jimenez
 * @author Keynell Molina Mora
 * @author Valery Alfaro Morales
 * @author Julissa Solano Valverde
 * @author Roy Arias Mejia
 */
public sealed interface ParseResult<T>
        permits ParseSuccess, ParseFailure {

    /**
     * Indicates whether this result represents a successful parse.
     *
     * @return true for success and false for failure
     */
    boolean isSuccess();
}