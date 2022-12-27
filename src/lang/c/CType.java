package lang.c;

public class CType {
	public static final int T_err = 0;		// 型エラー
	public static final int T_int = 1;		// int
	public static final int T_pint = 2;		// int*
	public static final int T_array = 3;
	public static final int T_parray = 4;

	private static CType[] typeArray = {
			new CType(T_err,	"error"),
			new CType(T_int,	"int"),
			new CType(T_pint,	"int*"),
			new CType(T_array,  "int[]"),
			new CType(T_parray, "int[]*"),
	};

	private int type;
	private String string;

	private CType(int type, String s) {
		this.type = type;
		this.string = s;
	}
	public static CType getCType(int type) {
		return typeArray[type];
	}
	public boolean isCType(int t)	{ return t == type; }
	public int getType()			{ return type; }
	public String toString()		{ return string; }

	public static CType toPointer(CType tk) {
		if(tk.isCType(T_int)) {
			return getCType(T_pint);
		}
		return getCType(T_err);
	}
	public static CType toValue(CType t) {
		if(t.isCType(T_pint)) return getCType(T_int);
		else if(t.isCType(T_array)) return getCType(T_int);
		else if(t.isCType(T_parray)) return getCType(T_pint);
		return getCType(T_err);
	}
}
