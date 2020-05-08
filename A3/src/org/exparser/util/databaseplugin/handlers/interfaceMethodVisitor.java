package org.exparser.util.databaseplugin.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class interfaceMethodVisitor extends ASTVisitor {
	List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
	List<MethodDeclaration> implementedMethods = new ArrayList<MethodDeclaration>();

	@Override
	public boolean visit(MethodDeclaration node) {
		String nodeName = node.getName().getFullyQualifiedName();
		
		for(MethodDeclaration impMethod : implementedMethods){
			String implementationName =impMethod.getName().getFullyQualifiedName();
			
			
			if(nodeName.equals(implementationName)){
				methods.add(node);
			}
		}

		return super.visit(node);
	}
	

	public List<MethodDeclaration> getMethods() {
		return methods;
	}
	
	public void setImplementedMethods(List<MethodDeclaration> implementedMethods) {
		this.implementedMethods = implementedMethods;
	}

}
