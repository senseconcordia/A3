package org.exparser.util.databaseplugin.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

//Builds a thread to visit classes and check for database accesses. This is treaded to increase speed.
public class ClassVisitorThread extends Thread {
	
	private ICompilationUnit unit;
	private List<MethodDeclaration> databaseCallMethods;
	private List<ITypeBinding> interfaceBindings;
	
	public ClassVisitorThread(ICompilationUnit unit, List<ITypeBinding> interfaceBindings){
		this.unit = unit;
		this.databaseCallMethods = new ArrayList<>();
		this.interfaceBindings = interfaceBindings;
	}
	
	private void buildMethodDeclarations(){

		CompilationUnit parse = parse(unit);
		ClassVisitor classVisitor = new ClassVisitor();
		classVisitor.setInterfaceBindings(interfaceBindings);
		parse.accept(classVisitor);
		

		for (MethodDeclaration method : classVisitor.getMethods()) {
			//if a method exists, then add it to the list
			if(method != null){
				databaseCallMethods.add(method);
			}
		}
		
	}
	
	public void run(){
		buildMethodDeclarations();
	}
	
	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null); // parse
	}

	public ICompilationUnit getUnit() {
		return unit;
	}
	public void setUnit(ICompilationUnit unit) {
		this.unit = unit;
	}
	
	public List<MethodDeclaration> getDatabaseCallMethods() {
		return databaseCallMethods;
	}

}
