
package cr.ac.una.eif400.cyphail.engine.mqi;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias del procesamiento de respuestas MQI.
 *
 * Proyecto: Cyphail - Sprint P2
 * Grupo: 04-10am
 *
 * Basadas en los formatos de respuesta del demo MQI
 * proporcionado por el profesor.
 */
class MqiResponseParserTest {

    @Test
    void shouldParseFailure() {
        PrologResult result = MqiResponseParser.parse("false");

        assertInstanceOf(PrologResult.Failure.class, result);
    }

    @Test
    void shouldParseSuccessWithoutVariables() {
        PrologResult result = MqiResponseParser.parse("true");

        var success = assertInstanceOf(
                PrologResult.Success.class, result
        );

        assertEquals(1, success.rows().size());
        assertTrue(success.rows().get(0).isEmpty());
    }

    @Test
    void shouldExtractRowsBinding() {
        String json = """
                {
                  "functor": "true",
                  "args": [
                    [
                      [
                        {
                          "functor": "=",
                          "args": [
                            "Rows",
                            "nombre edad\\nJorge 43"
                          ]
                        }
                      ]
                    ]
                  ]
                }
                """;

        var success = assertInstanceOf(
                PrologResult.Success.class,
                MqiResponseParser.parse(json)
        );

        assertEquals(1, success.rows().size());
        assertEquals(
                "nombre edad\nJorge 43",
                success.rows().get(0).get("Rows")
        );
    }

    @Test
    void shouldParseMultipleSolutions() {
        String json = """
                {
                  "functor": "true",
                  "args": [
                    [
                      [
                        {"functor":"=","args":["Name","Juan"]},
                        {"functor":"=","args":["Age",28]}
                      ],
                      [
                        {"functor":"=","args":["Name","Jorge"]},
                        {"functor":"=","args":["Age",43]}
                      ]
                    ]
                  ]
                }
                """;

        var success = assertInstanceOf(
                PrologResult.Success.class,
                MqiResponseParser.parse(json)
        );

        assertEquals(2, success.rows().size());

        assertEquals("Juan", success.rows().get(0).get("Name"));
        assertEquals(28L, success.rows().get(0).get("Age"));

        assertEquals("Jorge", success.rows().get(1).get("Name"));
        assertEquals(43L, success.rows().get(1).get("Age"));

        assertEquals(
                List.of("Name", "Age"),
                new ArrayList<>(success.rows().get(0).keySet())
        );
    }

    @Test
    void shouldPreserveNumericAndBooleanTypes() {
        String json = """
                {
                  "functor": "true",
                  "args": [
                    [
                      [
                        {"functor":"=","args":["Score",9.5]},
                        {"functor":"=","args":["Active",true]}
                      ]
                    ]
                  ]
                }
                """;

        var success = assertInstanceOf(
                PrologResult.Success.class,
                MqiResponseParser.parse(json)
        );

        var row = success.rows().get(0);

        assertEquals(9.5, (Double) row.get("Score"), 0.00001);
        assertEquals(Boolean.TRUE, row.get("Active"));
    }

    @Test
    void shouldParsePrologException() {
        String json = """
                {
                  "functor": "exception",
                  "args": [
                    {
                      "functor": "existence_error",
                      "args": ["procedure", "unknown/1"]
                    }
                  ]
                }
                """;

        var error = assertInstanceOf(
                PrologResult.PrologError.class,
                MqiResponseParser.parse(json)
        );

        var compound = assertInstanceOf(
                MqiResponseParser.Compound.class,
                error.term()
        );

        assertEquals("existence_error", compound.functor());
        assertEquals(
                List.of("procedure", "unknown/1"),
                compound.args()
        );
    }

    @Test
    void shouldRejectEmptyAndUnknownResponses() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MqiResponseParser.parse("")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> MqiResponseParser.parse(
                        "{\"functor\":\"unknown\",\"args\":[]}"
                )
        );
    }

    @Test
    void shouldRejectInvalidBinding() {
        String json = """
                {
                  "functor": "true",
                  "args": [
                    [
                      [
                        {
                          "functor": "invalid",
                          "args": ["X", 5]
                        }
                      ]
                    ]
                  ]
                }
                """;

        assertThrows(
                IllegalArgumentException.class,
                () -> MqiResponseParser.parse(json)
        );
    }
}
