package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;
import lang.c.CType;

public class Factor extends CParseRule {
	// factor ::= plusFactor | minsuFactor | unsignedFactor
	private CParseRule number;
	private CParseRule unsignedfactor;

	public Factor(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return PlusFactor.isFirst(tk) || MinusFactor.isFirst(tk) || UnsignedFactor.isFirst(tk);
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		if(PlusFactor.isFirst(tk)) {
			number = new PlusFactor(pcx);
			number.parse(pcx);
		}else if(MinusFactor.isFirst(tk)) {
			number = new MinusFactor(pcx);
			number.parse(pcx);
		}else {
			unsignedfactor = new UnsignedFactor(pcx);
			unsignedfactor.parse(pcx);
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (number != null) {
			number.semanticCheck(pcx);
			setCType(number.getCType());		// number の型をそのままコピー
			setConstant(number.isConstant());	// number は常に定数
		}else if(unsignedfactor != null) {
			unsignedfactor.semanticCheck(pcx);
			setCType(unsignedfactor.getCType());		
			setConstant(unsignedfactor.isConstant());
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; factor starts");
		if (number != null) { number.codeGen(pcx); }
		if (unsignedfactor != null) { unsignedfactor.codeGen(pcx); }
		o.println(";;; factor completes");
	}
}

class PlusFactor extends CParseRule {
	// plusFactor ::= PLUS unsignedFactor
	private CToken plus;
	private CParseRule unsignedfactor;

	public PlusFactor(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_PLUS;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		plus = ct.getCurrentToken(pcx);
		CToken tk = ct.getNextToken(pcx);
		if(UnsignedFactor.isFirst(tk)) {
			unsignedfactor = new UnsignedFactor(pcx);
			unsignedfactor.parse(pcx);
		}else {
			pcx.fatalError(plus.toExplainString() + "+の後ろはunsigendfactor");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		final int s[] = {CType.T_err, CType.T_int, CType.T_err};
		int t = 0;
		boolean c = false;
		if(unsignedfactor != null) {
			unsignedfactor.semanticCheck(pcx);
			t = unsignedfactor.getCType().getType();
			c = unsignedfactor.isConstant();
		}else {
			pcx.fatalError(plus.toExplainString() + "演算対象が見つかりません");
		}
		int nt = s[t];
		if(nt == CType.T_err) {
			pcx.fatalError(plus.toExplainString() + unsignedfactor.getCType().toString() + "は符号を変えられない");
		}
		this.setCType(CType.getCType(nt));
		this.setConstant(c);
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		if (unsignedfactor != null) { unsignedfactor.codeGen(pcx);}
	}
}

class MinusFactor extends CParseRule {
	// minusFactor ::= MINUS unsignedFactor
	private CToken minus;
	private CParseRule unsignedfactor;

	public MinusFactor(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_MINUS;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		minus = ct.getCurrentToken(pcx);
		CToken tk = ct.getNextToken(pcx);
		if(UnsignedFactor.isFirst(tk)) {
			unsignedfactor = new UnsignedFactor(pcx);
			unsignedfactor.parse(pcx);
		}else {
			pcx.fatalError(minus.toExplainString() + "-の後ろはunsigendfactor");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		final int s[] = {CType.T_err, CType.T_int, CType.T_err};
		int t = 0;
		boolean c = false;
		if(unsignedfactor != null) {
			unsignedfactor.semanticCheck(pcx);
			t = unsignedfactor.getCType().getType();
			c = unsignedfactor.isConstant();
		}else {
			pcx.fatalError(minus.toExplainString() + "演算対象が見つかりません");
		}
		int nt = s[t];
		if(nt == CType.T_err) {
			pcx.fatalError(minus.toExplainString() + unsignedfactor.getCType().toString() + "は符号を変えられない");
		}
		this.setCType(CType.getCType(nt));
		this.setConstant(c);
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		if (unsignedfactor != null) {
			unsignedfactor.codeGen(pcx);
			o.println("\tMOV\t#0, R0\t; MinusFactor: 0に数を引き符号を反転する<" + minus.toExplainString() + ">");
			o.println("\tMOV\t-(R6), R1\t; MinusFactor:");
			o.println("\tSUB\tR1, R0\t\t; MinusFactor:");
			o.println("\tMOV\tR0, (R6)+\t; MinusFactor:");

		}
	}
}