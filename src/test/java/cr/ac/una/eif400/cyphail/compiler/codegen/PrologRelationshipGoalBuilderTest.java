
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.expression.FloatLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.IntegerLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.StringLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipDirection;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipPattern;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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
 * Tests the generation of SWI-Prolog goals
 * for Cyphail relationship patterns.
 *
 * Covers relationship directions, types,
 * variables, properties and validation.
 */
class PrologRelationshipGoalBuilderTest {

    private final PrologRelationshipGoalBuilder builder =
            new PrologRelationshipGoalBuilder();

    private RelationshipPattern relationship(
            String name,
            RelationshipDirection direction
    ) {

        return new RelationshipPattern(
                Optional.of(name),
                List.of("TRABAJA_EN"),
                List.of(),
                direction
        );
    }

    @Test
    void shouldGenerateOutgoingRelationship() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        var result = builder.generate(
                relationship("r", RelationshipDirection.OUT),
                "__A",
                "__B",
                symbols
        );

        assertEquals("__R", result.variable());

        assertEquals(
                List.of(
                        "user_amigos_edge(__R, 'TRABAJA_EN', __A, __B)"
                ),
                result.goals()
        );
    }

    @Test
    void shouldGenerateIncomingRelationship() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        var result = builder.generate(
                relationship("r", RelationshipDirection.IN),
                "__A",
                "__B",
                symbols
        );

        assertEquals("__R", result.variable());

        assertEquals(
                List.of(
                        "user_amigos_edge(__R, 'TRABAJA_EN', __B, __A)"
                ),
                result.goals()
        );
    }

    @Test
    void shouldGenerateUndirectedRelationship() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        var result = builder.generate(
                relationship("r", RelationshipDirection.NONE),
                "__A",
                "__B",
                symbols
        );

        assertEquals(1, result.goals().size());

        String goal = result.goals().getFirst();

        assertTrue(
                goal.contains(
                        "user_amigos_edge(__R, 'TRABAJA_EN', __A, __B)"
                )
        );

        assertTrue(
                goal.contains(
                        "user_amigos_edge(__R, 'TRABAJA_EN', __B, __A)"
                )
        );

        assertTrue(goal.contains("dif(__A, __B)"));
        assertTrue(goal.contains(" ; "));
    }

    @Test
    void shouldGenerateTemporaryVariableForAnonymousRelationship() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.empty(),
                        List.of("CONOCE"),
                        List.of(),
                        RelationshipDirection.OUT
                );

        var result = builder.generate(
                relationship,
                "__A",
                "__B",
                symbols
        );

        assertEquals("__TMP_1", result.variable());

        assertEquals(
                List.of(
                        "user_amigos_edge(__TMP_1, 'CONOCE', __A, __B)"
                ),
                result.goals()
        );
    }

    @Test
    void shouldSupportRelationshipWithoutSpecifiedType() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.of("r"),
                        List.of(),
                        List.of(),
                        RelationshipDirection.OUT
                );

        var result = builder.generate(
                relationship,
                "__A",
                "__B",
                symbols
        );

        assertEquals("__R", result.variable());

        assertEquals(
                List.of(
                        "user_amigos_edge(__R, __TMP_1, __A, __B)"
                ),
                result.goals()
        );
    }

    @Test
    void shouldSupportMultipleRelationshipTypes() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.of("r"),
                        List.of("CONOCE", "TRABAJA_CON"),
                        List.of(),
                        RelationshipDirection.OUT
                );

        var result = builder.generate(
                relationship,
                "__A",
                "__B",
                symbols
        );

        assertEquals(2, result.goals().size());

        assertEquals(
                "member(__TMP_1, ['CONOCE', 'TRABAJA_CON'])",
                result.goals().get(0)
        );

        assertEquals(
                "user_amigos_edge(__R, __TMP_1, __A, __B)",
                result.goals().get(1)
        );
    }

    @Test
    void shouldGenerateStringPropertyConstraint() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.of("r"),
                        List.of("TRABAJA_EN"),
                        List.of(
                                new PatternProperty(
                                        "rol",
                                        new StringLiteralExpression("Dev")
                                )
                        ),
                        RelationshipDirection.OUT
                );

        var result = builder.generate(
                relationship,
                "__A",
                "__B",
                symbols
        );

        assertEquals(3, result.goals().size());

        assertEquals(
                "user_amigos_prop(__R, 'rol', __R_ROL)",
                result.goals().get(1)
        );

        assertEquals(
                "cy_eq(__R_ROL, 'Dev')",
                result.goals().get(2)
        );

        assertEquals(
                "__R_ROL",
                symbols.propertyMappings().get("r.rol")
        );
    }

    @Test
    void shouldGenerateIntegerAndDecimalProperties() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.of("r"),
                        List.of("EVALUA"),
                        List.of(
                                new PatternProperty(
                                        "anio",
                                        new IntegerLiteralExpression(2026)
                                ),
                                new PatternProperty(
                                        "nota",
                                        new FloatLiteralExpression(9.5)
                                )
                        ),
                        RelationshipDirection.OUT
                );

        var result = builder.generate(
                relationship,
                "__A",
                "__B",
                symbols
        );

        assertTrue(
                result.goals().contains(
                        "cy_eq(__R_ANIO, 2026)"
                )
        );

        assertTrue(
                result.goals().contains(
                        "cy_eq(__R_NOTA, 9.5)"
                )
        );
    }

    @Test
    void shouldGeneratePropertyForAnonymousRelationship() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.empty(),
                        List.of("TRABAJA_EN"),
                        List.of(
                                new PatternProperty(
                                        "rol",
                                        new StringLiteralExpression("Dev")
                                )
                        ),
                        RelationshipDirection.OUT
                );

        var result = builder.generate(
                relationship,
                "__A",
                "__B",
                symbols
        );

        assertEquals("__TMP_1", result.variable());

        assertTrue(
                result.goals().contains(
                        "user_amigos_prop(__TMP_1, 'rol', __TMP_2)"
                )
        );

        assertTrue(
                result.goals().contains(
                        "cy_eq(__TMP_2, 'Dev')"
                )
        );
    }

    @Test
    void shouldEscapeQuotesInPrologAtoms() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        RelationshipPattern relationship =
                new RelationshipPattern(
                        Optional.of("r"),
                        List.of("MANAGER'S"),
                        List.of(
                                new PatternProperty(
                                        "descripcion",
                                        new StringLiteralExpression(
                                                "Developer's role"
                                        )
                                )
                        ),
                        RelationshipDirection.OUT
                );

        var result = builder.generate(
                relationship,
                "__A",
                "__B",
                symbols
        );

        assertTrue(
                result.goals().getFirst().contains(
                        "'MANAGER''S'"
                )
        );

        assertTrue(
                result.goals().contains(
                        "cy_eq(__R_DESCRIPCION, 'Developer''s role')"
                )
        );
    }

    @Test
    void shouldExposeImmutableGeneratedGoals() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        var result = builder.generate(
                relationship("r", RelationshipDirection.OUT),
                "__A",
                "__B",
                symbols
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.goals().add("true")
        );
    }

    @Test
    void shouldRejectInvalidInputs() {

        PrologVariableTable symbols =
                new PrologVariableTable();

        RelationshipPattern relationship =
                relationship(
                        "r",
                        RelationshipDirection.OUT
                );

        assertThrows(
                NullPointerException.class,
                () -> builder.generate(
                        null,
                        "__A",
                        "__B",
                        symbols
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> builder.generate(
                        relationship,
                        "__A",
                        "__B",
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.generate(
                        relationship,
                        "A",
                        "__B",
                        symbols
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.generate(
                        relationship,
                        "__A",
                        "invalid-name",
                        symbols
                )
        );
    }
}
