package org.exparser.util.extraVisitorsExamples;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ClassVisitor extends ASTVisitor{
	List<MethodInvocation> nestedTransactionInvocationList = new ArrayList<MethodInvocation>();
	List<MethodDeclaration> interfaceTransactionalMethods;
	
	@Override
	public boolean visit(TypeDeclaration node) {
		
		visitMethods(node);
		
		return super.visit(node);
	}
	
	public void visitMethods(TypeDeclaration node){
		NestedMethodVisitor methodVisitor = new NestedMethodVisitor();
		methodVisitor.setInterfaceTransactionalMethods(interfaceTransactionalMethods);
		node.accept(methodVisitor);
		nestedTransactionInvocationList.addAll(methodVisitor.getNestedTransactionInvocationList());
	}
	
	public List<MethodInvocation> getNestedTransactionInvocationList() {
		return nestedTransactionInvocationList;
	}

	public void setInterfaceTransactionalMethods(List<MethodDeclaration> interfaceTransactionalMethods) {
		this.interfaceTransactionalMethods = interfaceTransactionalMethods;
	}
	

}
