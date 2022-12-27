package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;
import lang.c.CType;

public class Term extends CParseRule {
	// term ::= factor { termMult | termDiv }
	private CParseRule factor;
	private CParseRule list;
	public Term(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return Factor.isFirst(tk);
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		factor = new Factor(pcx);
		factor.parse(pcx);

		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		while(termMult.isFirst(tk) || termDiv.isFirst(tk)) {
			if(termMult.isFirst(tk)) {
				list = new termMult(pcx, factor);
			}else if(termDiv.isFirst(tk)) {
				list = new termDiv(pcx, factor);
			}
			list.parse(pcx);
			factor = list;
			tk = ct.getCurrentToken(pcx);
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (factor != null) {
			factor.semanticCheck(pcx);
			this.setCType(factor.getCType());		// factor の型をそのままコピー
			this.setConstant(factor.isConstant());
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; term starts");
		if (factor != null) { factor.codeGen(pcx); }
		o.println(";;; term completes");
	}
}

class termMult extends CParseRule {
	// termMult ::= MULT factor
	CToken mult;
	private CParseRule right,left;
	public termMult(CParseContext pcx,CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_MULT;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている

		CTokenizer ct = pcx.getTokenizer();
		mult = ct.getCurrentToken(pcx);
		CToken tk = ct.getNextToken(pcx);
		if(Term.isFirst(tk)) {
			right = new Factor(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "*の後ろはfactorです");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		// 掛け算の型計算規則
		final int s[][] = {
				//		T_err			T_int					T_pint
				{	CType.T_err,	CType.T_err, CType.T_err },	// T_err
				{	CType.T_err,	CType.T_int,  CType.T_err },	// T_int
				{	CType.T_err, CType.T_err, CType.T_err }	// T_pint
		};
		if(left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			int lt = left.getCType().getType();
			int rt = right.getCType().getType();
			int nt = s[lt][rt]; 
			if( nt == CType.T_err) {
				pcx.fatalError(mult.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]は掛けられません");
			}
			this.setCType(CType.getCType(nt));
			this.setConstant(left.isConstant() && right.isConstant());
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		if (left != null && right != null) {
			left.codeGen(pcx);		// 左部分木のコード生成を頼む
			right.codeGen(pcx);		// 右部分木のコード生成を頼む
			o.println("\tJSR \tMUL\t\t\t; TermMult: 掛け算をするサブルーチンを呼び出す<" + mult.toExplainString() + ">");
			o.println("\tSUB\t#2, R6\t\t; TermMult: 引数を捨てる");
			o.println("\tMOV\tR2, (R6)+\t; TermMult: 結果が入っているR2の値をスタックに積む");
		}
	}
}

class termDiv extends CParseRule {
	// termDiv ::= DIV factor
	private CParseRule right, left;
	private CToken div;
	public termDiv(CParseContext pcx,CParseRule left) {
		this.left = left;
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_DIV;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		div = ct.getCurrentToken(pcx);
		CToken tk = ct.getNextToken(pcx); // /の次の字句を読む
		if(Term.isFirst(tk)) {
			right = new Factor(pcx);
			right.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "/の後ろはfactorです。");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		// 割り算の型計算規則
		final int s[][] = {
				//		T_err			T_int					T_pint
				{	CType.T_err,	CType.T_err, CType.T_err },	// T_err
				{	CType.T_err,	CType.T_int,  CType.T_err },	// T_int
				{	CType.T_err, CType.T_err, CType.T_err }	// T_pint
		};
		if(left != null && right != null) {
			left.semanticCheck(pcx);
			right.semanticCheck(pcx);
			int lt = left.getCType().getType();
			int rt = right.getCType().getType();
			int nt = s[lt][rt]; 
			if(nt == CType.T_err) {
				pcx.fatalError(div.toExplainString() + "左辺の型[" + left.getCType().toString() + "]と右辺の型[" + right.getCType().toString() + "]は割れません");
			}
			this.setCType(CType.getCType(nt));
			this.setConstant(left.isConstant() && right.isConstant());
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		if (left != null && right != null) {
			left.codeGen(pcx);		// 左部分木のコード生成を頼む
			right.codeGen(pcx);		// 右部分木のコード生成を頼む
			o.println("\tJSR \tDIV\t\t; TermDiv: 割り算をするサブルーチンを呼び出す<" + div.toExplainString() + ">");
			o.println("\tSUB\t#2, R6\t\t; TermDiv: 引数を捨てる");
			o.println("\tMOV\tR2, (R6)+\t; TermDiv: 結果が入っているR2の値をスタックに積む");
		}
	}
}
