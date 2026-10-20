package cr.ac.una.eif400.cyphail.frontend;

import cr.ac.una.eif400.cyphail.engine.mqi.MqiResponseParser;
import cr.ac.una.eif400.cyphail.engine.mqi.PrologResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Sprint P2 Prolog result table formatting.
 *
 * Verifies successful queries, multiple rows, column
 * ordering, missing bindings, primitive and compound
 * values, empty results, failures and Prolog errors.
 *
 * These tests use the real MQI result model without
 * relying on Sprint P1 fake query data.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * University: Universidad Nacional de Costa Rica
 * School: Escuela de Informatica
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Nunez Jimenez
 * Valery Alfaro Morales
 * Roy Arias Mejia
 * Keynell Molina Mora
 * Julissa Solano Valverde
 *
 * @author Emmanuel Nunez Jimenez
 * @author Valery Alfaro Morales
 * @author Roy Arias Mejia
 * @author Keynell Molina Mora
 * @author Julissa Solano Valverde
 */
class PrologTablePrinterTest {

    private ByteArrayOutputStream output;
    private PrologTablePrinter printer;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();

        PrintStream stream = new PrintStream(
                output,
                true,
                StandardCharsets.UTF_8
        );

        printer = new PrologTablePrinter(stream);
    }

    @Test
    void shouldPrintSingleSuccessfulRow() {
        PrologResult result = new PrologResult.Success(
                List.of(Map.of("Name", "Ana"))
        );

        printer.print(result);

        String text = consoleOutput();

        assertTrue(text.contains("| Name |"));
        assertTrue(text.contains("| Ana  |"));
        assertTrue(text.contains("Rows: 1"));
    }

    @Test
    void shouldPrintMultipleRowsAndCorrectCount() {
        PrologResult result = new PrologResult.Success(
                List.of(
                        Map.of("Name", "Ana"),
                        Map.of("Name", "Luis")
                )
        );

        printer.print(result);

        String text = consoleOutput();

        assertTrue(text.contains("Ana"));
        assertTrue(text.contains("Luis"));
        assertTrue(text.contains("Rows: 2"));

        assertTrue(text.contains("+------+"));
    }

    @Test
    void shouldPreserveBindingColumnOrder() {
        Map<String, Object> row = new LinkedHashMap<>();

        row.put("Name", "Ana");
        row.put("Age", 28L);
        row.put("City", "Heredia");

        printer.print(
                new PrologResult.Success(List.of(row))
        );

        String text = consoleOutput();

        int namePosition = text.indexOf("Name");
        int agePosition = text.indexOf("Age");
        int cityPosition = text.indexOf("City");

        assertTrue(namePosition >= 0);
        assertTrue(agePosition > namePosition);
        assertTrue(cityPosition > agePosition);
    }

    @Test
    void shouldCollectColumnsFromAllRows() {
        Map<String, Object> firstRow =
                new LinkedHashMap<>();

        firstRow.put("Name", "Ana");

        Map<String, Object> secondRow =
                new LinkedHashMap<>();

        secondRow.put("Age", 35L);

        printer.print(
                new PrologResult.Success(
                        List.of(firstRow, secondRow)
                )
        );

        String text = consoleOutput();

        assertTrue(text.contains("Name"));
        assertTrue(text.contains("Age"));
        assertTrue(text.contains("Ana"));
        assertTrue(text.contains("35"));
        assertTrue(text.contains("Rows: 2"));
    }

    @Test
    void shouldFormatNumbersAndBooleanValues() {
        Map<String, Object> row = new LinkedHashMap<>();

        row.put("Price", 19.99);
        row.put("Quantity", 5L);
        row.put("Available", true);

        printer.print(
                new PrologResult.Success(List.of(row))
        );

        String text = consoleOutput();

        assertTrue(text.contains("19.99"));
        assertTrue(text.contains("5"));
        assertTrue(text.contains("true"));
        assertTrue(text.contains("Rows: 1"));
    }

    @Test
    void shouldPrintNullValues() {
        Map<String, Object> row = new LinkedHashMap<>();

        row.put("Name", null);

        printer.print(
                new PrologResult.Success(List.of(row))
        );

        String text = consoleOutput();

        assertTrue(text.contains("| Name |"));
        assertTrue(text.contains("| null |"));
    }

    @Test
    void shouldHandleSuccessWithNoRows() {
        printer.print(
                new PrologResult.Success(List.of())
        );

        String text = consoleOutput();

        assertTrue(
                text.contains(
                        "Query completed. No rows returned."
                )
        );
    }

    @Test
    void shouldHandleSuccessWithoutVariableBindings() {
        printer.print(
                new PrologResult.Success(
                        List.of(Map.of(), Map.of())
                )
        );

        String text = consoleOutput();

        assertTrue(
                text.contains(
                        "Query succeeded. Solutions: 2"
                )
        );

        assertTrue(
                text.contains("no variable bindings")
        );
    }

    @Test
    void shouldReportQueryWithoutSolutions() {
        printer.print(
                new PrologResult.Failure()
        );

        String text = consoleOutput();

        assertTrue(text.contains("No solutions."));

        assertFalse(text.contains("Rows:"));
    }

    @Test
    void shouldReportPrologError() {
        MqiResponseParser.Compound error =
                new MqiResponseParser.Compound(
                        "error",
                        List.of("bad_query")
                );

        printer.print(
                new PrologResult.PrologError(error)
        );

        String text = consoleOutput();

        assertTrue(
                text.contains(
                        "Prolog error: error(bad_query)"
                )
        );
    }

    @Test
    void shouldFormatCompoundAndListValues() {
        MqiResponseParser.Compound compound =
                new MqiResponseParser.Compound(
                        "edge",
                        List.of(4L, 9L)
                );

        Map<String, Object> row = new LinkedHashMap<>();

        row.put("Relationship", compound);
        row.put("Values", List.of(1L, compound));

        printer.print(
                new PrologResult.Success(List.of(row))
        );

        String text = consoleOutput();

        assertTrue(text.contains("edge(4, 9)"));

        assertTrue(
                text.contains("[1, edge(4, 9)]")
        );
    }

    @Test
    void shouldFormatCompoundWithoutArguments() {
        MqiResponseParser.Compound compound =
                new MqiResponseParser.Compound(
                        "active",
                        List.of()
                );

        printer.print(
                new PrologResult.Success(
                        List.of(Map.of("Status", compound))
                )
        );

        String text = consoleOutput();

        assertTrue(text.contains("active"));
        assertTrue(text.contains("Rows: 1"));
    }

    @Test
    void shouldEscapeControlCharactersInCells() {
        String original =
                "first\nsecond\tthird\rend";

        printer.print(
                new PrologResult.Success(
                        List.of(Map.of("Text", original))
                )
        );

        String text = consoleOutput();

        assertTrue(
                text.contains(
                        "first\\nsecond\\tthird\\rend"
                )
        );

        assertTrue(text.contains("Rows: 1"));
    }

    @Test
    void shouldRejectNullResult() {
        assertThrows(
                NullPointerException.class,
                () -> printer.print(null)
        );
    }

    @Test
    void shouldRejectNullOutputStream() {
        assertThrows(
                NullPointerException.class,
                () -> new PrologTablePrinter(null)
        );
    }

    /**
     * Retrieves the output produced by the printer.
     *
     * @return captured UTF-8 text
     */
    private String consoleOutput() {
        return output.toString(StandardCharsets.UTF_8);
    }
}