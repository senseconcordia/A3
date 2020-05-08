package org.exparser.util.scrapeVisitors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.ArrayList;
import java.util.List;

public class ApiMethodVisitor extends ASTVisitor {
    List<MethodDeclaration> ApiMethodDeclaration = new ArrayList<>();

    @Override
    public boolean visit(MethodDeclaration node){

        ApiMethodDeclaration.add(node);
        return super.visit(node);
    }

    public List<MethodDeclaration> getApiMethodDeclaration() {
        return ApiMethodDeclaration;
    }

}

