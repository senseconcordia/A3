package org.exparser.util.databaseplugin.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

//A simple visitor to visit simple names nodes
public class SimpleNameVisitor extends ASTVisitor{
	List<SimpleName> simpleNames = new ArrayList<SimpleName>();
	List<FieldDeclaration> fields;
	
	@Override
	public boolean visit(SimpleName node){
	
		if(node != null){
			simpleNames.add(node);
		}
			
	return super.visit(node);
	}
	
	public List<SimpleName> getSimpleNames() {
		return simpleNames;
	}
	
}
