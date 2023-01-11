package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;
import lang.c.CType;

public class Condition extends CParseRule {
	// conditon ::= TRUE | FALSE | expression (conditionLT | conditionLE | conditionGT | conditionGE | conditionEQ | conditionNE )

	private CParseRule condition;
	private int constValue;

	public Condition(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return Expression.isFirst(tk) || tk.getType() == CToken.TK_TRUE || tk.getType() == CToken.TK_FALSE ;
	}
	boolean ConditionRightIsFirst(CToken tk) {
		return ConditionLT.isFirst(tk) || ConditionLE.isFirst(tk) || ConditionGT.isFirst(tk) || ConditionGE.isFirst(tk) || ConditionEQ.isFirst(tk) || ConditionNE.isFirst(tk);
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		if(tk.getType() == CToken.TK_TRUE) {
			constValue = 1;
			tk = ct.getNextToken(pcx);
		}else if (tk.getType() == CToken.TK_FALSE) {
			constValue = 0;
			tk = ct.getNextToken(pcx);
		}else {
			CParseRule expr = null;
			CParseRule list = null;
			expr = new Expression(pcx);
			expr.parse(pcx);
			tk = ct.getCurrentToken(pcx);
			if(ConditionLT.isFirst(tk)) {
				list = new ConditionLT(pcx,expr);
			}else if(ConditionLE.isFirst(tk)) {
				list = new ConditionLE(pcx,expr);
			}else if(ConditionGT.isFirst(tk)) {
				list = new ConditionGT(pcx,expr);
			}else if(ConditionGE.isFirst(tk)) {
				list = new ConditionGE(pcx,expr);
			}else if(ConditionEQ.isFirst(tk)) {
				list = new ConditionEQ(pcx,expr);
			}else if(ConditionNE.isFirst(tk)) {
				list = new ConditionNE(pcx,expr);
			}else {
				pcx.fatalError(tk.toExplainString() + "比較できません");
			}
			list.parse(pcx);
			expr = list;
			tk = ct.getCurrentToken(pcx);
			condition = expr;
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (condition != null) {
			condition.semanticCheck(pcx);
			setCType(condition.getCType());
			setConstant(condition.isConstant());
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; condition starts");
		if (condition != null) condition.codeGen(pcx);
		else {
			o.println("\tMOV\t#" + constValue + ", (R6)+\t; Condition");
		}
		o.println(";;; condition completes");
	}
}

class ConditionLT extends CParseRule {
	// conditionLT ::= LT expression
	private CToken op;
	private CParseRule left, right;
	public ConditionLT(CParseContext pcx, CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_LT;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		op = tk;
		tk = ct.getNextToken(pcx);
		if (Expression.isFirst(tk)) {
			right = new Expression(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "<の後ろはexpression　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　です");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			if(!left.getCType().equals(right.getCType())) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]が一致しないので足せません");
			}else if(left.getCType().isCType(CType.T_err)) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]は比較できません");
			}else {
				this.setCType(CType.getCType(CType.T_bool));
				this.setConstant(true);
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; condition < starts");
		if(left != null && right != null) {
			left.codeGen(pcx);
			right.codeGen(pcx);
			int seq = pcx.getSeqId();

			o.println("\tMOV\t-(R6), R0\t; ConditionLT: 2数を取り出して比較する");
			o.println("\tMOV\t-(R6), R1\t; ConditionLT:");
			o.println("\tMOV\t#1, R2\t\t; ConditionLT: set true");
			o.println("\tCMP\tR0, R1\t\t; ConditionLT: R1 < R0 = R1 - R0 < 0");
			o.println("\tBRN\tLT" + seq + "\t\t; ConditionLT:");
			o.println("\tCLR\tR2\t\t; ConditionLT: set false");
			o.println("LT" + seq + ":\tMOV\tR2, (R6)+\t; ConditionLT:");
		}
		o.println(";;; condition < completes");
	}
}

class ConditionLE extends CParseRule {
	// conditionLE ::= LE expression
	private CToken op;
	private CParseRule left, right;
	public ConditionLE(CParseContext pcx, CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_LE;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		op = tk;
		tk = ct.getNextToken(pcx);
		if (Expression.isFirst(tk)) {
			right = new Expression(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "<=の後ろはexpression　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　です");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			if(!left.getCType().equals(right.getCType())) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]が一致しないので足せません");
			}else if(left.getCType().isCType(CType.T_err)) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]は比較できません");
			}else {
				this.setCType(CType.getCType(CType.T_bool));
				this.setConstant(true);
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; condition < starts");
		if(left != null && right != null) {
			left.codeGen(pcx);
			right.codeGen(pcx);
			int seq = pcx.getSeqId();

			o.println("\tMOV\t-(R6), R0\t; ConditionLE: 2数を取り出して比較する");
			o.println("\tMOV\t-(R6), R1\t; ConditionLE:");
			o.println("\tMOV\t#1, R2\t\t: ConditionLE: set true");
			o.println("\tCMP\tR0, R1\t\t; ConditionLE: R1 <= R0 = R1 - R0 <= 0");
			o.println("\tBRN\tLE" + seq + "\t\t; ConditionLE:");
			o.println("\tBRZ\tLE" + seq + "\t\t; ConditionLE:");
			o.println("\tCLR\tR2\t\t; ConditionLE: set false");
			o.println("LE" + seq + ":\tMOV\tR2, (R6)+\t; ConditionLE:");
		}
		o.println(";;; condition <= completes");
	}
}

class ConditionGT extends CParseRule {
	// conditionGT ::= GT expression
	private CToken op;
	private CParseRule left, right;
	public ConditionGT(CParseContext pcx, CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_GT;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		op = tk;
		tk = ct.getNextToken(pcx);
		if (Expression.isFirst(tk)) {
			right = new Expression(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + ">の後ろはexpression　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　です");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			if(!left.getCType().equals(right.getCType())) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]が一致しないので足せません");
			}else if(left.getCType().isCType(CType.T_err)) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]は比較できません");
			}else {
				this.setCType(CType.getCType(CType.T_bool));
				this.setConstant(true);
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; condition < starts");
		if(left != null && right != null) {
			left.codeGen(pcx);
			right.codeGen(pcx);
			int seq = pcx.getSeqId();

			o.println("\tMOV\t-(R6), R0\t; ConditionGT: 2数を取り出して比較する");
			o.println("\tMOV\t-(R6), R1\t; ConditionGT:");
			o.println("\tMOV\t#1, R2\t\t: ConditionGT: set true");
			o.println("\tCMP\tR1, R0\t\t; ConditionGT: R1 > R0 = R1 - R0 > 0");
			o.println("\tBRN\tGT" + seq + "\t\t; ConditionGT:");
			o.println("\tCLR\tR2\t\t; ConditionGT: set false");
			o.println("GT" + seq + ":\tMOV\tR2, (R6)+\t; ConditionGT:");
		}
		o.println(";;; condition > completes");
	}
}

class ConditionGE extends CParseRule {
	// conditionGE ::= GE expression
	private CToken op;
	private CParseRule left, right;
	public ConditionGE(CParseContext pcx, CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_GE;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		op = tk;
		tk = ct.getNextToken(pcx);
		if (Expression.isFirst(tk)) {
			right = new Expression(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + ">の後ろはexpression　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　です");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			if(!left.getCType().equals(right.getCType())) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]が一致しないので足せません");
			}else if(left.getCType().isCType(CType.T_err)) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]は比較できません");
			}else {
				this.setCType(CType.getCType(CType.T_bool));
				this.setConstant(true);
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; condition < starts");
		if(left != null && right != null) {
			left.codeGen(pcx);
			right.codeGen(pcx);
			int seq = pcx.getSeqId();

			o.println("\tMOV\t-(R6), R0\t; ConditionGE: 2数を取り出して比較する");
			o.println("\tMOV\t-(R6), R1\t; ConditionGE:");
			o.println("\tMOV\t#1, R2\t\t: ConditionGE: set true");
			o.println("\tCMP\tR1, R0\t\t; ConditionGE: R1 >= R0 = R1 - R0 >= 0");
			o.println("\tBRN\tGE" + seq + "\t\t; ConditionGE:");
			o.println("\tBRZ\tGE" + seq + "\t\t; ConditionGE:");
			o.println("\tCLR\tR2\t\t; ConditionGE: set false");
			o.println("GE" + seq + ":\tMOV\tR2, (R6)+\t; ConditionGE:");
		}
		o.println(";;; condition >= completes");
	}
}

class ConditionEQ extends CParseRule {
	// conditionEQ ::= EQ expression
	private CToken op;
	private CParseRule left, right;
	public ConditionEQ(CParseContext pcx, CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_EQ;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		op = tk;
		tk = ct.getNextToken(pcx);
		if (Expression.isFirst(tk)) {
			right = new Expression(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "==の後ろはexpression　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　です");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			if(!left.getCType().equals(right.getCType())) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]が一致しないので足せません");
			}else if(left.getCType().isCType(CType.T_err)) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]は比較できません");
			}else {
				this.setCType(CType.getCType(CType.T_bool));
				this.setConstant(true);
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; condition < starts");
		if(left != null && right != null) {
			left.codeGen(pcx);
			right.codeGen(pcx);
			int seq = pcx.getSeqId();

			o.println("\tMOV\t-(R6), R0\t; ConditionEQ: 2数を取り出して比較する");
			o.println("\tMOV\t-(R6), R1\t; ConditionEQ:");
			o.println("\tMOV\t#1, R2\t\t: ConditionEQ: set true");
			o.println("\tCMP\tR0, R1\t\t; ConditionEQ: R1 == R0 = R1 - R0 == 0");
			o.println("\tBRZ\tEQ" + seq + "\t\t; ConditionEQ:");
			o.println("\tCLR\tR2\t\t; ConditionEQ: set false");
			o.println("EQ" + seq + ":\tMOV\tR2, (R6)+\t; ConditionEQ:");
		}
		o.println(";;; condition == completes");
	}
}

class ConditionNE extends CParseRule {
	// conditionNE ::= NE expression
	private CToken op;
	private CParseRule left, right;
	public ConditionNE(CParseContext pcx, CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_NE;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		op = tk;
		tk = ct.getNextToken(pcx);
		if (Expression.isFirst(tk)) {
			right = new Expression(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "!=の後ろはexpression　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　　です");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			if(!left.getCType().equals(right.getCType())) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]が一致しないので足せません");
			}else if(left.getCType().isCType(CType.T_err)) {
				pcx.fatalError(op.toExplainString() + "左辺の型[" + left.getCType().toString() + "]は比較できません");
			}else {
				this.setCType(CType.getCType(CType.T_bool));
				this.setConstant(true);
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; condition < starts");
		if(left != null && right != null) {
			left.codeGen(pcx);
			right.codeGen(pcx);
			int seq = pcx.getSeqId();

			o.println("\tMOV\t-(R6), R0\t; ConditionNE: 2数を取り出して比較する");
			o.println("\tMOV\t-(R6), R1\t; ConditionNE:");
			o.println("\tCLR\tR2\t\t; ConditionNE: set false");
			o.println("\tCMP\tR0, R1\t\t; ConditionNE: R1 != R0 = R1 - R0 != 0");
			o.println("\tBRZ\tNE" + seq + "\t\t; ConditionNE:");
			o.println("\tMOV\t#1, R2\t\t;ConditionNE: set true");
			o.println("NE" + seq + ":\tMOV\tR2, (R6)+\t; ConditionNE");
		}
		o.println(";;; condition != completes");
	}
}