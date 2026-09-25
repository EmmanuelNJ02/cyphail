package cr.ac.una.eif400.cyphail.ast;

import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.clause.MatchClause;
import cr.ac.una.eif400.cyphail.ast.clause.Projection;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionItem;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionModifiers;
import cr.ac.una.eif400.cyphail.ast.clause.ReturnClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetAllProperties;
import cr.ac.una.eif400.cyphail.ast.clause.SetClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetItem;
import cr.ac.una.eif400.cyphail.ast.clause.SetLabels;
import cr.ac.una.eif400.cyphail.ast.clause.SetProperty;
import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryOperator;
import cr.ac.una.eif400.cyphail.ast.expression.IntegerLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;
import cr.ac.una.eif400.cyphail.ast.expression.StringLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.NodePattern;
import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the Sprint P1 Cyphail AST model.
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
class QueryAstTest {

    @Test
    void buildsCaseNineStyleQuery() {
        NodePattern person = new NodePattern(
                Optional.of("p"),
                List.of("Person"),
                List.of()
        );

        NodePattern order = new NodePattern(
                Optional.of("o"),
                List.of("Order"),
                List.of(
                        new PatternProperty(
                                "personId",
                                new PropertyExpression(
                                        new VariableExpression("p"),
                                        "id"
                                )
                        ),
                        new PatternProperty(
                                "status",
                                new StringLiteralExpression("cancelled")
                        )
                )
        );

        MatchClause match = new MatchClause(
                new Pattern(List.of(person, order))
        );

        WhereClause where = new WhereClause(
                new BinaryExpression(
                        new PropertyExpression(
                                new VariableExpression("p"),
                                "age"
                        ),
                        BinaryOperator.GREATER_THAN,
                        new IntegerLiteralExpression(60)
                )
        );

        NodePattern archive = new NodePattern(
                Optional.of("a"),
                List.of("Archive"),
                List.of(
                        new PatternProperty(
                                "id",
                                new PropertyExpression(
                                        new VariableExpression("o"),
                                        "id"
                                )
                        ),
                        new PatternProperty(
                                "name",
                                new StringLiteralExpression("retired")
                        ),
                        new PatternProperty(
                                "year",
                                new IntegerLiteralExpression(2026)
                        )
                )
        );

        CreateClause create = new CreateClause(
                new Pattern(List.of(archive))
        );

        DeleteClause delete = new DeleteClause(
                false,
                List.of(new VariableExpression("o"))
        );

        ProjectionItem projectionItem = new ProjectionItem(
                new PropertyExpression(
                        new VariableExpression("p"),
                        "name"
                ),
                Optional.of("name")
        );

        ProjectionModifiers modifiers = new ProjectionModifiers(
                List.of(),
                Optional.empty(),
                Optional.empty()
        );

        ReturnClause returnClause = new ReturnClause(
                false,
                new Projection(
                        false,
                        List.of(projectionItem),
                        modifiers
                )
        );

        Query query = new Query(
                match,
                Optional.of(where),
                List.of(create, delete),
                Optional.of(returnClause)
        );

        assertEquals(2, query.match().pattern().nodes().size());
        assertTrue(query.where().isPresent());
        assertEquals(2, query.updates().size());
        assertInstanceOf(CreateClause.class, query.updates().get(0));
        assertInstanceOf(DeleteClause.class, query.updates().get(1));
        assertTrue(query.returnClause().isPresent());
        assertEquals(
                Optional.of("name"),
                query.returnClause()
                        .orElseThrow()
                        .projection()
                        .items()
                        .get(0)
                        .alias()
        );
    }

    @Test
    void requiresProjectionItemWhenWildcardIsAbsent() {
        ProjectionModifiers modifiers = new ProjectionModifiers(
                List.of(),
                Optional.empty(),
                Optional.empty()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Projection(
                        false,
                        List.of(),
                        modifiers
                )
        );
    }

    @Test
    void allowsWildcardProjectionWithoutExplicitItems() {
        ProjectionModifiers modifiers = new ProjectionModifiers(
                List.of(),
                Optional.empty(),
                Optional.empty()
        );

        assertDoesNotThrow(
                () -> new Projection(
                        true,
                        List.of(),
                        modifiers
                )
        );
    }

    @Test
    void supportsAllSetItemVariants() {
        SetItem setProperty = new SetProperty(
                new PropertyExpression(
                        new VariableExpression("p"),
                        "age"
                ),
                new IntegerLiteralExpression(31)
        );

        SetItem setAllProperties = new SetAllProperties(
                new VariableExpression("p"),
                new VariableExpression("data")
        );

        SetItem setLabels = new SetLabels(
                new VariableExpression("p"),
                List.of("Employee", "Manager")
        );

        SetClause setClause = new SetClause(
                List.of(
                        setProperty,
                        setAllProperties,
                        setLabels
                )
        );

        assertEquals(3, setClause.items().size());
        assertInstanceOf(SetProperty.class, setClause.items().get(0));
        assertInstanceOf(SetAllProperties.class, setClause.items().get(1));
        assertInstanceOf(SetLabels.class, setClause.items().get(2));
    }
}