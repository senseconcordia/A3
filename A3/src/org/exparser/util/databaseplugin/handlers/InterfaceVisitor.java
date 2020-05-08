package org.exparser.util.databaseplugin.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class InterfaceVisitor extends ASTVisitor{
	List<ITypeBinding> interfaceBindings = new ArrayList<ITypeBinding>();
	
	@Override
	public boolean visit(TypeDeclaration node){
		ITypeBinding interfaceBinds = null;
		
		ITypeBinding typeBinding = node.resolveBinding();
		
		if(typeBinding != null){
			ITypeBinding[] interfaceBindar = typeBinding.getInterfaces();
			if(interfaceBindar.length > 0){
				interfaceBinds = interfaceBindar[0];
			}
			else{
				String shout = "shoud";
			}
		}
		
		
		if(interfaceBinds !=null){
			interfaceBindings.add(interfaceBinds);
		}
		return super.visit(node);
		
	}
	
	public List<ITypeBinding> getInterfaceBindings(){
		return this.interfaceBindings;
	}
}
