
package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.*;
import cr.ac.una.eif400.cyphail.ast.expression.*;
import cr.ac.una.eif400.cyphail.ast.pattern.*;
import cr.ac.una.eif400.cyphail.compiler.lexer.*;
import cr.ac.una.eif400.cyphail.compiler.parser.combinator.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Paradigmas de Programacion
 * Universidad Nacional de Costa Rica
 * Escuela de Informatica
 * Group Code: 04-10am
 *
 * Authors:
 * Emmanuel Nunez Jimenez
 * Julissa Solano Valverde
 * Valery Alfaro Morales
 * Keynell Molina Mora
 * Roy Arias Mejia
 *
 * Functional Cyphail parser.
 *
 * Supports:
 * - MATCH and relationship patterns.
 * - WHERE with comparisons, AND and OR.
 * - Parenthesized expressions.
 * - Standalone CREATE.
 * - SET with one or more property assignments.
 * - REMOVE and DELETE.
 * - RETURN projections and modifiers.
 *
 * Preserves existing P1/P2 parser behavior.
 */
public final class CyphailParser {

    private final CyphailLexer lexer;

    public CyphailParser() {
        this(new CyphailLexer());
    }

    public CyphailParser(CyphailLexer lexer) {
        this.lexer = Objects.requireNonNull(
                lexer,
                "Lexer cannot be null"
        );
    }

    public ParseResult<Query> parse(String source) {

        Objects.requireNonNull(
                source,
                "Source cannot be null"
        );

        List<Token> tokens = lexer.tokenize(source);

        return queryParser().parse(
                ParserInput.from(tokens)
        );
    }

    public Query parseOrThrow(String source) {

        ParseResult<Query> result = parse(source);

        if (result instanceof ParseSuccess<?> success) {
            return (Query) success.value();
        }

        ParseFailure<?> failure =
                (ParseFailure<?>) result;

        throw new IllegalArgumentException(
                failure.reason()
                        + " at token position "
                        + failure.rest().position()
        );
    }

    // =====================================================
    // QUERY PARSING
    // =====================================================

    private Parser<Query> queryParser() {

        return input ->
                input.current().type() == TokenType.CREATE
                        ? createStartingQueryParser().parse(input)
                        : matchStartingQueryParser().parse(input);
    }

    private Parser<Query> matchStartingQueryParser() {

        return matchClauseParser().flatMap(match ->
                Parsers.optional(whereClauseParser()).flatMap(where ->
                        Parsers.many(updatingClauseParser()).flatMap(updates ->
                                Parsers.optional(returnClauseParser()).flatMap(returnClause ->
                                        Parsers.token(TokenType.EOF).map(eof ->
                                                new Query(
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

    private Parser<Query> createStartingQueryParser() {

        return createClauseParser().flatMap(firstCreate ->
                Parsers.many(updatingClauseParser()).flatMap(followingUpdates ->
                        Parsers.optional(returnClauseParser()).flatMap(returnClause ->
                                Parsers.token(TokenType.EOF).map(eof ->
                                        new Query(
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

    // =====================================================
    // CLAUSE PARSING
    // =====================================================

    private Parser<MatchClause> matchClauseParser() {

        return Parsers.token(TokenType.MATCH).flatMap(token ->
                patternParser().map(MatchClause::new)
        );
    }

    private Parser<WhereClause> whereClauseParser() {

        return Parsers.token(TokenType.WHERE).flatMap(token ->
                expressionParser().map(WhereClause::new)
        );
    }

    private Parser<UpdatingClause> updatingClauseParser() {

        Parser<UpdatingClause> create =
                createClauseParser().map(clause -> clause);

        Parser<UpdatingClause> set =
                setClauseParser().map(clause -> clause);

        Parser<UpdatingClause> remove =
                removeClauseParser().map(clause -> clause);

        Parser<UpdatingClause> delete =
                deleteClauseParser().map(clause -> clause);

        return Parsers.or(
                create,
                Parsers.or(
                        set,
                        Parsers.or(remove, delete)
                )
        );
    }

    // =====================================================
    // CREATE
    // =====================================================

    private Parser<CreateClause> createClauseParser() {

        return Parsers.token(TokenType.CREATE).flatMap(token ->
                patternParser().map(CreateClause::new)
        );
    }

    // =====================================================
    // SET
    // =====================================================

    /**
     * Parses single and multiple SET assignments.
     *
     * Examples:
     *
     * SET p.precio = 19.99
     *
     * SET p.precio = 19.99,
     *     p.estado = 'activo'
     *
     * SET r.grade = 9.5
     *
     * The source order of assignments is preserved.
     */
    private Parser<SetClause> setClauseParser() {

        return Parsers.token(TokenType.SET).flatMap(token ->
                setPropertyParser().flatMap(first ->
                        Parsers.many(
                                commaThenSetPropertyParser()
                        ).map(rest ->
                                new SetClause(
                                        new ArrayList<SetItem>(
                                                prepend(first, rest)
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Parses one SET property assignment.
     *
     * Grammar:
     *
     * identifier "." identifier "=" expression
     *
     * Produces a SetProperty AST node.
     */
    private Parser<SetProperty> setPropertyParser() {

        return identifierNameParser().flatMap(variable ->
                Parsers.token(TokenType.DOT).flatMap(dot ->
                        identifierNameParser().flatMap(property ->
                                Parsers.token(TokenType.EQUAL).flatMap(equal ->
                                        expressionParser().map(value ->
                                                new SetProperty(
                                                        new PropertyExpression(
                                                                new VariableExpression(
                                                                        variable
                                                                ),
                                                                property
                                                        ),
                                                        value
                                                )
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Parses an additional SET item
     * preceded by a comma.
     */
    private Parser<SetProperty> commaThenSetPropertyParser() {

        return Parsers.token(TokenType.COMMA).flatMap(token ->
                setPropertyParser()
        );
    }

    // =====================================================
    // REMOVE
    // =====================================================

    private Parser<RemoveClause> removeClauseParser() {

        return Parsers.token(TokenType.REMOVE).flatMap(token ->
                removePropertyParser().flatMap(first ->
                        Parsers.many(
                                commaThenRemovePropertyParser()
                        ).map(rest ->
                                new RemoveClause(
                                        prepend(first, rest)
                                )
                        )
                )
        );
    }

    private Parser<PropertyExpression> removePropertyParser() {

        return identifierNameParser().flatMap(variable ->
                Parsers.token(TokenType.DOT).flatMap(dot ->
                        identifierNameParser().map(property ->
                                new PropertyExpression(
                                        new VariableExpression(variable),
                                        property
                                )
                        )
                )
        );
    }

    private Parser<PropertyExpression> commaThenRemovePropertyParser() {

        return Parsers.token(TokenType.COMMA).flatMap(token ->
                removePropertyParser()
        );
    }

    // =====================================================
    // DELETE
    // =====================================================

    private Parser<DeleteClause> deleteClauseParser() {

        return Parsers.optional(
                Parsers.token(TokenType.DETACH)
        ).flatMap(detach ->
                Parsers.token(TokenType.DELETE).flatMap(token ->
                        expressionParser().flatMap(first ->
                                Parsers.many(
                                        commaThenExpressionParser()
                                ).map(rest ->
                                        new DeleteClause(
                                                detach.isPresent(),
                                                prepend(first, rest)
                                        )
                                )
                        )
                )
        );
    }

    private Parser<Expression> commaThenExpressionParser() {

        return Parsers.token(TokenType.COMMA).flatMap(token ->
                expressionParser()
        );
    }

    // =====================================================
    // PATTERNS AND RELATIONSHIPS
    // =====================================================

    private Parser<Pattern> patternParser() {

        return patternPathParser().flatMap(first ->
                Parsers.many(
                        commaThenPatternPathParser()
                ).map(rest ->
                        Pattern.fromPaths(
                                prepend(first, rest)
                        )
                )
        );
    }

    private Parser<PatternPath> patternPathParser() {

        return PatternPathParser.path(
                nodePatternParser(),
                identifierNameParser(),
                labelParser(),
                nodePropertiesParser()
        );
    }

    private Parser<PatternPath> commaThenPatternPathParser() {

        return Parsers.token(TokenType.COMMA).flatMap(token ->
                patternPathParser()
        );
    }

    private Parser<NodePattern> nodePatternParser() {

        return Parsers.token(TokenType.LEFT_PAREN).flatMap(leftParen ->
                Parsers.optional(identifierNameParser()).flatMap(variable ->
                        Parsers.many(labelParser()).flatMap(labels ->
                                Parsers.optional(nodePropertiesParser()).flatMap(properties ->
                                        Parsers.token(TokenType.RIGHT_PAREN).map(rightParen ->
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

        return Parsers.token(TokenType.COLON).flatMap(token ->
                identifierNameParser()
        );
    }

    // =====================================================
    // PATTERN PROPERTIES
    // =====================================================

    private Parser<List<PatternProperty>> nodePropertiesParser() {

        return Parsers.token(TokenType.LEFT_BRACE).flatMap(leftBrace ->
                Parsers.optional(propertyListParser()).flatMap(properties ->
                        Parsers.token(TokenType.RIGHT_BRACE).map(rightBrace ->
                                properties.orElse(List.of())
                        )
                )
        );
    }

    private Parser<List<PatternProperty>> propertyListParser() {

        return patternPropertyParser().flatMap(first ->
                Parsers.many(
                        commaThenPatternPropertyParser()
                ).map(rest ->
                        prepend(first, rest)
                )
        );
    }

    private Parser<PatternProperty> commaThenPatternPropertyParser() {

        return Parsers.token(TokenType.COMMA).flatMap(token ->
                patternPropertyParser()
        );
    }

    private Parser<PatternProperty> patternPropertyParser() {

        return identifierNameParser().flatMap(name ->
                Parsers.token(TokenType.COLON).flatMap(token ->
                        expressionParser().map(value ->
                                new PatternProperty(
                                        name,
                                        value
                                )
                        )
                )
        );
    }

    // =====================================================
    // RETURN
    // =====================================================

    private Parser<ReturnClause> returnClauseParser() {

        return Parsers.token(TokenType.RETURN).flatMap(token ->
                Parsers.optional(
                        Parsers.token(TokenType.DISTINCT)
                ).flatMap(distinct ->
                        projectionParser().map(projection ->
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

        return Parsers.token(TokenType.STAR).flatMap(token ->
                Parsers.many(
                        commaThenProjectionItemParser()
                ).flatMap(items ->
                        projectionModifiersParser().map(modifiers ->
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

        return projectionItemParser().flatMap(first ->
                Parsers.many(
                        commaThenProjectionItemParser()
                ).flatMap(rest ->
                        projectionModifiersParser().map(modifiers ->
                                new Projection(
                                        false,
                                        prepend(first, rest),
                                        modifiers
                                )
                        )
                )
        );
    }

    private Parser<ProjectionItem> commaThenProjectionItemParser() {

        return Parsers.token(TokenType.COMMA).flatMap(token ->
                projectionItemParser()
        );
    }

    private Parser<ProjectionItem> projectionItemParser() {

        return expressionParser().flatMap(expression ->
                Parsers.optional(aliasParser()).map(alias ->
                        new ProjectionItem(
                                expression,
                                alias
                        )
                )
        );
    }

    private Parser<String> aliasParser() {

        return Parsers.token(TokenType.AS).flatMap(token ->
                identifierNameParser()
        );
    }

    // =====================================================
    // RETURN MODIFIERS
    // =====================================================

    private Parser<ProjectionModifiers> projectionModifiersParser() {

        return Parsers.optional(orderByParser()).flatMap(orderBy ->
                Parsers.optional(skipParser()).flatMap(skip ->
                        Parsers.optional(limitParser()).map(limit ->
                                new ProjectionModifiers(
                                        orderBy.orElse(List.of()),
                                        skip,
                                        limit
                                )
                        )
                )
        );
    }

    private Parser<List<SortItem>> orderByParser() {

        return Parsers.token(TokenType.ORDER).flatMap(order ->
                Parsers.token(TokenType.BY).flatMap(by ->
                        sortItemParser().flatMap(first ->
                                Parsers.many(
                                        commaThenSortItemParser()
                                ).map(rest ->
                                        prepend(first, rest)
                                )
                        )
                )
        );
    }

    private Parser<SortItem> commaThenSortItemParser() {

        return Parsers.token(TokenType.COMMA).flatMap(token ->
                sortItemParser()
        );
    }

    private Parser<SortItem> sortItemParser() {

        return expressionParser().flatMap(expression ->
                Parsers.optional(sortDirectionParser()).map(direction ->
                        new SortItem(
                                expression,
                                direction
                        )
                )
        );
    }

    private Parser<SortDirection> sortDirectionParser() {

        Parser<SortDirection> ascending =
                Parsers.token(TokenType.ASCENDING).map(token ->
                        SortDirection.ASCENDING
                );

        Parser<SortDirection> descending =
                Parsers.token(TokenType.DESCENDING).map(token ->
                        SortDirection.DESCENDING
                );

        return Parsers.or(
                ascending,
                descending
        );
    }

    private Parser<Expression> skipParser() {

        return Parsers.token(TokenType.SKIP).flatMap(token ->
                expressionParser()
        );
    }

    private Parser<Expression> limitParser() {

        return Parsers.token(TokenType.LIMIT).flatMap(token ->
                expressionParser()
        );
    }

    // =====================================================
    // EXPRESSIONS
    // =====================================================

    /**
     * Precedence:
     *
     * OR < AND < comparisons < grouped expressions
     */
    private Parser<Expression> expressionParser() {

        return logicalOrExpressionParser();
    }

    private Parser<Expression> logicalOrExpressionParser() {

        return logicalAndExpressionParser().flatMap(first ->
                Parsers.many(
                        Parsers.token(TokenType.OR).flatMap(token ->
                                logicalAndExpressionParser()
                        )
                ).map(rest ->
                        foldBinaryExpressions(
                                first,
                                BinaryOperator.OR,
                                rest
                        )
                )
        );
    }

    private Parser<Expression> logicalAndExpressionParser() {

        return comparisonExpressionParser().flatMap(first ->
                Parsers.many(
                        Parsers.token(TokenType.AND).flatMap(token ->
                                comparisonExpressionParser()
                        )
                ).map(rest ->
                        foldBinaryExpressions(
                                first,
                                BinaryOperator.AND,
                                rest
                        )
                )
        );
    }

    private Parser<Expression> comparisonExpressionParser() {

        return simpleExpressionParser().flatMap(left ->
                Parsers.optional(comparisonTailParser()).map(comparison ->
                        comparison.<Expression>map(tail ->
                                new BinaryExpression(
                                        left,
                                        tail.operator(),
                                        tail.right()
                                )
                        ).orElse(left)
                )
        );
    }

    private static Expression foldBinaryExpressions(
            Expression first,
            BinaryOperator operator,
            List<Expression> rest
    ) {

        Expression result = first;

        for (Expression right : rest) {
            result = new BinaryExpression(
                    result,
                    operator,
                    right
            );
        }

        return result;
    }

    private Parser<ComparisonTail> comparisonTailParser() {

        return comparisonOperatorParser().flatMap(operator ->
                simpleExpressionParser().map(right ->
                        new ComparisonTail(
                                operator,
                                right
                        )
                )
        );
    }

    private Parser<BinaryOperator> comparisonOperatorParser() {

        Parser<BinaryOperator> equal =
                Parsers.token(TokenType.EQUAL).map(token ->
                        BinaryOperator.EQUAL
                );

        Parser<BinaryOperator> notEqual =
                Parsers.token(TokenType.NOT_EQUAL).map(token ->
                        BinaryOperator.NOT_EQUAL
                );

        Parser<BinaryOperator> lessThan =
                Parsers.token(TokenType.LESS_THAN).map(token ->
                        BinaryOperator.LESS_THAN
                );

        Parser<BinaryOperator> greaterThan =
                Parsers.token(TokenType.GREATER_THAN).map(token ->
                        BinaryOperator.GREATER_THAN
                );

        Parser<BinaryOperator> lessThanOrEqual =
                Parsers.token(TokenType.LESS_THAN_OR_EQUAL).map(token ->
                        BinaryOperator.LESS_THAN_OR_EQUAL
                );

        Parser<BinaryOperator> greaterThanOrEqual =
                Parsers.token(TokenType.GREATER_THAN_OR_EQUAL).map(token ->
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
                parenthesizedExpressionParser(),
                Parsers.or(
                        literalExpressionParser(),
                        variableOrPropertyParser()
                )
        );
    }

    private Parser<Expression> parenthesizedExpressionParser() {

        return Parsers.token(TokenType.LEFT_PAREN).flatMap(left ->
                expressionParser().flatMap(expression ->
                        Parsers.token(TokenType.RIGHT_PAREN).map(right ->
                                expression
                        )
                )
        );
    }

    // =====================================================
    // VARIABLES AND PROPERTY REFERENCES
    // =====================================================

    private Parser<Expression> variableOrPropertyParser() {

        return identifierNameParser().flatMap(name ->
                Parsers.many(
                        propertyNameParser()
                ).map(properties ->
                        buildPropertyExpression(
                                name,
                                properties
                        )
                )
        );
    }

    private Parser<String> propertyNameParser() {

        return Parsers.token(TokenType.DOT).flatMap(token ->
                identifierNameParser()
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

    // =====================================================
    // LITERAL EXPRESSIONS
    // =====================================================

    private Parser<Expression> literalExpressionParser() {

        Parser<Expression> integer =
                Parsers.token(TokenType.INTEGER).map(token ->
                        new IntegerLiteralExpression(
                                Long.parseLong(
                                        token.lexeme()
                                )
                        )
                );

        Parser<Expression> decimal =
                Parsers.token(TokenType.FLOAT).map(token ->
                        new FloatLiteralExpression(
                                Double.parseDouble(
                                        token.lexeme()
                                )
                        )
                );

        Parser<Expression> string =
                Parsers.token(TokenType.STRING).map(token ->
                        new StringLiteralExpression(
                                stripQuotes(
                                        token.lexeme()
                                )
                        )
                );

        Parser<Expression> trueLiteral =
                Parsers.token(TokenType.TRUE).map(token ->
                        new BooleanLiteralExpression(true)
                );

        Parser<Expression> falseLiteral =
                Parsers.token(TokenType.FALSE).map(token ->
                        new BooleanLiteralExpression(false)
                );

        Parser<Expression> nullLiteral =
                Parsers.token(TokenType.NULL).map(token ->
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

    // =====================================================
    // HELPERS
    // =====================================================

    private Parser<String> identifierNameParser() {

        return Parsers.identifier().map(
                Token::lexeme
        );
    }

    private String stripQuotes(String lexeme) {

        return lexeme.length() < 2
                ? lexeme
                : lexeme.substring(
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
