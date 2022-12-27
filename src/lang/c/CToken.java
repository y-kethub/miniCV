package lang.c;

import lang.SimpleToken;

public class CToken extends SimpleToken {
	public static final int TK_PLUS	= 2;	// +
	public static final int TK_MINUS = 3;	// -
	public static final int TK_AMP = 4;		// 番地
	public static final int TK_MULT = 5;		// *
	public static final int TK_DIV = 6;		// /
	public static final int TK_LPAR = 7;		// (
	public static final int TK_RPAR = 8;		// )
	public static final int TK_LBRA = 9;		// [
	public static final int TK_RBRA = 10;	// ]

	public CToken(int type, int lineNo, int colNo, String s) {
		super(type, lineNo, colNo, s);
	}
}
