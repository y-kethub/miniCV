package lang.c;

import lang.SimpleToken;

public class CToken extends SimpleToken {
	public static final int TK_PLUS	= 2;				// +
	public static final int TK_MINUS = 3;				// -
	public static final int TK_ADDRESS = 4;				//番地

	public CToken(int type, int lineNo, int colNo, String s) {
		super(type, lineNo, colNo, s);
	}
}
