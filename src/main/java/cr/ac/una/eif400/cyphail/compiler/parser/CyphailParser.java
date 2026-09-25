package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.clause.MatchClause;
import cr.ac.una.eif400.cyphail.ast.clause.Projection;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionItem;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionModifiers;
import cr.ac.una.eif400.cyphail.ast.clause.ReturnClause;
import cr.ac.una.eif400.cyphail.ast.clause.SortDirection;
import cr.ac.una.eif400.cyphail.ast.clause.SortItem;
import cr.ac.una.eif400.cyphail.ast.clause.UpdatingClause;
import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryOperator;
import cr.ac.una.eif400.cyphail.ast.expression.BooleanLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.Expression;
import cr.ac.una.eif400.cyphail.ast.expression.IntegerLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.NullLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.PropertyExpression;
import cr.ac.una.eif400.cyphail.ast.expression.StringLiteralExpression;
import cr.ac.una.eif400.cyphail.ast.expression.VariableExpression;
import cr.ac.una.eif400.cyphail.ast.pattern.NodePattern;
import cr.ac.una.eif400.cyphail.ast.pattern.Pattern;
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
 * Concrete functional parser for the Cyphail Sprint P1 language subset.
 *
 * <p>The parser consumes the immutable token stream produced by the
 * CyphailLexer and builds the Cyphail AST through the functional parser
 * combinator infrastructure.</p>
 *
 * <p>The Sprint P1 public cases handled here include node patterns,
 * multiple disconnected node patterns, node properties, WHERE comparisons,
 * CREATE, DELETE and RETURN projections.</p>
 *
 * <p>Project: Cyphail - Sprint P1<br>
 * Course: EIF400 - Paradigmas de Programacion<br>
 * Universidad Nacional de Costa Rica<br>
 * Group: 04-10am</p>
 *
 * @author Emmanuel Nunez Jimenez
 * @author Keynell Molina Mora
 * @author Valery Alfaro Morales
 * @author Julissa Solano Valverde
 * @author Roy Arias Mejia
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
        Objects.requireNonNull(source, "Source cannot be null");

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

    private Parser<Query> queryParser() {
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

    private Parser<UpdatingClause> updatingClauseParser() {
        Parser<UpdatingClause> create =
                createClauseParser().map(
                        clause -> clause
                );

        Parser<UpdatingClause> delete =
                deleteClauseParser().map(
                        clause -> clause
                );

        return Parsers.or(create, delete);
    }

    private Parser<CreateClause> createClauseParser() {
        return Parsers.token(TokenType.CREATE)
                .flatMap(
                        create -> patternParser()
                                .map(CreateClause::new)
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

    private Parser<Pattern> patternParser() {
        return nodePatternParser().flatMap(
                first -> Parsers.many(
                        commaThenNodePatternParser()
                ).map(
                        rest -> new Pattern(
                                prepend(first, rest)
                        )
                )
        );
    }

    private Parser<NodePattern> commaThenNodePatternParser() {
        return Parsers.token(TokenType.COMMA)
                .flatMap(
                        comma -> nodePatternParser()
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
                        string,
                        Parsers.or(
                                trueLiteral,
                                Parsers.or(
                                        falseLiteral,
                                        nullLiteral
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