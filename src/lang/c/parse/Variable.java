package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;
import lang.c.CType;

public class Variable extends CParseRule {
	// variable ::= ident [ array ]
	private CParseRule ident, array;
	private CToken tk;
	public Variable(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return Ident.isFirst(tk);
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		tk = ct.getCurrentToken(pcx);
		ident = new Ident(pcx);
		ident.parse(pcx);
		tk = ct.getCurrentToken(pcx);
		if(Array.isFirst(tk)) {
			array = new Array(pcx);
			array.parse(pcx);
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(ident != null) {
			ident.semanticCheck(pcx);
			if(array != null) {
				array.semanticCheck(pcx);
				if(ident.getCType().isCType(CType.T_array) || ident.getCType().isCType(CType.T_parray)) {
					setCType(CType.toValue(ident.getCType()));
					setConstant(ident.isConstant());
				}else {
					pcx.fatalError(tk.toExplainString() + "添え字は配列のみ使用可能");	
				}
			}else {
				if (ident.getCType() == null ) {
					pcx.fatalError(tk.toExplainString() + "変数表記エラー");
				}
				if(ident.getCType().isCType(CType.T_array) || ident.getCType().isCType(CType.T_parray)) {
					pcx.fatalError(tk.toExplainString() + "配列の使い方に問題あり");
				}else {
					setCType(ident.getCType());
					setConstant(ident.isConstant());
				}
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; variable starts");
		if (ident != null) {
			ident.codeGen(pcx);
			if(array != null) {
				array.codeGen(pcx);
				o.println("\tMOV\t-(R6), R0\t; Variable: 配列の先頭アドレスをと添字を加算し、積む");
				o.println("\tADD\t-(R6), R0\t; Variable:");
				o.println("\tMOV\tR0, (R6)+\t; Variable:");
			}
		}
		o.println(";;; variable completes");
	}
}

class Array extends CParseRule {
	// array ::= LBRA expression RBRA
	private CParseRule cpr;
	private CToken lbra;
	public Array (CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_LBRA;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		lbra = ct.getCurrentToken(pcx);
		CToken tk = ct.getNextToken(pcx);
		if(Expression.isFirst(tk)) {
			cpr = new Expression(pcx);
			cpr.parse(pcx);
			tk = ct.getCurrentToken(pcx);
			if(tk.getType() != CToken.TK_RBRA) {
				pcx.fatalError(lbra.toExplainString() + "]が見つかりません");
			}
			ct.getNextToken(pcx);
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(cpr != null) {
			cpr.semanticCheck(pcx);
			if(cpr.getCType().isCType(CType.T_int)) {
				setCType(CType.getCType(CType.T_int));
				setConstant(true);
			}else {
				pcx.fatalError(lbra.toExplainString() + "添え字が整数ではありません");
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; array starts");
		if (cpr != null) {
			cpr.codeGen(pcx);
		}
		o.println(";;; array completes");
	}
}
