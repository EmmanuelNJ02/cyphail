
package cr.ac.una.eif400.cyphail.compiler.codegen;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Unit tests for PrologGenerationResult.
 *
 * Verifies generated goals, RETURN column metadata,
 * aliases, defensive copies, immutability,
 * and invalid input handling.
 */
class PrologGenerationResultTest {

    @Test
    void shouldStoreGeneratedPrologGoal() {

        String goal = "user_amigos_node(__A)";

        PrologGenerationResult result =
                new PrologGenerationResult(
                        goal,
                        List.of(),
                        Map.of("a", "__A"),
                        Map.of()
                );

        assertEquals(goal, result.goal());
        assertEquals("__A",
                result.variableMappings().get("a"));
    }

    @Test
    void shouldPreserveReturnColumnOrder() {

        List<PrologGenerationResult.OutputColumn> columns =
                List.of(
                        new PrologGenerationResult.OutputColumn(
                                "a.nombre",
                                "nombre",
                                "__A_NOMBRE"
                        ),
                        new PrologGenerationResult.OutputColumn(
                                "b.edad",
                                "edad",
                                "__B_EDAD"
                        )
                );

        PrologGenerationResult result =
                new PrologGenerationResult(
                        "true",
                        columns,
                        Map.of(),
                        Map.of()
                );

        assertEquals(
                List.of("nombre", "edad"),
                result.columnHeaders()
        );

        assertEquals(columns, result.columns());
        assertTrue(result.hasOutputColumns());
    }

    @Test
    void shouldPreserveReturnAliases() {

        PrologGenerationResult.OutputColumn column =
                new PrologGenerationResult.OutputColumn(
                        "a.nombre",
                        "nombrePersona",
                        "__A_NOMBRE"
                );

        PrologGenerationResult result =
                new PrologGenerationResult(
                        "user_amigos_prop(__A,name,__A_NOMBRE)",
                        List.of(column),
                        Map.of("a", "__A"),
                        Map.of("a.nombre", "__A_NOMBRE")
                );

        assertEquals(
                List.of("nombrePersona"),
                result.columnHeaders()
        );

        assertEquals(
                "a.nombre",
                result.columns().get(0).sourceExpression()
        );

        assertEquals(
                "__A_NOMBRE",
                result.columns().get(0).prologVariable()
        );
    }

    @Test
    void shouldSupportStatementsWithoutReturnColumns() {

        PrologGenerationResult result =
                new PrologGenerationResult(
                        "cy_create_node(amigos,['Persona'],[],__A)",
                        List.of(),
                        Map.of(),
                        Map.of()
                );

        assertFalse(result.hasOutputColumns());
        assertTrue(result.columnHeaders().isEmpty());
    }

    @Test
    void shouldPreserveVariableAndPropertyMappings() {

        Map<String, String> variables =
                new LinkedHashMap<>();

        variables.put("a", "__A");
        variables.put("b", "__B");

        Map<String, String> properties =
                new LinkedHashMap<>();

        properties.put("a.nombre", "__A_NOMBRE");

        PrologGenerationResult result =
                new PrologGenerationResult(
                        "true",
                        List.of(),
                        variables,
                        properties
                );

        assertEquals(variables, result.variableMappings());
        assertEquals(properties, result.propertyMappings());

        assertEquals(
                List.of("a", "b"),
                List.copyOf(result.variableMappings().keySet())
        );
    }

    @Test
    void shouldMakeDefensiveCopiesOfMappings() {

        Map<String, String> variables =
                new LinkedHashMap<>();

        Map<String, String> properties =
                new LinkedHashMap<>();

        variables.put("a", "__A");
        properties.put("a.nombre", "__A_NOMBRE");

        PrologGenerationResult result =
                new PrologGenerationResult(
                        "true",
                        List.of(),
                        variables,
                        properties
                );

        variables.put("b", "__B");
        properties.clear();

        assertEquals(
                Map.of("a", "__A"),
                result.variableMappings()
        );

        assertEquals(
                Map.of("a.nombre", "__A_NOMBRE"),
                result.propertyMappings()
        );
    }

    @Test
    void shouldMakeDefensiveCopiesOfColumns() {

        List<PrologGenerationResult.OutputColumn> columns =
                new ArrayList<>();

        columns.add(
                new PrologGenerationResult.OutputColumn(
                        "a.nombre",
                        "nombre",
                        "__A_NOMBRE"
                )
        );

        PrologGenerationResult result =
                new PrologGenerationResult(
                        "true",
                        columns,
                        Map.of(),
                        Map.of()
                );

        columns.clear();

        assertEquals(1, result.columns().size());
        assertEquals(
                List.of("nombre"),
                result.columnHeaders()
        );
    }

    @Test
    void shouldExposeImmutableCollections() {

        PrologGenerationResult result =
                new PrologGenerationResult(
                        "true",
                        List.of(
                                new PrologGenerationResult.OutputColumn(
                                        "a.nombre",
                                        "nombre",
                                        "__A_NOMBRE"
                                )
                        ),
                        Map.of("a", "__A"),
                        Map.of("a.nombre", "__A_NOMBRE")
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.columns().clear()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.variableMappings()
                        .put("b", "__B")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.propertyMappings().clear()
        );
    }

    @Test
    void shouldRejectNullAndBlankGoals() {

        assertThrows(
                NullPointerException.class,
                () -> new PrologGenerationResult(
                        null,
                        List.of(),
                        Map.of(),
                        Map.of()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PrologGenerationResult(
                        "   ",
                        List.of(),
                        Map.of(),
                        Map.of()
                )
        );
    }

    @Test
    void shouldRejectNullCollections() {

        assertThrows(
                NullPointerException.class,
                () -> new PrologGenerationResult(
                        "true",
                        null,
                        Map.of(),
                        Map.of()
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new PrologGenerationResult(
                        "true",
                        List.of(),
                        null,
                        Map.of()
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new PrologGenerationResult(
                        "true",
                        List.of(),
                        Map.of(),
                        null
                )
        );
    }

    @Test
    void shouldRejectInvalidOutputColumns() {

        assertThrows(
                NullPointerException.class,
                () -> new PrologGenerationResult.OutputColumn(
                        null,
                        "nombre",
                        "__A_NOMBRE"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PrologGenerationResult.OutputColumn(
                        "a.nombre",
                        "",
                        "__A_NOMBRE"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new PrologGenerationResult.OutputColumn(
                        "a.nombre",
                        "nombre",
                        "   "
                )
        );
    }
}
