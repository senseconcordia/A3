package org.exparser.util.CFG;

import org.eclipse.jdt.core.dom.*;
import org.exparser.util.databaseplugin.handlers.SimpleNameVisitor;
import org.exparser.util.fileHandler.FileSearcher;
import org.exparser.util.scrapeVisitors.ApiClassVisitor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class CFGFactory {

    public LinkedList<CFG> createCFG(CompilationUnit compilationUnit){

        LinkedList<CFG> cfgs = new LinkedList<>();

        ApiClassVisitor apiClassVisitor = new ApiClassVisitor();
        compilationUnit.accept(apiClassVisitor);
        for (MethodDeclaration method : apiClassVisitor.getMethods()) {
            //if a method exists, then add it to the list
            if(method != null){
                MethodDeclaration currentDeclaration = method;
                cfgs.add(CFGFactory.startCFGbuild(method));
            }
        }
        return cfgs;
    }

    public LinkedList<CFG> createRelevantCFG(CompilationUnit compilationUnit, LinkedList<MethodInvocation> knownInvocations){

        LinkedList<CFG> cfgs = new LinkedList<>();

        ApiClassVisitor apiClassVisitor = new ApiClassVisitor();
        compilationUnit.accept(apiClassVisitor);
        for (MethodDeclaration method : apiClassVisitor.getMethods()) {
            //if a method exists, then add it to the list
            if(method != null){
                // if method contains a relevant invocation go ahead
                if (method.getBody() != null) {
                    if (FileSearcher.stringContainsInvocation(method.getBody().toString(), knownInvocations)) {
                        MethodDeclaration currentDeclaration = method;
                        cfgs.add(CFGFactory.startCFGbuild(method));
                    }
                }
            }
        }
        return cfgs;
    }

    public LinkedList<CFG> createRelevantCFG(CompilationUnit compilationUnit, String knownInvocation){

        LinkedList<CFG> cfgs = new LinkedList<>();

        ApiClassVisitor apiClassVisitor = new ApiClassVisitor();
        compilationUnit.accept(apiClassVisitor);
        for (MethodDeclaration method : apiClassVisitor.getMethods()) {
            //if a method exists, then add it to the list
            if(method != null){
                // if method contains a relevant invocation go ahead
                if (method.getBody() != null) {
                    if (FileSearcher.stringContainsName(method.getBody().toString(),knownInvocation)) {

                        MethodDeclaration currentDeclaration = method;
                        CFG completeCFG = CFGFactory.startCFGbuild(method);
                        //CFG slicedCFG = CFGFactory.sliceCFG(completeCFG, knownInvocation);
                        cfgs.add(completeCFG);
                    }
                }
            }
        }
        return cfgs;
    }

    public static CFG sliceCFG(CFG cfg, String knownInvocation){

        //get all relevant nodes;
        LinkedList<CFGNode> DataLinkedNodes = new LinkedList<>();
        //for(CFGNode node : cfg.findNodesWithStringMatch(knownInvocation)){
        CFGNode matchedNode = cfg.findNodeWithStringMatch(knownInvocation);
        DataLinkedNodes.addAll(cfg.getDataFeederNodes(matchedNode));
        //}

        //remove unnecessary nodes
        for (CFGNode node : cfg.getAllNodes()) {
            String test = node.getAstNode().toString();
            if (!DataLinkedNodes.contains(node)
                    && (node.getNodeHash() != matchedNode.getNodeHash())
                    && !(node.getAstNode().toString().equalsIgnoreCase("METHOD_ENTRY"))
                    && !(node.getAstNode().toString().equalsIgnoreCase("METHOD_EXIT"))) {
                node.removeAllEdges();
            }
        }

        return cfg;
    }



    private static CFG startCFGbuild(MethodDeclaration method){
        CFGNode startMethod = new CFGNode(method.getAST().newName("METHOD_ENTRY"));
        CFG cfg = new CFG(startMethod);

        CFG cfgInternals = CFGFactory.buildCFG(method);

        // if we don't find any internals to a method we just create a dead node
        if(cfgInternals == null){
            CFGNode noEntry = new CFGNode(method.getAST().newName("ENTRY_To_Nothing"));
            CFGNode noExit = new CFGNode(method.getAST().newName("EXIT_from_nothing"));
            cfgInternals = new CFG(noEntry);
            cfgInternals.addExitNode(noExit);
        }

        //this adds a link to the start if no other data link has been found internally FIXME to have if statement to not add this link if data linked
        startMethod.addEdgeOut(cfgInternals.getStartNode());

        //adds an exit node for any internals that might have exits in case of multiple return statements
        CFGNode noExit = new CFGNode(method.getAST().newName("METHOD_EXIT"));
        for(CFGNode exitNode : cfgInternals.getExitNodes()){
            exitNode.addEdgeOut(noExit);
        }
        cfg.addExitNode(noExit);

        return cfg;
    }


    private static CFG buildCFG(ASTNode node){

        if(node == null){
            return null;
        }
        else{

            switch (node.getNodeType()){
                // CASE 31 is METHOD_DECLARATION
                case 31:
                    ASTNode nodeBody = ((MethodDeclaration)node).getBody();
                    return buildCFG(nodeBody);
                // CASE 8 is BLOCK
                case 8:
                    Block blockTypeNode = (Block)node;
                    return CFGFactory.buildCFG(blockTypeNode);
                // CASE 2 is Expression Statements
                // Add more states as needed
                // https://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fdom%2FExpression.html
                default:
                    CFGNode expressionNode = new CFGNode(node);
                    CFG cfg = new CFG(expressionNode);
                    cfg.addExitNode(expressionNode);
                    return cfg;
            }
        }
    }

    private static CFG buildCFG(Statement statementTypeNode){
        CFGNode expressionNode = new CFGNode(statementTypeNode);
        CFG cfg = new CFG(expressionNode);
        return cfg;
    }

    //This is mostly used recursively to build a subCFG CFG inside another CFG block
    private static CFG buildCFG(Block blockTypeNode){
        CFG cfg = null;

        for(Statement statement : (List<Statement>)blockTypeNode.statements()) {

            // handle nodes like if, while, for
            if (hasInternalBlock(statement)){
                cfg = handleInternalNodes(cfg, statement);
            }

            //handle regular nodes
            else {
                cfg = handleRegularNodes(cfg, statement);
            }
        }
        if (cfg != null) {
            tieUnattachedNodesToExit(cfg);
        }

        return cfg;
    }

    private static CFG buildCFG(List<Statement> statementsList){
        CFG cfg = null;

        for(Statement statement : statementsList) {

            // handle nodes like if, while, for
            if (hasInternalBlock(statement)){
                cfg = handleInternalNodes(cfg, statement);
            }

            //handle regular nodes
            else {
                cfg = handleRegularNodes(cfg, statement);
            }
        }
        if (cfg != null) {
            tieUnattachedNodesToExit(cfg);
        }

        return cfg;
    }

    private static CFG handleRegularNodes(CFG cfg, Statement statement) {

        if (cfg != null) {
            handleRegularNodeLinks(cfg, statement);
        }
        // if the cfg does not yet exist, then it will start with this statement as the start node.
        else{
            cfg = CFGFactory.buildCFG(statement);
        }
        return cfg;
    }

    private static void handleRegularNodeLinks(CFG cfg, Statement statement) {
        LinkedList<CFGNode> dataLinks = getDataLink(statement, cfg);
        if (dataLinks.size() > 0) {
            CFGNode statementNode = new CFGNode(statement);

            for (CFGNode dataLinkedNode : dataLinks) {
                statementNode.addEdgeIn(dataLinkedNode);
            }
        }
        // if the regular node happens to have no data links we just add it to the step after the start of the cfg
        else {
            CFG unlinkedCFG = CFGFactory.buildCFG(statement);

            if (unlinkedCFG != null) {
                cfg.getStartNode().addEdgeOut(unlinkedCFG.getStartNode());
            }
        }
    }

    private static CFG handleInternalNodes(CFG cfg, Statement statement) {
        CFG internalBlock = castToNodeType(statement);
        CFGNode internalStart = internalBlock.getStartNode();

        if (cfg != null){
            LinkedList<CFGNode> dataLinks = getDataLink((Statement)internalStart.getAstNode(), cfg);
            if (dataLinks.size() > 0){
                for (CFGNode dataLinkedNode: dataLinks){
                    internalStart.addEdgeIn(dataLinkedNode);
                }
            }
            else {
                cfg.getStartNode().addEdgeOut(internalStart);
            }
        }
        else {
            cfg = internalBlock;
        }
        return cfg;
    }

    private static CFG castToNodeType(ASTNode node){

        switch (node.getNodeType()){
            // FOR
            case 24:
                ForStatement castForNode = ((ForStatement)node);
                CFGNode startMethod = new CFGNode(castForNode);
                CFG cfg = new CFG(startMethod);
                try {
                    CFG internalBlockCFG = buildCFG((Block) castForNode.getBody());
                    startMethod.addEdgeOut(internalBlockCFG.getStartNode());
                }
                catch (ClassCastException e){
                    startMethod.addEdgeOut(buildCFG((Statement) castForNode).getStartNode());
                }
                return cfg;
            // IF
            case 25:
                IfStatement castIfNode = ((IfStatement)node);
                startMethod = new CFGNode(castIfNode);
                cfg = new CFG(startMethod);

                try {
                    CFG internalBlockCFG = buildCFG((Block) castIfNode.getThenStatement());
                    startMethod.addEdgeOut(internalBlockCFG.getStartNode());
                }
                catch (ClassCastException e){
                    startMethod.addEdgeOut(buildCFG((Statement) castIfNode).getStartNode());
                }
                catch (NullPointerException e){
                    System.out.println("Could not cast if node to block, node was Null, ignoring node");
                }

                Statement elseStatement = castIfNode.getElseStatement();

                if (elseStatement != null) {
                    if (elseStatement.getNodeType() == 25){
                        startMethod.addEdgeOut(castToNodeType(elseStatement).getStartNode());
                    }
                    else {
                        try {
                            CFG internalBlockCFG2 = buildCFG((Block) elseStatement);
                            startMethod.addEdgeOut(internalBlockCFG2.getStartNode());
                        }
                        catch (ClassCastException e){
                            startMethod.addEdgeOut(buildCFG((Statement) elseStatement).getStartNode());
                        }
                    }
                }
                return cfg;
            // WHILE
            case 61:
                WhileStatement castWhileNode = ((WhileStatement)node);
                startMethod = new CFGNode(castWhileNode);
                cfg = new CFG(startMethod);

                try {
                    CFG internalBlockCFG = buildCFG((Block) castWhileNode.getBody());
                    startMethod.addEdgeOut(internalBlockCFG.getStartNode());
                }
                catch (ClassCastException e){
                    startMethod.addEdgeOut(buildCFG((Statement) castWhileNode).getStartNode());
                }
                return cfg;
            // Switch
            case 50:
                SwitchStatement castSwitch = ((SwitchStatement)node);
                startMethod = new CFGNode(castSwitch);
                cfg = new CFG(startMethod);

                    CFG internalStatementsCFG = buildCFG(castSwitch.statements());
                    startMethod.addEdgeOut(internalStatementsCFG.getStartNode());
                return cfg;
            //switchCase
            case 49:
            SwitchCase castSwitchCase = ((SwitchCase)node);
            startMethod = new CFGNode(castSwitchCase);
            cfg = new CFG(startMethod);

            startMethod.addEdgeOut(buildCFG((Statement) castSwitchCase).getStartNode());
            return cfg;
            // ENHANCED FOR
            // ENHANCED FOR

            case 70:
                EnhancedForStatement castEnhForNode = ((EnhancedForStatement)node);
                startMethod = new CFGNode(castEnhForNode);
                cfg = new CFG(startMethod);
                try {
                    CFG internalBlockCFG = buildCFG((Block) castEnhForNode.getBody());
                    startMethod.addEdgeOut(internalBlockCFG.getStartNode());
                }
                catch (ClassCastException e){
                    startMethod.addEdgeOut(buildCFG((Statement) castEnhForNode).getStartNode());
                }
                return cfg;
            default:
                return null;
        }
    }

    private static boolean hasInternalBlock(ASTNode statement){
        int nodeType = statement.getNodeType();

        return nodeType == 24 || nodeType == 25 || nodeType == 61 || nodeType == 70 || nodeType == 50;
    }

    private static void tieUnattachedNodesToExit(CFG cfg){
        LinkedList<CFGNode> nodes = cfg.getAllNodes();

        for (CFGNode node : nodes){
            if (node.getEdgesOut().size() == 0){
                cfg.addExitNode(node);
            }
        }
    }

    private static LinkedList<CFGNode> getDataLink(Statement statement, CFG cfg){
        HashMap<SimpleName, CFGNode> dataLinkedNodes = new HashMap<>();

        LinkedList<CFGNode> nodes = cfg.getAllNodes();

        SimpleNameVisitor statementNameVisitor = new SimpleNameVisitor();
        statement.accept(statementNameVisitor);

        List<SimpleName> statementNames = statementNameVisitor.getSimpleNames();

        for(CFGNode node : nodes){
            SimpleNameVisitor nodeNameVisitor = new SimpleNameVisitor();
            node.getAstNode().accept(nodeNameVisitor);

            for (SimpleName statementName : statementNames){

                for (SimpleName nodeName : nodeNameVisitor.getSimpleNames()){
                    if (statementName.getFullyQualifiedName().equalsIgnoreCase(nodeName.getFullyQualifiedName())){
                        updateDataLinks(dataLinkedNodes, node, statementName);
                    }
                }
            }
        }
        return extractNodesFromMap(dataLinkedNodes);
    }

    private static void updateDataLinks(HashMap<SimpleName, CFGNode> list, CFGNode node, SimpleName name){

        list.put(name, node);

    }

    private static LinkedList<CFGNode> extractNodesFromMap(HashMap<SimpleName, CFGNode> map){
        LinkedList<CFGNode> extractedNodes = new LinkedList<>();

        for (SimpleName key: map.keySet()){
            extractedNodes.add(map.get(key));
        }
        return extractedNodes;
    }
}
