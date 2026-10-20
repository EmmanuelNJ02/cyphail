package cr.ac.una.eif400.cyphail.ast;

import cr.ac.una.eif400.cyphail.ast.expression.FloatLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.LiteralExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for floating-point literal AST nodes in Sprint P2.
 *
 * Verifies decimal values, literal type compatibility,
 * immutable record equality, and invalid numeric values.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * Group Code: 04-10am
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
class FloatLiteralExpressionTest {

    @Test
    void shouldStoreDecimalValue() {
        FloatLiteralExpression expression =
                new FloatLiteralExpression(19.99);

        assertEquals(19.99, expression.value());
    }

    @Test
    void shouldImplementLiteralExpression() {
        LiteralExpression expression =
                new FloatLiteralExpression(9.5);

        assertInstanceOf(FloatLiteralExpression.class, expression);
    }

    @Test
    void shouldProvideValueBasedEquality() {
        FloatLiteralExpression first =
                new FloatLiteralExpression(3.14);

        FloatLiteralExpression second =
                new FloatLiteralExpression(3.14);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldRejectNonFiniteValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FloatLiteralExpression(Double.NaN)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FloatLiteralExpression(
                        Double.POSITIVE_INFINITY
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new FloatLiteralExpression(
                        Double.NEGATIVE_INFINITY
                )
        );
    }
}