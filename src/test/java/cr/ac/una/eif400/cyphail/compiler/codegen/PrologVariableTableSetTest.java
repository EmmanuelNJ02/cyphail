
package cr.ac.una.eif400.cyphail.compiler.codegen;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Nunez Jimenez
 * Julissa Solano Valverde
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 *
 * Tests property-binding invalidation for SET.
 *
 * Ensures that:
 * - Updated properties receive fresh Prolog variables.
 * - Original property bindings remain independent.
 * - Node identity remains unchanged.
 * - Repeated updates never reuse old bindings.
 * - Other properties remain unaffected.
 */
class PrologVariableTableSetTest {

    @Test
    void shouldInvalidateUpdatedProperty() {

        PrologVariableTable table =
                new PrologVariableTable();

        String original =
                table.resolveProperty("p", "precio");

        String updated =
                table.invalidateProperty("p", "precio");

        assertEquals("__P_PRECIO", original);

        assertEquals("__P_PRECIO_2", updated);

        assertNotEquals(original, updated);

        assertEquals(
                updated,
                table.resolveProperty("p", "precio")
        );
    }

    @Test
    void shouldPreserveNodeVariableAfterSet() {

        PrologVariableTable table =
                new PrologVariableTable();

        String nodeId =
                table.resolveVariable("p");

        table.resolveProperty("p", "precio");

        table.invalidateProperty("p", "precio");

        assertEquals(
                nodeId,
                table.resolveVariable("p")
        );

        assertEquals("__P", nodeId);

        assertEquals(
                1,
                table.variableMappings().size()
        );
    }

    @Test
    void shouldGenerateFreshVariableForEveryUpdate() {

        PrologVariableTable table =
                new PrologVariableTable();

        String first =
                table.resolveProperty("p", "precio");

        String second =
                table.invalidateProperty("p", "precio");

        String third =
                table.invalidateProperty("p", "precio");

        assertEquals("__P_PRECIO", first);

        assertEquals("__P_PRECIO_2", second);

        assertEquals("__P_PRECIO_3", third);

        assertNotEquals(first, second);

        assertNotEquals(second, third);

        assertEquals(
                third,
                table.resolveProperty("p", "precio")
        );
    }

    @Test
    void shouldNotInvalidateUnrelatedProperties() {

        PrologVariableTable table =
                new PrologVariableTable();

        String originalPrice =
                table.resolveProperty("p", "precio");

        String originalName =
                table.resolveProperty("p", "nombre");

        String updatedPrice =
                table.invalidateProperty("p", "precio");

        assertNotEquals(
                originalPrice,
                updatedPrice
        );

        assertEquals(
                originalName,
                table.resolveProperty("p", "nombre")
        );

        assertEquals(
                updatedPrice,
                table.resolveProperty("p", "precio")
        );
    }

    @Test
    void shouldPreservePreviousMappingSnapshot() {

        PrologVariableTable table =
                new PrologVariableTable();

        table.resolveProperty("p", "precio");

        Map<String, String> beforeSet =
                table.propertyMappings();

        table.invalidateProperty("p", "precio");

        Map<String, String> afterSet =
                table.propertyMappings();

        assertEquals(
                "__P_PRECIO",
                beforeSet.get("p.precio")
        );

        assertEquals(
                "__P_PRECIO_2",
                afterSet.get("p.precio")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> afterSet.clear()
        );
    }
}
