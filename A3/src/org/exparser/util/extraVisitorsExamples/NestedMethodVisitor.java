package org.exparser.util.extraVisitorsExamples;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;

public class NestedMethodVisitor extends ASTVisitor{
	List<MethodInvocation> nestedTransactionInvocationList = new ArrayList<MethodInvocation>();
	List<MethodDeclaration> interfaceTransactionalMethods;
	
	@Override
	public boolean visit(MethodDeclaration node) {
		boolean isTransaction = false;
		boolean shouldCheck = false;
		
		List modifiers = node.modifiers();
		for(Object item : modifiers){
			isTransaction = shouldCheckNode(item, isTransaction);
		}
		if(isTransaction){
			shouldCheck = true;
		}
		if(shouldCheck){
			
			NestedInvocationVisitor invocationVisit = new NestedInvocationVisitor();
			invocationVisit.setInterfaceTransactionalMethods(interfaceTransactionalMethods);
			node.accept(invocationVisit);

			nestedTransactionInvocationList.addAll(invocationVisit.getNestedTransactionInvocationList());
			
		}

		return super.visit(node);
	}
	
	private boolean shouldCheckNode(Object item, boolean isTransaction) {
		
		//check to see if any of the modifiers of the current class node are MarkerAnnotations
		if (item instanceof MarkerAnnotation) {
			MarkerAnnotation marker = (MarkerAnnotation) item;
			Name name = marker.getTypeName();
			
			isTransaction = checkIfTransactional(isTransaction, name);
		}
		if (item instanceof NormalAnnotation) {
			NormalAnnotation marker = (NormalAnnotation) item;
			Name name = marker.getTypeName();
			
			isTransaction = checkIfTransactional(isTransaction, name);
		}
		return isTransaction;
	}
		
	private boolean checkIfTransactional(boolean isTransactional, Name name) {
		if(name.toString().equals("Transactional")){
			isTransactional = true;
		}
		return isTransactional;
	}

	public List<MethodInvocation> getNestedTransactionInvocationList() {
		return nestedTransactionInvocationList;
	}

	public void setInterfaceTransactionalMethods(List<MethodDeclaration> interfaceTransactionalMethods) {
		this.interfaceTransactionalMethods = interfaceTransactionalMethods;
	}
}

