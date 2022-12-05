package lang.c;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import lang.Tokenizer;

public class CTokenizer extends Tokenizer<CToken, CParseContext> {
	@SuppressWarnings("unused")
	private CTokenRule	rule;
	private int			lineNo, colNo;
	private char		backCh;
	private boolean		backChExist = false;

	public CTokenizer(CTokenRule rule) {
		this.rule = rule;
		lineNo = 1; colNo = 1;
	}

	private InputStream in;
	private PrintStream err;

	private char readChar() {
		char ch;
		if (backChExist) {
			ch = backCh;
			backChExist = false;
		} else {
			try {
				ch = (char) in.read();
			} catch (IOException e) {
				e.printStackTrace(err);
				ch = (char) -1;
			}
		}
		++colNo;
		if (ch == '\n')  { colNo = 1; ++lineNo; }
		//		System.out.print("'"+ch+"'("+(int)ch+")");
		return ch;
	}
	private void backChar(char c) {
		backCh = c;
		backChExist = true;
		--colNo;
		if (c == '\n') { --lineNo; }
	}

	// 現在読み込まれているトークンを返す
	private CToken currentTk = null;
	public CToken getCurrentToken(CParseContext pctx) {
		return currentTk;
	}
	// 次のトークンを読んで返す
	public CToken getNextToken(CParseContext pctx) {
		in = pctx.getIOContext().getInStream();
		err = pctx.getIOContext().getErrStream();
		currentTk = readToken();
		//		System.out.println("Token='" + currentTk.toString());
		return currentTk;
	}
	private CToken readToken() {
		CToken tk = null;
		char ch;
		int  startCol = colNo;
		StringBuffer text = new StringBuffer();

		int state = 0;
		boolean accept = false;
		while (!accept) {
			switch (state) {
			case 0:					// 初期状態
				ch = readChar();
				if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
				} else if (ch == (char) -1) {	// EOF
					startCol = colNo - 1;
					state = 1;
				} else if (ch >= '1' && ch <= '9') {
					startCol = colNo - 1;
					text.append(ch);
					state = 3;
				} else if (ch == '+') {
					startCol = colNo - 1;
					text.append(ch);
					state = 4;
				} else if (ch == '-') {
					startCol = colNo - 1;
					text.append(ch);
					state = 5;
				} else if (ch == '-') {
					startCol = colNo - 1;
					text.append(ch);
					state = 5;
				} else if (ch == '/') {
					startCol = colNo - 1;
					text.append(ch);
					state = 6;
				}else if ( ch == '&') {
					startCol = colNo - 1;
					text.append(ch);
					state = 7;
				} else if ( ch == '0') {
					startCol = colNo - 1;
					text.append(ch);
					state = 8;
				} else {			// ヘンな文字を読んだ
					startCol = colNo - 1;
					text.append(ch);
					state = 2;
				}
				break;
			case 1:					// EOFを読んだ
				tk = new CToken(CToken.TK_EOF, lineNo, startCol, "end_of_file");
				accept = true;
				break;
			case 2:					// ヘンな文字を読んだ
				tk = new CToken(CToken.TK_ILL, lineNo, startCol, text.toString());
				accept = true;
				break;
			case 3:					// 数（10進数）の開始
				ch = readChar();
				if (Character.isDigit(ch)) {
					text.append(ch);
				} else {
					// 数の終わり
					backChar(ch);	// 数を表さない文字は戻す（読まなかったことにする）
					if(Integer.decode(text.toString()) > 32767 || Integer.decode(text.toString()) < -32768) {
						System.err.println("下の値は16ビット符号付き整数で表現できる範囲を超えています");
						state = 2;
					}else {
						tk = new CToken(CToken.TK_NUM,lineNo,startCol,text.toString());
						accept = true;
					}
				}
				break;
			case 4:					// +を読んだ
				tk = new CToken(CToken.TK_PLUS, lineNo, startCol, "+");
				accept = true;
				break;
			case 5: // -を読んだ
				tk = new CToken(CToken.TK_MINUS, lineNo, startCol, "-");
				accept = true;
				break;
			case 6: //コメントアウト処理
				ch = readChar();
				if (ch == '/') { //1行コメント
					ch = readChar();
					while (ch != '\n' && ch != '\r') {
						ch = readChar();
						if (ch == (char) -1) {
							state = 1;
							break;
						}
					}
				} else if(ch == '*') {  //複数行コメント
					ch = readChar();
					while (true) {
						while (ch != '*') {
							ch = readChar();
							if (ch == (char) -1) {
								state = 2;
								System.err.print("コメントが閉じる*がありません。");
								break;
							}
						}
						ch = readChar();
						if (ch == '/') {
							break;
						}
						if (ch == (char) -1) {
							state = 2;
							System.err.print("コメントを閉じる/がありません。");
							break;
						}
					}

				}
				text = new StringBuffer();
				state = 0;
				break;
			case 7: // &を読んだ
				tk = new CToken(CToken.TK_AMP,lineNo,startCol,text.toString());
				accept = true;
				break;
			case 8: //0を読んだ
				ch = readChar();
				if( ch == 'x') {
					startCol = colNo - 1;
					text.append(ch);
					state = 9;
				} else if ( '0' <= ch && ch <= '7') {
					startCol = colNo - 1;
					text.append(ch);
					state = 11;
				}else {
					backChar(ch);
					state = 3;
				}
				break;
			case 9: // 0xを読んだ
				ch = readChar();
				if( ( '0' <= ch && ch <= '9' ) || ( 'a' <= ch && ch <= 'f' ) || ( 'A' <= ch && ch <= 'F' ) ) {
					startCol = colNo - 1;
					text.append(ch);
					state = 10;
				} else {
					startCol = colNo - 1;
					state = 2;
				}
				break;
			case 10: // 16進数を読んだ
				ch = readChar();
				if( ( '0' <= ch && ch <= '9' ) || ( 'a' <= ch && ch <= 'f' ) || ( 'A' <= ch && ch <= 'F' ) ) {
					startCol = colNo - 1;
					text.append(ch);
					state = 10;
				} else {
					backChar(ch);
					if(Integer.decode(text.toString()) > 65535) {
						System.err.println("下の値は16ビット符号なし整数で表現できる範囲を超えています");
						state = 2;
					}else {
						tk = new CToken(CToken.TK_NUM,lineNo,startCol,text.toString());
						accept = true;
					}
				}
				break;
			case 11: // 8進数を読んだ
				ch = readChar();
				if( '0' <= ch && ch <= '7' ) {
					startCol = colNo - 1;
					text.append(ch);
					state = 11;
				} else if ( ch == '8' || ch == '9') {
					backChar(ch);
					if(Integer.decode(text.toString()) > 65535) {
						System.err.println("下の値は16ビット符号なし整数で表現できる範囲を超えています");
						state = 2;
					}else {
						tk = new CToken(CToken.TK_NUM,lineNo,startCol,text.toString());
						accept = true;
					}
					text.append(ch);
					state = 2;
				} else {
					backChar(ch);
					if(Integer.decode(text.toString()) > 65535) {
						System.err.println("下の値は16ビット符号なし整数で表現できる範囲を超えています");
						state = 2;
					}else {
						tk = new CToken(CToken.TK_NUM,lineNo,startCol,text.toString());
						accept = true;
					}
				}
				break;
			}
		}
		return tk;
	}
}
