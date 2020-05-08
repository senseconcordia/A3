package org.exparser.util.scrapeVisitors;

import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class ApiClassVisitor extends ASTVisitor{
    LinkedList<MethodDeclaration> methods = new LinkedList<>();
    TypeDeclaration classNode;

    @Override
    public boolean visit(TypeDeclaration node) {
        this.classNode = node;

        ITypeBinding typeBinding = node.resolveBinding();

        checkNodeMethods(node);

        return super.visit(node);
    }

    private void checkNodeMethods(TypeDeclaration node) {
        LinkedHashSet<MethodDeclaration> uniqueMethods = new LinkedHashSet<>();
        ApiMethodVisitor apiMethodVisitor = new ApiMethodVisitor();
        node.accept(apiMethodVisitor);

        for (MethodDeclaration method : apiMethodVisitor.getApiMethodDeclaration()){
            if(method != null){
                for (Object modifier : method.modifiers()) {
                    //if (modifier.toString().equals("public")){
                    //break;
                    //}
                    if(modifier.toString().equals("@Test")){
                        break;
                    }
                    else {
                        uniqueMethods.add(method);
                    }
                }
            }
            // TODO make this only look at public methods.
            /* TODO look into making this work. Example is ClassVisitor in database plugin.handlers
            for(ITypeBinding bind: interfaceBindings){
                if(typeBinding.equals(bind)){
                    methods.add(method);
                }
            }
            */
        }
        methods.addAll(uniqueMethods);
    }

    public LinkedList<MethodDeclaration> getMethods() {
        return methods;
    }

    public TypeDeclaration getClassNode() {
        return classNode;
    }
}
