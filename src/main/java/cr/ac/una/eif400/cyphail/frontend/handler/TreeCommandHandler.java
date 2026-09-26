package cr.ac.una.eif400.cyphail.frontend.handler;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.visitor.AstTreePrinter;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalysisResult;
import cr.ac.una.eif400.cyphail.compiler.analyzer.SemanticAnalyzer;
import cr.ac.una.eif400.cyphail.compiler.parser.CyphailParser;

import java.util.Objects;

/**
 * Handles the Sprint P1 .tree REPL command.
 *
 * <p>The command activates a tree-query capture mode. Subsequent query
 * lines are accumulated until the parser recognizes a complete query
 * containing a RETURN clause. The resulting AST is then semantically
 * analyzed and, when valid, rendered by the AST tree visitor.</p>
 *
 * <p>Undefined-variable problems are reported in English as required
 * by the Sprint P1 specification.</p>
 *
 * <p>Project: Cyphail</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica - Escuela de Informatica</p>
 * <p>Group Code: 04-10am</p>
 *
 * <p>Authors:</p>
 * <ul>
 *     <li>Emmanuel Nunez Jimenez</li>
 *     <li>Valery Alfaro Morales</li>
 *     <li>Roy Arias Mejia</li>
 *     <li>Keynell Molina Mora</li>
 *     <li>Julissa Solano Valverde</li>
 * </ul>
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

        if (TREE_COMMAND.equalsIgnoreCase(normalized)) {
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

        if (TREE_COMMAND.equalsIgnoreCase(normalized)) {
            startCapture();
            return true;
        }

        if (!awaitingQuery) {
            return true;
        }

        appendQueryLine(input);
        processBufferedQuery();

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

    private void processBufferedQuery() {
        try {
            Query query = parser.parseOrThrow(
                    queryBuffer.toString()
            );

            /*
             * MATCH, WHERE and updating clauses can form a syntactically
             * valid partial P1 query because RETURN is optional in the AST.
             * The official .tree cases end in RETURN, so capture continues
             * until that final clause has been parsed.
             */
            if (query.returnClause().isEmpty()) {
                return;
            }

            SemanticAnalysisResult analysis =
                    analyzer.analyze(query);

            if (analysis.hasErrors()) {
                analysis.errors().forEach(
                        System.out::println
                );
            } else {
                System.out.println(
                        treePrinter.print(query)
                );
            }

            resetCapture();
        } catch (IllegalArgumentException exception) {
            /*
             * A multiline query can be temporarily incomplete while the
             * user is still entering its next line. In that situation the
             * handler keeps the accumulated input and waits for more.
             */
        }
    }

    private void resetCapture() {
        queryBuffer.setLength(0);
        awaitingQuery = false;
    }
}