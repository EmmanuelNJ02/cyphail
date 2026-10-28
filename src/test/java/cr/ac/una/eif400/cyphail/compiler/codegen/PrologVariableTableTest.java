
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
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * Unit tests for PrologVariableTable.
 *
 * Verifies variable conversion, property mappings,
 * temporary variable generation, collision handling,
 * and identifier validation.
 */
class PrologVariableTableTest {

    @Test
    void shouldConvertCyphailVariablesToProlog() {
        PrologVariableTable table =
                new PrologVariableTable();

        assertEquals("__A", table.resolveVariable("a"));
        assertEquals("__B", table.resolveVariable("b"));
        assertEquals(
                "__PERSONA",
                table.resolveVariable("persona")
        );
    }

    @Test
    void shouldReuseExistingVariableMapping() {
        PrologVariableTable table =
                new PrologVariableTable();

        String first = table.resolveVariable("a");
        String second = table.resolveVariable("a");

        assertEquals(first, second);
        assertEquals(1, table.variableMappings().size());
    }

    @Test
    void shouldMapPropertyReferences() {
        PrologVariableTable table =
                new PrologVariableTable();

        assertEquals(
                "__A_NOMBRE",
                table.resolveProperty("a", "nombre")
        );

        assertEquals(
                "__B_EDAD",
                table.resolveProperty("b", "edad")
        );

        assertEquals(
                "__A_NOMBRE",
                table.resolveProperty("a", "nombre")
        );

        assertEquals(2, table.propertyMappings().size());
    }

    @Test
    void shouldPreventCaseConversionCollisions() {
        PrologVariableTable table =
                new PrologVariableTable();

        String first = table.resolveVariable("a");
        String second = table.resolveVariable("A");

        assertEquals("__A", first);
        assertEquals("__A_2", second);
        assertNotEquals(first, second);
    }

    @Test
    void shouldPreventPropertyVariableCollisions() {
        PrologVariableTable table =
                new PrologVariableTable();

        String variable =
                table.resolveVariable("a_nombre");

        String property =
                table.resolveProperty("a", "nombre");

        assertEquals("__A_NOMBRE", variable);
        assertEquals("__A_NOMBRE_2", property);
    }

    @Test
    void shouldGenerateUniqueTemporaryVariables() {
        PrologVariableTable table =
                new PrologVariableTable();

        assertEquals(
                "__TMP_1",
                table.newTemporaryVariable()
        );

        assertEquals(
                "__TMP_2",
                table.newTemporaryVariable()
        );

        assertEquals(
                "__TMP_3",
                table.newTemporaryVariable()
        );
    }

    @Test
    void shouldAvoidTemporaryVariableCollisions() {
        PrologVariableTable table =
                new PrologVariableTable();

        table.resolveVariable("tmp_1");

        assertEquals(
                "__TMP_1_2",
                table.newTemporaryVariable()
        );
    }

    @Test
    void shouldRejectReservedDoubleUnderscorePrefix() {
        PrologVariableTable table =
                new PrologVariableTable();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> table.resolveVariable("__person")
                );

        assertTrue(
                exception.getMessage().contains("__")
        );
    }

    @Test
    void shouldRejectInvalidIdentifiers() {
        PrologVariableTable table =
                new PrologVariableTable();

        assertThrows(
                IllegalArgumentException.class,
                () -> table.resolveVariable("2persona")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> table.resolveVariable("mi-persona")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> table.resolveProperty("a", "full-name")
        );

        assertThrows(
                NullPointerException.class,
                () -> table.resolveVariable(null)
        );
    }

    @Test
    void shouldExposeReadOnlyMappings() {
        PrologVariableTable table =
                new PrologVariableTable();

        table.resolveVariable("a");
        table.resolveProperty("a", "nombre");

        Map<String, String> variables =
                table.variableMappings();

        Map<String, String> properties =
                table.propertyMappings();

        assertEquals("__A", variables.get("a"));

        assertEquals(
                "__A_NOMBRE",
                properties.get("a.nombre")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> variables.put("b", "__B")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> properties.clear()
        );
    }

    @Test
    void shouldKeepMappingsIndependentBetweenQueries() {
        PrologVariableTable firstQuery =
                new PrologVariableTable();

        PrologVariableTable secondQuery =
                new PrologVariableTable();

        firstQuery.resolveVariable("a");

        assertTrue(
                secondQuery.variableMappings().isEmpty()
        );

        assertEquals(
                "__A",
                secondQuery.resolveVariable("a")
        );

        assertEquals(
                1,
                firstQuery.variableMappings().size()
        );

        assertEquals(
                1,
                secondQuery.variableMappings().size()
        );
    }
}
