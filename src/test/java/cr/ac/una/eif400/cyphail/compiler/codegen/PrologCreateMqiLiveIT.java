
package cr.ac.una.eif400.cyphail.compiler.codegen;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;
import cr.ac.una.eif400.cyphail.engine.mqi.PrologResult;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
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
 * Keynell Molina Mora
 * Roy Arias Mejia
 *
 * Real SWI-Prolog and MQI integration tests
 * for Cyphail CREATE statements.
 *
 * Covered functionality:
 * - Standalone CREATE with RETURN.
 * - Standalone CREATE without RETURN.
 * - CREATE relationships and properties.
 * - MATCH followed by CREATE.
 * - Persistence across queries in one session.
 *
 * Each test starts a separate Prolog process.
 *
 * Run explicitly with:
 * mvn -Dtest=PrologCreateMqiLiveIT test
 */
class PrologCreateMqiLiveIT {

    private final CyphailParser parser =
            new CyphailParser();

    private final PrologCodeGenerator generator =
            new PrologCodeGenerator();

    /**
     * Locates the real SWI-Prolog engine.
     */
    private static Path enginePath() {

        Path path = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath().normalize();

        assertTrue(
                Files.isRegularFile(path),
                "The SWI-Prolog engine must exist."
        );

        return path;
    }

    /**
     * Parses Cyphail source and generates
     * the corresponding executable Prolog goal.
     */
    private String compile(String source) {

        Query query = parser.parseOrThrow(source);

        PrologGenerationResult result =
                generator.generate(query);

        assertNotNull(result);
        assertNotNull(result.goal());
        assertFalse(result.goal().isBlank());

        return result.goal();
    }

    /**
     * Executes a generated query through MQI
     * and obtains its formatted result table.
     */
    private static String execute(
            MqiBroker broker,
            String goal
    ) throws Exception {

        PrologResult result = broker.execute(goal);

        PrologResult.Success success =
                assertInstanceOf(
                        PrologResult.Success.class,
                        result,
                        "The Prolog goal must succeed. Actual: "
                                + result
                );

        assertEquals(
                1,
                success.rows().size(),
                "The generated query must return one table."
        );

        Map<String, Object> bindings =
                success.rows().getFirst();

        assertTrue(
                bindings.containsKey("Rows"),
                "The response must contain Rows."
        );

        return assertInstanceOf(
                String.class,
                bindings.get("Rows")
        );
    }

    /**
     * Compiles and executes a Cyphail statement
     * using an existing MQI session.
     */
    private String run(
            MqiBroker broker,
            String source
    ) throws Exception {

        return execute(
                broker,
                compile(source)
        );
    }

    /**
     * Standalone CREATE must create a node,
     * return its identifier and preserve it
     * for subsequent MATCH queries.
     */
    @Test
    void shouldCreateNodeAndFindItLater()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String created = run(
                    broker,
                    """
                    CREATE (p:Person {
                        name: 'P2_Lucia_Create',
                        age: 21
                    })
                    RETURN p
                    """
            );

            assertTrue(
                    created.startsWith("p\n"),
                    "CREATE must return the p column."
            );

            assertTrue(
                    created.contains("cy_node_"),
                    "CREATE must return a generated node ID."
            );

            String found = run(
                    broker,
                    """
                    MATCH (p:Person {
                        name: 'P2_Lucia_Create'
                    })
                    RETURN p.name AS nombre,
                           p.age AS edad
                    """
            );

            assertEquals(
                    "nombre edad\n'P2_Lucia_Create' 21",
                    found,
                    "The created node must be queryable."
            );
        }
    }

    /**
     * CREATE without RETURN must still execute
     * the mutation and persist the new node.
     */
    @Test
    void shouldCreateNodeWithoutReturn()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            run(
                    broker,
                    """
                    CREATE (p:Person {
                        name: 'P2_Sin_Return'
                    })
                    """
            );

            String found = run(
                    broker,
                    """
                    MATCH (p:Person {
                        name: 'P2_Sin_Return'
                    })
                    RETURN p.name
                    """
            );

            assertEquals(
                    "p.name\n'P2_Sin_Return'",
                    found,
                    "CREATE without RETURN must persist the node."
            );
        }
    }

    /**
     * CREATE must persist both endpoint nodes
     * and their directed relationship.
     *
     * The relationship must also preserve
     * its property value.
     */
    @Test
    void shouldCreateRelationshipAndFindItLater()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String created = run(
                    broker,
                    """
                    CREATE (a:Person {
                        name: 'P2_Ana_Create'
                    })
                    -[r:Amigo {since: 2026}]->
                    (b:Person {
                        name: 'P2_Luis_Create'
                    })
                    RETURN a, r, b
                    """
            );

            assertTrue(
                    created.startsWith("a r b\n"),
                    "CREATE must return node and edge identifiers."
            );

            assertTrue(
                    created.contains("cy_edge_"),
                    "The relationship must receive an ID."
            );

            String found = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'P2_Ana_Create'
                    })
                    -[r:Amigo]->
                    (b:Person {
                        name: 'P2_Luis_Create'
                    })
                    RETURN a.name AS origen,
                           r.since AS desde,
                           b.name AS destino
                    """
            );

            assertEquals(
                    "origen desde destino\n"
                            + "'P2_Ana_Create' 2026 'P2_Luis_Create'",
                    found,
                    "The relationship and its property must persist."
            );
        }
    }

    /**
     * A CREATE clause following MATCH must
     * reuse the matched node's property value.
     *
     * The new Certificate must remain available
     * to a later MATCH query.
     */
    @Test
    void shouldCreateNodeUsingMatchedProperty()
            throws Exception {

        try (MqiBroker broker =
                     MqiBroker.start(enginePath())) {

            String created = run(
                    broker,
                    """
                    MATCH (a:Person {
                        name: 'Juan Perez'
                    })
                    CREATE (c:Certificate {
                        issuedTo: a.name,
                        year: 2026
                    })
                    RETURN c
                    """
            );

            assertTrue(
                    created.startsWith("c\n"),
                    "The created certificate must be returned."
            );

            assertTrue(
                    created.contains("cy_node_"),
                    "The certificate must have a generated ID."
            );

            String found = run(
                    broker,
                    """
                    MATCH (c:Certificate {
                        issuedTo: 'Juan Perez'
                    })
                    RETURN c.issuedTo AS titular,
                           c.year AS anio
                    """
            );

            assertEquals(
                    "titular anio\n'Juan Perez' 2026",
                    found,
                    "CREATE must reuse and persist MATCH properties."
            );
        }
    }
}
