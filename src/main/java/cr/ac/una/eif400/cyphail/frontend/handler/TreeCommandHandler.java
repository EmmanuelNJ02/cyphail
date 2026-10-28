
package cr.ac.una.eif400.cyphail.frontend.handler;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.visitor.AstTreePrinter;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalysisResult;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalyzer;
import cr.ac.una.eif400.cyphail.compiler.lexer.LexerException;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;

import java.util.Objects;

/**
 * National University of Costa Rica
 * School of Informatics
 *
 * Course: EIF400 - Programming Paradigms
 * Project: Cyphail
 * Sprint: P2
 * Group Code: 04-10am
 *
 * Class: TreeCommandHandler
 *
 * Description:
 *
 * Handles the .tree command in the Cyphail REPL.
 *
 * The command parses Cyphail statements into an
 * Abstract Syntax Tree (AST), performs static
 * semantic analysis, and displays the resulting
 * tree without executing the query.
 *
 * Sprint P2 preserves the AST visualization
 * functionality introduced in Sprint P1 and
 * extends it to support node and relationship
 * patterns, relationship properties, directions,
 * and graph update statements.
 *
 * Supported input forms:
 *
 * 1. Inline query without a semicolon:
 *
 *    .tree MATCH (p:Person) RETURN p.name
 *
 * 2. Inline query with a semicolon:
 *
 *    .tree MATCH (p:Person) RETURN p.name;
 *
 * 3. Multiline query:
 *
 *    .tree
 *    MATCH (p:Person)
 *    WHERE p.age > 18
 *    RETURN p.name;
 *
 * 4. Update statement without RETURN:
 *
 *    .tree
 *    CREATE (p:Person {name: 'Alice'});
 *
 * The optional terminating semicolon is removed
 * before passing the source to the Cyphail parser.
 *
 * This ensures compatibility with the semicolon
 * termination convention introduced in Sprint P2.
 *
 * All lexical, syntactic, and semantic errors
 * are reported in English.
 *
 * This handler does not execute queries, modify
 * graph data, or communicate with the MQI broker.
 *
 * Team Members:
 *
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
public final class TreeCommandHandler implements CommandHandler {

    // ============================================
    // COMMAND CONFIGURATION
    // ============================================

    private static final String TREE_COMMAND = ".tree";

    // ============================================
    // DEPENDENCIES
    // ============================================

    private final CyphailParser parser;
    private final SemanticAnalyzer analyzer;
    private final AstTreePrinter treePrinter;

    // ============================================
    // MULTILINE CAPTURE STATE
    // ============================================

    private final StringBuilder queryBuffer =
            new StringBuilder();

    private boolean awaitingQuery;

    // ============================================
    // PRODUCTION CONSTRUCTOR
    // ============================================

    /**
     * Creates the production tree command handler.
     *
     * Initializes the parser, semantic analyzer,
     * and AST visualization visitor.
     */
    public TreeCommandHandler() {
        this(
                new CyphailParser(),
                new SemanticAnalyzer(),
                new AstTreePrinter()
        );
    }

    // ============================================
    // DEPENDENCY-INJECTION CONSTRUCTOR
    // ============================================

    /**
     * Creates a tree command handler with
     * explicit collaborators.
     *
     * @param parser Cyphail query parser
     * @param analyzer static semantic analyzer
     * @param treePrinter AST visualization visitor
     */
    TreeCommandHandler(
            CyphailParser parser,
            SemanticAnalyzer analyzer,
            AstTreePrinter treePrinter
    ) {
        this.parser = Objects.requireNonNull(
                parser,
                "Parser cannot be null"
        );

        this.analyzer = Objects.requireNonNull(
                analyzer,
                "Analyzer cannot be null"
        );

        this.treePrinter = Objects.requireNonNull(
                treePrinter,
                "Tree printer cannot be null"
        );
    }

    // ============================================
    // COMMAND SUPPORT
    // ============================================

    /**
     * Determines whether this handler supports
     * the supplied REPL input.
     *
     * The handler accepts the .tree command,
     * inline .tree queries, and continuation
     * lines during multiline AST capture.
     *
     * The .exit command remains available
     * while a multiline query is being entered.
     *
     * @param input input received from the REPL
     * @return true if this handler can process
     *         the supplied input
     */
    @Override
    public boolean supports(String input) {
        Objects.requireNonNull(
                input,
                "Input cannot be null"
        );

        String normalized = input.trim();

        if (isTreeCommand(normalized)
                || isInlineTreeCommand(normalized)) {
            return true;
        }

        return awaitingQuery
                && !".exit".equalsIgnoreCase(normalized);
    }

    // ============================================
    // COMMAND EXECUTION
    // ============================================

    /**
     * Processes a .tree command or an additional
     * line of a multiline query.
     *
     * Query execution is not performed.
     * Only parsing, semantic validation,
     * and AST visualization are supported.
     *
     * @param input REPL input
     * @return true to keep the REPL running
     */
    @Override
    public boolean handle(String input) {
        Objects.requireNonNull(
                input,
                "Input cannot be null"
        );

        String normalized = input.trim();

        if (isTreeCommand(normalized)) {
            startCapture();
            return true;
        }

        if (isInlineTreeCommand(normalized)) {
            processInlineQuery(
                    extractInlineQuery(normalized)
            );

            return true;
        }

        if (!awaitingQuery) {
            return true;
        }

        appendQueryLine(input);
        processBufferedQuery(input);

        return true;
    }

    // ============================================
    // CAPTURE STATE
    // ============================================

    /**
     * Indicates whether the handler is waiting
     * for additional lines of a .tree query.
     *
     * @return true while multiline capture
     *         remains active
     */
    public boolean isAwaitingQuery() {
        return awaitingQuery;
    }

    // ============================================
    // COMMAND IDENTIFICATION
    // ============================================

    private boolean isTreeCommand(String input) {
        return TREE_COMMAND.equalsIgnoreCase(input);
    }

    private boolean isInlineTreeCommand(String input) {
        if (input.length() <= TREE_COMMAND.length()) {
            return false;
        }

        return input.regionMatches(
                true,
                0,
                TREE_COMMAND,
                0,
                TREE_COMMAND.length()
        ) && Character.isWhitespace(
                input.charAt(TREE_COMMAND.length())
        );
    }

    private String extractInlineQuery(String input) {
        return input.substring(
                TREE_COMMAND.length()
        ).trim();
    }

    // ============================================
    // MULTILINE CAPTURE
    // ============================================

    /**
     * Starts a new multiline AST capture.
     */
    private void startCapture() {
        queryBuffer.setLength(0);
        awaitingQuery = true;
    }

    /**
     * Appends a query line to the multiline buffer.
     *
     * @param input source line
     */
    private void appendQueryLine(String input) {
        if (!queryBuffer.isEmpty()) {
            queryBuffer.append(
                    System.lineSeparator()
            );
        }

        queryBuffer.append(input);
    }

    // ============================================
    // INLINE QUERY PROCESSING
    // ============================================

    /**
     * Processes a query supplied on the same
     * line as the .tree command.
     *
     * @param source Cyphail source statement
     */
    private void processInlineQuery(String source) {
        resetCapture();
        processQuery(source, true);
    }

    // ============================================
    // MULTILINE QUERY PROCESSING
    // ============================================

    /**
     * Processes buffered multiline input.
     *
     * A trailing semicolon explicitly marks
     * the end of a Sprint P2 statement.
     *
     * For backward compatibility with Sprint P1,
     * a completed RETURN clause may also trigger
     * AST visualization without a semicolon.
     *
     * @param lastLine most recently entered line
     */
    private void processBufferedQuery(String lastLine) {

        String source = queryBuffer.toString();

        boolean terminated =
                hasTerminatingSemicolon(source);

        boolean completedReturn =
                containsReturnClause(source)
                        && isLikelyFinalLine(lastLine);

        boolean finalAttempt =
                terminated || completedReturn;

        processQuery(
                source,
                finalAttempt
        );
    }

    // ============================================
    // PARSING AND VALIDATION
    // ============================================

    /**
     * Parses a Cyphail statement and displays
     * its AST after semantic validation.
     *
     * The optional statement terminator is
     * removed before invoking the parser.
     *
     * @param source original Cyphail source
     * @param finalAttempt whether the statement
     *                     should be considered complete
     */
    private void processQuery(
            String source,
            boolean finalAttempt
    ) {
        String normalizedSource =
                removeOptionalTerminatingSemicolon(source);

        try {
            Query query =
                    parser.parseOrThrow(normalizedSource);

            /*
             * Preserve the original Sprint P1
             * multiline behavior.
             *
             * Some partial MATCH or update
             * statements can be valid prefixes.
             *
             * Without an explicit terminator,
             * capture continues until a complete
             * RETURN clause is available.
             *
             * Sprint P2 statements ending with
             * a semicolon are processed even
             * when RETURN is absent.
             */
            if (!finalAttempt
                    && query.returnClause().isEmpty()) {
                return;
            }

            renderQuery(query);
            resetCapture();

        } catch (LexerException exception) {
            if (finalAttempt) {
                reportSyntaxError(
                        exception.getMessage()
                );

                resetCapture();
            }

        } catch (IllegalArgumentException exception) {
            if (finalAttempt) {
                reportSyntaxError(
                        exception.getMessage()
                );

                resetCapture();
            }
        }
    }

    // ============================================
    // SEMICOLON HANDLING
    // ============================================

    /**
     * Checks whether a statement ends with
     * a semicolon after trailing whitespace
     * has been removed.
     *
     * @param source Cyphail source
     * @return true if a semicolon terminates
     *         the statement
     */
    private boolean hasTerminatingSemicolon(
            String source
    ) {
        return source.stripTrailing().endsWith(";");
    }

    /**
     * Removes one optional trailing semicolon
     * before passing the statement to the parser.
     *
     * A semicolon occurring elsewhere in the
     * source is not removed.
     *
     * @param source original Cyphail statement
     * @return statement without its optional
     *         terminating semicolon
     */
    private String removeOptionalTerminatingSemicolon(
            String source
    ) {
        String normalized = source.stripTrailing();

        if (!normalized.endsWith(";")) {
            return normalized;
        }

        return normalized.substring(
                0,
                normalized.length() - 1
        ).stripTrailing();
    }

    // ============================================
    // AST VISUALIZATION
    // ============================================

    /**
     * Performs semantic validation and prints
     * the AST when no errors are found.
     *
     * @param query parsed Cyphail AST
     */
    private void renderQuery(Query query) {
        SemanticAnalysisResult analysis =
                analyzer.analyze(query);

        if (analysis.hasErrors()) {
            analysis.errors().forEach(
                    System.out::println
            );

            return;
        }

        System.out.println(
                treePrinter.print(query)
        );
    }

    // ============================================
    // MULTILINE COMPLETION DETECTION
    // ============================================

    /**
     * Detects whether the buffered source
     * contains a RETURN clause.
     *
     * @param source buffered query text
     * @return true if RETURN is present
     */
    private boolean containsReturnClause(String source) {
        return source.matches(
                "(?is).*\\bRETURN\\b.*"
        );
    }

    /**
     * Determines whether the most recent line
     * appears to complete a RETURN statement.
     *
     * @param line most recently entered line
     * @return true when the line appears complete
     */
    private boolean isLikelyFinalLine(String line) {
        String normalized = line.trim();

        if (normalized.isEmpty()) {
            return false;
        }

        if ("RETURN".equalsIgnoreCase(normalized)) {
            return false;
        }

        return !normalized.endsWith(",");
    }

    // ============================================
    // ERROR REPORTING
    // ============================================

    /**
     * Prints a syntax error in English.
     *
     * @param message detailed parser error
     */
    private void reportSyntaxError(String message) {
        System.out.println(
                "Syntax error: " + message
        );
    }

    // ============================================
    // CAPTURE RESET
    // ============================================

    /**
     * Clears the multiline buffer and exits
     * AST capture mode.
     */
    private void resetCapture() {
        queryBuffer.setLength(0);
        awaitingQuery = false;
    }
}
