package org.exparser.util.databaseplugin.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

//visits class nodes of interest
public class ClassVisitor extends ASTVisitor{
	List<MethodDeclaration> methods = new ArrayList<MethodDeclaration>();
	List<MethodDeclaration> interfaceMethods = new ArrayList<MethodDeclaration>();
	List<ITypeBinding> interfaceBindings;

	
	
	@Override
	public boolean visit(TypeDeclaration node) {
		boolean isEntity = false;
		boolean shouldCheck = false;
		List modifiers = node.modifiers();
		
		ITypeBinding typeBinding = node.resolveBinding();
		
		for(Object item : modifiers){
			isEntity = shouldCheckNode(item, isEntity);
		}
		if(isEntity){
			shouldCheck = true;
		}
			
			//Check to see if the MarkerAnnotation is of type (database connection), and doesnt have other annotations
			if(shouldCheck){
				
				//check to see if class has methods that access Db and save them.
				checkNodeMethods(node, typeBinding);
				
				//check to see if class has interface
				if(node.superInterfaceTypes().size() >0 && (node.superInterfaceTypes() != null)){
					List interfaceNodes = node.superInterfaceTypes();
					
					TypeDeclaration decl = null;
					for(Object interfaceNode: interfaceNodes){
						ITypeBinding binding = ((Type) interfaceNode).resolveBinding();
					
						ICompilationUnit unit =null;
						if(binding != null){
							 unit = (ICompilationUnit) binding.getJavaElement().getAncestor( IJavaElement.COMPILATION_UNIT );
						}
						if ( unit != null ) {
							//If an interface exists, get interesting methods
							getInterfaceMethods(binding, unit);
						}
					}					
				}	
			}
		return super.visit(node);
	}

	private void checkNodeMethods(TypeDeclaration node, ITypeBinding typeBinding) {
		//Check all the fields in the class of interest
		FieldDeclarationVisitor fieldVisitor = new FieldDeclarationVisitor();
		node.accept(fieldVisitor);
		
		//Check all methods in class of interest
		MethodVisitor methodVisitor = new MethodVisitor();
		//Give the method visitor access to the fields so it can return only methods that access fields of interest
		methodVisitor.setFields(fieldVisitor.getFields());
		node.accept(methodVisitor);
		
		for (MethodDeclaration method : methodVisitor.getMethods()){
			if(method != null){
				methods.add(method);
			}
			for(ITypeBinding bind: interfaceBindings){
				if(typeBinding.equals(bind)){
					methods.add(method);
				}
			}
		}
	}

	private boolean shouldCheckNode(Object item, boolean isEntity) {
	
		//check to see if any of the modifiers of the current class node are SingleMemberAnnotation
		
		//check to see if any of the modifiers of the current class node are MarkerAnnotations
		if (item instanceof MarkerAnnotation) {
			MarkerAnnotation marker = (MarkerAnnotation) item;
			Name name = marker.getTypeName();
			
			isEntity = checkIfEntity(isEntity, name);
		}
		return isEntity;
	}

	private void getInterfaceMethods(ITypeBinding binding, ICompilationUnit unit) {
		TypeDeclaration decl;
		ASTParser parser = ASTParser.newParser( AST.JLS8 );
		parser.setKind( ASTParser.K_COMPILATION_UNIT );
		parser.setSource( unit );
		parser.setResolveBindings( true );
		CompilationUnit cu = (CompilationUnit) parser.createAST( null );
		decl = (TypeDeclaration)cu.findDeclaringNode( binding.getKey() );
		interfaceMethodVisitor interfaceMethodVisitor = new interfaceMethodVisitor();
		interfaceMethodVisitor.setImplementedMethods(methods);
		if(interfaceMethodVisitor != null && decl != null){
			decl.accept(interfaceMethodVisitor);
			methods.addAll(interfaceMethodVisitor.getMethods());
		}	
	}

	private boolean checkIfEntity(boolean isEntity, Name name) {
		if(name.toString().equals("Entity")){
			isEntity = true;
		}
		return isEntity;
	}
	
	public List<MethodDeclaration> getMethods() {
		return methods;
	}

	public void setInterfaceBindings(List<ITypeBinding> interfaceBindings) {
		this.interfaceBindings = interfaceBindings;
	}
	


}
