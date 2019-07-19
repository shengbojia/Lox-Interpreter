package com.shengbojia.lox;

import com.shengbojia.lox.Expr;
import com.shengbojia.lox.errors.RuntimeError;
import com.shengbojia.lox.token.Token;
import com.shengbojia.lox.token.TokenType;

public class Interpreter implements Expr.Visitor<Object> {

    void interpret(Expr expr) {
        try {
            Object value = evaluate(expr);
            System.out.println(makeIntoString(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private String makeIntoString(Object o) {

        // Special cases for nil and Lox numbers
        if (o == null) {
            return "nil";
        }

        if (o instanceof Double) {
            String text = o.toString();

            // Make it into Lox-style integer double
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        // Everything else is same in Lox as java
        return o.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        // Simply evaluate the subexpression within the grouping
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {

        // Post-order traversal, so evaluate the child nodes first
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                // Lox is dynamically typed, hence the casting which occurs at runtime
                return -(double) right;
        }

        // Should be unreachable code
        return null;
    }

    /**
     * Lox is similar to Ruby: 'false' and 'nil' are falsey and everything else is truthy.
     *
     * @param object the input to determine whether or is truthy
     * @return true if truthy, false if falsey
     */
    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        } else if (object instanceof Boolean) {
            return (boolean) object;
        }

        return true;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {

        // Again, our interpreter evaluates the children first
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {

            // Arithmetic
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0) {
                    throw new RuntimeError(expr.operator, "Cannot divide by zero.");
                }
                return (double) left / (double) right;
            case PLUS:
                // Overloaded as string concatenation
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                } else if (left instanceof String) {
                    return (String) left + makeIntoString(right);
                } else if (right instanceof String) {
                    return makeIntoString(left
                    ) + right;
                } else {
                    throw new RuntimeError(expr.operator, "Operands must both be numbers or one of them a string.");
                }
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;

            // Comparison
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;

            // Equality
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case BANG_EQUAL:
                return !isEqual(left, right);

            // Comma
            case COMMA:
                return right; // comma op evaluates left, discards it, and then returns the right
        }

        // Should be unreachable
        return null;
    }

    /**
     * Lox equality '==' is defined as follows: For any non-null x, y, z, we have
     * <p>
     * i) Reflexive - x == x is true
     * ii) Symmetric - x == y is true iff y == x is true
     * iii) Transitive - if x == y is true and y == z is true, then x == z is true
     * iv) Consistent - if x == y is true/false, then it will always be true/false provided x, y are not altered
     * v) For null/nil - x == nil and nil == x is always false, and nil == nil is true
     *
     * @param o1
     * @param o2
     * @return
     */
    private boolean isEqual(Object o1, Object o2) {

        // nil is only equal to nil
        if (o1 == null && o2 == null) {
            return true;

            // Make sure o1 is non-null in order to use java's Object.equals()
        } else if (o1 == null) {
            return false;
        }

        return o1.equals(o2);
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {

        // Only evaluate the left operand right now, since only one of the latter two execute
        Object left = evaluate(expr.left);

        switch (expr.firstOp.type) {
            case QUERY:
                if (expr.secondOp.type == TokenType.COLON) {
                    // TODO: Dynamically typed so I probably (???) don't have to check same type of middle vs right
                    // Which of the latter two operands gets evaluated is determined here
                    return isTruthy(left) ? evaluate(expr.middle) : evaluate(expr.right);
                }
        }

        // Unreachable
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers");
    }
}