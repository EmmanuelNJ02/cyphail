package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.visitor.AstTreePrinter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CREATE statements without a preceding MATCH clause.
 *
 * Sprint P2 allows standalone CREATE statements, including
 * node patterns, relationship patterns and optional RETURN clauses.
 *
 * Existing MATCH-based queries must remain compatible.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * School: School of Informatics
 * Group Code: 04-10am
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
class CyphailParserP2CreateTest {

    private final CyphailParser parser = new CyphailParser();

    @Test
    void shouldParseCreateNodeWithoutMatch() {
        Query query = parser.parseOrThrow(
                "CREATE (a:Persona {nombre: 'Marta'})"
        );

        assertEquals(1, query.updates().size());

        assertInstanceOf(
                CreateClause.class,
                query.updates().getFirst()
        );

        assertTrue(query.returnClause().isEmpty());
    }

    @Test
    void shouldParseCreateWithReturnWithoutMatch() {
        Query query = parser.parseOrThrow(
                "CREATE (a:Persona {nombre: 'Marta'}) RETURN a"
        );

        assertEquals(1, query.updates().size());

        assertInstanceOf(
                CreateClause.class,
                query.updates().getFirst()
        );

        assertTrue(query.returnClause().isPresent());
    }

    @Test
    void shouldParseCreateRelationshipWithoutMatch() {
        String source = """
                CREATE (a:Persona {nombre: 'Marta'})
                       -[r:TRABAJA_EN {rol: 'Dev'}]->
                       (e:Empresa {nombre: 'UCR'})
                RETURN a, r, e
                """;

        Query query = parser.parseOrThrow(source);

        CreateClause create = assertInstanceOf(
                CreateClause.class,
                query.updates().getFirst()
        );

        assertEquals(
                1,
                create.pattern().paths().size()
        );

        assertTrue(
                create.pattern()
                        .paths()
                        .getFirst()
                        .hasRelationships()
        );

        assertTrue(query.returnClause().isPresent());
    }

    @Test
    void shouldDisplayCreateWithoutMatchInTree() {
        Query query = parser.parseOrThrow(
                "CREATE (a:Persona {nombre: 'Marta'}) RETURN a"
        );

        String tree = new AstTreePrinter().print(query);

        assertTrue(tree.contains("Create"));
        assertTrue(tree.contains("Persona"));
        assertTrue(tree.contains("Marta"));
    }

    @Test
    void shouldPreserveMatchAndCreateCompatibility() {
        Query query = parser.parseOrThrow(
                "MATCH (a:Persona) CREATE (b:Persona) RETURN a, b"
        );

        assertNotNull(query.match());

        assertEquals(1, query.updates().size());

        assertInstanceOf(
                CreateClause.class,
                query.updates().getFirst()
        );
    }

    @Test
    void shouldRejectStandaloneReturnWithoutMatchOrCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseOrThrow("RETURN a")
        );
    }
}