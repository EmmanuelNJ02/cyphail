package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.mqi.MqiResponseParser;
import cr.ac.una.eif400.cyphail.engine.mqi.PrologResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the presentation of preformatted Rows bindings
 * returned by SWI-Prolog through MQI.
 *
 * The Sprint P2 specification allows the Prolog engine
 * to return a Rows variable containing column headers
 * and data rows separated by newline characters.
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
class PrologRowsBindingTest {

    private ByteArrayOutputStream output;
    private PrologTablePrinter printer;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();

        printer = new PrologTablePrinter(
                new PrintStream(
                        output,
                        true,
                        StandardCharsets.UTF_8
                )
        );
    }

    @Test
    void shouldDisplayRowsBindingFromSprintP2Specification() {
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
                            "a.name b.name\\njuan  maria"
                          ]
                        }
                      ]
                    ]
                  ]
                }
                """;

        PrologResult result = MqiResponseParser.parse(json);

        printer.print(result);

        String text = consoleOutput();

        assertTrue(
                text.contains("a.name b.name\njuan  maria")
        );

        assertFalse(
                text.contains("\\njuan")
        );

        assertFalse(
                text.contains("| Rows ")
        );
    }

    @Test
    void shouldPreserveMultipleRowsAndSpacing() {
        String rows =
                "Name Age\n"
                        + "Ana  28\n"
                        + "Luis 35";

        PrologResult result = new PrologResult.Success(
                List.of(
                        Map.of("Rows", rows)
                )
        );

        printer.print(result);

        String text = consoleOutput();

        assertTrue(
                text.contains(rows)
        );

        assertFalse(
                text.contains("\\nAna")
        );

        assertFalse(
                text.contains("| Rows ")
        );
    }

    @Test
    void shouldPreserveNormalVariableBindingTables() {
        PrologResult result = new PrologResult.Success(
                List.of(
                        Map.of(
                                "Name", "Ana",
                                "Age", 28L
                        )
                )
        );

        printer.print(result);

        String text = consoleOutput();

        assertTrue(
                text.contains("Name")
        );

        assertTrue(
                text.contains("Age")
        );

        assertTrue(
                text.contains("Ana")
        );

        assertTrue(
                text.contains("28")
        );

        assertTrue(
                text.contains("Rows: 1")
        );
    }

    /**
     * Returns the text written by the table printer.
     *
     * @return captured UTF-8 console output
     */
    private String consoleOutput() {
        return output.toString(StandardCharsets.UTF_8);
    }
}