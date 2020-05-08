package org.exparser.util.scrapeVisitors;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class ImportVisitor extends ASTVisitor{
	List<MethodInvocation> importInvocation = new ArrayList<>();
	
	@Override
	public boolean visit(ImportDeclaration node){
		Name nodeName = node.getName();
		if(nodeName.toString().contains("android")){

			ASTNode nodeRoot = node.getRoot();
			ApiMethodVisitor apiMethodVisitor = new ApiMethodVisitor();
			nodeRoot.accept(apiMethodVisitor);
			
		}

		
		return super.visit(node);
		
	}
	
	public List<MethodInvocation> getImportInvocation() {
		return importInvocation;
	}
	
}
