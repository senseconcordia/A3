package org.exparser.util.databaseplugin.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

//Visits method nodes of interest
public class MethodVisitor extends ASTVisitor {
	List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
	List<FieldDeclaration> fields;
			
	@Override
	public boolean visit(MethodDeclaration node) {
		SimpleNameVisitor nameVisitor = new SimpleNameVisitor();
		node.accept(nameVisitor);
		//get all the simple names inside the methodDeclaration
		List<SimpleName> simpleNamesList = nameVisitor.getSimpleNames();
		
		//for any Fields given check to see if any field names match the method
		if(fields != null){
			for(FieldDeclaration field : fields){
				
				SimpleNameVisitor fieldNameVisitor = new SimpleNameVisitor();
				field.accept(fieldNameVisitor);
				//get all simple names for the field of interest
				List<SimpleName> fieldNameList = fieldNameVisitor.getSimpleNames();
				
				for(SimpleName fieldName : fieldNameList){
					for(SimpleName instanceVariable: simpleNamesList){
						
						//if the methods simple name is the same as one of the fields, add the method node to the list.
						if(instanceVariable.getIdentifier().equals(fieldName.getIdentifier())){
							methods.add(node);
							return super.visit(node);
						}
					}
				}	
			}
		}
		return super.visit(node);
	}

	public List<MethodDeclaration> getMethods() {
		return methods;
	}
	
	public void setFields(List<FieldDeclaration> fieldsList){
		this.fields = fieldsList;
	}
}