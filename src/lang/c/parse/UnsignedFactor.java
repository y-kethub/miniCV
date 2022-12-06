package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;

public class UnsignedFactor extends CParseRule {
	// unsignedFactor ::= factorAmp | number | LPAR expression RPAR
	private CToken lpar;
	private CParseRule cpr;
	
	public UnsignedFactor(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return Number.isFirst(tk) || FactorAmp.isFirst(tk) | tk.getType() == CToken.TK_LPAR;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		CToken tk = ct.getCurrentToken(pcx);
		if(FactorAmp.isFirst(tk)) {
			cpr = new FactorAmp(pcx);
			cpr.parse(pcx);
		}else if(Number.isFirst(tk)) {
			cpr = new Number(pcx);
			cpr.parse(pcx);
		} else {
			tk = ct.getNextToken(pcx);
			if (!Expression.isFirst(tk)) {
				pcx.fatalError(tk.toExplainString() + "(の後ろがexpressionではありません");
			}
			cpr = new Expression(pcx);
			cpr.parse(pcx);
			tk = ct.getCurrentToken(pcx);
			if(tk.getType() != CToken.TK_RPAR) {
				pcx.fatalError(/*lpar.toExplainString() + */")が見つかりません");
			}
			ct.getNextToken(pcx);
		}
		
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (cpr != null) {
			cpr.semanticCheck(pcx);
			setCType(cpr.getCType());		// number の型をそのままコピー
			setConstant(cpr.isConstant());	// number は常に定数
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; unsignedfactor starts");
		if (cpr != null) { cpr.codeGen(pcx); }
		o.println(";;; unsignedfactor completes");
	}
}