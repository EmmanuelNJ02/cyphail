
package cr.ac.una.eif400.cyphail.frontend.handler;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalysisResult;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalyzer;
import cr.ac.una.eif400.cyphail.compiler.codegen.PrologCodeGenerator;
import cr.ac.una.eif400.cyphail.compiler.codegen.PrologGenerationResult;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;
import cr.ac.una.eif400.cyphail.engine.mqi.MqiBroker;
import cr.ac.una.eif400.cyphail.engine.mqi.PrologResult;
import cr.ac.una.eif400.cyphail.frontend.PrologTablePrinter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 *
 * Course: EIF400 - Paradigmas de Programacion
 * Project: Cyphail - Sprint P2
 * Group: 04-10am
 *
 * Executes Cyphail statements through the real
 * SWI-Prolog engine using MQI.
 *
 * Pipeline:
 *
 * Source -> Parser -> AST -> Semantic Analyzer
 *        -> Prolog CodeGen -> MQI -> Result Table
 *
 * The MQI connection is managed by the REPL.
 *
 * This handler reuses that connection and does
 * not create or terminate the Prolog process.
 */
public final class PrologQueryHandler
        implements CommandHandler {

    private static final String ROWS_BINDING = "Rows";

    private final MqiBroker broker;

    private final CyphailParser parser;

    private final SemanticAnalyzer analyzer;

    private final PrologCodeGenerator generator;

    private final PrologTablePrinter tablePrinter;

    /**
     * Creates a query handler using an existing
     * authenticated MQI connection.
     *
     * @param broker active MQI broker
     */
    public PrologQueryHandler(MqiBroker broker) {

        this.broker = Objects.requireNonNull(
                broker,
                "MQI broker cannot be null"
        );

        this.parser = new CyphailParser();

        this.analyzer = new SemanticAnalyzer();

        this.generator = new PrologCodeGenerator();

        this.tablePrinter = new PrologTablePrinter();
    }

    /**
     * Accepts nonempty Cyphail statements.
     *
     * REPL commands beginning with a dot are
     * processed by their respective handlers.
     *
     * @param input source statement
     * @return true for Cyphail source input
     */
    @Override
    public boolean supports(String input) {

        if (input == null || input.isBlank()) {
            return false;
        }

        return !input.stripLeading().startsWith(".");
    }

    /**
     * Parses, validates, compiles and executes
     * one Cyphail statement.
     *
     * Errors are reported without closing
     * the interactive REPL session.
     *
     * @param input Cyphail source statement
     * @return true to continue the REPL
     */
    @Override
    public boolean handle(String input) {

        Objects.requireNonNull(
                input,
                "Cyphail source cannot be null"
        );

        try {

            // =====================================
            // STEP 1 - PARSE CYPHAIL SOURCE
            // =====================================

            Query query = parser.parseOrThrow(input);

            // =====================================
            // STEP 2 - SEMANTIC VALIDATION
            // =====================================

            SemanticAnalysisResult analysis =
                    analyzer.analyze(query);

            if (analysis.hasErrors()) {

                for (String error : analysis.errors()) {

                    System.out.println(
                            "Semantic error: " + error
                    );
                }

                System.out.println();

                return true;
            }

            // =====================================
            // STEP 3 - GENERATE PROLOG GOAL
            // =====================================

            PrologGenerationResult generated =
                    generator.generate(query);

            String goal = generated.goal();

            // =====================================
            // STEP 4 - EXECUTE THROUGH MQI
            // =====================================

            PrologResult result =
                    broker.execute(goal);

            // =====================================
            // STEP 5 - NORMALIZE QUERY RESULT
            // =====================================

            /*
             * The generated Prolog goal may expose
             * internal bindings such as:
             *
             * __QueryRef
             * __P
             * __P_NAME
             *
             * These variables are implementation
             * details and should not be displayed
             * as Cyphail result columns.
             *
             * The Rows binding already contains
             * the formatted query output from
             * cy_exec_query/3 in engine.pl.
             *
             * Keep only Rows when that binding
             * contains the preformatted table.
             *
             * Other MQI result formats are
             * preserved without modification.
             */

            PrologResult printableResult =
                    normalizeResult(result);

            // =====================================
            // STEP 6 - PRINT REAL QUERY RESULTS
            // =====================================

            tablePrinter.print(printableResult);

        } catch (IOException exception) {

            System.out.println(
                    "MQI communication error: "
                            + exception.getMessage()
            );

            System.out.println();

        } catch (RuntimeException exception) {

            String message = exception.getMessage();

            System.out.println(
                    "Cyphail query error: "
                            + (
                                message == null
                                        ? exception.getClass()
                                                .getSimpleName()
                                        : message
                            )
            );

            System.out.println();
        }

        return true;
    }

    /**
     * Removes internal Prolog bindings from
     * successful preformatted query results.
     *
     * When all returned solutions contain a
     * string-valued Rows binding, only that
     * binding is retained.
     *
     * This allows PrologTablePrinter to display
     * the actual multiline output without
     * showing internal compiler variables.
     *
     * Failure, PrologError, empty results and
     * ordinary variable-binding results remain
     * unchanged.
     *
     * @param result original MQI result
     * @return result ready for presentation
     */
    private PrologResult normalizeResult(
            PrologResult result
    ) {

        if (!(result instanceof PrologResult.Success success)) {
            return result;
        }

        List<Map<String, Object>> originalRows =
                success.rows();

        if (originalRows.isEmpty()) {
            return result;
        }

        // Check the complete result before modifying it.

        for (Map<String, Object> row : originalRows) {

            if (!(row.get(ROWS_BINDING) instanceof String)) {
                return result;
            }
        }

        // Preserve every solution's preformatted table
        // while excluding the internal Prolog variables.

        List<Map<String, Object>> normalizedRows =
                new ArrayList<>();

        for (Map<String, Object> row : originalRows) {

            String table =
                    (String) row.get(ROWS_BINDING);

            normalizedRows.add(
                    Map.of(ROWS_BINDING, table)
            );
        }

        return new PrologResult.Success(
                List.copyOf(normalizedRows)
        );
    }
}
