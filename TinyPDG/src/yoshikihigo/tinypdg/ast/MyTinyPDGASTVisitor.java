package yoshikihigo.tinypdg.ast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;

import yoshikihigo.tinypdg.pe.BlockInfo;
import yoshikihigo.tinypdg.pe.ClassInfo;
import yoshikihigo.tinypdg.pe.CompilationUnitInfo;
import yoshikihigo.tinypdg.pe.ExpressionInfo;
import yoshikihigo.tinypdg.pe.MethodInfo;
import yoshikihigo.tinypdg.pe.OperatorInfo;
import yoshikihigo.tinypdg.pe.ProgramElementInfo;
import yoshikihigo.tinypdg.pe.StatementInfo;
import yoshikihigo.tinypdg.pe.TypeInfo;
import yoshikihigo.tinypdg.pe.VariableInfo;

public class MyTinyPDGASTVisitor extends NaiveASTFlattener {

	static public CompilationUnit createAST(final File file) {

		final String lineSeparator = System.getProperty("line.separator");
		final StringBuffer text = new StringBuffer();
		final BufferedReader reader;

		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), "JISAutoDetect"));

			while (reader.ready()) {
				final String line = reader.readLine();
				text.append(line);
				text.append(lineSeparator);
			}
			reader.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		final ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(text.toString().toCharArray());
		return (CompilationUnit) parser.createAST(null);
	}

	final private String path;
	final private CompilationUnit root;
	final private List<MethodInfo> methods;
	final private Stack<ProgramElementInfo> stack;

	private final CompilationUnitInfo rootInfo;

	public MyTinyPDGASTVisitor(final String path, final CompilationUnit root,
			final List<MethodInfo> methods) {
		this.path = path;
		this.root = root;
		this.methods = methods;
		this.stack = new Stack<ProgramElementInfo>();
		this.rootInfo = new CompilationUnitInfo(path);
	}

	@Override
	public void endVisit(final CompilationUnit node) {
		this.rootInfo.setText(buffer.toString());
	}

	@Override
	public boolean visit(final TypeDeclaration node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ClassInfo typeDeclaration = new ClassInfo(this.path, node
				.getName().toString(), startLine, endLine);
		typeDeclaration.setRoot(rootInfo);
		typeDeclaration.setStartOffset(buffer.length());
		this.stack.push(typeDeclaration);

		buffer.append("class ");
		buffer.append(node.getName().toString());
		buffer.append("{");
		buffer.append(System.getProperty("line.separator"));

		for (final Object o : node.bodyDeclarations()) {
			if (o instanceof MethodDeclaration) {
				((ASTNode) o).accept(this);
				final ProgramElementInfo method = this.stack.pop();
				this.methods.add((MethodInfo) method);
				typeDeclaration.addMethod((MethodInfo) method);
				// text.append(method.getText());
				buffer.append(System.getProperty("line.separator"));
			}
		}
		buffer.append("}");
		// typeDeclaration.setText(text.toString());
		typeDeclaration.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final TypeDeclarationStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo statement = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.TypeDeclaration, startLine, endLine);
			statement.setRoot(rootInfo);
			statement.setStartOffset(buffer.length());
			this.stack.push(statement);

			node.getDeclaration().accept(this);
			final ProgramElementInfo typeDeclaration = this.stack.pop();
			statement.addExpression(typeDeclaration);

			// statement.setText(typeDeclaration.getText());
			statement.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final AnnotationTypeDeclaration node) {

		for (final Object o : node.bodyDeclarations()) {
			((ASTNode) o).accept(this);
			final ProgramElementInfo method = this.stack.pop();
		}

		return false;
	}

	@Override
	public boolean visit(final AnonymousClassDeclaration node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ClassInfo anonymousClass = new ClassInfo(this.path, null,
				startLine, endLine);
		anonymousClass.setRoot(rootInfo);
		anonymousClass.setStartOffset(buffer.length());
		this.stack.push(anonymousClass);

		buffer.append("{");
		buffer.append(System.getProperty("line.separator"));

		for (final Object o : node.bodyDeclarations()) {
			if (o instanceof MethodDeclaration) {
				((ASTNode) o).accept(this);
				final ProgramElementInfo method = this.stack.pop();
				anonymousClass.addMethod((MethodInfo) method);
				// text.append(method.getText());
			}
		}

		buffer.append("}");
		// anonymousClass.setText(text.toString());
		anonymousClass.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final MethodDeclaration node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final String name = node.getName().getIdentifier();
		final MethodInfo method = new MethodInfo(this.path, name, startLine,
				endLine);
		method.setRoot(rootInfo);
		method.setStartOffset(buffer.length());
		this.stack.push(method);

		for (final Object modifier : node.modifiers()) {
			method.addModifier(modifier.toString());
			buffer.append(modifier.toString());
			buffer.append(" ");
		}
		if (null != node.getReturnType2()) {
			buffer.append(node.getReturnType2().toString());
			buffer.append(" ");
		}
		buffer.append(name);
		buffer.append("(");

		for (final Object o : node.parameters()) {
			((ASTNode) o).accept(this);
			final VariableInfo parameter = (VariableInfo) this.stack.pop();
			parameter.setCategory(VariableInfo.CATEGORY.PARAMETER);
			method.addParameter(parameter);
			// text.append(parameter.getText());
			buffer.append(",");
		}
		if (0 < node.parameters().size()) {
			buffer.deleteCharAt(buffer.length() - 1);
		}
		buffer.append(")");

		if (null != node.getBody()) {
			node.getBody().accept(this);
			final ProgramElementInfo body = this.stack.pop();
			method.setStatement((StatementInfo) body);
			// text.append(body.getText());
		}
		// method.setText(text.toString());
		method.setEndOffset(buffer.length() - 1);

		return false;
	}

	private int getStartLineNumber(final ASTNode node) {
		return root.getLineNumber(node.getStartPosition());
	}

	private int getEndLineNumber(final ASTNode node) {
		if (node instanceof IfStatement) {
			final ASTNode elseStatement = ((IfStatement) node)
					.getElseStatement();
			final int thenEnd = (elseStatement == null) ? node
					.getStartPosition() + node.getLength() : elseStatement
					.getStartPosition() - 1;
			return root.getLineNumber(thenEnd);
		} else if (node instanceof TryStatement) {
			final TryStatement tryStatement = (TryStatement) node;
			int tryEnd = 0;
			for (Object obj : tryStatement.catchClauses()) {
				CatchClause catchClause = (CatchClause) obj;
				tryEnd = catchClause.getStartPosition() - 1;
				break;
			}
			if (tryEnd == 0) {
				final Block finallyBlock = tryStatement.getFinally();
				if (finallyBlock != null) {
					tryEnd = finallyBlock.getStartPosition() - 1;
				}
			}
			if (tryEnd == 0) {
				tryEnd = node.getStartPosition() + node.getLength();
			}
			return root.getLineNumber(tryEnd);
		} else {
			return root.getLineNumber(node.getStartPosition()
					+ node.getLength());
		}
	}

	@Override
	public boolean visit(final AssertStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startOffset = buffer.length();

			node.getExpression().accept(this);
			final ProgramElementInfo expression = (ProgramElementInfo) this.stack
					.pop();

			node.getMessage().accept(this);
			final ProgramElementInfo message = (ProgramElementInfo) this.stack
					.pop();

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo statement = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Assert, startLine, endLine);
			statement.setRoot(rootInfo);
			statement.setStartOffset(startOffset);

			statement.addExpression(expression);
			statement.addExpression(message);
			buffer.append(";");
			statement.setEndOffset(buffer.length() - 1);
			this.stack.push(statement);
		}

		return false;
	}

	@Override
	public boolean visit(final ArrayAccess node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo expression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.ArrayAccess, startLine, endLine);
		expression.setRoot(rootInfo);
		expression.setStartOffset(buffer.length());
		this.stack.push(expression);

		node.getArray().accept(this);
		final ProgramElementInfo array = this.stack.pop();
		expression.addExpression((ProgramElementInfo) array);

		buffer.append("[");

		node.getIndex().accept(this);
		final ProgramElementInfo index = this.stack.pop();
		expression.addExpression((ProgramElementInfo) index);

		// text.append(array.getText());

		// text.append(index.getText());
		buffer.append("]");
		// expression.setText(text.toString());
		expression.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ArrayType node) {

		final StringBuffer text = new StringBuffer();
		text.append(node.getElementType().toString());
		for (int i = 0; i < node.getDimensions(); i++) {
			text.append("[]");
		}
		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final TypeInfo type = new TypeInfo(text.toString(), startLine, endLine);
		this.stack.push(type);

		buffer.append(text.toString());

		return false;
	}

	@Override
	public boolean visit(final NullLiteral node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ProgramElementInfo expression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Null, startLine, endLine);
		expression.setRoot(rootInfo);
		expression.setStartOffset(buffer.length());
		// expression.setText("null");
		buffer.append("null");
		expression.setEndOffset(buffer.length() - 1);
		this.stack.push(expression);

		return false;
	}

	@Override
	public boolean visit(final NumberLiteral node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ProgramElementInfo expression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Number, startLine, endLine);
		expression.setRoot(rootInfo);
		expression.setStartOffset(buffer.length());
		// expression.setText(node.getToken());
		buffer.append(node.getToken());
		expression.setEndOffset(buffer.length() - 1);
		this.stack.push(expression);

		return false;
	}

	@Override
	public boolean visit(final PostfixExpression node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo postfixExpression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Postfix, startLine, endLine);
		postfixExpression.setRoot(rootInfo);
		postfixExpression.setStartOffset(buffer.length());
		this.stack.push(postfixExpression);

		node.getOperand().accept(this);
		final ProgramElementInfo operand = this.stack.pop();
		postfixExpression.addExpression((ProgramElementInfo) operand);

		final OperatorInfo operator = new OperatorInfo(node.getOperator()
				.toString(), startLine, endLine);
		buffer.append(node.getOperator().toString());
		postfixExpression.addExpression(operator);

		postfixExpression.setEndOffset(buffer.length() - 1);

		// final StringBuilder text = new StringBuilder();
		// text.append(operand.getText());
		// text.append(operator.getText());
		// postfixExpression.setText(text.toString());

		return false;
	}

	@Override
	public boolean visit(final PrefixExpression node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo prefixExpression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Prefix, startLine, endLine);
		prefixExpression.setRoot(rootInfo);
		prefixExpression.setStartOffset(buffer.length());
		this.stack.push(prefixExpression);

		final OperatorInfo operator = new OperatorInfo(node.getOperator()
				.toString(), startLine, endLine);
		buffer.append(node.getOperator().toString());
		prefixExpression.addExpression(operator);

		node.getOperand().accept(this);
		final ProgramElementInfo operand = this.stack.pop();
		prefixExpression.addExpression((ProgramElementInfo) operand);

		prefixExpression.setEndOffset(buffer.length() - 1);

		// final StringBuilder text = new StringBuilder();
		// text.append(operator.getText());
		// text.append(operand.getText());
		// prefixExpression.setText(text.toString());

		return false;
	}

	@Override
	public boolean visit(final StringLiteral node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ProgramElementInfo expression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.String, startLine, endLine);
		expression.setRoot(rootInfo);
		expression.setStartOffset(buffer.length());

		buffer.append("\"" + node.getLiteralValue() + "\"");
		expression.setEndOffset(buffer.length() - 1);

		this.stack.push(expression);

		return false;
	}

	@Override
	public boolean visit(final SuperFieldAccess node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo superFieldAccess = new ExpressionInfo(
				ExpressionInfo.CATEGORY.SuperFieldAccess, startLine, endLine);
		superFieldAccess.setRoot(rootInfo);
		superFieldAccess.setStartOffset(buffer.length());
		this.stack.push(superFieldAccess);

		buffer.append("super.");

		node.getName().accept(this);
		final ProgramElementInfo name = this.stack.pop();
		superFieldAccess.addExpression(name);

		superFieldAccess.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final SuperMethodInvocation node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo superMethodInvocation = new ExpressionInfo(
				ExpressionInfo.CATEGORY.SuperMethodInvocation, startLine,
				endLine);
		superMethodInvocation.setRoot(rootInfo);
		superMethodInvocation.setStartOffset(buffer.length());
		this.stack.push(superMethodInvocation);

		buffer.append("super.");

		node.getName().accept(this);
		final ProgramElementInfo name = this.stack.pop();
		superMethodInvocation.addExpression(name);

		buffer.append("(");
		
		boolean anyArgument = false;
		for (final Object argument : node.arguments()) {
			anyArgument = true;
			((ASTNode) argument).accept(this);
			final ProgramElementInfo argumentExpression = this.stack.pop();
			superMethodInvocation.addExpression(argumentExpression);
			buffer.append(",");
		}
		
		if (anyArgument) {
			buffer.deleteCharAt(buffer.length() - 1);
		}

		buffer.append(")");
		
		superMethodInvocation.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final TypeLiteral node) {

		/*
		 * XXX no text has been set in the original, which will be set in this
		 * implementation
		 */

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ProgramElementInfo expression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.TypeLiteral, startLine, endLine);
		this.stack.push(expression);
		buffer.append(node.toString());

		return false;
	}

	@Override
	public boolean visit(final QualifiedName node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo qualifiedName = new ExpressionInfo(
				ExpressionInfo.CATEGORY.QualifiedName, startLine, endLine);
		qualifiedName.setRoot(rootInfo);
		qualifiedName.setStartOffset(buffer.length());
		this.stack.push(qualifiedName);

		node.getQualifier().accept(this);
		final ProgramElementInfo qualifier = this.stack.pop();
		qualifiedName.setQualifier(qualifier);

		buffer.append(".");

		node.getName().accept(this);
		final ProgramElementInfo name = this.stack.pop();
		qualifiedName.addExpression((ProgramElementInfo) name);

		qualifiedName.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final SimpleName node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ProgramElementInfo simpleName = new ExpressionInfo(
				ExpressionInfo.CATEGORY.SimpleName, startLine, endLine);
		simpleName.setRoot(rootInfo);
		simpleName.setStartOffset(buffer.length());
		buffer.append(node.getIdentifier());
		this.stack.push(simpleName);

		simpleName.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final CharacterLiteral node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ProgramElementInfo expression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Character, startLine, endLine);
		expression.setRoot(rootInfo);
		expression.setStartOffset(buffer.length());
		buffer.append("\'" + node.charValue() + "\'");
		this.stack.push(expression);

		expression.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final FieldAccess node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo fieldAccess = new ExpressionInfo(
				ExpressionInfo.CATEGORY.FieldAccess, startLine, endLine);
		fieldAccess.setRoot(rootInfo);
		fieldAccess.setStartOffset(buffer.length());
		this.stack.push(fieldAccess);

		node.getExpression().accept(this);
		final ProgramElementInfo expression = this.stack.pop();
		fieldAccess.addExpression((ProgramElementInfo) expression);

		buffer.append(".");

		node.getName().accept(this);
		final ProgramElementInfo name = this.stack.pop();
		fieldAccess.addExpression((ProgramElementInfo) name);

		fieldAccess.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final InfixExpression node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo infixExpression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Infix, startLine, endLine);
		infixExpression.setRoot(rootInfo);
		infixExpression.setStartOffset(buffer.length());
		this.stack.push(infixExpression);

		node.getLeftOperand().accept(this);
		final ProgramElementInfo left = this.stack.pop();
		infixExpression.addExpression(left);

		buffer.append(" ");

		final OperatorInfo operator = new OperatorInfo(node.getOperator()
				.toString(), startLine, endLine);
		infixExpression.addExpression(operator);

		buffer.append(operator.getText());
		buffer.append(" ");

		node.getRightOperand().accept(this);
		final ProgramElementInfo right = this.stack.pop();
		infixExpression.addExpression(right);

		if (node.hasExtendedOperands()) {
			for (final Object operand : node.extendedOperands()) {
				buffer.append(" ");
				buffer.append(operator.getText());
				buffer.append(" ");
				((ASTNode) operand).accept(this);
				final ProgramElementInfo operandExpression = this.stack.pop();
				infixExpression.addExpression(operator);
				infixExpression.addExpression(operandExpression);
			}
		}

		infixExpression.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ArrayCreation node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo arrayCreation = new ExpressionInfo(
				ExpressionInfo.CATEGORY.ArrayCreation, startLine, endLine);
		arrayCreation.setRoot(rootInfo);
		arrayCreation.setStartOffset(buffer.length());
		this.stack.push(arrayCreation);

		buffer.append("new ");

		node.getType().accept(this);
		final ProgramElementInfo type = this.stack.pop();
		arrayCreation.addExpression(type);

		buffer.append("[]");

		if (null != node.getInitializer()) {
			node.getInitializer().accept(this);
			final ProgramElementInfo initializer = this.stack.pop();
			arrayCreation.addExpression((ProgramElementInfo) initializer);
		}

		arrayCreation.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ArrayInitializer node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo initializer = new ExpressionInfo(
				ExpressionInfo.CATEGORY.ArrayInitializer, startLine, endLine);
		initializer.setRoot(rootInfo);
		initializer.setStartOffset(buffer.length());
		this.stack.push(initializer);

		buffer.append("{");
		for (final Object expression : node.expressions()) {
			((ASTNode) expression).accept(this);
			final ProgramElementInfo subexpression = this.stack.pop();
			initializer.addExpression(subexpression);
			buffer.append(",");
		}
		if (0 < node.expressions().size()) {
			buffer.deleteCharAt(buffer.length() - 1);
		}
		buffer.append("}");
		initializer.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final BooleanLiteral node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ProgramElementInfo expression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Boolean, startLine, endLine);
		expression.setRoot(rootInfo);
		expression.setStartOffset(buffer.length());
		this.stack.push(expression);
		buffer.append(node.toString());
		expression.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final Assignment node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo assignment = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Assignment, startLine, endLine);
		assignment.setRoot(rootInfo);
		assignment.setStartOffset(buffer.length());
		this.stack.push(assignment);

		node.getLeftHandSide().accept(this);
		final ProgramElementInfo left = this.stack.pop();
		assignment.addExpression(left);

		buffer.append(" ");

		final OperatorInfo operator = new OperatorInfo(node.getOperator()
				.toString(), startLine, endLine);
		assignment.addExpression(operator);

		buffer.append(node.getOperator().toString());
		buffer.append(" ");

		node.getRightHandSide().accept(this);
		final ProgramElementInfo right = this.stack.pop();
		assignment.addExpression(right);

		assignment.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final CastExpression node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo cast = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Cast, startLine, endLine);
		cast.setRoot(rootInfo);
		cast.setStartOffset(buffer.length());
		this.stack.push(cast);

		final TypeInfo type = new TypeInfo(node.getType().toString(),
				startLine, endLine);
		cast.addExpression(type);

		buffer.append("(" + type.getText() + ")");

		node.getExpression().accept(this);
		final ProgramElementInfo expression = this.stack.pop();
		cast.addExpression(expression);

		cast.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ClassInstanceCreation node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo classInstanceCreation = new ExpressionInfo(
				ExpressionInfo.CATEGORY.ClassInstanceCreation, startLine,
				endLine);
		classInstanceCreation.setRoot(rootInfo);
		classInstanceCreation.setStartOffset(buffer.length());
		this.stack.push(classInstanceCreation);

		final TypeInfo type = new TypeInfo(node.getType().toString(),
				startLine, endLine);
		classInstanceCreation.addExpression(type);

		buffer.append("new ");
		buffer.append(type.getText());
		buffer.append("(");
		for (final Object argument : node.arguments()) {
			((ASTNode) argument).accept(this);
			final ProgramElementInfo argumentExpression = this.stack.pop();
			classInstanceCreation
					.addExpression((ProgramElementInfo) argumentExpression);

			buffer.append(",");
		}
		if (0 < node.arguments().size()) {
			buffer.deleteCharAt(buffer.length() - 1);
		}
		buffer.append(")");

		if (null != node.getExpression()) {
			node.getExpression().accept(this);
			final ProgramElementInfo expression = this.stack.pop();
			classInstanceCreation
					.addExpression((ProgramElementInfo) expression);
		}

		if (null != node.getAnonymousClassDeclaration()) {
			node.getAnonymousClassDeclaration().accept(this);
			final ProgramElementInfo anonymousClass = this.stack.pop();
			classInstanceCreation
					.setAnonymousClassDeclaration((ClassInfo) anonymousClass);
		}

		classInstanceCreation.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ConditionalExpression node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo trinomial = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Trinomial, startLine, endLine);
		trinomial.setRoot(rootInfo);
		trinomial.setStartOffset(buffer.length());
		this.stack.push(trinomial);

		node.getExpression().accept(this);
		final ProgramElementInfo expression = this.stack.pop();
		trinomial.addExpression((ProgramElementInfo) expression);

		buffer.append("? ");

		node.getThenExpression().accept(this);
		final ProgramElementInfo thenExpression = this.stack.pop();
		trinomial.addExpression((ProgramElementInfo) thenExpression);

		buffer.append(": ");

		node.getElseExpression().accept(this);
		final ProgramElementInfo elseExpression = this.stack.pop();
		trinomial.addExpression((ProgramElementInfo) elseExpression);

		trinomial.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ConstructorInvocation node) {

		final int startOffset = buffer.length();

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo invocation = new ExpressionInfo(
				ExpressionInfo.CATEGORY.ConstructorInvocation, startLine,
				endLine);
		invocation.setRoot(rootInfo);
		invocation.setStartOffset(startOffset);
		this.stack.push(invocation);

		buffer.append("this(");
		for (final Object argument : node.arguments()) {
			((ASTNode) argument).accept(this);
			final ProgramElementInfo argumentExpression = this.stack.pop();
			invocation.addExpression((ProgramElementInfo) argumentExpression);
			buffer.append(",");
		}
		if (0 < node.arguments().size()) {
			buffer.deleteCharAt(buffer.length() - 1);
		}
		buffer.append(")");
		invocation.setEndOffset(buffer.length() - 1);

		this.stack.pop();
		final ProgramElementInfo ownerBlock = this.stack.peek();
		final StatementInfo statement = new StatementInfo(ownerBlock,
				StatementInfo.CATEGORY.Expression, startLine, endLine);
		statement.setRoot(rootInfo);
		statement.setStartOffset(startOffset);
		this.stack.push(statement);

		statement.addExpression(invocation);
		buffer.append(";");
		statement.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ExpressionStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startOffset = buffer.length();

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo statement = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Expression, startLine, endLine);
			statement.setRoot(rootInfo);
			statement.setStartOffset(startOffset);
			this.stack.push(statement);

			node.getExpression().accept(this);
			final ProgramElementInfo expression = (ProgramElementInfo) this.stack
					.pop();
			statement.addExpression(expression);

			buffer.append(";");
			statement.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final InstanceofExpression node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo instanceofExpression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Instanceof, startLine, endLine);
		instanceofExpression.setRoot(rootInfo);
		instanceofExpression.setStartOffset(buffer.length());
		this.stack.push(instanceofExpression);

		node.getLeftOperand().accept(this);
		final ProgramElementInfo left = this.stack.pop();
		instanceofExpression.addExpression(left);

		buffer.append(" instanceof ");

		node.getRightOperand().accept(this);
		final ProgramElementInfo right = this.stack.pop();
		instanceofExpression.addExpression(right);

		instanceofExpression.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final MethodInvocation node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo methodInvocation = new ExpressionInfo(
				ExpressionInfo.CATEGORY.MethodInvocation, startLine, endLine);
		methodInvocation.setRoot(rootInfo);
		methodInvocation.setStartOffset(buffer.length());
		this.stack.push(methodInvocation);

		if (null != node.getExpression()) {
			node.getExpression().accept(this);
			final ProgramElementInfo expression = this.stack.pop();
			methodInvocation.setQualifier(expression);

			buffer.append(".");
		}

		node.getName().accept(this);
		final ProgramElementInfo name = this.stack.pop();
		methodInvocation.addExpression(name);

		buffer.append("(");
		for (final Object argument : node.arguments()) {
			((ASTNode) argument).accept(this);
			final ProgramElementInfo argumentExpression = this.stack.pop();
			methodInvocation
					.addExpression((ProgramElementInfo) argumentExpression);

			buffer.append(",");
		}
		if (0 < node.arguments().size()) {
			buffer.deleteCharAt(buffer.length() - 1);
		}
		buffer.append(")");
		methodInvocation.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ParenthesizedExpression node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo parenthesizedExpression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.Parenthesized, startLine, endLine);
		parenthesizedExpression.setRoot(rootInfo);
		parenthesizedExpression.setStartOffset(buffer.length());
		this.stack.push(parenthesizedExpression);
		
		buffer.append("(");

		node.getExpression().accept(this);
		final ProgramElementInfo expression = this.stack.pop();
		parenthesizedExpression.addExpression(expression);

		buffer.append(")");

		parenthesizedExpression.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ReturnStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo returnStatement = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Return, startLine, endLine);
			returnStatement.setRoot(rootInfo);
			returnStatement.setStartOffset(buffer.length());
			this.stack.push(returnStatement);

			buffer.append("return");

			if (null != node.getExpression()) {
				buffer.append(" ");
				node.getExpression().accept(this);
				final ProgramElementInfo expression = this.stack.pop();
				returnStatement.addExpression((ProgramElementInfo) expression);
			}

			buffer.append(";");
			returnStatement.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final SuperConstructorInvocation node) {

		final int startOffset = buffer.length();
		
		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo superConstructorInvocation = new ExpressionInfo(
				ExpressionInfo.CATEGORY.SuperConstructorInvocation, startLine,
				endLine);
		superConstructorInvocation.setRoot(rootInfo);
		superConstructorInvocation.setStartOffset(startOffset);
		this.stack.push(superConstructorInvocation);

		if (null != node.getExpression()) {
			node.getExpression().accept(this);
			final ProgramElementInfo qualifier = this.stack.pop();
			superConstructorInvocation.setQualifier(qualifier);
			buffer.append(".super(");
		} else {
			buffer.append("super(");
		}

		for (final Object argument : node.arguments()) {
			((ASTNode) argument).accept(this);
			final ProgramElementInfo argumentExpression = this.stack.pop();
			superConstructorInvocation
					.addExpression((ProgramElementInfo) argumentExpression);
			buffer.append(",");
		}
		if (0 < node.arguments().size()) {
			buffer.deleteCharAt(buffer.length() - 1);
		}
		buffer.append(")");
		superConstructorInvocation.setEndOffset(buffer.length() - 1);

		this.stack.pop();
		final ProgramElementInfo ownerBlock = this.stack.peek();
		final StatementInfo statement = new StatementInfo(ownerBlock,
				StatementInfo.CATEGORY.Expression, startLine, endLine);
		statement.setRoot(rootInfo);
		statement.setStartOffset(startOffset);
		this.stack.push(statement);

		statement.addExpression(superConstructorInvocation);
		buffer.append(";");
		statement.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final ThisExpression node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ProgramElementInfo expression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.This, startLine, endLine);
		expression.setRoot(rootInfo);
		expression.setStartOffset(buffer.length());
		this.stack.push(expression);
		buffer.append("this");
		expression.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final VariableDeclarationExpression node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo vdExpression = new ExpressionInfo(
				ExpressionInfo.CATEGORY.VariableDeclarationExpression,
				startLine, endLine);
		vdExpression.setRoot(rootInfo);
		vdExpression.setStartOffset(buffer.length());
		this.stack.push(vdExpression);

		final TypeInfo type = new TypeInfo(node.getType().toString(),
				startLine, endLine);
		vdExpression.addExpression(type);

		buffer.append(type.getText());
		buffer.append(" ");

		for (final Object fragment : node.fragments()) {
			((ASTNode) fragment).accept(this);
			final ProgramElementInfo fragmentExpression = this.stack.pop();
			vdExpression.addExpression((ProgramElementInfo) fragmentExpression);
		}

		vdExpression.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final VariableDeclarationStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo vdStatement = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.VariableDeclaration, startLine,
					endLine);
			vdStatement.setRoot(rootInfo);
			vdStatement.setStartOffset(buffer.length());
			this.stack.push(vdStatement);

			for (final Object modifier : node.modifiers()) {
				buffer.append(modifier.toString());
				buffer.append(" ");
			}

			final ProgramElementInfo type = new TypeInfo(node.getType()
					.toString(), startLine, endLine);
			vdStatement.addExpression(type);

			buffer.append(node.getType().toString());
			buffer.append(" ");

			boolean anyExpression = false;
			for (final Object fragment : node.fragments()) {
				anyExpression = true;
				((ASTNode) fragment).accept(this);
				final ProgramElementInfo fragmentExpression = this.stack.pop();
				vdStatement
						.addExpression((ProgramElementInfo) fragmentExpression);
				buffer.append(",");
			}
			if (anyExpression) {
				buffer.deleteCharAt(buffer.length() - 1);
			}

			buffer.append(";");
			vdStatement.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final VariableDeclarationFragment node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final ExpressionInfo vdFragment = new ExpressionInfo(
				ExpressionInfo.CATEGORY.VariableDeclarationFragment, startLine,
				endLine);
		vdFragment.setRoot(rootInfo);
		vdFragment.setStartOffset(buffer.length());
		this.stack.push(vdFragment);

		node.getName().accept(this);
		final ProgramElementInfo name = (ProgramElementInfo) this.stack.pop();
		vdFragment.addExpression(name);

		if (null != node.getInitializer()) {
			buffer.append(" = ");
			node.getInitializer().accept(this);
			final ProgramElementInfo expression = this.stack.pop();
			vdFragment.addExpression((ProgramElementInfo) expression);
		}

		vdFragment.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final DoStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo doBlock = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Do, startLine, endLine);
			doBlock.setRoot(rootInfo);
			doBlock.setStartOffset(buffer.length());
			this.stack.push(doBlock);
			
			buffer.append("do ");

			node.getBody().accept(this);
			final StatementInfo body = (StatementInfo) this.stack.pop();
			doBlock.setStatement(body);

			buffer.append("while (");
			
			node.getExpression().accept(this);
			final ProgramElementInfo condition = (ProgramElementInfo) this.stack
					.pop();
			doBlock.setCondition(condition);
			condition.setOwnerConditinalBlock(doBlock);
			
			buffer.append(");");
			
			/*
			 * XXX no text has been set in the original, which is now set in the current implementation
			 */

			doBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final EnhancedForStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startOffset = buffer.length();
			buffer.append("for (");
			
			node.getParameter().accept(this);
			final ProgramElementInfo parameter = this.stack.pop();

			buffer.append(" : ");
			
			node.getExpression().accept(this);
			final ProgramElementInfo expression = this.stack.pop();
			
			buffer.append(")");

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo foreachBlock = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Foreach, startLine, endLine);
			foreachBlock.setRoot(rootInfo);
			foreachBlock.setStartOffset(startOffset);
			foreachBlock.addInitializer(parameter);
			foreachBlock.addInitializer(expression);
			this.stack.push(foreachBlock);

			node.getBody().accept(this);
			final StatementInfo body = (StatementInfo) this.stack.pop();
			foreachBlock.setStatement(body);

			foreachBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final ForStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo forBlock = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.For, startLine, endLine);
			forBlock.setRoot(rootInfo);
			forBlock.setStartOffset(buffer.length());
			this.stack.push(forBlock);

			buffer.append("for (");

			for (final Object o : node.initializers()) {
				((ASTNode) o).accept(this);
				final ExpressionInfo initializer = (ExpressionInfo) this.stack
						.pop();
				forBlock.addInitializer(initializer);
				buffer.append(",");
			}
			if (0 < node.initializers().size()) {
				buffer.deleteCharAt(buffer.length() - 1);
			}

			buffer.append("; ");

			if (null != node.getExpression()) {
				node.getExpression().accept(this);
				final ProgramElementInfo condition = (ProgramElementInfo) this.stack
						.pop();
				forBlock.setCondition(condition);
				condition.setOwnerConditinalBlock(forBlock);
			}

			buffer.append("; ");

			for (final Object o : node.updaters()) {
				((ASTNode) o).accept(this);
				final ExpressionInfo updater = (ExpressionInfo) this.stack
						.pop();
				forBlock.addUpdater(updater);
				buffer.append(",");
			}
			if (0 < node.updaters().size()) {
				buffer.deleteCharAt(buffer.length() - 1);
			}

			buffer.append(")");

			node.getBody().accept(this);
			final StatementInfo body = (StatementInfo) this.stack.pop();
			forBlock.setStatement(body);

			forBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final IfStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo ifBlock = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.If, startLine, endLine);
			ifBlock.setRoot(rootInfo);
			ifBlock.setStartOffset(buffer.length());
			this.stack.push(ifBlock);
			
			buffer.append("if (");

			node.getExpression().accept(this);
			final ProgramElementInfo condition = (ProgramElementInfo) this.stack
					.pop();
			ifBlock.setCondition(condition);
			condition.setOwnerConditinalBlock(ifBlock);
			
			buffer.append(") ");

			if (null != node.getThenStatement()) {
				node.getThenStatement().accept(this);
				final StatementInfo thenBody = (StatementInfo) this.stack.pop();
				ifBlock.setStatement(thenBody);
			}

			if (null != node.getElseStatement()) {
				node.getElseStatement().accept(this);
				final StatementInfo elseBody = (StatementInfo) this.stack.pop();
				ifBlock.setElseStatement(elseBody);
			}

			ifBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final SwitchStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo switchBlock = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Switch, startLine, endLine);
			switchBlock.setRoot(rootInfo);
			switchBlock.setStartOffset(buffer.length());
			this.stack.push(switchBlock);

			buffer.append("switch (");
			
			node.getExpression().accept(this);
			final ProgramElementInfo condition = (ProgramElementInfo) this.stack
					.pop();
			switchBlock.setCondition(condition);
			condition.setOwnerConditinalBlock(switchBlock);

			buffer.append(") {");
			buffer.append(System.getProperty("line.separator"));

			for (final Object o : node.statements()) {
				((ASTNode) o).accept(this);
				final StatementInfo statement = (StatementInfo) this.stack
						.pop();
				switchBlock.addStatement(statement);
				buffer.append(System.getProperty("line.separator"));
			}

			switchBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final SynchronizedStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo synchronizedBlock = new StatementInfo(
					ownerBlock, StatementInfo.CATEGORY.Synchronized, startLine,
					endLine);
			synchronizedBlock.setRoot(rootInfo);
			synchronizedBlock.setStartOffset(buffer.length());
			this.stack.push(synchronizedBlock);
			
			buffer.append("synchronized (");

			node.getExpression().accept(this);
			final ProgramElementInfo condition = (ProgramElementInfo) this.stack
					.pop();
			synchronizedBlock.setCondition(condition);
			condition.setOwnerConditinalBlock(synchronizedBlock);

			buffer.append(") ");
			
			node.getBody().accept(this);
			final StatementInfo body = (StatementInfo) this.stack.pop();
			synchronizedBlock.setStatement(body);

			synchronizedBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final ThrowStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {
			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo throwStatement = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Throw, startLine, endLine);
			throwStatement.setRoot(rootInfo);
			throwStatement.setStartOffset(buffer.length());
			this.stack.push(throwStatement);

			buffer.append("throw ");
			
			node.getExpression().accept(this);
			final ProgramElementInfo expression = (ProgramElementInfo) this.stack
					.pop();
			throwStatement.addExpression(expression);

			buffer.append(";");
			
			throwStatement.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final TryStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo tryBlock = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Try, startLine, endLine);
			tryBlock.setRoot(rootInfo);
			tryBlock.setStartOffset(buffer.length());
			this.stack.push(tryBlock);

			buffer.append("try ");
			
			node.getBody().accept(this);
			final StatementInfo body = (StatementInfo) this.stack.pop();
			tryBlock.setStatement(body);

			for (final Object o : node.catchClauses()) {
				((ASTNode) o).accept(this);
				final StatementInfo catchBlock = (StatementInfo) this.stack
						.pop();
				tryBlock.addCatchStatement(catchBlock);
			}

			if (null != node.getFinally()) {
				node.getFinally().accept(this);
				final StatementInfo finallyBlock = (StatementInfo) this.stack
						.pop();
				tryBlock.setFinallyStatement(finallyBlock);
			}

			tryBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final WhileStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo whileBlock = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.While, startLine, endLine);
			whileBlock.setRoot(rootInfo);
			whileBlock.setStartOffset(buffer.length());
			this.stack.push(whileBlock);
			
			buffer.append("while (");

			node.getExpression().accept(this);
			final ProgramElementInfo condition = (ProgramElementInfo) this.stack
					.pop();
			whileBlock.setCondition(condition);
			condition.setOwnerConditinalBlock(whileBlock);
			
			buffer.append(") ");

			node.getBody().accept(this);
			StatementInfo body = (StatementInfo) this.stack.pop();
			whileBlock.setStatement(body);

			whileBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final SwitchCase node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {
			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo switchCase = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Case, startLine, endLine);
			switchCase.setRoot(rootInfo);
			switchCase.setStartOffset(buffer.length());
			this.stack.push(switchCase);

			if (null != node.getExpression()) {
				buffer.append("case ");
				
				node.getExpression().accept(this);
				final ProgramElementInfo expression = this.stack.pop();
				switchCase.addExpression((ProgramElementInfo) expression);
			} else {
				buffer.append("default");
			}

			buffer.append(":");
			switchCase.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final BreakStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo breakStatement = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Break, startLine, endLine);
			breakStatement.setRoot(rootInfo);
			breakStatement.setStartOffset(buffer.length());
			this.stack.push(breakStatement);

			buffer.append("break");

			if (null != node.getLabel()) {
				buffer.append(" ");
				node.getLabel().accept(this);
				final ProgramElementInfo label = this.stack.pop();
				breakStatement.addExpression((ProgramElementInfo) label);
			}

			buffer.append(";");
			breakStatement.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final ContinueStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo continuekStatement = new StatementInfo(
					ownerBlock, StatementInfo.CATEGORY.Continue, startLine,
					endLine);
			continuekStatement.setRoot(rootInfo);
			continuekStatement.setStartOffset(buffer.length());
			this.stack.push(continuekStatement);

			buffer.append("continue");

			if (null != node.getLabel()) {
				buffer.append(" ");
				node.getLabel().accept(this);
				final ProgramElementInfo label = this.stack.pop();
				continuekStatement.addExpression((ProgramElementInfo) label);
			}

			buffer.append(";");
			continuekStatement.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final LabeledStatement node) {

		node.getBody().accept(this);
		final StatementInfo statement = (StatementInfo) this.stack.peek();

		final String label = node.getLabel().toString();
		statement.setLabel(label);

		return false;
	}

	@Override
	public boolean visit(final Block node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo simpleBlock = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.SimpleBlock, startLine, endLine);
			simpleBlock.setRoot(rootInfo);
			simpleBlock.setStartOffset(buffer.length());
			this.stack.push(simpleBlock);

			buffer.append("{");
			buffer.append(System.getProperty("line.separator"));

			for (final Object o : node.statements()) {
				((ASTNode) o).accept(this);
				final ProgramElementInfo statement = this.stack.pop();
				simpleBlock.addStatement((StatementInfo) statement);
				buffer.append(System.getProperty("line.separator"));
			}

			buffer.append("}");
			simpleBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final CatchClause node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo catchBlock = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Catch, startLine, endLine);
			catchBlock.setRoot(rootInfo);
			catchBlock.setStartOffset(buffer.length());
			this.stack.push(catchBlock);

			buffer.append("catch (");
			
			node.getException().accept(this);
			final ProgramElementInfo exception = this.stack.pop();
			exception.setOwnerConditinalBlock(catchBlock);
			catchBlock.setCondition(exception);

			buffer.append(") ");
			
			node.getBody().accept(this);
			final StatementInfo body = (StatementInfo) this.stack.pop();
			catchBlock.setStatement(body);

			catchBlock.setEndOffset(buffer.length() - 1);
		}

		return false;
	}

	@Override
	public boolean visit(final SingleVariableDeclaration node) {

		final int startLine = this.getStartLineNumber(node);
		final int endLine = this.getEndLineNumber(node);
		final TypeInfo type = new TypeInfo(node.getType().toString(),
				startLine, endLine);
		final String name = node.getName().toString();
		final VariableInfo variable = new VariableInfo(
				VariableInfo.CATEGORY.LOCAL, type, name, startLine, endLine);
		variable.setRoot(rootInfo);
		variable.setStartOffset(buffer.length());
		this.stack.push(variable);

		for (final Object modifier : node.modifiers()) {
			variable.addModifier(modifier.toString());
			buffer.append(modifier.toString());
			buffer.append(" ");
		}
		buffer.append(type.getText());
		buffer.append(" ");
		buffer.append(name);
		variable.setEndOffset(buffer.length() - 1);

		return false;
	}

	@Override
	public boolean visit(final EmptyStatement node) {

		if (!this.stack.isEmpty() && this.stack.peek() instanceof BlockInfo) {

			final int startLine = this.getStartLineNumber(node);
			final int endLine = this.getEndLineNumber(node);
			final ProgramElementInfo ownerBlock = this.stack.peek();
			final StatementInfo emptyStatement = new StatementInfo(ownerBlock,
					StatementInfo.CATEGORY.Empty, startLine, endLine);
			this.stack.push(emptyStatement);
			buffer.append(";");
			emptyStatement.setText(";");
		}

		return false;
	}

}
