package cr.ac.una.eif400.cyphail.ast.visitor;

import cr.ac.una.eif400.cyphail.ast.AstNode;
import cr.ac.una.eif400.cyphail.ast.Query;
import cr.ac.una.eif400.cyphail.ast.clause.CreateClause;
import cr.ac.una.eif400.cyphail.ast.clause.DeleteClause;
import cr.ac.una.eif400.cyphail.ast.clause.MatchClause;
import cr.ac.una.eif400.cyphail.ast.clause.Projection;
import cr.ac.una.eif400.cyphail.ast.clause.ProjectionItem;
import cr.ac.una.eif400.cyphail.ast.clause.ReturnClause;
import cr.ac.una.eif400.cyphail.ast.clause.UpdatingClause;
import cr.ac.una.eif400.cyphail.ast.clause.WhereClause;
import cr.ac.una.eif400.cyphail.ast.expression.BinaryExpression;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Visitor that produces the logical tree representation used by
 * the Sprint P1 .tree REPL command.
 *
 * <p>The output follows the structure shown in the Sprint P1
 * specification: Query, Match, Where, Updates and Return. Expressions
 * are rendered in preorder notation.</p>
 *
 * <p>Examples of preorder expressions:</p>
 *
 * <pre>
 * m.year        -> (. m year)
 * m.year > 1990 -> (> (. m year) 1990)
 * </pre>
 *
 * <p>The visitor is intentionally independent from the parser and the
 * REPL. It receives an already built AST and returns only its logical
 * textual representation.</p>
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

        output.append(
                indent(
                        visitMatchClause(query.match()),
                        2
                )
        );

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

    @Override
    public String visitPattern(Pattern pattern) {
        StringBuilder output = new StringBuilder();

        output.append("[\n");

        for (int index = 0;
             index < pattern.nodes().size();
             index++) {

            output.append(
                    indent(
                            visitNodePattern(
                                    pattern.nodes().get(index)
                            ),
                            2
                    )
            );

            if (index < pattern.nodes().size() - 1) {
                output.append(",");
            }

            output.append("\n");
        }

        output.append("]");

        return output.toString();
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

    private String visitUpdatingClause(
            UpdatingClause update
    ) {
        if (update instanceof CreateClause create) {
            return visitCreateClause(create);
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