package org.exparser.util.CFG;

import org.eclipse.jdt.core.dom.*;
import org.exparser.util.databaseplugin.handlers.SimpleNameVisitor;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class CFGMapper {

    private HashMap<CFG, HashMap> mappingCFGS;

    public CFGMapper(HashMap<CFG, HashMap> mostSimilarCFG){
        this.mappingCFGS = mostSimilarCFG;
    }

    public void createMapping(HashMap<File, HashMap> filesAndMatches){

       for (Object file : filesAndMatches.keySet()){
           String fileName = file.toString();
           System.out.println("In file: " + fileName + " we recommend:");
           migrateFileCFGs(filesAndMatches.get(file), fileName);
       }
    }

    private void migrateFileCFGs(HashMap<CFG, LinkedList<CFG>> fileCFGs, String fileName){
        for (CFG seedCFG : fileCFGs.keySet()){
            HashMap<CFGNode, CFGNode> migrationMap = mappingCFGS.get(seedCFG);
            LinkedList<CFGNode> changedInvocations = getChangedInvocations(migrationMap);

            for (CFGNode changedInvocation : changedInvocations) {
                LinkedList<CFGNode> migrateableNodes = getMigrateableDataLinkedNodes(changedInvocation, seedCFG, changedInvocations);
                LinkedList<CFGNode> missingNodes = getNodesToAdd(migrationMap.get(changedInvocation), migrationMap);
                for (CFG cfg : fileCFGs.get(seedCFG)) {
                    addNewNodes(cfg, missingNodes, migrationMap);
                    migrateCfg(cfg, migrateableNodes, migrationMap);
                }
            }
        }
        printCompilationUnit(fileCFGs.values().iterator().next(), fileName);
    }

    private void saveCompilationUnitToFile(String compilationUnit, String fileName){
        String random_add = randomAlphaNumeric(6);
        try (PrintWriter out = new PrintWriter(fileName + "_migrated_" + random_add)) {
            out.println(compilationUnit);
        }
        catch (java.io.FileNotFoundException e){
            System.out.print("Could not save output file");
        }
    }

    public static String randomAlphaNumeric(int count) {
        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    private void printCompilationUnit(LinkedList<CFG> fileCFGs, String fileName){
        Object CU = fileCFGs.getFirst().getStartNode().getChildNodeLinks().getFirst().getAstNode().getRoot();

        System.out.println(CU.toString());
        saveCompilationUnitToFile(CU.toString(), fileName);
    }

    private void addNewNodes(CFG cfgToMigrate, LinkedList<CFGNode> nodesToAdd, HashMap<CFGNode, CFGNode> migrationMap){
        for (CFGNode node: nodesToAdd){
            if (!nodeTextExistsInCFG(node, cfgToMigrate)) {
                addBeforeNode(node, cfgToMigrate, findFollowingNode(node, migrationMap));
            }
        }
    }

    private boolean nodeTextExistsInCFG(CFGNode node, CFG cfg){
        String nodeText = node.getAstNode().toString();

        for (CFGNode testNode : cfg.getAllNodes()){
            if (testNode.getAstNode().toString().equals(nodeText)){
                return true;
            }
        }
        return false;
    }

    private void addBeforeNode(CFGNode nodeToAdd, CFG cfgToMigrate, CFGNode followingNode){

        for (CFGNode node : cfgToMigrate.getAllNodes()){
            if (node.nodeMatches(followingNode)){

                addInAST(nodeToAdd, node);
            }
        }
    }

    private void addInAST(CFGNode nodeToAdd, CFGNode referenceNode){

        Block parentBlock = getParentBlock(referenceNode);
        if (parentBlock != null) {
            ASTNode test = ASTNode.copySubtree(referenceNode.getAstNode().getAST(), nodeToAdd.getAstNode());

            int statementLocation = getStatementToReplace(parentBlock, referenceNode.getAstNode());
            if (statementLocation >= 0) {
                parentBlock.statements().add(statementLocation, test);
                CFGNode newNode = new CFGNode(test);
                newNode.insertBeforeNode(referenceNode);
            }
        }
    }

    private void replaceInAST(CFGNode nodeToAdd, CFGNode referenceNode){

        Block parentBlock = getParentBlock(referenceNode);
        if (parentBlock != null) {
            ASTNode test = ASTNode.copySubtree(referenceNode.getAstNode().getAST(), nodeToAdd.getAstNode());

            parentBlock.statements().add(getStatementToReplace(parentBlock, referenceNode.getAstNode()), test);
            parentBlock.statements().remove(referenceNode.getAstNode());

            referenceNode.setAstNode(test);
        }
    }

    private CFGNode findFollowingNode(CFGNode node, HashMap<CFGNode, CFGNode> migrationMap){

        for (CFGNode relatedNode : node.getDecendentNodes()){
            if (migrationMap.containsValue(relatedNode)){
                CFGNode keyNode = getKeyNodeFromValudeNode(relatedNode, migrationMap);
                return keyNode;
            }
        }
        return null;
    }

    private int getStatementToReplace(Block parentBlock, Object parentStatement){

        for (int i=0; i< parentBlock.statements().size(); i++){

            if (parentBlock.statements().get(i).equals(parentStatement)){
                return i;
            }
        }
        return -1;
    }

    private Block getParentBlock(CFGNode node){
        boolean parentIsNotBlock = true;

        ASTNode currentNode = node.getAstNode();
        if (currentNode.getNodeType() ==ASTNode.BLOCK){
            return (Block) currentNode;
        }

        while (parentIsNotBlock){
            if(currentNode.getParent() != null) {
                if (currentNode.getParent().getNodeType() == ASTNode.BLOCK) {
                    parentIsNotBlock = false;
                } else {
                    currentNode = currentNode.getParent();
                }
            }
            else{
                return null;
            }
        }
        return (Block) currentNode.getParent();
    }

    private CFGNode getKeyNodeFromValudeNode(CFGNode node, HashMap<CFGNode, CFGNode> set){
        for (CFGNode testNode : set.keySet()){
            if (set.get(testNode) == node){
                return testNode;
            }
        }
        return null;
    }

    private void migrateCfg(CFG cfgToMigrate, LinkedList<CFGNode> nodesToMigrate, HashMap<CFGNode, CFGNode> migrationMap){
        for (CFGNode nodeToMigrate : nodesToMigrate){
            for (CFGNode cfgNode : cfgToMigrate.getAllNodes()) {
                if (nodeToMigrate.nodeMatches(cfgNode)) {
                    migrateNode(cfgNode, nodeToMigrate, migrationMap);
                }
            }
        }
    }

    private void migrateNode(CFGNode nodeToMigrate, CFGNode seedNode, HashMap<CFGNode, CFGNode> migrationMap){
        CFGNode mappedNode = migrationMap.get(seedNode);

        modifyNode(nodeToMigrate, mappedNode);
    }

    private void modifyNode(CFGNode original, CFGNode migrationNode){
        useOriginalNames(original.getAstNode(), migrationNode.getAstNode());

        replaceInAST(migrationNode, original);
        //original.setAstNode(migrationNode.getAstNode());
    }

    private void useOriginalNames(ASTNode original, ASTNode migrationNode){
        SimpleNameVisitor originalNameVisitor = new SimpleNameVisitor();
        SimpleNameVisitor migrationNamesVisitor = new SimpleNameVisitor();
        original.accept(originalNameVisitor);
        migrationNode.accept(migrationNamesVisitor);

        HashMap<SimpleName, SimpleName> nameMap = getNamesMap(originalNameVisitor.getSimpleNames(), migrationNamesVisitor.getSimpleNames());
        for (SimpleName toChange : nameMap.keySet()){
            toChange.setIdentifier(nameMap.get(toChange).getIdentifier());
        }
    }

    private HashMap<SimpleName, SimpleName> getNamesMap(List<SimpleName> originalNames, List<SimpleName> seedMigrationNames){
        HashMap<SimpleName, SimpleName> namesMap = new HashMap<>();

        for (SimpleName migrationName : seedMigrationNames){

            for (SimpleName originalName : originalNames){
                boolean bindingsAreMatched = false;
                IBinding nameBinding = originalName.resolveBinding();
                ITypeBinding nameType = originalName.resolveTypeBinding();

                IBinding testNameBinding = migrationName.resolveBinding();
                ITypeBinding testNameType = migrationName.resolveTypeBinding();

                if(nameBinding != null && testNameBinding != null){
                    if (nameBinding.isEqualTo(testNameBinding)){
                        bindingsAreMatched = true;
                    }
                }
                if(nameType != null && testNameType != null){
                    if (nameType.isEqualTo(testNameType)){
                        bindingsAreMatched = true;
                    }
                }
                if (bindingsAreMatched) {
                    if (!migrationName.getFullyQualifiedName().equals(originalName.getFullyQualifiedName())) {
                        namesMap.put(migrationName, originalName);
                    }
                }
            }
        }
        return namesMap;
    }

    private LinkedList<CFGNode> getNodesToAdd(CFGNode migratedInvocationNode, HashMap<CFGNode, CFGNode> migrationMap){
        LinkedList<CFGNode> nodesToAdd = new LinkedList<>();

        for (CFGNode node : migratedInvocationNode.getAncestorNodes()){
            if (!migrationMap.containsValue(node)){
                nodesToAdd.add(node);
            }
        }
        // here we reverse the list to add them in the correct order to the AST later, closest node to a migration mode must be added last.
        return reverseLinkedListOrder(nodesToAdd);
    }

    private static LinkedList reverseLinkedListOrder(LinkedList linkedList){
        LinkedList reversedLinkedList = new LinkedList();

        ListIterator iterator = linkedList.listIterator(linkedList.size());
        while (iterator.hasPrevious()){
            reversedLinkedList.add(iterator.previous());
        }

        return reversedLinkedList;
    }

    private LinkedList<CFGNode> getMigrateableDataLinkedNodes(CFGNode originalNode, CFG seedCFG, LinkedList<CFGNode> futureMigrations){
        LinkedList<CFGNode> migrateableNodes = new LinkedList<>();
        HashMap<CFGNode, CFGNode> migrationMap = mappingCFGS.get(seedCFG);

        for (CFGNode node : seedCFG.getDataLinkedNodes(originalNode)){
            if (migrationMap.containsKey(node) && (node.simpleNamesMatch(migrationMap.get(node), true) != 100 || !node.invocationsMatch(migrationMap.get(node)))){
                if (!futureMigrations.contains(node)) {
                    migrateableNodes.add(node);
                }
            }
        }
        //adding the original node to migrateable nodes for completion, this was already qualified as migrateable in discovery
        migrateableNodes.add(originalNode);

        return migrateableNodes;
    }

    private LinkedList<CFGNode> getChangedInvocations(HashMap<CFGNode, CFGNode> seedMap){
        LinkedList<CFGNode> changedInvocations = new LinkedList<>();

        for (CFGNode node : seedMap.keySet()){
            if (node.hasInvocation() &&
                    (node.simpleNamesMatch(seedMap.get(node), false) != 100 ||
                            !node.invocationsMatch(seedMap.get(node)) ||
                            !node.sameNumberSimpleNames(seedMap.get(node))
                    )
                ){
                changedInvocations.add(node);
            }
        }
        return changedInvocations;
    }
}
