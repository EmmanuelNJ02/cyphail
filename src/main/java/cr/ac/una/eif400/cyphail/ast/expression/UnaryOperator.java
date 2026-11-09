package cr.ac.una.eif400.cyphail.ast.expression;

/**
 * Unary operators supported by Cyphail expressions.
 *
 * NOT negates a boolean condition.
 *
 * Project: Cyphail - Sprint P2
 * Course: EIF400 - Programming Paradigms
 * University: National University of Costa Rica
 * Group Code: 04-10am
 */
public enum UnaryOperator {

    NOT("NOT");

    private final String symbol;

    UnaryOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }
}
