package lang.c.parse;

import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;
import lang.c.CType;

public class Primary extends CParseRule {
	// Primary ::= PrimaryMult | variable
	private CParseRule rule;
	private CToken tk;
	public Primary(CParseContext pcx) {
	}
	public boolean hasPrimaryMult() {
		return rule instanceof PrimaryMult;
	}
	public static boolean isFirst(CToken tk) {
		return PrimaryMult.isFirst(tk) || Variable.isFirst(tk);
	}
	
	public void parse(CParseContext pcx) throws FatalErrorException {
		// ここにやってくるときは、必ずisFirst()が満たされている
		CTokenizer ct = pcx.getTokenizer();
		tk = ct.getCurrentToken(pcx);
		
		if(PrimaryMult.isFirst(tk)) {
			rule = new PrimaryMult(pcx);
			rule.parse(pcx);
		}else if(Variable.isFirst(tk)) {
			rule = new Variable(pcx);
			rule.parse(pcx);
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if( rule != null) {
			rule.semanticCheck(pcx);
			setCType(rule.getCType());
			setConstant(rule.isConstant());
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; Primary starts");
		if (rule != null) rule.codeGen(pcx);
		o.println(";;; Primary completes");
	}
}

class PrimaryMult extends CParseRule {
	// PrimaryMult ::= MULT variable
	private CParseRule rule;
	private CToken mul;
	public PrimaryMult(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_MULT;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		CTokenizer ct = pcx.getTokenizer();
		mul = ct.getCurrentToken(pcx);
		CToken tk = ct.getNextToken(pcx);
		
		if(Variable.isFirst(tk)) {
			rule = new Variable(pcx);
			rule.parse(pcx);
		}else {
			pcx.fatalError(tk.toExplainString() + "*の後ろはvariable");
		}
	}
	
	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (rule != null) {
			rule.semanticCheck(pcx);
			if(!rule.getCType().isCType(CType.T_int)) {
				System.err.println(rule.getCType().toString());
				setCType(CType.toValue(rule.getCType()));
				setConstant(rule.isConstant());
			} else {
				pcx.fatalError(mul.toExplainString() + "*の後にintがある");
			}
		}
	}
	
	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; PrimaryMult starts");
		if(rule != null) {
			rule.codeGen(pcx);
			o.println("\tMOV\t-(R6), R0\t; PrimaryMult: アドレスを取り出して、内容を参照して積む<" + mul.toExplainString() + ">");
			o.println("\tMOV\t(R0), (R6)+\t; PrimaryMult:");
		}
		o.println(";;; PrimaryMult completes");
	}
}
	

