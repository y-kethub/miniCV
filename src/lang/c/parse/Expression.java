package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;
import lang.c.CType;

public class Expression extends CParseRule {
	// expression ::= term { expressionAdd }
	private CParseRule expression;

	public Expression(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return Term.isFirst(tk);
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CParseRule term = null, list = null;
		term = new Term(pcx);
		term.parse(pcx);
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		while (ExpressionAdd.isFirst(tk) || ExpressionSub.isFirst(tk)) {
			while (ExpressionAdd.isFirst(tk)) {
				list = new ExpressionAdd(pcx, term);
				list.parse(pcx);
				term = list;
				tk = ct.getCurrentToken(pcx);
			}

			while (ExpressionSub.isFirst(tk)) {
				list = new ExpressionSub(pcx, term);
				list.parse(pcx);
				term = list;
				tk = ct.getCurrentToken(pcx);
			}	
		}

		expression = term;
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (expression != null) {
			expression.semanticCheck(pcx);
			this.setCType(expression.getCType());		// expression の型をそのままコピー
			this.setConstant(expression.isConstant());
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; expression starts");
		if (expression != null) expression.codeGen(pcx);
		o.println(";;; expression completes");
	}
}

class ExpressionAdd extends CParseRule {
	// expressionAdd ::= '+' term
	private CToken op;
	private CParseRule left, right;
	public ExpressionAdd(CParseContext pcx, CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_PLUS;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		op = ct.getCurrentToken(pcx);
		// +の次の字句を読む
		CToken tk = ct.getNextToken(pcx);
		if (Term.isFirst(tk)) {
			right = new Term(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "+の後ろはtermです");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		// 足し算の型計算規則
		final int s[][] = {
				//		T_err			T_int
				{	CType.T_err,	CType.T_err,	CType.T_err },	// T_err
				{	CType.T_err,	CType.T_int,	CType.T_pint },	// T_int
				{	CType.T_err,	CType.T_pint,	CType.T_err }, //  T_pint
		};
		if (left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			int lt = left.getCType().getType();		// +の左辺の型
			int rt = right.getCType().getType();	// +の右辺の型
			int nt = s[lt][rt];						// 規則による型計算
			if (nt == CType.T_err) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]は足せません");
			}
			this.setCType(CType.getCType(nt));
			this.setConstant(left.isConstant() && right.isConstant());	// +の左右両方が定数のときだけ定数
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		if (left != null && right != null) {
			left.codeGen(pcx);		// 左部分木のコード生成を頼む
			right.codeGen(pcx);		// 右部分木のコード生成を頼む
			o.println("\tMOV\t-(R6), R0\t; ExpressionAdd: ２数を取り出して、足し、積む<" + op.toExplainString() + ">");
			o.println("\tMOV\t-(R6), R1\t; ExpressionAdd:");
			o.println("\tADD\tR1, R0\t; ExpressionAdd:");
			o.println("\tMOV\tR0, (R6)+\t; ExpressionAdd:");
		}
	}
}

class ExpressionSub extends CParseRule {
	// expressionSub ::= '-' term
	private CToken op;
	private CParseRule left, right;
	public ExpressionSub(CParseContext pcx, CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_MINUS;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		op = ct.getCurrentToken(pcx);
		// -の次の字句を読む
		CToken tk = ct.getNextToken(pcx);
		if (Term.isFirst(tk)) {
			right = new Term(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "-の後ろはtermです");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		// 引き算の型計算規則
		final int s[][] = {
				//		T_err			T_int
				{	CType.T_err,	CType.T_err,	CType.T_err },	// T_err
				{	CType.T_err,	CType.T_int,	CType.T_err },	// T_int
				{	CType.T_err,	CType.T_pint,	CType.T_int },
		};
		if (left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			int lt = left.getCType().getType();		// -の左辺の型
			int rt = right.getCType().getType();	// -の右辺の型
			int nt = s[lt][rt];						// 規則による型計算
			if (nt == CType.T_err) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]は引けません");
			}
			this.setCType(CType.getCType(nt));
			this.setConstant(left.isConstant() && right.isConstant());	// +の左右両方が定数のときだけ定数
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		if (left != null && right != null) {
			left.codeGen(pcx);		// 左部分木のコード生成を頼む
			right.codeGen(pcx);		// 右部分木のコード生成を頼む
			o.println("\tMOV\t-(R6), R0\t; ExpressionSub: ２数を取り出して、引き、積む<" + op.toExplainString() + ">");
			o.println("\tMOV\t-(R6), R1\t; ExpressionSub:");
			o.println("\tSUB\tR0, R1\t; ExpressionSub:");
			o.println("\tMOV\tR1, (R6)+\t; ExpressionSub:");
		}
	}
}
