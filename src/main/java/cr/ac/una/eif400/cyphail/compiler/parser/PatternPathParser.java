package cr.ac.una.eif400.cyphail.compiler.parser;

import cr.ac.una.eif400.cyphail.ast.pattern.NodePattern;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternPath;
import cr.ac.una.eif400.cyphail.ast.pattern.PatternProperty;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipDirection;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipPattern;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipStep;
import cr.ac.una.eif400.cyphail.compiler.lexer.TokenType;
import cr.ac.una.eif400.cyphail.compiler.parser.combinator.Parser;
import cr.ac.una.eif400.cyphail.compiler.parser.combinator.Parsers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Functional parser for relationship paths in Cyphail Sprint P2.
 *
 * <p>This parser extends the existing parser combinator approach
 * without duplicating the Sprint P1 node and property parsers.</p>
 *
 * <p>Supported relationship directions:</p>
 * <ul>
 *     <li>OUT: (a)-[r:KNOWS]->(b)</li>
 *     <li>IN: (a)&lt;-[r:KNOWS]-(b)</li>
 *     <li>NONE: (a)-[r:KNOWS]-(b)</li>
 * </ul>
 *
 * <p>Relationship variables, types and properties are optional.
 * Multiple relationship steps are preserved in source order.</p>
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
 *
 * <p>Sprint P2 contribution:
 * Functional relationship path parsing.</p>
 */
public final class PatternPathParser {

    private PatternPathParser() {
    }

    /**
     * Builds a parser for a node followed by zero or more
     * relationship steps.
     *
     * @param nodeParser existing node pattern parser
     * @param identifierParser existing identifier parser
     * @param typeParser existing colon-prefixed type parser
     * @param propertiesParser existing braced property parser
     * @return functional parser for a complete pattern path
     */
    public static Parser<PatternPath> path(
            Parser<NodePattern> nodeParser,
            Parser<String> identifierParser,
            Parser<String> typeParser,
            Parser<List<PatternProperty>> propertiesParser
    ) {
        Objects.requireNonNull(nodeParser, "Node parser cannot be null");
        Objects.requireNonNull(
                identifierParser,
                "Identifier parser cannot be null"
        );
        Objects.requireNonNull(typeParser, "Type parser cannot be null");
        Objects.requireNonNull(
                propertiesParser,
                "Properties parser cannot be null"
        );

        return nodeParser.flatMap(
                firstNode -> Parsers.many(
                        relationshipStep(
                                nodeParser,
                                identifierParser,
                                typeParser,
                                propertiesParser
                        )
                ).map(
                        steps -> new PatternPath(firstNode, steps)
                )
        );
    }

    /**
     * Parses one relationship and its following node.
     */
    private static Parser<RelationshipStep> relationshipStep(
            Parser<NodePattern> nodeParser,
            Parser<String> identifierParser,
            Parser<String> typeParser,
            Parser<List<PatternProperty>> propertiesParser
    ) {
        Parser<RelationshipStep> incoming =
                Parsers.token(TokenType.ARROW_LEFT).flatMap(
                        arrow -> relationshipBody(
                                identifierParser,
                                typeParser,
                                propertiesParser
                        ).flatMap(
                                body -> Parsers.token(
                                        TokenType.MINUS
                                ).flatMap(
                                        minus -> nodeParser.map(
                                                rightNode ->
                                                        new RelationshipStep(
                                                                buildRelationship(
                                                                        body,
                                                                        RelationshipDirection.IN
                                                                ),
                                                                rightNode
                                                        )
                                        )
                                )
                        )
                );

        Parser<RelationshipStep> outgoingOrUndirected =
                Parsers.token(TokenType.MINUS).flatMap(
                        minus -> relationshipBody(
                                identifierParser,
                                typeParser,
                                propertiesParser
                        ).flatMap(
                                body -> closingDirection().flatMap(
                                        direction -> nodeParser.map(
                                                rightNode ->
                                                        new RelationshipStep(
                                                                buildRelationship(
                                                                        body,
                                                                        direction
                                                                ),
                                                                rightNode
                                                        )
                                        )
                                )
                        )
                );

        return Parsers.or(incoming, outgoingOrUndirected);
    }

    /**
     * Parses a relationship body enclosed in square brackets.
     */
    private static Parser<RelationshipBody> relationshipBody(
            Parser<String> identifierParser,
            Parser<String> typeParser,
            Parser<List<PatternProperty>> propertiesParser
    ) {
        return Parsers.token(TokenType.LEFT_BRACKET).flatMap(
                leftBracket -> Parsers.optional(
                        identifierParser
                ).flatMap(
                        variable -> Parsers.many(
                                typeParser
                        ).flatMap(
                                types -> Parsers.optional(
                                        propertiesParser
                                ).flatMap(
                                        properties -> Parsers.token(
                                                TokenType.RIGHT_BRACKET
                                        ).map(
                                                rightBracket ->
                                                        new RelationshipBody(
                                                                variable,
                                                                types,
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

    /**
     * Parses the closing direction of a relationship that
     * starts with a minus token.
     */
    private static Parser<RelationshipDirection> closingDirection() {
        Parser<RelationshipDirection> outgoing =
                Parsers.token(TokenType.ARROW_RIGHT).map(
                        token -> RelationshipDirection.OUT
                );

        Parser<RelationshipDirection> undirected =
                Parsers.token(TokenType.MINUS).map(
                        token -> RelationshipDirection.NONE
                );

        return Parsers.or(outgoing, undirected);
    }

    /**
     * Constructs an immutable relationship AST node.
     */
    private static RelationshipPattern buildRelationship(
            RelationshipBody body,
            RelationshipDirection direction
    ) {
        return new RelationshipPattern(
                body.variable(),
                body.types(),
                body.properties(),
                direction
        );
    }

    /**
     * Intermediate representation of relationship content.
     */
    private record RelationshipBody(
            Optional<String> variable,
            List<String> types,
            List<PatternProperty> properties
    ) {
    }
}