package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.clause.MatchClause;
import cr.ac.una.eif400.cyphail.ast.clause.Projection;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionItem;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionModifiers;
import cr.ac.una.eif400.cyphail.ast.clause.RemoveClause;
import cr.ac.una.eif400.cyphail.ast.clause.ReturnClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetProperty;
import cr.ac.una.eif400.cyphail.ast.clause.SortDirection;
import cr.ac.una.eif400.cyphail.ast.clause.SortItem;
import cr.ac.una.eif400.cyphail.ast.clause.UpdatingClause;
import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryOperator;
import cr.ac.una.eif400.cyphail.ast.expression.BooleanLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;
import cr.ac.una.eif400.cyphail.ast.expression.FloatLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.IntegerLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.NullLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;
import cr.ac.una.eif400.cyphail.ast.expression.StringLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.NodePattern;
import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternPath;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;
import cr.ac.una.eif400.cyphail.compiler.lexer.CyphailLexer;
import cr.ac.una.eif400.cyphail.compiler.lexer.Token;
import cr.ac.una.eif400.cyphail.compiler.lexer.TokenType;
import cr.ac.una.eif400.cyphail.compiler.parser.combinator.ParseFailure;
import cr.ac.una.eif400.cyphail.compiler.parser.combinator.ParseResult;
import cr.ac.una.eif400.cyphail.compiler.parser.combinator.ParseSuccess;
import cr.ac.una.eif400.cyphail.compiler.parser.combinator.Parser;
import cr.ac.una.eif400.cyphail.compiler.parser.combinator.ParserInput;
import cr.ac.una.eif400.cyphail.compiler.parser.combinator.Parsers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Concrete functional parser for the Cyphail language.
 *
 * <p>The parser consumes the immutable token stream produced by
 * CyphailLexer and builds the Cyphail AST through functional
 * parser combinators.</p>
 *
 * <p>Sprint P1 supports node patterns, node properties, WHERE
 * comparisons, CREATE, DELETE and RETURN projections.</p>
 *
 * <p>Sprint P2 extends pattern parsing to support relationship
 * paths, relationship properties and the directions IN, OUT
 * and NONE. The original Sprint P1 parsing behavior is preserved.</p>
 *
 * <p>Sprint P2 also supports decimal literals, standalone
 * CREATE statements without a preceding MATCH clause,
 * SET assignments and REMOVE property clauses.</p>
 *
 * <p>Project: Cyphail - Sprint P2</p>
 * <p>Course: EIF400 - Paradigmas de Programacion</p>
 * <p>Universidad Nacional de Costa Rica</p>
 * <p>Escuela de Informatica</p>
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
public final class CyphailParser {

    private final CyphailLexer lexer;

    /**
     * Creates a parser using the standard Cyphail lexer.
     */
    public CyphailParser() {
        this(new CyphailLexer());
    }

    /**
     * Creates a parser with the supplied lexer.
     *
     * @param lexer lexer used to tokenize source queries
     */
    public CyphailParser(CyphailLexer lexer) {
        this.lexer = Objects.requireNonNull(
                lexer,
                "Lexer cannot be null"
        );
    }

    /**
     * Parses Cyphail source text and returns a functional parse result.
     *
     * @param source Cyphail query
     * @return parse result containing the Query AST or a failure
     */
    public ParseResult<Query> parse(String source) {
        Objects.requireNonNull(
                source,
                "Source cannot be null"
        );

        List<Token> tokens = lexer.tokenize(source);
        ParserInput input = ParserInput.from(tokens);

        return queryParser().parse(input);
    }

    /**
     * Parses a query and returns its AST.
     *
     * @param source Cyphail query
     * @return parsed Query AST
     * @throws IllegalArgumentException when parsing fails
     */
    public Query parseOrThrow(String source) {
        ParseResult<Query> result = parse(source);

        if (result instanceof ParseSuccess<?> success) {
            return (Query) success.value();
        }

        ParseFailure<?> failure = (ParseFailure<?>) result;

        throw new IllegalArgumentException(
                failure.reason()
                        + " at token position "
                        + failure.rest().position()
        );
    }

    /**
     * Selects the query grammar based on the first token.
     *
     * <p>Queries starting with CREATE use the Sprint P2
     * standalone CREATE parser. All other queries follow
     * the original MATCH-first parser.</p>
     *
     * <p>This avoids hiding a syntax error inside a malformed
     * MATCH query by attempting an unrelated CREATE parser.</p>
     *
     * @return parser for a complete query
     */
    private Parser<Query> queryParser() {
        return input -> {
            if (input.current().type() == TokenType.CREATE) {
                return createStartingQueryParser().parse(input);
            }

            return matchStartingQueryParser().parse(input);
        };
    }

    /**
     * Parses queries that begin with MATCH.
     *
     * <p>Preserves the original Sprint P1 syntax, including
     * optional WHERE, updating clauses and RETURN.</p>
     *
     * @return parser for MATCH-first queries
     */
    private Parser<Query> matchStartingQueryParser() {
        return matchClauseParser().flatMap(
                match -> Parsers.optional(
                        whereClauseParser()
                ).flatMap(
                        where -> Parsers.many(
                                updatingClauseParser()
                        ).flatMap(
                                updates -> Parsers.optional(
                                        returnClauseParser()
                                ).flatMap(
                                        returnClause ->
                                                Parsers.token(
                                                        TokenType.EOF
                                                ).map(
                                                        eof -> new Query(
                                                                match,
                                                                where,
                                                                updates,
                                                                returnClause
                                                        )
                                                )
                                )
                        )
                )
        );
    }

    /**
     * Parses standalone CREATE queries without MATCH.
     *
     * <p>The first CREATE is mandatory. Additional update
     * clauses and an optional RETURN may follow it.</p>
     *
     * @return parser for CREATE-first queries
     */
    private Parser<Query> createStartingQueryParser() {
        return createClauseParser().flatMap(
                firstCreate -> Parsers.many(
                        updatingClauseParser()
                ).flatMap(
                        followingUpdates -> Parsers.optional(
                                returnClauseParser()
                        ).flatMap(
                                returnClause -> Parsers.token(
                                        TokenType.EOF
                                ).map(
                                        eof -> new Query(
                                                null,
                                                Optional.empty(),
                                                prepend(
                                                        firstCreate,
                                                        followingUpdates
                                                ),
                                                returnClause
                                        )
                                )
                        )
                )
        );
    }

    private Parser<MatchClause> matchClauseParser() {
        return Parsers.token(TokenType.MATCH)
                .flatMap(
                        match -> patternParser()
                                .map(MatchClause::new)
                );
    }

    private Parser<WhereClause> whereClauseParser() {
        return Parsers.token(TokenType.WHERE)
                .flatMap(
                        where -> expressionParser()
                                .map(WhereClause::new)
                );
    }

    /**
     * Parses supported updating clauses.
     *
     * <p>Sprint P2 adds SET and REMOVE while preserving
     * CREATE and DELETE.</p>
     *
     * @return parser for an updating clause
     */
    private Parser<UpdatingClause> updatingClauseParser() {
        Parser<UpdatingClause> create =
                createClauseParser().map(
                        clause -> clause
                );

        Parser<UpdatingClause> set =
                setClauseParser().map(
                        clause -> clause
                );

        Parser<UpdatingClause> remove =
                removeClauseParser().map(
                        clause -> clause
                );

        Parser<UpdatingClause> delete =
                deleteClauseParser().map(
                        clause -> clause
                );

        return Parsers.or(
                create,
                Parsers.or(
                        set,
                        Parsers.or(remove, delete)
                )
        );
    }

    private Parser<CreateClause> createClauseParser() {
        return Parsers.token(TokenType.CREATE)
                .flatMap(
                        create -> patternParser()
                                .map(CreateClause::new)
                );
    }

    /**
     * Parses a SET assignment for a node or relationship property.
     *
     * <p>Examples:</p>
     * <pre>
     * SET p.precio = 19.99
     * SET r.grade = 9.5
     * </pre>
     *
     * <p>The AST stores the assignment as a SetProperty
     * inside the original SetClause items collection.</p>
     *
     * @return parser for a SET clause
     */
    private Parser<SetClause> setClauseParser() {
        return Parsers.token(TokenType.SET).flatMap(
                set -> identifierNameParser().flatMap(
                        variable -> Parsers.token(
                                TokenType.DOT
                        ).flatMap(
                                dot -> identifierNameParser().flatMap(
                                        property -> Parsers.token(
                                                TokenType.EQUAL
                                        ).flatMap(
                                                equal -> expressionParser().map(
                                                        value -> new SetClause(
                                                                List.of(
                                                                        new SetProperty(
                                                                                new PropertyExpression(
                                                                                        new VariableExpression(variable),
                                                                                        property
                                                                                ),
                                                                                value
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Parses a REMOVE clause containing one or more
     * property references.
     *
     * <p>Examples:</p>
     * <pre>
     * REMOVE p.discount
     * REMOVE r.grade
     * REMOVE p.discount, p.oldPrice
     * </pre>
     *
     * @return parser for a REMOVE clause
     */
    private Parser<RemoveClause> removeClauseParser() {
        return Parsers.token(TokenType.REMOVE).flatMap(
                remove -> removePropertyParser().flatMap(
                        first -> Parsers.many(
                                commaThenRemovePropertyParser()
                        ).map(
                                rest -> new RemoveClause(
                                        prepend(first, rest)
                                )
                        )
                )
        );
    }

    /**
     * Parses a property reference used by REMOVE.
     *
     * <p>The property may belong to a node or
     * relationship variable.</p>
     *
     * @return parser for a property expression
     */
    private Parser<PropertyExpression> removePropertyParser() {
        return identifierNameParser().flatMap(
                variable -> Parsers.token(
                        TokenType.DOT
                ).flatMap(
                        dot -> identifierNameParser().map(
                                property -> new PropertyExpression(
                                        new VariableExpression(variable),
                                        property
                                )
                        )
                )
        );
    }

    /**
     * Parses a comma followed by another REMOVE property.
     *
     * @return parser for another property expression
     */
    private Parser<PropertyExpression>
    commaThenRemovePropertyParser() {
        return Parsers.token(TokenType.COMMA).flatMap(
                comma -> removePropertyParser()
        );
    }

    private Parser<DeleteClause> deleteClauseParser() {
        return Parsers.optional(
                Parsers.token(TokenType.DETACH)
        ).flatMap(
                detach -> Parsers.token(
                        TokenType.DELETE
                ).flatMap(
                        delete -> expressionParser()
                                .flatMap(
                                        first -> Parsers.many(
                                                commaThenExpressionParser()
                                        ).map(
                                                rest -> new DeleteClause(
                                                        detach.isPresent(),
                                                        prepend(
                                                                first,
                                                                rest
                                                        )
                                                )
                                        )
                                )
                )
        );
    }

    private Parser<Expression> commaThenExpressionParser() {
        return Parsers.token(TokenType.COMMA)
                .flatMap(
                        comma -> expressionParser()
                );
    }

    /**
     * Parses one or more comma-separated pattern paths.
     *
     * <p>Sprint P1 node-only patterns are represented as
     * isolated paths. Sprint P2 relationship patterns retain
     * their ordered relationship steps.</p>
     *
     * @return parser for a complete pattern
     */
    private Parser<Pattern> patternParser() {
        return patternPathParser().flatMap(
                first -> Parsers.many(
                        commaThenPatternPathParser()
                ).map(
                        rest -> Pattern.fromPaths(
                                prepend(first, rest)
                        )
                )
        );
    }

    /**
     * Parses a node followed by zero or more relationship steps.
     *
     * <p>Existing node, identifier, label and property parsers
     * are reused to preserve the functional design.</p>
     *
     * @return parser for a pattern path
     */
    private Parser<PatternPath> patternPathParser() {
        return PatternPathParser.path(
                nodePatternParser(),
                identifierNameParser(),
                labelParser(),
                nodePropertiesParser()
        );
    }

    /**
     * Parses a comma followed by another pattern path.
     *
     * @return parser for a comma-separated pattern path
     */
    private Parser<PatternPath> commaThenPatternPathParser() {
        return Parsers.token(TokenType.COMMA)
                .flatMap(
                        comma -> patternPathParser()
                );
    }

    private Parser<NodePattern> nodePatternParser() {
        return Parsers.token(
                TokenType.LEFT_PAREN
        ).flatMap(
                leftParen -> Parsers.optional(
                        identifierNameParser()
                ).flatMap(
                        variable -> Parsers.many(
                                labelParser()
                        ).flatMap(
                                labels -> Parsers.optional(
                                        nodePropertiesParser()
                                ).flatMap(
                                        properties ->
                                                Parsers.token(
                                                        TokenType.RIGHT_PAREN
                                                ).map(
                                                        rightParen ->
                                                                new NodePattern(
                                                                        variable,
                                                                        labels,
                                                                        properties.orElse(
                                                                                List.of()
                                                                        )
                                                                )
                                                )
                                )
                        )
                )
        );
    }

    private Parser<String> labelParser() {
        return Parsers.token(TokenType.COLON)
                .flatMap(
                        colon -> identifierNameParser()
                );
    }

    private Parser<List<PatternProperty>> nodePropertiesParser() {
        return Parsers.token(
                TokenType.LEFT_BRACE
        ).flatMap(
                leftBrace -> Parsers.optional(
                        propertyListParser()
                ).flatMap(
                        properties -> Parsers.token(
                                TokenType.RIGHT_BRACE
                        ).map(
                                rightBrace -> properties.orElse(
                                        List.of()
                                )
                        )
                )
        );
    }

    private Parser<List<PatternProperty>> propertyListParser() {
        return patternPropertyParser().flatMap(
                first -> Parsers.many(
                        commaThenPatternPropertyParser()
                ).map(
                        rest -> prepend(first, rest)
                )
        );
    }

    private Parser<PatternProperty>
    commaThenPatternPropertyParser() {
        return Parsers.token(TokenType.COMMA)
                .flatMap(
                        comma -> patternPropertyParser()
                );
    }

    private Parser<PatternProperty> patternPropertyParser() {
        return identifierNameParser().flatMap(
                name -> Parsers.token(
                        TokenType.COLON
                ).flatMap(
                        colon -> expressionParser()
                                .map(
                                        value -> new PatternProperty(
                                                name,
                                                value
                                        )
                                )
                )
        );
    }

    private Parser<ReturnClause> returnClauseParser() {
        return Parsers.token(TokenType.RETURN)
                .flatMap(
                        returnToken -> Parsers.optional(
                                Parsers.token(
                                        TokenType.DISTINCT
                                )
                        ).flatMap(
                                distinct -> projectionParser()
                                        .map(
                                                projection ->
                                                        new ReturnClause(
                                                                distinct.isPresent(),
                                                                projection
                                                        )
                                        )
                        )
                );
    }

    private Parser<Projection> projectionParser() {
        return Parsers.or(
                wildcardProjectionParser(),
                explicitProjectionParser()
        );
    }

    private Parser<Projection> wildcardProjectionParser() {
        return Parsers.token(TokenType.STAR)
                .flatMap(
                        star -> Parsers.many(
                                commaThenProjectionItemParser()
                        ).flatMap(
                                items -> projectionModifiersParser()
                                        .map(
                                                modifiers ->
                                                        new Projection(
                                                                true,
                                                                items,
                                                                modifiers
                                                        )
                                        )
                        )
                );
    }

    private Parser<Projection> explicitProjectionParser() {
        return projectionItemParser().flatMap(
                first -> Parsers.many(
                        commaThenProjectionItemParser()
                ).flatMap(
                        rest -> projectionModifiersParser()
                                .map(
                                        modifiers ->
                                                new Projection(
                                                        false,
                                                        prepend(
                                                                first,
                                                                rest
                                                        ),
                                                        modifiers
                                                )
                                )
                )
        );
    }

    private Parser<ProjectionItem>
    commaThenProjectionItemParser() {
        return Parsers.token(TokenType.COMMA)
                .flatMap(
                        comma -> projectionItemParser()
                );
    }

    private Parser<ProjectionItem> projectionItemParser() {
        return expressionParser().flatMap(
                expression -> Parsers.optional(
                        aliasParser()
                ).map(
                        alias -> new ProjectionItem(
                                expression,
                                alias
                        )
                )
        );
    }

    private Parser<String> aliasParser() {
        return Parsers.token(TokenType.AS)
                .flatMap(
                        as -> identifierNameParser()
                );
    }

    private Parser<ProjectionModifiers>
    projectionModifiersParser() {
        return Parsers.optional(
                orderByParser()
        ).flatMap(
                orderBy -> Parsers.optional(
                        skipParser()
                ).flatMap(
                        skip -> Parsers.optional(
                                limitParser()
                        ).map(
                                limit -> new ProjectionModifiers(
                                        orderBy.orElse(
                                                List.of()
                                        ),
                                        skip,
                                        limit
                                )
                        )
                )
        );
    }

    private Parser<List<SortItem>> orderByParser() {
        return Parsers.token(TokenType.ORDER)
                .flatMap(
                        order -> Parsers.token(
                                TokenType.BY
                        ).flatMap(
                                by -> sortItemParser()
                                        .flatMap(
                                                first -> Parsers.many(
                                                        commaThenSortItemParser()
                                                ).map(
                                                        rest -> prepend(
                                                                first,
                                                                rest
                                                        )
                                                )
                                        )
                        )
                );
    }

    private Parser<SortItem> commaThenSortItemParser() {
        return Parsers.token(TokenType.COMMA)
                .flatMap(
                        comma -> sortItemParser()
                );
    }

    private Parser<SortItem> sortItemParser() {
        return expressionParser().flatMap(
                expression -> Parsers.optional(
                        sortDirectionParser()
                ).map(
                        direction -> new SortItem(
                                expression,
                                direction
                        )
                )
        );
    }

    private Parser<SortDirection> sortDirectionParser() {
        Parser<SortDirection> ascending =
                Parsers.token(
                        TokenType.ASCENDING
                ).map(
                        token -> SortDirection.ASCENDING
                );

        Parser<SortDirection> descending =
                Parsers.token(
                        TokenType.DESCENDING
                ).map(
                        token -> SortDirection.DESCENDING
                );

        return Parsers.or(
                ascending,
                descending
        );
    }

    private Parser<Expression> skipParser() {
        return Parsers.token(TokenType.SKIP)
                .flatMap(
                        skip -> expressionParser()
                );
    }

    private Parser<Expression> limitParser() {
        return Parsers.token(TokenType.LIMIT)
                .flatMap(
                        limit -> expressionParser()
                );
    }

    private Parser<Expression> expressionParser() {
        return simpleExpressionParser().flatMap(
                left -> Parsers.optional(
                        comparisonTailParser()
                ).map(
                        comparison -> comparison
                                .<Expression>map(
                                        tail -> new BinaryExpression(
                                                left,
                                                tail.operator(),
                                                tail.right()
                                        )
                                )
                                .orElse(left)
                )
        );
    }

    private Parser<ComparisonTail> comparisonTailParser() {
        return comparisonOperatorParser().flatMap(
                operator -> simpleExpressionParser()
                        .map(
                                right -> new ComparisonTail(
                                        operator,
                                        right
                                )
                        )
        );
    }

    private Parser<BinaryOperator>
    comparisonOperatorParser() {
        Parser<BinaryOperator> equal =
                Parsers.token(
                        TokenType.EQUAL
                ).map(
                        token -> BinaryOperator.EQUAL
                );

        Parser<BinaryOperator> notEqual =
                Parsers.token(
                        TokenType.NOT_EQUAL
                ).map(
                        token -> BinaryOperator.NOT_EQUAL
                );

        Parser<BinaryOperator> lessThan =
                Parsers.token(
                        TokenType.LESS_THAN
                ).map(
                        token -> BinaryOperator.LESS_THAN
                );

        Parser<BinaryOperator> greaterThan =
                Parsers.token(
                        TokenType.GREATER_THAN
                ).map(
                        token -> BinaryOperator.GREATER_THAN
                );

        Parser<BinaryOperator> lessThanOrEqual =
                Parsers.token(
                        TokenType.LESS_THAN_OR_EQUAL
                ).map(
                        token ->
                                BinaryOperator.LESS_THAN_OR_EQUAL
                );

        Parser<BinaryOperator> greaterThanOrEqual =
                Parsers.token(
                        TokenType.GREATER_THAN_OR_EQUAL
                ).map(
                        token ->
                                BinaryOperator.GREATER_THAN_OR_EQUAL
                );

        return Parsers.or(
                equal,
                Parsers.or(
                        notEqual,
                        Parsers.or(
                                lessThan,
                                Parsers.or(
                                        greaterThan,
                                        Parsers.or(
                                                lessThanOrEqual,
                                                greaterThanOrEqual
                                        )
                                )
                        )
                )
        );
    }

    private Parser<Expression> simpleExpressionParser() {
        return Parsers.or(
                literalExpressionParser(),
                variableOrPropertyParser()
        );
    }

    private Parser<Expression> variableOrPropertyParser() {
        return identifierNameParser().flatMap(
                name -> Parsers.many(
                        propertyNameParser()
                ).map(
                        properties -> buildPropertyExpression(
                                name,
                                properties
                        )
                )
        );
    }

    private Parser<String> propertyNameParser() {
        return Parsers.token(TokenType.DOT)
                .flatMap(
                        dot -> identifierNameParser()
                );
    }

    private Expression buildPropertyExpression(
            String variable,
            List<String> properties
    ) {
        Expression expression =
                new VariableExpression(variable);

        for (String property : properties) {
            expression = new PropertyExpression(
                    expression,
                    property
            );
        }

        return expression;
    }

    private Parser<Expression> literalExpressionParser() {
        Parser<Expression> integer =
                Parsers.token(
                        TokenType.INTEGER
                ).map(
                        token -> new IntegerLiteralExpression(
                                Long.parseLong(
                                        token.lexeme()
                                )
                        )
                );

        // Sprint P2: decimal literals.
        Parser<Expression> decimal =
                Parsers.token(
                        TokenType.FLOAT
                ).map(
                        token -> new FloatLiteralExpression(
                                Double.parseDouble(
                                        token.lexeme()
                                )
                        )
                );

        Parser<Expression> string =
                Parsers.token(
                        TokenType.STRING
                ).map(
                        token -> new StringLiteralExpression(
                                stripQuotes(
                                        token.lexeme()
                                )
                        )
                );

        Parser<Expression> trueLiteral =
                Parsers.token(
                        TokenType.TRUE
                ).map(
                        token ->
                                new BooleanLiteralExpression(
                                        true
                                )
                );

        Parser<Expression> falseLiteral =
                Parsers.token(
                        TokenType.FALSE
                ).map(
                        token ->
                                new BooleanLiteralExpression(
                                        false
                                )
                );

        Parser<Expression> nullLiteral =
                Parsers.token(
                        TokenType.NULL
                ).map(
                        token ->
                                new NullLiteralExpression()
                );

        return Parsers.or(
                integer,
                Parsers.or(
                        decimal,
                        Parsers.or(
                                string,
                                Parsers.or(
                                        trueLiteral,
                                        Parsers.or(
                                                falseLiteral,
                                                nullLiteral
                                        )
                                )
                        )
                )
        );
    }

    private Parser<String> identifierNameParser() {
        return Parsers.identifier()
                .map(Token::lexeme);
    }

    private String stripQuotes(String lexeme) {
        if (lexeme.length() < 2) {
            return lexeme;
        }

        return lexeme.substring(
                1,
                lexeme.length() - 1
        );
    }

    private <T> List<T> prepend(
            T first,
            List<T> rest
    ) {
        List<T> values = new ArrayList<>();
        values.add(first);
        values.addAll(rest);

        return List.copyOf(values);
    }

    private record ComparisonTail(
            BinaryOperator operator,
            Expression right
    ) {
    }
}