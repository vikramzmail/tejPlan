/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.math;

import java.util.Stack;

import com.tejas.math.func.Expression;
import com.tejas.math.func.Function;
import com.tejas.math.numbers.Real;
import com.tejas.math.oper.BinaryOp;
import com.tejas.math.oper.Operation;
import com.tejas.math.oper.UnaryOp;
import com.tejas.math.oper.bi.*;
import com.tejas.math.oper.unary.*;
import com.tejas.math.oper.unary.trig.*;
import com.tejas.math.oper.unary.trig.hyperbol.CosecantH;
import com.tejas.math.oper.unary.trig.hyperbol.CosineH;
import com.tejas.math.oper.unary.trig.hyperbol.CotangentH;
import com.tejas.math.oper.unary.trig.hyperbol.SecantH;
import com.tejas.math.oper.unary.trig.hyperbol.SineH;
import com.tejas.math.oper.unary.trig.hyperbol.TangentH;
import com.tejas.math.oper.unary.trig.hyperbol.inv.ArCosecantH;
import com.tejas.math.oper.unary.trig.hyperbol.inv.ArCosineH;
import com.tejas.math.oper.unary.trig.hyperbol.inv.ArCotangentH;
import com.tejas.math.oper.unary.trig.hyperbol.inv.ArSecantH;
import com.tejas.math.oper.unary.trig.hyperbol.inv.ArSineH;
import com.tejas.math.oper.unary.trig.hyperbol.inv.ArTangentH;
import com.tejas.math.oper.unary.trig.inv.ArcCosecant;
import com.tejas.math.oper.unary.trig.inv.ArcCosine;
import com.tejas.math.oper.unary.trig.inv.ArcCotangent;
import com.tejas.math.oper.unary.trig.inv.ArcSecant;
import com.tejas.math.oper.unary.trig.inv.ArcSine;
import com.tejas.math.oper.unary.trig.inv.ArcTangent;

import javolution.text.Text;

/**
 *
 * @author Kristopher T. Beck
 */
public class Parser {

    Text var = Text.EMPTY;
    Mode type;
    Expression root = new Expression();
    Expression expr = root;
    Stack<Expression> groupStack = new Stack<Expression>();
    boolean checkForMinus;

    public enum Mode {

        STRING, NUMBER, NUMBER_EXT, BEGIN_GROUP, END_GROUP, OTHER
    }

    public Function parse(String expr) {
        var = Text.EMPTY;
        type = null;
        for (int i = 0; i < expr.length(); i++) {
            prosess(expr.charAt(i));
        }
        return null;
    }

    public void prosess(char ch) {
        if (checkForMinus) {
            if (ch == '-') {
                prosess("uminus");
            }
            checkForMinus = false;
        } else if (type == Mode.NUMBER_EXT && (ch == '-' || Character.isDigit(ch))) {
            type = Mode.NUMBER;
            var.concat(Text.valueOf(ch));
            return;
        } else if (type == Mode.NUMBER && ch == 'e') {
            type = Mode.NUMBER_EXT;
            var.concat(Text.valueOf(ch));
            return;
        } else if (ch == '(') {
            prosess(ch, Mode.BEGIN_GROUP);
            expr.setEnclosed(true);
            checkForMinus = true;
        } else if (ch == ')') {
            prosess(ch, Mode.END_GROUP);
        } else if (Character.isLetter(ch)) {
            prosess(ch, Mode.STRING);
        } else if (Character.isDigit(ch)) {
            prosess(ch, Mode.NUMBER);
        } else if (ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '^' || ch == '=') {
            prosess(String.valueOf(ch));
        }
    }

    public void prosess(char c, Mode m) {
        if (!m.equals(type)) {
            if (type == Mode.STRING) {
                prosess(var.toString());
            } else if (type == Mode.NUMBER) {
                expr.addElement(new Constant(Real.valueOf(Double.parseDouble(var.toString()))));
            }
            if (m == Mode.BEGIN_GROUP) {
                Expression ex = new Expression();
                expr.addElement(ex);
                groupStack.push(expr);
                expr = ex;
            } else if (m == Mode.END_GROUP) {
                expr = groupStack.pop();
                //type = Mode.OTHER;
            } else if (m == Mode.STRING || m == Mode.NUMBER) {
                var = Text.valueOf(c);
            }
            type = m;
        } else {
            var.concat(Text.valueOf(c));
        }
    }

    private void prosess(String token) {
        if (type == Mode.STRING) {
            if (token.equals("e")) {
                expr.addElement(new E());
            } else if (token.equals("pi")) {
                expr.addElement(new Pi());
            } else {
                Operation op = createOperation(token);
                if (op != null) {
                    expr.addElement(op);
                } else {
                    Term term = new Term(token);
                    root.addVarable(token, term);
                    expr.addElement(term);
                }
            }
        } else if (type == Mode.NUMBER) {
            expr.addElement(new Constant(Real.valueOf(Double.parseDouble(token))));
        }
    }

    public Operation createOperation(String token) {
        Operation op = createUnary(token);
        if (op == null) {
            op = createBinary(token);
        }
        return op;
    }

    public UnaryOp createUnary(String token) {
        UnaryOp op = null;
        if (token.equals("abs")) {
            op = new Abs();
        } else if (token.equals("exp")) {
            op = new Exp();
        } else if (token.equals("log") || token.equals("ln")) {
            op = new Log();
        } else if (token.equals("log10")) {
            op = new Log10();
        } else if (token.equals("pow2") || token.equals("^2")) {
            op = new Pow2();
        } else if (token.equals("sqrt")) {
            op = new Sqrt();
        } else if (token.equals("uminus")) {
            op = new UMinus();
        } else if (token.equals("cos")) {
            op = new Cosine();
        } else if (token.equals("csc")) {
            op = new Cosecant();
        } else if (token.equals("cot")) {
            op = new Cotangent();
        } else if (token.equals("sec")) {
            op = new Secant();
        } else if (token.equals("sin")) {
            op = new Sine();
        } else if (token.equals("tan")) {
            op = new Tangent();
        } else if (token.equals("acos")) {
            op = new ArcCosine();
        } else if (token.equals("acsc")) {
            op = new ArcCosecant();
        } else if (token.equals("acot")) {
            op = new ArcCotangent();
        } else if (token.equals("asec")) {
            op = new ArcSecant();
        } else if (token.equals("asin")) {
            op = new ArcSine();
        } else if (token.equals("atan")) {
            op = new ArcTangent();
        } else if (token.equals("cosh")) {
            op = new CosineH();
        } else if (token.equals("csch")) {
            op = new CosecantH();
        } else if (token.equals("coth")) {
            op = new CotangentH();
        } else if (token.equals("sech")) {
            op = new SecantH();
        } else if (token.equals("sinh")) {
            op = new SineH();
        } else if (token.equals("tanh")) {
            op = new TangentH();
        } else if (token.equals("acosh")) {
            op = new ArCosineH();
        } else if (token.equals("acsch")) {
            op = new ArCosecantH();
        } else if (token.equals("acoth")) {
            op = new ArCotangentH();
        } else if (token.equals("asech")) {
            op = new ArSecantH();
        } else if (token.equals("asinh")) {
            op = new ArSineH();
        } else if (token.equals("atanh")) {
            op = new ArTangentH();
        }/* else if (token.equals("")) {
        function = new UnaryFunction(new );
        } else if (token.equals("ln")) {
        function = new UnaryFunction(new );
        }*/
        return op;
    }

    public BinaryOp createBinary(String token) {
        BinaryOp op = null;
        if (token.equals("+")) {
            op = new Plus();
        } else if (token.equals("-")) {
            op = new Minus();
        } else if (token.equals("*")) {
            op = new Times();
        } else if (token.equals("/")) {
            op = new Divide();
        } else if (token.equals("^") || token.equals("pow")) {
            op = new Pow();
        } else if (token.equals("=")) {
            op = new Assign();
        }/* else if (token.equals("")) {
        function = new BinaryFunction(new );
        }*/
        return op;
    }
}
