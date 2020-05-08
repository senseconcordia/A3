package org.exparser.util.scrapeVisitors;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class ApiUsageVisitor extends ASTVisitor{
    List<MethodInvocation> methodsInvoked = new ArrayList();

    @Override
    public boolean visit(TypeDeclaration node) {

        ITypeBinding typeBinding = node.resolveBinding();

        checkNodeMethods(node);

        return super.visit(node);
    }

    private void checkNodeMethods(TypeDeclaration node) {

        MethodInvocationVisitor methodInvocationVisitor = new MethodInvocationVisitor();
        node.accept(methodInvocationVisitor);

        for (MethodInvocation method : methodInvocationVisitor.getAPImethodInvocations()){
            if(method != null){
                methodsInvoked.add(method);
            }
            /* TODO look into making this work. Example is ClassVisitor in database plugin.handlers
            for(ITypeBinding bind: interfaceBindings){
                if(typeBinding.equals(bind)){
                    methods.add(method);
                }
            }
            */
        }
    }

    public List<MethodInvocation> getMethodsInvoked() {
        return methodsInvoked;
    }

}
