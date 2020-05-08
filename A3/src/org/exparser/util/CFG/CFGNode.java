package org.exparser.util.CFG;

import org.eclipse.jdt.core.dom.*;
import org.exparser.util.databaseplugin.handlers.SimpleNameVisitor;
import org.exparser.util.scrapeVisitors.MethodInvocationVisitor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

public class CFGNode {

    private int nodeHash;

    private ASTNode astNode;

    private LinkedList<CFGEdge> edgesOut;

    private LinkedList<CFGEdge> edgesIn;

    public CFGNode(ASTNode node){
        this.astNode = node;
        this.edgesOut = new LinkedList<>();
        this.edgesIn = new LinkedList<>();
        this.nodeHash = createHash();
    }

    private int createHash(){
        String toHash = astNode.toString();
        return toHash.hashCode();
    }

    public void addEdgeOut(CFGNode exitNode){
        CFGEdge edge = new CFGEdge(this, exitNode);

        if(!this.hasEdge(edge)){
            this.edgesOut.add(edge);
            exitNode.addEdgeIn(this);
        }
    }

    public void removeEdgeOut(CFGEdge edgeToRemove){
        LinkedList<CFGEdge> edgesToRemove = new LinkedList<>();

        for (CFGEdge edge: edgesOut){
            if (edge.getHash() == edgeToRemove.getHash()){
                edgesToRemove.add(edge);
            }
        }

        for (CFGEdge edge: edgesToRemove){
            this.edgesOut.remove(edge);
        }
    }

    public void removeEdgeIn(CFGEdge edgeToRemove){
        LinkedList<CFGEdge> edgesToRemove = new LinkedList<>();

        for (CFGEdge edge: edgesIn){
            if (edge.getHash() == edgeToRemove.getHash()){
                edgesToRemove.add(edge);
            }
        }

        for (CFGEdge edge: edgesToRemove){
            this.edgesIn.remove(edge);
        }
    }

    public void addEdgeIn(CFGNode entranceNode){
        CFGEdge edge = new CFGEdge(entranceNode, this);

        if(!this.hasEdge(edge)) {
            this.edgesIn.add(edge);
            entranceNode.addEdgeOut(this);
        }
    }

    private boolean hasEdge(CFGEdge edge){
        CFGNode fromCFGnode = edge.getFromNode();
        CFGNode toCFGnode = edge.getToNode();

        for(CFGEdge edgeIn : edgesIn){
            if (edgeIn.getFromNode().equals(fromCFGnode) && edgeIn.getToNode().equals(toCFGnode)){
                return true;
            }
        }

        for(CFGEdge edgeOut : edgesOut){
            if (edgeOut.getFromNode().equals(fromCFGnode) && edgeOut.getToNode().equals(toCFGnode)){
                return true;
            }
        }
        return false;
    }

    public void removeAllEdges(){
        LinkedList<CFGEdge> edgesInRemove = new LinkedList<>(this.getEdgesIn());
        LinkedList<CFGEdge> edgesOutRemove = new LinkedList<>(this.getEdgesOut());

        LinkedList<CFGNode> priorNodes = this.getParentNodeList();
        LinkedList<CFGNode> afterNodes = this.getChildNodeLinks();



        for (CFGNode priorNode : priorNodes){
            for (CFGNode afterNode: afterNodes){
                priorNode.addEdgeOut(afterNode);
            }
        }

        for (CFGEdge edge: edgesInRemove){
            edge.destroyEdge();
        }

        for (CFGEdge edge: edgesOutRemove){
            edge.destroyEdge();
        }


    }

    public int getNodeHash() {
        return nodeHash;
    }

    public ASTNode getAstNode() {
        return astNode;
    }

    public void setAstNode(ASTNode node){
        this.astNode = node;
    }

    private void removeIncomingNode(CFGNode nodeToRemove){
        HashSet<CFGEdge> edgesToRemove = new HashSet<>();
        for (CFGEdge edge:edgesIn){
            if (edge.getFromNode().equals(nodeToRemove)){
                edgesToRemove.add(edge);
            }
        }
        for (CFGEdge edge : edgesToRemove){
            edge.destroyEdge();
        }
    }

    public void insertBeforeNode(CFGNode followingNode){
        LinkedList<CFGNode> enteringNodes = followingNode.getParentNodeList();
        for (CFGNode previousNode : enteringNodes){
            followingNode.removeIncomingNode(previousNode);
            previousNode.addEdgeOut(this);
        }
        this.addEdgeOut(followingNode);
    }

    public LinkedList<CFGEdge> getEdgesOut() {
        return edgesOut;
    }

    public LinkedList<CFGEdge> getEdgesIn() {
        return edgesIn;
    }

    public LinkedList<CFGNode> getChildNodeLinks(){
        LinkedList<CFGNode> links = new LinkedList<>();

        for(CFGEdge edge : edgesOut){
            links.add(edge.getToNode());
        }
        return links;
    }

    public LinkedList<CFGNode> getNode(){
        LinkedList<CFGNode> links = new LinkedList<>();

        links.add(this);

        return links;
    }

    public LinkedList<CFGNode> getParentNodeList(){
        LinkedList<CFGNode> links = new LinkedList<>();

        for(CFGEdge edge : edgesIn){
            links.add(edge.getFromNode());
        }
        return links;
    }

    public int nodeSimilarity(CFGNode node){
        int points;
        ASTMatcher matcher = new ASTMatcher();

        // If the method is an exact match AST wise, we give 100% match and move on.
        if (this.astNode.subtreeMatch(matcher,node.getAstNode())){
            return 100;
        }
        // Here we try to look at internal method invocations to see if we have some similarity available.
        else{
            List<MethodInvocation> nodeInvocations = getMethodInvocations(node);
            if(nodeInvocations.size() >0){
                try {
                    points = (simpleNamesMatch(node, false));
                    if (points == 0) {
                        points = internalSimilarity(nodeInvocations);
                    }
                }
                catch (NullPointerException e){
                    int LD = Levenshtein.distance(this.astNode.toString(), node.astNode.toString());
                    double score = 100.0 / (0.004*Math.pow(LD,2) + 1);
                    points = ((int) score);
                }
            }
            // if there are no invocations to look at then we don't care., will not be a migration, will be an addition or subtraction.
            else {
                points = 0;
            }
        }
        return points;
    }

    private int internalSimilarity(List<MethodInvocation> nodeInvocations) throws NullPointerException {
        try {
            int similarityPoints = 0;

            List<MethodInvocation> invocationsToCheck = getMethodInvocations(this);

            for (MethodInvocation nodeInvocation : nodeInvocations) {
                int nodeInvocationSimilarity = 0;

                for (MethodInvocation toCheck : invocationsToCheck) {

                    int internalScore = scoreInternals(nodeInvocation, toCheck);
                    if (internalScore > nodeInvocationSimilarity) {
                        nodeInvocationSimilarity = internalScore;
                    }
                }
                similarityPoints += nodeInvocationSimilarity / nodeInvocations.size();
            }
            return similarityPoints;
        }
        catch (NullPointerException e){
            throw e;
        }
    }

    private int scoreInternals(MethodInvocation invocation1, MethodInvocation invocation2) throws NullPointerException{
        int score = 0;
        try {
            //we check four different things, each worth 25 points

            //compare the names of the method call
            if (invocation1.getName().toString().equalsIgnoreCase(invocation2.getName().toString())) {
                score += 25;
            }

            //get the expression types and compare them
            ITypeBinding expression1 = invocation1.getExpression().resolveTypeBinding();
            ITypeBinding expression2 = invocation2.getExpression().resolveTypeBinding();

            if (expression1 != null && expression2 != null) {
                if (expression1.isEqualTo(expression2)) {
                    score += 25;
                }
            }

            List stuff = invocation1.typeArguments();
            List teststuff = invocation2.typeArguments();

            for (Object item : invocation1.arguments()) {
                for (Object item2 : invocation2.arguments()) {
                    if (item.toString().equalsIgnoreCase(item2.toString())) {
                        score += 25 / invocation1.arguments().size();
                    }
                }
            }
        }
        catch (NullPointerException e){
            throw e;
        }

        return score;
    }

    private int occurencesOfThisKeywork(String stringToCountInstances){

        String findStr = "this";
        int lastIndex = 0;
        int count = 0;

        while(lastIndex != -1){

            lastIndex = stringToCountInstances.indexOf(findStr,lastIndex);

            if(lastIndex != -1){
                count ++;
                lastIndex += findStr.length();
            }
        }
        return count;
    }

    public boolean sameNumberSimpleNames(CFGNode testNode){
        SimpleNameVisitor nodeNameVisitor = new SimpleNameVisitor();
        this.astNode.accept(nodeNameVisitor);

        SimpleNameVisitor testNodeNameVisitor = new SimpleNameVisitor();
        testNode.getAstNode().accept(testNodeNameVisitor);

        return nodeNameVisitor.getSimpleNames().size() == testNodeNameVisitor.getSimpleNames().size();
    }


    public int simpleNamesMatch(CFGNode testNode, boolean forMigration){
        int numberOfNameMatches = 0;
        int numberOfNames = 1;
        int couldGetType = 0;
        int matchedType = 0;

        SimpleNameVisitor nodeNameVisitor = new SimpleNameVisitor();
        this.astNode.accept(nodeNameVisitor);

        // store the number of simple names to match
        if (nodeNameVisitor.getSimpleNames().size() > 0) {
            numberOfNames = nodeNameVisitor.getSimpleNames().size();
        }

        //count number of times "this" occurs in string
        int thisOccurences = occurencesOfThisKeywork(testNode.getAstNode().toString());

        SimpleNameVisitor testNodeNameVisitor = new SimpleNameVisitor();
        testNode.getAstNode().accept(testNodeNameVisitor);

        for (SimpleName name : nodeNameVisitor.getSimpleNames()){
            IBinding nameBinding = name.resolveBinding();
            ITypeBinding nameType = name.resolveTypeBinding();
            if (nameBinding != null){
                couldGetType +=1;
            }
            else if(nameType != null){
                couldGetType +=1;
            }

            for (SimpleName testName : testNodeNameVisitor.getSimpleNames()){

                IBinding testNameBinding = testName.resolveBinding();
                ITypeBinding testNameType = testName.resolveTypeBinding();

                if(nameBinding != null && testNameBinding != null){
                    if (nameBinding.isEqualTo(testNameBinding)){
                        numberOfNameMatches +=1;
                        matchedType +=1;
                        break;
                    }
                }
                if(nameType != null && testNameType != null){
                    if (nameType.isEqualTo(testNameType)){
                        numberOfNameMatches +=1;
                        matchedType +=1;
                        break;
                    }
                }
                if (name.getFullyQualifiedName().equalsIgnoreCase(testName.getFullyQualifiedName())){
                    numberOfNameMatches += 1;
                    break;
                }
            }
        }
        //relaxed condition
        boolean matchedTypeWhenWeCould = couldGetType == matchedType ;

        if (forMigration) {
            if (matchedTypeWhenWeCould) {
                return 100;
            } else {
                return 0;
            }
        }
        else{
            return (numberOfNameMatches * 100 / numberOfNames);
        }

    }

    public boolean hasInvocation(){
        if (getMethodInvocations(this).size() > 0){
            return true;
        }else {
            return false;
        }
    }

    public boolean invocationsMatch(CFGNode testNode){
        boolean invocationsMatch = false;
        MethodInvocation currentInvocation;

        List<MethodInvocation> invocations = getMethodInvocations(this);
        List<MethodInvocation> testInvocations = getMethodInvocations(testNode);

        if (invocations.size() == 0 && testInvocations.size() == 0){
            invocationsMatch = true;
        }
        for (MethodInvocation invocation : invocations){

            boolean invocationHasMatch = false;
            for (MethodInvocation testInvocation : testInvocations){
                currentInvocation = testInvocation;

                if (invocation.arguments().size() == testInvocation.arguments().size()){
                    invocationHasMatch = true;
                    testInvocations.remove(currentInvocation);
                    break;
                }
            }
            if (invocationHasMatch){
                invocationsMatch = true;
            }
            else {
                return false;
            }
        }

        return invocationsMatch;
    }

    private List<MethodInvocation> getMethodInvocations(CFGNode node){
        MethodInvocationVisitor methodInvocationVisitor = new MethodInvocationVisitor();
        node.getAstNode().accept(methodInvocationVisitor);

        return methodInvocationVisitor.getAPImethodInvocations();

    }

    public LinkedList<CFGNode> getAncestorNodes(){
        LinkedList<CFGNode> predicateNodes = new LinkedList<>();

        predicateNodes.addAll(getOrderedAncestorNodes());

        return predicateNodes;
    }

    private LinkedHashSet<CFGNode> traverseParentNodes(LinkedList<CFGNode> nodes){
        LinkedHashSet<CFGNode> nodesFound = new LinkedHashSet<>();

        for (CFGNode node : nodes){
            nodesFound.addAll(traverseParentNode(node));
        }
        return nodesFound;
    }

    private LinkedHashSet<CFGNode> traverseParentNode(CFGNode node){
        LinkedHashSet<CFGNode> nodesFound = new LinkedHashSet<>();

        LinkedList<CFGNode> newNodes =node.getParentNodeList();
        if (newNodes.size() > 0) {
            //add nodes in specific order only keep last instance
            for (CFGNode item : newNodes) {
                nodesFound.add(item);
            }
            //recursively check all nodes
            nodesFound.addAll(traverseParentNodes(newNodes));
        }
        return nodesFound;

    }

    public LinkedList<CFGNode> getDecendentNodes(){
        LinkedList<CFGNode> predicateNodes = new LinkedList<>();

        predicateNodes.addAll(getOrderedDecendentNodes());

        return predicateNodes;
    }

    private LinkedHashSet<CFGNode> traverseChildNodes(LinkedList<CFGNode> nodes){
        LinkedHashSet<CFGNode> nodesFound = new LinkedHashSet<>();

        for (CFGNode node : nodes){
            nodesFound.addAll(traverseChildNode(node));
        }
        return nodesFound;
    }

    private LinkedHashSet<CFGNode> getOrderedDecendentNodes(){
        LinkedHashSet<CFGNode> visitedNodes = new LinkedHashSet<>();
        LinkedList<CFGNode> queue = new LinkedList<>();

        queue.add(this);
        visitedNodes.add(this);

        while (queue.size() != 0){
            CFGNode currentNode = queue.pop();

            queue.addAll(currentNode.getChildNodeLinks());
            visitedNodes.add(currentNode);
        }

        visitedNodes.remove(this);
        return visitedNodes;
    }

    private LinkedHashSet<CFGNode> getOrderedAncestorNodes(){
        LinkedHashSet<CFGNode> visitedNodes = new LinkedHashSet<>();
        LinkedList<CFGNode> queue = new LinkedList<>();

        queue.add(this);
        visitedNodes.add(this);

        while (queue.size() != 0){
            CFGNode currentNode = queue.pop();

            queue.addAll(currentNode.getParentNodeList());
            visitedNodes.add(currentNode);
        }

        visitedNodes.remove(this);
        return visitedNodes;
    }

    private LinkedHashSet<CFGNode> traverseChildNode(CFGNode node){
        LinkedHashSet<CFGNode> nodesFound = new LinkedHashSet<>();

        LinkedList<CFGNode> newNodes =node.getChildNodeLinks();
        if (newNodes.size() > 0) {
            //add nodes in specific order only keep last instance
            for (CFGNode item : newNodes) {
                nodesFound.add(item);
            }
            //recursively check all nodes
            nodesFound.addAll(traverseChildNodes(newNodes));
        }
        return nodesFound;
    }

    public boolean nodeMatches(CFGNode testNode){
        boolean nodeMatches = false;

        boolean typeMatch = this.getAstNode().getNodeType() == testNode.getAstNode().getNodeType();

        if (typeMatch && (this.invocationsMatch(testNode) || !(this.hasInvocation() && testNode.hasInvocation())) && (this.simpleNamesMatch(testNode, true) == 100) ){
            nodeMatches = true;
        }
        return nodeMatches;
    }

    public boolean thisNodeGivesDataTo(CFGNode potentialDataUserNode){
        SimpleNameVisitor simpleNameVisitor = new SimpleNameVisitor();
        potentialDataUserNode.getAstNode().accept(simpleNameVisitor);

        SimpleNameVisitor thisNodesSimpleNamesVisitor = new SimpleNameVisitor();
        this.getAstNode().accept(thisNodesSimpleNamesVisitor);

        for(SimpleName name: thisNodesSimpleNamesVisitor.getSimpleNames()){
            if (name.isDeclaration()){
                for (SimpleName testName : simpleNameVisitor.getSimpleNames()){
                    if (testName.getFullyQualifiedName().equalsIgnoreCase(name.getFullyQualifiedName())){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean thisNodeTakesDataFrom(CFGNode potentialDataGiverNode){
        SimpleNameVisitor simpleNameVisitor = new SimpleNameVisitor();
        potentialDataGiverNode.getAstNode().accept(simpleNameVisitor);

        SimpleNameVisitor thisNodesSimpleNamesVisitor = new SimpleNameVisitor();
        this.getAstNode().accept(thisNodesSimpleNamesVisitor);

        for(SimpleName name: simpleNameVisitor.getSimpleNames()){
            if (name.isDeclaration()){
                for (SimpleName testName : thisNodesSimpleNamesVisitor.getSimpleNames()){
                    if (testName.getFullyQualifiedName().equalsIgnoreCase(name.getFullyQualifiedName())){
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
