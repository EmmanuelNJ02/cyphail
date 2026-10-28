
package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 *
 * Course: EIF400 - Paradigmas de Programacion
 * Project: Cyphail
 * Sprint: P2
 * Group Code: 04-10am
 *
 * Integration tests for real Cyphail query execution
 * using SWI-Prolog and MQI.
 *
 * These tests replace the simulated query engine
 * tests from Sprint P1.
 *
 * Tests cover:
 * 1. Real MATCH execution.
 * 2. Property filtering with WHERE.
 * 3. Relationship pattern execution.
 * 4. Queries without matching nodes.
 * 5. Semantic error handling before execution.
 *
 * Each test uses an independent MQI session.
 *
 * Team Members:
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 */
class PrologQueryHandlerIntegrationTest {

    private PrintStream originalOut;

    private PrintStream testOutput;

    private ByteArrayOutputStream output;

    private MqiBroker broker;

    private CommandRouter router;

    @BeforeEach
    void setUp() throws IOException {

        originalOut = System.out;

        output = new ByteArrayOutputStream();

        testOutput = new PrintStream(
                output,
                true,
                StandardCharsets.UTF_8
        );

        System.setOut(testOutput);

        Path engineFile = Path.of(
                "src",
                "main",
                "prolog",
                "engine.pl"
        ).toAbsolutePath().normalize();

        broker = MqiBroker.start(engineFile);

        router = new CommandRouter(broker);
    }

    @AfterEach
    void tearDown() throws IOException {

        System.setOut(originalOut);

        try {
            if (broker != null) {
                broker.close();
            }
        } finally {
            if (testOutput != null) {
                testOutput.close();
            }
        }
    }

    /**
     * Test 1:
     * Executes MATCH against the real Prolog graph.
     */
    @Test
    void shouldExecuteRealMatchQuery() {

        boolean shouldContinue = router.route(
                "MATCH (p:Person) RETURN p.name"
        );

        String result = consoleOutput();

        assertTrue(shouldContinue);

        assertTrue(
                result.contains("p.name")
        );

        assertTrue(
                result.contains("'Juan Perez'")
        );

        assertTrue(
                result.contains("'Jorge Soto'")
        );

        assertFalse(
                result.contains("MQI communication error:")
        );

        assertFalse(
                result.contains("Cyphail query error:")
        );

        assertFalse(
                result.contains("42 ms")
        );
    }

    /**
     * Test 2:
     * Verifies real filtering with WHERE.
     *
     * Only people older than 30 should appear.
     */
    @Test
    void shouldExecuteWhereCondition() {

        boolean shouldContinue = router.route(
                "MATCH (p:Person) "
                        + "WHERE p.age > 30 "
                        + "RETURN p.name, p.age"
        );

        String result = consoleOutput();

        assertTrue(shouldContinue);

        assertTrue(
                result.contains("p.name p.age")
        );

        assertTrue(
                result.contains("'Jorge Soto' 43")
        );

        // Juan Perez must not be returned by this filter.
        assertFalse(
                result.contains("'Juan Perez'")
        );

        assertFalse(
                result.contains("MQI communication error:")
        );

        assertFalse(
                result.contains("Cyphail query error:")
        );
    }

    /**
     * Test 3:
     * Executes a relationship pattern against
     * the original Amigos graph.
     */
    @Test
    void shouldExecuteRelationshipPattern() {

        boolean shouldContinue = router.route(
                "MATCH (a:Person)-[r:Amigo]->(b:Person) "
                        + "RETURN a.name, b.name"
        );

        String result = consoleOutput();

        assertTrue(shouldContinue);

        assertTrue(
                result.contains("a.name b.name")
        );

        assertTrue(
                result.contains("'Juan Perez'")
        );

        assertTrue(
                result.contains("'Jorge Soto'")
        );

        assertFalse(
                result.contains("MQI communication error:")
        );

        assertFalse(
                result.contains("Cyphail query error:")
        );
    }

    /**
     * Test 4:
     * An unknown label must not return
     * fabricated records.
     */
    @Test
    void shouldReturnNoRowsForUnknownLabel() {

        boolean shouldContinue = router.route(
                "MATCH (p:UnknownP2Label) RETURN p.name"
        );

        String result = consoleOutput();

        assertTrue(shouldContinue);

        assertTrue(
                result.contains("p.name")
        );

        assertFalse(
                result.contains("'Juan Perez'")
        );

        assertFalse(
                result.contains("'Jorge Soto'")
        );

        assertFalse(
                result.contains("MQI communication error:")
        );

        assertFalse(
                result.contains("Cyphail query error:")
        );
    }

    /**
     * Test 5:
     * A duplicated relationship variable
     * must produce a semantic error.
     *
     * The query handler should report the error
     * without producing a normal query table
     * or stopping the interactive session.
     */
    @Test
    void shouldRejectSemanticErrorBeforeExecution() {

        boolean shouldContinue = router.route(
                "MATCH (a:Person)-[r:Amigo]->(b:Person), "
                        + "(b)-[r:Amigo]->(a) "
                        + "RETURN a.name"
        );

        String result = consoleOutput();

        assertTrue(shouldContinue);

        assertTrue(
                result.contains("Semantic error:")
        );

        assertTrue(
                result.contains(
                        "Relationship variable 'r' "
                                + "is declared more than once"
                )
        );

        // No normal result table should be printed.
        assertFalse(
                result.contains("a.name")
        );

        // The error must not be confused with MQI
        // communication or Prolog execution errors.
        assertFalse(
                result.contains("MQI communication error:")
        );

        assertFalse(
                result.contains("Cyphail query error:")
        );
    }

    /**
     * Returns the captured console output.
     */
    private String consoleOutput() {

        return output.toString(
                StandardCharsets.UTF_8
        );
    }
}
