package org.exparser.util.databaseplugin.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

public class FieldDeclarationVisitor extends ASTVisitor{
	List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
	
	@Override
	public boolean visit(FieldDeclaration node) {

		List modifiers = node.modifiers();
		boolean containsDatabaseLink = false;
		boolean containsBatchGet = false;
		boolean usesFetchStrategy = false;
		//check the modifiers of the field of interest
		for (Object item : modifiers){
			//There are two types of annotations possible for "Column" (a database link annotation), check both.
			if (item instanceof NormalAnnotation) {
				NormalAnnotation marker = (NormalAnnotation) item;
				Name name = marker.getTypeName();
				String annotationName = name.toString();
				//if(annotationName.equals("Column") || annotationName.equals("MapKey") || annotationName.equals("MapKeyColumn")){
					containsDatabaseLink = true;
				//}
				if(annotationName.equals("BatchSize")){
					containsBatchGet = true;
				}
				if(annotationName.equals("Cache")){
					containsBatchGet = true;
				}
			}
			
			if (item instanceof MarkerAnnotation) {
				MarkerAnnotation marker = (MarkerAnnotation) item;
				Name name = marker.getTypeName();
				if(name.toString().equals("Column")){
					containsDatabaseLink = true;
				}
				if(name.toString().equals("BatchSize")){
					containsBatchGet = true;
				}
			}
			
			usesFetchStrategy = checkFetchStrategy(usesFetchStrategy, item);
		}
		
		if(containsDatabaseLink && !containsBatchGet && !usesFetchStrategy){
			fields.add(node);
		}
		
	return super.visit(node);
	}

	private boolean checkFetchStrategy(boolean usesFetchStrategy, Object item) {
		if(item instanceof SingleMemberAnnotation){
			SingleMemberAnnotation marker = (SingleMemberAnnotation) item;
			Name name = marker.getTypeName();
			//Check to see if the MarkerAnnotation is of type Entity (database connection)
			if(name.toString().equals("Fetch")){
				if(marker.getValue() instanceof QualifiedName){
					QualifiedName valueName = (QualifiedName) marker.getValue();
					if(valueName.getName().getIdentifier().equals("SUBSELECT")){
						usesFetchStrategy = true;
					}
					else if (valueName.getName().getIdentifier().equals("JOIN")) {
						usesFetchStrategy = true;
					}
				}
			}
		}
		return usesFetchStrategy;
	}
	
	public List<FieldDeclaration> getFields() {
		return fields;
	}

}
