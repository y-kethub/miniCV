package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;
import lang.c.CType;

public class Statement extends CParseRule {
	// statement ::= statementAssign
	private CParseRule statement;

	public Statement(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return StatementAssign.isFirst(tk);
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		statement = new StatementAssign(pcx);
		statement.parse(pcx);
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(statement != null) {
			statement.semanticCheck(pcx);
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; statement starts");
		if (statement != null) statement.codeGen(pcx);
		o.println(";;; statement completes");
	}
}

class StatementAssign extends CParseRule {
	// statementAssign ::= primary ASSIGN expression SEMI
	private CToken tk, assign;
	private CParseRule left, right;
	public StatementAssign(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return Primary.isFirst(tk);
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		left = new Primary(pcx);
		left.parse(pcx);
		CTokenizer ct = pcx.getTokenizer();
		tk = ct.getCurrentToken(pcx);
		if(tk.getType() == CToken.TK_ASSIGN) {
			assign = tk;
			tk = ct.getNextToken(pcx);
			if(Expression.isFirst(tk)) {
				right = new Expression(pcx);
				right.parse(pcx);
				tk = ct.getCurrentToken(pcx);
				if(tk.getType() == CToken.TK_SEMI) {
					tk = ct.getNextToken(pcx);
				}else {
					pcx.fatalError(tk.toExplainString() + ";がありません");
				}
			}else {
				pcx.fatalError(assign.toExplainString() + "の後ろはexpression");
			}
		}else {
			pcx.fatalError(tk.toExplainString() + "=がありません");
		}
		
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			if(left.getCType().getType() != right.getCType().getType()) {
				pcx.fatalError(assign.toExplainString() + "左右の型[" + left.getCType().toString()+ "]と右辺の型[" + right.getCType().toString() + "]が一致しません");
			}else if(left.isConstant()) {
				pcx.fatalError(assign.toExplainString() + "定数には代入できません");
			}
			if(left.getCType().isCType(CType.T_array) || left.getCType().isCType(CType.T_parray)) {
				pcx.fatalError(assign.toExplainString() + "配列には代入できません");
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; StatementAssign starts");
		if (left != null && right != null) {
			left.codeGen(pcx);		// 左部分木のコード生成を頼む
			right.codeGen(pcx);		// 右部分木のコード生成を頼む
			o.println("\tMOV\t-(R6), R0\t; StatementAssign: 右辺と左辺の値を取り出して、左辺のアドレスに右辺の値を入れ積む<" + assign.toExplainString() + ">");
			o.println("\tMOV\t-(R6), R1\t; StatementAssign:");		
			o.println("\tMOV\tR0, (R1)\t; StatementAssign:");
		}
		o.println(";;; StatementAssign completes");
	}
}

