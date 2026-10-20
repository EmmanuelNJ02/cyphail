package cr.ac.una.eif400.cyphail.ast.visitor;

import cr.ac.una.eif400.cyphail.ast.AstNode;
import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.clause.MatchClause;
import cr.ac.una.eif400.cyphail.ast.clause.Projection;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionItem;
import cr.ac.una.eif400.cyphail.ast.clause.RemoveClause;
import cr.ac.una.eif400.cyphail.ast.clause.ReturnClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetAllProperties;
import cr.ac.una.eif400.cyphail.ast.clause.SetClause;
import cr.ac.una.eif400.cyphail.ast.clause.SetItem;
import cr.ac.una.eif400.cyphail.ast.clause.SetLabels;
import cr.ac.una.eif400.cyphail.ast.clause.SetProperty;
import cr.ac.una.eif400.cyphail.ast.clause.UpdatingClause;
import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
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
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipPattern;
import cr.ac.una.eif400.cyphail.ast.pattern.RelationshipStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Visitor that produces the logical tree representation
 * used by the Cyphail .tree REPL command.
 *
 * <p>Sprint P1 supports visualization of query clauses,
 * node patterns, properties, projections and expressions
 * in preorder notation.</p>
 *
 * <p>Sprint P2 extends the tree representation with
 * relationship paths, relationship variables, types,
 * properties and directions.</p>
 *
 * <p>Supported relationship directions:</p>
 * <ul>
 *     <li>OUT: outgoing relationship</li>
 *     <li>IN: incoming relationship</li>
 *     <li>NONE: undirected relationship</li>
 * </ul>
 *
 * <p>Node-only patterns retain the original Sprint P1
 * representation for backward compatibility.</p>
 *
 * <p>Examples of preorder expressions:</p>
 *
 * <pre>
 * m.year        -> (. m year)
 * m.year > 1990 -> (> (. m year) 1990)
 * m.price > 9.5 -> (> (. m price) 9.5)
 * </pre>
 *
 * <p>Sprint P2 additionally renders SET and REMOVE
 * clauses for node and relationship properties.</p>
 *
 * <p>The visitor is independent from the parser and REPL.
 * It receives an existing AST and produces its logical
 * textual representation.</p>
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
 * Relationship path visualization with backward
 * compatibility for Sprint P1, support for
 * floating-point literal expressions,
 * standalone CREATE statements without MATCH,
 * SET clause visualization and REMOVE clause
 * visualization.</p>
 */
public final class AstTreePrinter implements AstVisitor<String> {

    /**
     * Produces the tree representation of a query.
     *
     * @param query query AST
     * @return logical tree representation
     */
    public String print(Query query) {
        Objects.requireNonNull(
                query,
                "Query cannot be null"
        );

        return visitQuery(query);
    }

    @Override
    public String visitQuery(Query query) {
        StringBuilder output = new StringBuilder();

        output.append("Query{\n");

        if (query.match() != null) {
            output.append(
                    indent(
                            visitMatchClause(query.match()),
                            2
                    )
            );
        } else {
            output.append("  Match: null");
        }

        output.append("\n");

        if (query.where().isPresent()) {
            output.append(
                    indent(
                            visitWhereClause(query.where().get()),
                            2
                    )
            );
        } else {
            output.append("  Where: null");
        }

        output.append("\n");

        output.append(
                indent(
                        renderUpdates(query.updates()),
                        2
                )
        );

        output.append("\n");

        if (query.returnClause().isPresent()) {
            output.append(
                    indent(
                            visitReturnClause(
                                    query.returnClause().get()
                            ),
                            2
                    )
            );
        } else {
            output.append("  Return: null");
        }

        output.append("\n}");

        return output.toString();
    }

    @Override
    public String visitMatchClause(MatchClause clause) {
        return """
                Match:{
                  Patterns:%s
                }""".formatted(
                indentAfterFirstLine(
                        visitPattern(clause.pattern()),
                        2
                )
        );
    }

    @Override
    public String visitWhereClause(WhereClause clause) {
        return """
                Where:{
                  Expr: %s
                }""".formatted(
                visitExpression(clause.expression())
        );
    }

    @Override
    public String visitCreateClause(CreateClause clause) {
        return """
                Create:{
                  Patterns:%s
                }""".formatted(
                indentAfterFirstLine(
                        visitPattern(clause.pattern()),
                        2
                )
        );
    }

    @Override
    public String visitDeleteClause(DeleteClause clause) {
        List<String> expressions = clause.expressions()
                .stream()
                .map(this::visitExpression)
                .toList();

        return """
                Delete:{
                  Detach: %s
                  Expressions: [%s]
                }""".formatted(
                clause.detach(),
                String.join(", ", expressions)
        );
    }

    /**
     * Renders the SET items in their original order.
     *
     * <p>SET uses the existing SetClause and SetItem
     * AST structure.</p>
     *
     * @param clause SET clause
     * @return formatted SET clause
     */
    private String renderSetClause(SetClause clause) {
        List<String> items = clause.items()
                .stream()
                .map(this::renderSetItem)
                .toList();

        return """
                Set:{
                  Items: [%s]
                }""".formatted(
                String.join(", ", items)
        );
    }

    /**
     * Renders an individual SET item.
     *
     * <p>Property assignments work for both node
     * and relationship variables.</p>
     *
     * @param item SET item
     * @return formatted SET item
     */
    private String renderSetItem(SetItem item) {
        if (item instanceof SetProperty property) {
            return "{Property: "
                    + visitExpression(property.property())
                    + ", Value: "
                    + visitExpression(property.value())
                    + "}";
        }

        if (item instanceof SetAllProperties allProperties) {
            return "{Variable: "
                    + visitExpression(allProperties.variable())
                    + ", Value: "
                    + visitExpression(allProperties.value())
                    + "}";
        }

        if (item instanceof SetLabels labels) {
            return "{Variable: "
                    + visitExpression(labels.variable())
                    + ", Labels: ["
                    + String.join(", ", labels.labels())
                    + "]}";
        }

        throw new IllegalArgumentException(
                "Unsupported SET item type: "
                        + item.getClass().getName()
        );
    }

    /**
     * Renders a REMOVE clause for node or relationship
     * properties.
     *
     * <p>Examples:</p>
     * <pre>
     * REMOVE p.discount
     * REMOVE r.grade
     * REMOVE p.discount, p.oldPrice
     * </pre>
     *
     * <p>All properties are displayed in their original
     * source order, using the existing expression
     * rendering format.</p>
     *
     * @param clause REMOVE clause
     * @return formatted REMOVE clause
     */
    private String renderRemoveClause(RemoveClause clause) {
        List<String> properties = clause.properties()
                .stream()
                .map(this::visitPropertyExpression)
                .toList();

        return """
                Remove:{
                  Properties: [%s]
                }""".formatted(
                String.join(", ", properties)
        );
    }

    @Override
    public String visitReturnClause(ReturnClause clause) {
        return """
                Return:{
                  Projection:%s
                }""".formatted(
                indentAfterFirstLine(
                        visitProjection(clause.projection()),
                        2
                )
        );
    }

    /**
     * Renders comma-separated pattern paths.
     *
     * <p>Isolated nodes retain the Sprint P1 tree format.
     * Relationship paths include all relationships and
     * destination nodes in their original source order.</p>
     *
     * @param pattern pattern AST
     * @return formatted pattern collection
     */
    @Override
    public String visitPattern(Pattern pattern) {
        StringBuilder output = new StringBuilder();

        output.append("[\n");

        for (int index = 0;
             index < pattern.paths().size();
             index++) {

            PatternPath path = pattern.paths().get(index);

            output.append(
                    indent(
                            renderPatternPath(path),
                            2
                    )
            );

            if (index < pattern.paths().size() - 1) {
                output.append(",");
            }

            output.append("\n");
        }

        output.append("]");

        return output.toString();
    }

    /**
     * Renders an isolated node or a complete relationship path.
     *
     * <p>A relationship path is represented by its first node,
     * followed by ordered relationship and destination-node
     * pairs.</p>
     *
     * @param path pattern path to render
     * @return formatted path representation
     */
    private String renderPatternPath(PatternPath path) {
        if (!path.hasRelationships()) {
            return visitNodePattern(path.firstNode());
        }

        StringBuilder output = new StringBuilder();

        output.append("PatternPath:{\n");

        output.append(
                indent(
                        visitNodePattern(path.firstNode()),
                        2
                )
        );

        for (RelationshipStep step : path.steps()) {
            output.append("\n");

            output.append(
                    indent(
                            renderRelationshipPattern(
                                    step.relationship()
                            ),
                            2
                    )
            );

            output.append("\n");

            output.append(
                    indent(
                            visitNodePattern(
                                    step.rightNode()
                            ),
                            2
                    )
            );
        }

        output.append("\n}");

        return output.toString();
    }

    /**
     * Renders a relationship with its variable, types,
     * direction and properties.
     *
     * <p>The direction is relative to the preceding
     * and following nodes of the path.</p>
     *
     * @param relationship relationship pattern
     * @return formatted relationship representation
     */
    private String renderRelationshipPattern(
            RelationshipPattern relationship
    ) {
        String variable =
                relationship.variable().orElse("null");

        String types = relationship.types().isEmpty()
                ? ""
                : String.join(
                        ", ",
                        relationship.types()
                );

        String properties = renderProperties(
                relationship.properties()
        );

        return """
                PatternRelationship:{
                  var: %s
                  types: [%s]
                  direction: %s
                  properties: %s
                }""".formatted(
                variable,
                types,
                relationship.direction().name(),
                properties
        );
    }

    @Override
    public String visitNodePattern(NodePattern node) {
        String variable = node.variable().orElse("null");

        String labels = node.labels().isEmpty()
                ? ""
                : String.join(", ", node.labels());

        String properties = renderProperties(
                node.properties()
        );

        return """
                PatternNode:{
                  var: %s
                  labels: [%s]
                  properties: %s
                }""".formatted(
                variable,
                labels,
                properties
        );
    }

    @Override
    public String visitPatternProperty(PatternProperty property) {
        return property.name()
                + ": "
                + visitExpression(property.value());
    }

    @Override
    public String visitProjection(Projection projection) {
        StringBuilder output = new StringBuilder();

        output.append("{\n");
        output.append("  Items:[");

        List<String> items = new ArrayList<>();

        if (projection.wildcard()) {
            items.add("*");
        }

        for (ProjectionItem item : projection.items()) {
            items.add(
                    visitProjectionItem(item)
            );
        }

        if (!items.isEmpty()) {
            output.append("\n");

            for (int index = 0;
                 index < items.size();
                 index++) {

                output.append(
                        indent(
                                items.get(index),
                                4
                        )
                );

                if (index < items.size() - 1) {
                    output.append(",");
                }

                output.append("\n");
            }

            output.append("  ");
        }

        output.append("]\n");

        output.append(
                "  Modifiers:"
                        + renderModifiers(projection)
                        + "\n"
        );

        output.append("}");

        return output.toString();
    }

    @Override
    public String visitProjectionItem(ProjectionItem item) {
        String expression = visitExpression(
                item.expression()
        );

        if (item.alias().isPresent()) {
            return "{as "
                    + expression
                    + " "
                    + item.alias().get()
                    + "}";
        }

        return "{"
                + expression
                + "}";
    }

    @Override
    public String visitBinaryExpression(
            BinaryExpression expression
    ) {
        return "("
                + expression.operator().symbol()
                + " "
                + visitExpression(expression.left())
                + " "
                + visitExpression(expression.right())
                + ")";
    }

    @Override
    public String visitPropertyExpression(
            PropertyExpression expression
    ) {
        return "(. "
                + visitExpression(expression.target())
                + " "
                + expression.property()
                + ")";
    }

    @Override
    public String visitVariableExpression(
            VariableExpression expression
    ) {
        return expression.name();
    }

    @Override
    public String visitIntegerLiteralExpression(
            IntegerLiteralExpression expression
    ) {
        return Long.toString(
                expression.value()
        );
    }

    @Override
    public String visitStringLiteralExpression(
            StringLiteralExpression expression
    ) {
        return "\""
                + escapeString(expression.value())
                + "\"";
    }

    @Override
    public String visitBooleanLiteralExpression(
            BooleanLiteralExpression expression
    ) {
        return expression.value()
                ? "TRUE"
                : "FALSE";
    }

    @Override
    public String visitNullLiteralExpression(
            NullLiteralExpression expression
    ) {
        return "NULL";
    }

    /**
     * Renders an expression according to its AST type.
     *
     * <p>Sprint P2 adds support for FloatLiteralExpression.
     * Existing expression types retain their original
     * representation.</p>
     *
     * @param expression expression to render
     * @return textual representation
     */
    private String visitExpression(
            Expression expression
    ) {
        if (expression instanceof BinaryExpression binary) {
            return visitBinaryExpression(binary);
        }

        if (expression instanceof PropertyExpression property) {
            return visitPropertyExpression(property);
        }

        if (expression instanceof VariableExpression variable) {
            return visitVariableExpression(variable);
        }

        if (expression instanceof IntegerLiteralExpression integer) {
            return visitIntegerLiteralExpression(integer);
        }

        if (expression instanceof FloatLiteralExpression decimal) {
            return Double.toString(decimal.value());
        }

        if (expression instanceof StringLiteralExpression string) {
            return visitStringLiteralExpression(string);
        }

        if (expression instanceof BooleanLiteralExpression bool) {
            return visitBooleanLiteralExpression(bool);
        }

        if (expression instanceof NullLiteralExpression nullLiteral) {
            return visitNullLiteralExpression(nullLiteral);
        }

        throw new IllegalArgumentException(
                "Unsupported expression type: "
                        + expression.getClass().getName()
        );
    }

    private String renderUpdates(
            List<UpdatingClause> updates
    ) {
        if (updates.isEmpty()) {
            return "Updates:[]";
        }

        StringBuilder output = new StringBuilder();

        output.append("Updates:[\n");

        for (int index = 0;
             index < updates.size();
             index++) {

            UpdatingClause update = updates.get(index);

            String rendered = visitUpdatingClause(
                    update
            );

            output.append(
                    indent(rendered, 2)
            );

            if (index < updates.size() - 1) {
                output.append(",");
            }

            output.append("\n");
        }

        output.append("]");

        return output.toString();
    }

    /**
     * Selects the renderer for each updating clause.
     *
     * <p>Sprint P2 adds SET and REMOVE while retaining
     * CREATE and DELETE behavior.</p>
     *
     * @param update updating clause
     * @return formatted clause
     */
    private String visitUpdatingClause(
            UpdatingClause update
    ) {
        if (update instanceof CreateClause create) {
            return visitCreateClause(create);
        }

        if (update instanceof SetClause set) {
            return renderSetClause(set);
        }

        if (update instanceof RemoveClause remove) {
            return renderRemoveClause(remove);
        }

        if (update instanceof DeleteClause delete) {
            return visitDeleteClause(delete);
        }

        throw new IllegalArgumentException(
                "Unsupported updating clause type: "
                        + update.getClass().getName()
        );
    }

    private String renderProperties(
            List<PatternProperty> properties
    ) {
        if (properties.isEmpty()) {
            return "[]";
        }

        List<String> rendered = properties
                .stream()
                .map(this::visitPatternProperty)
                .toList();

        return "["
                + String.join(", ", rendered)
                + "]";
    }

    private String renderModifiers(
            Projection projection
    ) {
        var modifiers = projection.modifiers();

        if (modifiers.orderBy().isEmpty()
                && modifiers.skip().isEmpty()
                && modifiers.limit().isEmpty()) {

            return "[]";
        }

        List<String> rendered = new ArrayList<>();

        if (!modifiers.orderBy().isEmpty()) {
            rendered.add(
                    "ORDER_BY("
                            + modifiers.orderBy().size()
                            + ")"
            );
        }

        modifiers.skip().ifPresent(
                expression -> rendered.add(
                        "SKIP "
                                + visitExpression(expression)
                )
        );

        modifiers.limit().ifPresent(
                expression -> rendered.add(
                        "LIMIT "
                                + visitExpression(expression)
                )
        );

        return "["
                + String.join(", ", rendered)
                + "]";
    }

    private String escapeString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String indent(
            String text,
            int spaces
    ) {
        String prefix = " ".repeat(spaces);

        return text.replace(
                "\n",
                "\n" + prefix
        ).transform(
                value -> prefix + value
        );
    }

    private String indentAfterFirstLine(
            String text,
            int spaces
    ) {
        String prefix = " ".repeat(spaces);

        return text.replace(
                "\n",
                "\n" + prefix
        );
    }
}