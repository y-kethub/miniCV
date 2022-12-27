package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;

public class UnsignedFactor extends CParseRule {
	// unsignedFactor ::= factorAmp | number | LPAR expression RPAR
	private CParseRule cpr;
	
	public UnsignedFactor(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return Number.isFirst(tk) || FactorAmp.isFirst(tk) | tk.getType() == CToken.TK_LPAR | AddressToValue.isFirst(tk);
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
		} else if(AddressToValue.isFirst(tk)) {
			cpr = new AddressToValue(pcx);
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
				pcx.fatalError(tk.toExplainString() + ")が見つかりません");
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

class AddressToValue extends CParseRule {
	// addressToValue ::= primary
	private CParseRule cpr;
	public  AddressToValue(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return Primary.isFirst(tk);
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		cpr = new Primary(pcx);
		cpr.parse(pcx);
	}
	
	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if(cpr != null) {
			cpr.semanticCheck(pcx);
			setCType(cpr.getCType());
			setConstant(cpr.isConstant());
		}
	}
	
	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		cpr.codeGen(pcx);
		o.println(";;; AddressToValue starts");
		if(cpr != null) {
			o.println("\tMOV\t-(R6), R0\t	;AddressToValue: 変数アドレスを値に変換");
			o.println("\tMOV\t(R0), (R6)+\t	;AddressToValue: ");
		}
		o.println(";;; AddressToValue completes");

	}
}