package org.exparser.util.scrapeVisitors;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class MethodInvocationVisitor extends ASTVisitor {
        List<MethodInvocation> APImethodInvocations = new ArrayList<>();

        @Override
        public boolean visit(MethodInvocation node){


            APImethodInvocations.add(node);
            return super.visit(node);
        }

        public List<MethodInvocation> getAPImethodInvocations() {
            return APImethodInvocations;
        }

}
