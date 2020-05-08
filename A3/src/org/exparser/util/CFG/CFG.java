package org.exparser.util.CFG;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.exparser.util.databaseplugin.handlers.BlockVisitor;
import org.exparser.util.scrapeVisitors.MethodInvocationVisitor;

import java.util.LinkedList;
import java.util.List;

public class CFG {

    private CFGNode startNode;
    private List<CFGNode> exitNodes;

    public CFG(CFGNode startNode){
        this.startNode = startNode;
        this.exitNodes = new LinkedList<CFGNode>();
    }


    public CFGNode getStartNode() {
        return startNode;
    }

    public List<CFGNode> getExitNodes() {
        return exitNodes;
    }

    public void addExitNode(CFGNode exitNode){
        this.exitNodes.add(exitNode);
    }

    public void addExitNodes(List<CFGNode> exitNodes) {
        this.exitNodes.addAll(exitNodes);
    }

    public CFGNode getSimilarNode(CFGNode node){
        CFGNode currentMostSimilarNode = null;
        float previousSimilarity = 1;

        LinkedList<CFGNode> nodesToCheck = this.getAllNodes();

        ASTMatcher matcher = new ASTMatcher();

        for (CFGNode nodeToCheck : nodesToCheck){

            if (nodeToCheck.getAstNode().subtreeMatch(matcher,node.getAstNode())){
                return nodeToCheck;
            }
            else{
                float similarity = nodeSimilarity(nodeToCheck, node);

                if (similarity > previousSimilarity){
                    previousSimilarity = similarity;
                    currentMostSimilarNode = nodeToCheck;
                }
            }
        }
        return currentMostSimilarNode;
    }

    private boolean nodeTypesAreComparable(CFGNode node1, CFGNode node2){
        boolean acceptableNodeTypes = false;

        if (node1.getAstNode().getNodeType() == node2.getAstNode().getNodeType()) {
            acceptableNodeTypes =true;
        }
        else {
            if (nodeTypesMatchInsideBlocks(node1.getAstNode(), node2.getAstNode()) || nodeTypesMatchInsideBlocks(node2.getAstNode(), node1.getAstNode())){
                acceptableNodeTypes = true;
            }
        }
        return acceptableNodeTypes;
    }

    private boolean nodeTypesMatchInsideBlocks(ASTNode node1, ASTNode node2){
        BlockVisitor blockVisitor = new BlockVisitor();
        node1.accept(blockVisitor);

        // look to see if the first node contains blocks of statements that might match our 2nd node
        for (Block testBlock: blockVisitor.getBlocks()){
            for (Object statement : testBlock.statements()){
                if (((Statement) statement).getNodeType() == node2.getNodeType()){
                    return true;
                }
            }
        }
        return false;
    }

    private float nodeSimilarity(CFGNode node1, CFGNode node2){
        float nodeSimilarity;

        //look at the nodes themselves single node nothing around.
        LinkedList<CFGNode> fromNodes1 = node1.getNode();
        LinkedList<CFGNode> fromNodes2 = node2.getNode();

        if (nodeTypesAreComparable(node1, node2)) {
            nodeSimilarity = linkSimilarity(fromNodes1, fromNodes2);
        }
        else {
            nodeSimilarity = 0;
        }

        if(nodeSimilarity == 100 || nodeSimilarity == 0){
            return nodeSimilarity;
        }
        else{
            //look at the similarity of the node after
            LinkedList<CFGNode> childNodes1 = node1.getDecendentNodes();
            LinkedList<CFGNode> childNodes2 = node2.getDecendentNodes();

            float childSimilarity = linkSimilarity(childNodes1, childNodes2);

            //look at nodeSimilarity of nodes before
            LinkedList<CFGNode> parentNodes1 = node1.getAncestorNodes();
            LinkedList<CFGNode> parentNodes2 = node2.getAncestorNodes();

            float parentSimilarity = linkSimilarity(parentNodes1, parentNodes2);
            // here we give the node similarity a score weighted towards node, but taking neighbor nodes as useful
            float averageSimilarity = (nodeSimilarity*2 + childSimilarity + parentSimilarity) /4;

            return averageSimilarity;
        }
    }

    private float linkSimilarity(LinkedList<CFGNode> linkNodes1, LinkedList<CFGNode> linkNodes2) {
        int totalSimilarity = 0;
        for (CFGNode node : linkNodes1){
            int previousSimilarity = 0;
            for (CFGNode Node2: linkNodes2){
                int currentSimilarity = node.nodeSimilarity(Node2);
                if (currentSimilarity > previousSimilarity){
                    previousSimilarity = currentSimilarity;
                }
            }
            totalSimilarity += previousSimilarity/linkNodes1.size();
        }
        return totalSimilarity;
    }

    public LinkedList<CFGNode> getAllNodes(){
        LinkedList<CFGNode> allNodes = new LinkedList<>();

        allNodes.add(startNode);
        allNodes.addAll(startNode.getDecendentNodes());

        return allNodes;
    }

    public LinkedList<CFGNode> getDataLinkedNodes(CFGNode node){
        LinkedList<CFGNode> nodesFound = new LinkedList<>();

        for (CFGNode testNodes : node.getDecendentNodes()){
            if (node.thisNodeGivesDataTo(testNodes)){
                nodesFound.add(testNodes);
            }
        }

        for (CFGNode testNodes : node.getAncestorNodes()){
            if (node.thisNodeTakesDataFrom(testNodes)){
                nodesFound.add(testNodes);
            }
        }

        return removeDuplicate(nodesFound);
    }

    public LinkedList<CFGNode> getDataFeederNodes(CFGNode node){
        LinkedList<CFGNode> nodesFound = new LinkedList<>();

        for (CFGNode testNodes : node.getAncestorNodes()){
            if (node.thisNodeTakesDataFrom(testNodes)){
                nodesFound.add(testNodes);
            }
        }

        return removeDuplicate(nodesFound);
    }

    public static LinkedList removeDuplicate(LinkedList linkedList){
        LinkedList cleanList = new LinkedList<>();


        while (linkedList.size() > 0){
            Object temp = linkedList.pop();
            if (!cleanList.contains(temp)){
                cleanList.add(temp);
            }
        }
        return cleanList;
    }

    public CFGNode findRelevantNode(ASTNode astNode){
        for(CFGNode node : this.getAllNodes()){
            if (node.getAstNode().toString().contains(astNode.toString()))
                return node;
        }
        return null;
    }

    public CFGNode findExactSimpleNodeMatch(CFGNode exactNode){
        for (CFGNode node: this.getAllNodes()){
            if (exactNode.invocationsMatch(node) && exactNode.simpleNamesMatch(node, false) == 100){
                return node;
            }
        }
        return null;
    }

    public CFGNode findExactSimpleNodeMatchMigration(CFGNode exactNode){
        for (CFGNode node: this.getAllNodes()){
            if (exactNode.invocationsMatch(node) && exactNode.simpleNamesMatch(node, true) == 100){
                return node;
            }
        }
        return null;
    }

    public CFGNode findCloseSimpleNodeMatchMigration(CFGNode exactNode){
        for (CFGNode node: this.getAllNodes()){
            if (exactNode.invocationsMatch(node) && exactNode.simpleNamesMatch(node, true) == 100){
                return node;
            }
        }
        return null;
    }

    public CFGNode findNodeWithStringMatch(String relevantString){
        for (CFGNode node: this.getAllNodes()){
            if (node.getAstNode().toString().contains(relevantString)){
                return node;
            }
        }
        return null;
    }

    public boolean containsExactSimpleMatch(CFGNode exactNode){
        boolean containsMatch = false;

        if (findExactSimpleNodeMatch(exactNode) != null){
            containsMatch = true;
        }

        return containsMatch;
    }

    public boolean containsCloseSimpleMatch(CFGNode exactNode){
        boolean containsMatch = false;

        if (findExactSimpleNodeMatchMigration(exactNode) != null){
            containsMatch = true;
        }

        return containsMatch;
    }

    public LinkedList<CFGNode> getNodesWithMethodInvocations(){
        LinkedList<CFGNode> invocationNodes = new LinkedList<>();

        for (CFGNode node : this.getAllNodes()){
            MethodInvocationVisitor methodInvocationVisitor = new MethodInvocationVisitor();
            node.getAstNode().accept(methodInvocationVisitor);
            if (methodInvocationVisitor.getAPImethodInvocations().size() > 0){
                invocationNodes.add(node);
            }
        }
        return invocationNodes;
    }
}
