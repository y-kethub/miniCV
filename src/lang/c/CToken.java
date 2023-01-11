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
	public static final int TK_ASSIGN = 12;	// =
	public static final int TK_SEMI = 13;	// ;
	public static final int TK_LT = 14; // <
	public static final int TK_LE = 15; // <=
	public static final int TK_GT = 16; // >
	public static final int TK_GE = 17; // >=
	public static final int TK_EQ = 18; // ==
	public static final int TK_NE = 19; // !=
	public static final int TK_TRUE = 20; 
	public static final int TK_FALSE = 21;

	public CToken(int type, int lineNo, int colNo, String s) {
		super(type, lineNo, colNo, s);
	}
}
