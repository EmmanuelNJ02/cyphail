package cr.ac.una.eif400.cyphail.frontend.handler;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.visitor.AstTreePrinter;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalysisResult;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalyzer;
import cr.ac.una.eif400.cyphail.compiler.lexer.LexerException;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;

import java.util.Objects;

/**
 * Handles the Sprint P1 .tree REPL command.
 *
 * <p>The command supports two input forms:</p>
 *
 * <pre>
 * .tree MATCH (m:Movie) RETURN m.title
 * </pre>
 *
 * <p>and multiline capture:</p>
 *
 * <pre>
 * .tree
 * MATCH (m:Movie)
 * RETURN m.title
 * </pre>
 *
 * <p>The query is parsed into the Cyphail AST, analyzed semantically
 * and, when valid, rendered by the AST tree visitor. Lexical and
 * syntactic errors are reported directly in the REPL.</p>
 *
 * <p>Undefined-variable problems are reported in English as required
 * by the Sprint P1 specification.</p>
 *
 * <p>Project: Cyphail - Sprint P1<br>
 * Course: EIF400 - Paradigmas de Programacion<br>
 * Universidad Nacional de Costa Rica - Escuela de Informatica<br>
 * Group: 04-10am</p>
 *
 * @author Emmanuel Nunez Jimenez
 * @author Keynell Molina Mora
 * @author Valery Alfaro Morales
 * @author Julissa Solano Valverde
 * @author Roy Arias Mejia
 */
public final class TreeCommandHandler implements CommandHandler {

    private static final String TREE_COMMAND = ".tree";

    private final CyphailParser parser;
    private final SemanticAnalyzer analyzer;
    private final AstTreePrinter treePrinter;

    private final StringBuilder queryBuffer =
            new StringBuilder();

    private boolean awaitingQuery;

    /**
     * Creates the production .tree handler.
     */
    public TreeCommandHandler() {
        this(
                new CyphailParser(),
                new SemanticAnalyzer(),
                new AstTreePrinter()
        );
    }

    /**
     * Creates a .tree handler with explicit collaborators.
     *
     * @param parser query parser
     * @param analyzer semantic analyzer
     * @param treePrinter AST tree visitor
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

    /**
     * Indicates whether the handler is currently waiting for
     * query lines after .tree.
     *
     * @return true while a .tree query is being captured
     */
    public boolean isAwaitingQuery() {
        return awaitingQuery;
    }

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

    private void startCapture() {
        queryBuffer.setLength(0);
        awaitingQuery = true;
    }

    private void appendQueryLine(String input) {
        if (!queryBuffer.isEmpty()) {
            queryBuffer.append(
                    System.lineSeparator()
            );
        }

        queryBuffer.append(input);
    }

    private void processInlineQuery(String source) {
        resetCapture();
        processQuery(source, true);
    }

    private void processBufferedQuery(String lastLine) {
        boolean finalAttempt =
                containsReturnClause(queryBuffer.toString())
                        && isLikelyFinalLine(lastLine);

        processQuery(
                queryBuffer.toString(),
                finalAttempt
        );
    }

    private void processQuery(
            String source,
            boolean finalAttempt
    ) {
        try {
            Query query = parser.parseOrThrow(source);

            /*
             * In multiline mode MATCH, WHERE and update clauses can
             * temporarily form a syntactically valid prefix because
             * RETURN is optional in the AST. The official P1 .tree
             * cases finish with RETURN, so capture continues until
             * that clause is available.
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

    private boolean containsReturnClause(String source) {
        return source.matches(
                "(?is).*\\bRETURN\\b.*"
        );
    }

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

    private void reportSyntaxError(String message) {
        System.out.println(
                "Syntax error: " + message
        );
    }

    private void resetCapture() {
        queryBuffer.setLength(0);
        awaitingQuery = false;
    }
}