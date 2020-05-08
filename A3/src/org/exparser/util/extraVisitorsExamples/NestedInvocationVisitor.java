package org.exparser.util.extraVisitorsExamples;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;


public class NestedInvocationVisitor extends ASTVisitor{
	List<MethodInvocation> nestedTransactionInvocationList = new ArrayList<MethodInvocation>();
	List<MethodDeclaration> interfaceTransactionalMethods;
	List<MethodDeclaration> visitedNodes = new ArrayList<>();
	
	public void setVisitedNodes(List<MethodDeclaration> visitedNodes) {
		this.visitedNodes = visitedNodes;
	}

	@Override
	public boolean visit(MethodInvocation node){
		try{
			
			IBinding methodDeclarationBinding = node.resolveMethodBinding().getMethodDeclaration();
			IAnnotationBinding[] methodBindings = methodDeclarationBinding.getAnnotations();
			
			for(IAnnotationBinding annotationBinding : methodBindings){
				String bindingString = annotationBinding.toString();
				if(bindingString.contains("REQUIRES_NEW")){
					nestedTransactionInvocationList.add(node);
					return super.visit(node);
				}			
			}
			MethodDeclaration declarationNode = getNodeFromBinding(methodDeclarationBinding);
			visitedNodes.add(declarationNode);
			if(declarationNode != null){
				if(!visitedNodes.contains(declarationNode)){
					if(!interfaceTransactionalMethods.contains(declarationNode)){
						NestedInvocationVisitor recursedMethodVisitor = new NestedInvocationVisitor();
						recursedMethodVisitor.setInterfaceTransactionalMethods(interfaceTransactionalMethods);
						//makes sure nodes are not visited twice
						recursedMethodVisitor.setVisitedNodes(visitedNodes);
						declarationNode.accept(recursedMethodVisitor);
						nestedTransactionInvocationList.addAll(recursedMethodVisitor.getNestedTransactionInvocationList());
					}
				}
			}
			
			
		}
		catch(NullPointerException e){
			System.out.println("Node Binding was Null");
		}
		
		//check list of interface methods
		try{
			for(MethodDeclaration method: interfaceTransactionalMethods){
				IMethodBinding methodBinding = method.resolveBinding();
				IMethodBinding nodeBinding = node.resolveMethodBinding();
				if(nodeBinding != null){
					if(nodeBinding.isEqualTo(methodBinding)){
						nestedTransactionInvocationList.add(node);
						return super.visit(node);
					}
				}
				else if(method.getName().getFullyQualifiedName().equals(node.getName().getFullyQualifiedName())){
					nestedTransactionInvocationList.add(node);
					return super.visit(node);
				}
			}
		}
		catch(NullPointerException e){
			System.out.println("Node Binding was Null");
		}
		
		
		
		return super.visit(node);
	}

	public List<MethodInvocation> getNestedTransactionInvocationList() {
		return nestedTransactionInvocationList;
	}

	public void setInterfaceTransactionalMethods(List<MethodDeclaration> interfaceTransactionalMethods) {
		this.interfaceTransactionalMethods = interfaceTransactionalMethods;
	}
	
	private MethodDeclaration getNodeFromBinding(IBinding binding){
		MethodDeclaration decl = null;
		
		ICompilationUnit unit = (ICompilationUnit) binding.getJavaElement().getAncestor( IJavaElement.COMPILATION_UNIT );
		if ( unit == null ) {
		   // not available, external declaration
		}
		else{
			ASTParser parser = ASTParser.newParser( AST.JLS8 );
			parser.setKind( ASTParser.K_COMPILATION_UNIT );
			parser.setSource( unit );
			parser.setResolveBindings( true );
			CompilationUnit cu = (CompilationUnit) parser.createAST( null );
			decl = (MethodDeclaration)cu.findDeclaringNode( binding.getKey() );			
		}
		
		return decl ;
	}

}
