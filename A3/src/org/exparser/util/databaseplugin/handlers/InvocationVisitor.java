package org.exparser.util.databaseplugin.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class InvocationVisitor extends ASTVisitor {
	
	List<MethodInvocation> methods = new ArrayList<MethodInvocation>();
	List<IMethodBinding> databaseMethodBindings;
	
	
	//This method will visit Method invocation nodes of interest, and it will check to see if they are part of prefetched databaseMethodBindings
	//databaseMethodBindings must be passed in before nodes are visited.
	@Override
	public boolean visit(MethodInvocation node){
		IMethodBinding invocationBinding = node.resolveMethodBinding();
		
		try{
			for(IMethodBinding databaseMethodBinding : databaseMethodBindings){
				if(databaseMethodBinding.isEqualTo(invocationBinding)){
					methods.add(node);
					return super.visit(node);
				}
			}
		}
		catch(NullPointerException e){
			//do nothing;
		}
        return super.visit(node);
	}

	public List<MethodInvocation> getMethods() {
		return methods;
	}
	
	public void setDatabaseMethodBindings(List<IMethodBinding> databaseMethodBindings){
		this.databaseMethodBindings = databaseMethodBindings;
	}

}
