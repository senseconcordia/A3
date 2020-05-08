package org.exparser.util.databaseplugin.handlers;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.*;


/*Class is deprecated and no longer used
 * Keeping, in case it is required for future anti-pattern detection
 * 
 * Builds a HashMap call tree of all method invocations and which method declarations use them.
 */
public class CallTreeBuilder extends ASTVisitor {
	
	final HashMap<MethodDeclaration, ArrayList<MethodInvocation>> invocationsForMethods =
		    new HashMap<>();
	
	private MethodDeclaration activeMethod;

	@Override
	public boolean visit(MethodDeclaration node) {
		activeMethod = node;
		return super.visit(node);
		
	}
	
	@Override
	public boolean visit(MethodInvocation node){
		if (invocationsForMethods.get(activeMethod) == null) {
            invocationsForMethods.put(activeMethod, new ArrayList<MethodInvocation>());
        }
        invocationsForMethods.get(activeMethod).add(node);
        return super.visit(node);
	}

	public HashMap<MethodDeclaration, ArrayList<MethodInvocation>> getMethods() {
		return invocationsForMethods;
	}
}