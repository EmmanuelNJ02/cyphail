
package cr.ac.una.eif400.cyphail.engine.mqi;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias del cliente MQI.
 *
 * Proyecto Cyphail - Sprint P2.
 * Grupo 04-10am.
 *
 * Verifica el protocolo de mensajes del demo MQI
 * proporcionado por el profesor.
 */
class MqiClientTest {

    /**
     * Construye un mensaje usando el formato MQI.
     */
    private static String frame(String message) {

        String payload = message + ".\n";

        int size = payload.getBytes(
                StandardCharsets.UTF_8
        ).length;

        return size + ".\n" + payload;
    }

    /**
     * Prepara un cliente con una respuesta simulada.
     */
    private static MqiClient clientWithResponse(
            String response,
            ByteArrayOutputStream sent) {

        byte[] input = frame(response).getBytes(
                StandardCharsets.UTF_8
        );

        return new MqiClient(
                new ByteArrayInputStream(input),
                sent
        );
    }

    @Test
    void shouldAuthenticateSuccessfully() throws IOException {

        var sent = new ByteArrayOutputStream();

        var client = clientWithResponse("true", sent);

        client.authenticate("test-password");

        assertEquals(
                frame("test-password"),
                sent.toString(StandardCharsets.UTF_8)
        );
    }

    @Test
    void shouldRejectAuthenticationFailure() {

        var sent = new ByteArrayOutputStream();

        var client = clientWithResponse("false", sent);

        assertThrows(
                IOException.class,
                () -> client.authenticate("incorrect-password")
        );
    }

    @Test
    void shouldExecuteGoalAndParseResult() throws IOException {

        String response = """
                {
                  "functor": "true",
                  "args": [
                    [
                      [
                        {
                          "functor": "=",
                          "args": ["X", 5]
                        }
                      ]
                    ]
                  ]
                }
                """;

        var sent = new ByteArrayOutputStream();

        var client = clientWithResponse(response, sent);

        PrologResult result = client.run("X is 2+3");

        var success = assertInstanceOf(
                PrologResult.Success.class,
                result
        );

        assertEquals(1, success.rows().size());

        assertEquals(
                5L,
                success.rows().get(0).get("X")
        );

        assertEquals(
                frame("run((X is 2+3), -1)"),
                sent.toString(StandardCharsets.UTF_8)
        );
    }

    @Test
    void shouldHandleQueryWithoutSolutions() throws IOException {

        var sent = new ByteArrayOutputStream();

        var client = clientWithResponse("false", sent);

        PrologResult result = client.run("1 > 10");

        assertInstanceOf(
                PrologResult.Failure.class,
                result
        );
    }

    @Test
    void shouldCalculateUtf8MessageLengthCorrectly()
            throws IOException {

        var sent = new ByteArrayOutputStream();

        var client = clientWithResponse("true", sent);

        client.run("member(X, ['María'])");

        assertEquals(
                frame("run((member(X, ['María'])), -1)"),
                sent.toString(StandardCharsets.UTF_8)
        );
    }

    @Test
    void shouldRejectInvalidGoals() {

        var sent = new ByteArrayOutputStream();

        var client = clientWithResponse("true", sent);

        assertThrows(
                IllegalArgumentException.class,
                () -> client.run("")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.run("X is 2+3.")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> client.run("X is 2+3", -2)
        );

        assertEquals(0, sent.size());
    }

    @Test
    void shouldRejectMalformedJsonResponse() {

        var sent = new ByteArrayOutputStream();

        var client = clientWithResponse(
                "not-valid-json",
                sent
        );

        assertThrows(
                IOException.class,
                () -> client.run("true")
        );
    }

    @Test
    void shouldDetectTruncatedMessage() {

        // La cabecera anuncia 50 bytes,
        // pero la respuesta contiene menos.
        byte[] incomplete = "50.\nfalse.\n".getBytes(
                StandardCharsets.UTF_8
        );

        var client = new MqiClient(
                new ByteArrayInputStream(incomplete),
                new ByteArrayOutputStream()
        );

        assertThrows(
                EOFException.class,
                () -> client.run("true")
        );
    }
}
