package lang.c.parse;
import java.io.PrintStream;

import lang.FatalErrorException;
import lang.c.CParseContext;
import lang.c.CParseRule;
import lang.c.CToken;
import lang.c.CTokenizer;
import lang.c.CType;

public class FactorAmp extends CParseRule {
	// FactorAmp ::= AMP number
	// number ::= NUM
	private CParseRule number,primary;
	private CToken amp;
	public FactorAmp(CParseContext pcx) {
	}
	public static boolean isFirst(CToken tk) {
		return tk.getType() == CToken.TK_AMP;
	}
	public void parse(CParseContext pcx) throws FatalErrorException {
		CTokenizer ct = pcx.getTokenizer();
		amp = ct.getCurrentToken(pcx);
		CToken tk = ct.getNextToken(pcx);
		if(tk.getType() == CToken.TK_NUM) {
			number = new Number(pcx);
			number.parse(pcx);
		}else if(Primary.isFirst(tk)) {
			primary = new Primary(pcx);
			primary.parse(pcx);
		} else {
			pcx.fatalError(tk.toExplainString() + "&の後ろはnumberまたはprimaryです");
		}
	}

	public void semanticCheck(CParseContext pcx) throws FatalErrorException {
		if (number != null) {
			number.semanticCheck(pcx);
			if(number.getCType().isCType(CType.T_int) ) {
				setCType(CType.getCType(CType.T_pint));
			}else {
				setCType(CType.getCType(CType.T_err));
			}
			setConstant(number.isConstant());	// number は常に定数
		}else if(primary != null) {
			primary.semanticCheck(pcx);
			if(((Primary)primary).hasPrimaryMult()) {
				pcx.fatalError(amp.toExplainString() + "&の後ろにPrimaryMultがあります");
			}else if(primary.getCType().isCType(CType.T_int)) {
				setCType(CType.toPointer(primary.getCType()));
				setConstant(primary.isConstant());
			}else {
				pcx.fatalError(amp.toExplainString() + "&の後ろに配列やポインタがあります");
			}
		}
	}

	public void codeGen(CParseContext pcx) throws FatalErrorException {
		PrintStream o = pcx.getIOContext().getOutStream();
		o.println(";;; factorAmp starts");
		if (number != null) {
			number.codeGen(pcx);
		}else if(primary != null) {
			primary.codeGen(pcx);
		}
		o.println(";;; factorAmp completes");
	}
}