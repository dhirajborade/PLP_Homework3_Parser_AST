package cop5556fa17;

import cop5556fa17.Scanner.Kind;
import cop5556fa17.Scanner.Token;
import cop5556fa17.AST.ASTNode;
import cop5556fa17.AST.Declaration;
import cop5556fa17.AST.Declaration_Image;
import cop5556fa17.AST.Declaration_SourceSink;
import cop5556fa17.AST.Declaration_Variable;
import cop5556fa17.AST.Expression;
import cop5556fa17.AST.LHS;
import cop5556fa17.AST.Program;
import cop5556fa17.AST.Sink;
import cop5556fa17.AST.Sink_Ident;
import cop5556fa17.AST.Sink_SCREEN;
import cop5556fa17.AST.Source;
import cop5556fa17.AST.Statement;
import cop5556fa17.AST.Statement_Assign;
import cop5556fa17.AST.Statement_In;
import cop5556fa17.AST.Statement_Out;

import static cop5556fa17.Scanner.Kind.*;

import java.util.ArrayList;

public class Parser {

	@SuppressWarnings("serial")
	public class SyntaxException extends Exception {
		Token t;

		public SyntaxException(Token t, String message) {
			super(message);
			this.t = t;
		}

	}

	Scanner scanner;
	Token t;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	/**
	 * Main method called by compiler to parser input. Checks for EOF
	 * 
	 * @throws SyntaxException
	 */
	public Program parse() throws SyntaxException {
		Program p = program();
		matchEOF();
		return p;
	}

	/**
	 * Program ::= IDENTIFIER ( Declaration SEMI | Statement SEMI )*
	 * 
	 * Program is start symbol of our grammar.
	 * 
	 * @throws SyntaxException
	 */
	Program program() throws SyntaxException {
		// TODO implement this
		ArrayList<ASTNode> decsAndStatements = new ArrayList<>();
		Program p = null;
		Token firstToken = t;
		
		if (t.kind == IDENTIFIER) {
			Token name = t;
			matchToken(IDENTIFIER);
			while (t.kind == KW_int || t.kind == KW_boolean || t.kind == KW_image || t.kind == KW_url
					|| t.kind == KW_file || t.kind == IDENTIFIER) {
				if (t.kind == KW_int || t.kind == KW_boolean || t.kind == KW_image || t.kind == KW_url
						|| t.kind == KW_file) {
					decsAndStatements.add(declaration());
					if (t.kind == SEMI) {
						matchToken(SEMI);
					} else {
						throw new SyntaxException(t, "Missing Semicolon");
					}

				} else if (t.kind == IDENTIFIER) {
					decsAndStatements.add(statement());
					if (t.kind == SEMI) {
						matchToken(SEMI);
					} else {
						throw new SyntaxException(t, "Missing Semicolon");
					}
				}
				p = new Program(firstToken, name, decsAndStatements);
			}
		} else {
			throw new SyntaxException(t, "Illegal Start of Program");
		}
		return p;
	}

	Declaration declaration() throws SyntaxException {
		switch (t.kind) {
		case KW_int:
		case KW_boolean:
			return variableDeclaration();
		case KW_image:
			return imageDeclaration();
		case KW_url:
		case KW_file:
			return sourceSinkDeclaration();
		default:
			throw new SyntaxException(t, "Illegal Declaration");
		}
	}
	
	Declaration_Variable variableDeclaration() throws SyntaxException {
		Token firstToken = t;
		Token type = varType();
		Token name = t;
		Expression e = null;
		matchToken(IDENTIFIER);
		if (t.kind == OP_ASSIGN) {
			matchToken(OP_ASSIGN);
			e = expression();
		}
		return new Declaration_Variable(firstToken, type, name, e);
	}
	
	Declaration_Image imageDeclaration() throws SyntaxException {
		Token firstToken = t;
		Source source = null;
		Expression xSize = null;
		Expression ySize = null;
		matchToken(KW_image);
		if (t.kind == LSQUARE) {
			matchToken(LSQUARE);
			xSize = expression();
			matchToken(COMMA);
			ySize = expression();
			matchToken(RSQUARE);
		}
		Token name = t;
		matchToken(IDENTIFIER);
		if (t.kind == OP_LARROW) {
			matchToken(OP_LARROW);
			source = source();
		}
		return new Declaration_Image(firstToken, xSize, ySize, name, source);
	}
	
	Declaration_SourceSink sourceSinkDeclaration() throws SyntaxException {
		Token firstToken = t;
		Token type = sourceSinkType();
		Token name = t;
		matchToken(IDENTIFIER, OP_ASSIGN);
		Source source = source();
		return new Declaration_SourceSink(firstToken, type, name, source);
	}

	Statement statement() throws SyntaxException {
		if (t.kind == IDENTIFIER && scanner.peek().kind == OP_RARROW) {
			return imageOutStatement();
		} else if (t.kind == IDENTIFIER && scanner.peek().kind == OP_LARROW) {
			return imageInStatement();
		} else if ((t.kind == IDENTIFIER && scanner.peek().kind == OP_ASSIGN)
				|| (t.kind == IDENTIFIER && scanner.peek().kind == LSQUARE)) {
			return assignmentStatement();
		} else {
			throw new SyntaxException(t, "Illegal Statement");
		}
	}

	Statement_Out imageOutStatement() throws SyntaxException {
		Token firstToken = t;
		Token name = t;
		matchToken(IDENTIFIER, OP_RARROW);
		Sink sink = sink();
		return new Statement_Out(firstToken, name, sink);
	}
	
	Statement_In imageInStatement() throws SyntaxException {
		Token firstToken = t;
		Token name = t;
		matchToken(IDENTIFIER, OP_LARROW);
		Source source = source();
		return new Statement_In(firstToken, name, source);
	}
	
	Statement_Assign assignmentStatement() throws SyntaxException {
		Token firstToken = t;
		LHS lhs = lhs();
		matchToken(OP_ASSIGN);
		Expression e = expression();
		return new Statement_Assign(firstToken, lhs, e);
	}

	Sink sink() throws SyntaxException {
		Token firstToken = t;
		switch (t.kind) {
		case IDENTIFIER:
			Token name = t;
			matchToken(IDENTIFIER);
			return new Sink_Ident(firstToken, name);
		case KW_SCREEN:
			matchToken(KW_SCREEN);
			return new Sink_SCREEN(firstToken);
		default:
			throw new SyntaxException(t, "Illegal Sink");
		}
	}
	
	Source source() throws SyntaxException {
		switch (t.kind) {
		case STRING_LITERAL:
			matchToken(STRING_LITERAL);
			break;
		case OP_AT:
			matchToken(OP_AT);
			expression();
			break;
		case IDENTIFIER:
			matchToken(IDENTIFIER);
			break;
		default:
			throw new SyntaxException(t, "Illegal Source");
		}
	}

	LHS lhs() throws SyntaxException {
		matchToken(IDENTIFIER);
		if (t.kind == LSQUARE) {
			matchToken(LSQUARE);
			lhsSelector();
			matchToken(RSQUARE);
		}
	}

	void lhsSelector() throws SyntaxException {
		matchToken(LSQUARE);
		if (t.kind == KW_x) {
			xySelector();
		} else if (t.kind == KW_r) {
			raSelector();
		}
		matchToken(RSQUARE);
	}

	void xySelector() throws SyntaxException {
		matchToken(KW_x, COMMA, KW_y);
	}

	void raSelector() throws SyntaxException {
		matchToken(KW_r, COMMA, KW_A);
	}

	

	Token varType() throws SyntaxException {
		Token type;
		switch (t.kind) {
		case KW_boolean:
			type = t;
			matchToken(KW_boolean);
			return type;
		case KW_int:
			type = t;
			matchToken(KW_int);
			return type;
		default:
			throw new SyntaxException(t, "Illegal varType");
		}
	}

	Token sourceSinkType() throws SyntaxException {
		Token firstToken;
		switch (t.kind) {
		case KW_url:
			firstToken = t;
			matchToken(KW_url);
			return firstToken;
		case KW_file:
			firstToken = t;
			matchToken(KW_file);
			return firstToken;
		default:
			throw new SyntaxException(t, "Illegal Source Sink Type");
		}
	}

	/**
	 * Expression ::= OrExpression OP_Q Expression OP_COLON Expression |
	 * OrExpression
	 * 
	 * Our test cases may invoke this routine directly to support incremental
	 * development.
	 * @return 
	 * 
	 * @throws SyntaxException
	 */
	Expression expression() throws SyntaxException {
		// TODO implement this.
		switch (t.kind) {
		case OP_PLUS:
		case OP_MINUS:
		case OP_EXCL:
		case INTEGER_LITERAL:
		case LPAREN:
		case KW_sin:
		case KW_cos:
		case KW_atan:
		case KW_abs:
		case KW_cart_x:
		case KW_cart_y:
		case KW_polar_a:
		case KW_polar_r:
		case IDENTIFIER:
		case KW_x:
		case KW_y:
		case KW_r:
		case KW_a:
		case KW_X:
		case KW_Y:
		case KW_Z:
		case KW_A:
		case KW_R:
		case KW_DEF_X:
		case KW_DEF_Y:
		case BOOLEAN_LITERAL:
			orExpression();
			if (t.kind == OP_Q) {
				matchToken(OP_Q);
				expression();
				matchToken(OP_COLON);
				expression();
			}
			break;
		default:
			throw new SyntaxException(t, "Illegal Start of Expression");
		}
	}

	void orExpression() throws SyntaxException {
		andExpression();
		while (t.kind == OP_OR) {
			matchToken(OP_OR);
			andExpression();
		}
	}

	void andExpression() throws SyntaxException {
		eqExpression();
		while (t.kind == OP_AND) {
			matchToken(OP_AND);
			eqExpression();
		}
	}

	void eqExpression() throws SyntaxException {
		relExpression();
		while (t.kind == OP_EQ || t.kind == OP_NEQ) {
			if (t.kind == OP_EQ) {
				matchToken(OP_EQ);
			} else if (t.kind == OP_NEQ) {
				matchToken(OP_NEQ);
			}
			relExpression();
		}
	}

	void relExpression() throws SyntaxException {
		addExpression();
		while (t.kind == OP_LT || t.kind == OP_GT || t.kind == OP_LE || t.kind == OP_GE) {
			switch (t.kind) {
			case OP_LT:
				matchToken(OP_LT);
				break;
			case OP_GT:
				matchToken(OP_GT);
				break;
			case OP_LE:
				matchToken(OP_LE);
				break;
			case OP_GE:
				matchToken(OP_GE);
				break;
			default:
				throw new SyntaxException(t, "Illegal Compare Expression");
			}
			addExpression();
		}
	}

	void addExpression() throws SyntaxException {
		multExpression();
		while (t.kind == OP_PLUS || t.kind == OP_MINUS) {
			switch (t.kind) {
			case OP_PLUS:
				matchToken(OP_PLUS);
				break;
			case OP_MINUS:
				matchToken(OP_MINUS);
				break;
			default:
				throw new SyntaxException(t, "Illegal Add/Subtract Expression");
			}
			multExpression();
		}
	}

	void multExpression() throws SyntaxException {
		unaryExpression();
		while (t.kind == OP_TIMES || t.kind == OP_DIV || t.kind == OP_MOD) {
			switch (t.kind) {
			case OP_TIMES:
				matchToken(OP_TIMES);
				break;
			case OP_DIV:
				matchToken(OP_DIV);
				break;
			case OP_MOD:
				matchToken(OP_MOD);
				break;
			default:
				throw new SyntaxException(t, "Illegal Multiplication Expression");
			}
			unaryExpression();
		}
	}

	void unaryExpression() throws SyntaxException {
		switch (t.kind) {
		case OP_PLUS:
			matchToken(OP_PLUS);
			unaryExpression();
			break;
		case OP_MINUS:
			matchToken(OP_MINUS);
			unaryExpression();
			break;
		case OP_EXCL:
		case INTEGER_LITERAL:
		case LPAREN:
		case KW_sin:
		case KW_cos:
		case KW_atan:
		case KW_abs:
		case KW_cart_x:
		case KW_cart_y:
		case KW_polar_a:
		case KW_polar_r:
		case IDENTIFIER:
		case KW_x:
		case KW_y:
		case KW_r:
		case KW_a:
		case KW_X:
		case KW_Y:
		case KW_Z:
		case KW_A:
		case KW_R:
		case KW_DEF_X:
		case KW_DEF_Y:
		case BOOLEAN_LITERAL:
			unaryExpressionNotPlusMinus();
			break;
		default:
			throw new SyntaxException(t, "Illegal Unary Expression");
		}
	}

	void unaryExpressionNotPlusMinus() throws SyntaxException {
		switch (t.kind) {
		case OP_EXCL:
			matchToken(OP_EXCL);
			unaryExpression();
			break;
		case INTEGER_LITERAL:
		case LPAREN:
		case KW_sin:
		case KW_cos:
		case KW_atan:
		case KW_abs:
		case KW_cart_x:
		case KW_cart_y:
		case KW_polar_a:
		case KW_polar_r:
		case BOOLEAN_LITERAL:
			primary();
			break;
		case IDENTIFIER:
			identOrPixelSelectorExpression();
			break;
		case KW_x:
			matchToken(KW_x);
			break;
		case KW_y:
			matchToken(KW_y);
			break;
		case KW_r:
			matchToken(KW_r);
			break;
		case KW_a:
			matchToken(KW_a);
			break;
		case KW_X:
			matchToken(KW_X);
			break;
		case KW_Y:
			matchToken(KW_Y);
			break;
		case KW_Z:
			matchToken(KW_Z);
			break;
		case KW_A:
			matchToken(KW_A);
			break;
		case KW_R:
			matchToken(KW_R);
			break;
		case KW_DEF_X:
			matchToken(KW_DEF_X);
			break;
		case KW_DEF_Y:
			matchToken(KW_DEF_Y);
			break;
		default:
			throw new SyntaxException(t, "Illegal Unary Expression");
		}
	}

	void primary() throws SyntaxException {
		switch (t.kind) {
		case INTEGER_LITERAL:
			matchToken(INTEGER_LITERAL);
			break;
		case LPAREN:
			matchToken(LPAREN);
			expression();
			matchToken(RPAREN);
			break;
		case KW_sin:
		case KW_cos:
		case KW_atan:
		case KW_abs:
		case KW_cart_x:
		case KW_cart_y:
		case KW_polar_a:
		case KW_polar_r:
			functionApplication();
			break;
		case BOOLEAN_LITERAL:
			matchToken(BOOLEAN_LITERAL);
			break;
		default:
			throw new SyntaxException(t, "Illegal Primary Expression");
		}
	}

	void identOrPixelSelectorExpression() throws SyntaxException {
		matchToken(IDENTIFIER);
		if (t.kind == LSQUARE) {
			matchToken(LSQUARE);
			selector();
			matchToken(RSQUARE);
		}
	}

	void functionApplication() throws SyntaxException {
		functionName();
		if (t.kind == LPAREN) {
			matchToken(LPAREN);
			expression();
			matchToken(RPAREN);
		} else if (t.kind == LSQUARE) {
			matchToken(LSQUARE);
			selector();
			matchToken(RSQUARE);
		}
	}

	void functionName() throws SyntaxException {
		switch (t.kind) {
		case KW_sin:
			matchToken(KW_sin);
			break;
		case KW_cos:
			matchToken(KW_cos);
			break;
		case KW_atan:
			matchToken(KW_atan);
			break;
		case KW_abs:
			matchToken(KW_abs);
			break;
		case KW_cart_x:
			matchToken(KW_cart_x);
			break;
		case KW_cart_y:
			matchToken(KW_cart_y);
			break;
		case KW_polar_a:
			matchToken(KW_polar_a);
			break;
		case KW_polar_r:
			matchToken(KW_polar_r);
			break;
		default:
			throw new SyntaxException(t, "Illegal Function Name");
		}
	}

	void selector() throws SyntaxException {
		expression();
		matchToken(COMMA);
		expression();
	}

	/**
	 * Only for check at end of program. Does not "consume" EOF so no attempt to get
	 * nonexistent next Token.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (t.kind == EOF) {
			return t;
		}
		String message = "Expected EOL at " + t.line + ":" + t.pos_in_line;
		throw new SyntaxException(t, message);
	}

	private void matchToken(Kind kind) throws SyntaxException {
		if (t.kind == kind) {
			consume();
		} else {
			String message = "Got token of kind " + t.kind + " instead of " + kind;
			throw new SyntaxException(t, message);
		}
	}

	private void matchToken(Kind... kinds) throws SyntaxException {
		for (Kind k : kinds) {
			if (k == t.kind) {
				consume();
			} else {
				String message = "Got token of kind " + t.kind + " instead of " + k;
				throw new SyntaxException(t, message);
			}
		}
	}

	private Token consume() throws SyntaxException {
		Token currentToken = t;
		t = scanner.nextToken();
		return currentToken;
	}
}
