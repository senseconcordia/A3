package org.exparser.util.CFG;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.abs;

public class CFGComparer {

    private LinkedList<CFG> originalCFGs;
    private LinkedList<CFG> modifiedCFGs;
    private LinkedList similarCFGs;

    public CFGComparer(LinkedList<CFG> cfgList, LinkedList<CFG> modifiedCFGs){
        this.originalCFGs = (LinkedList) cfgList.clone();
        this.modifiedCFGs = (LinkedList) modifiedCFGs.clone();
        similarCFGs = new LinkedList();
    }

    public CFGComparer(LinkedList<CFG> cfgList){
        this.originalCFGs = (LinkedList) cfgList.clone();
        similarCFGs = new LinkedList();
    }

    public HashMap<CFG, HashMap> compareCFGs(){
        HashMap<CFG, HashMap> seedMapping = new HashMap<>();
        CFG currentMatch = null;

        while(originalCFGs.size() > 0){
            int currentSizeDiff = 0;
            CFG validatingCFG = originalCFGs.pop();

            for(CFG cfgToCompare : modifiedCFGs){

                HashMap newestMatch = bestMatchCompare(validatingCFG, cfgToCompare);
                if (seedMapping.containsKey(validatingCFG)) {
                    int currentMatchSize = seedMapping.get(validatingCFG).size();
                    if (isPerfectMatch(validatingCFG, cfgToCompare)){
                        seedMapping.put(validatingCFG, newestMatch);
                        break;
                    }
                    else if (newestMatch.size() > currentMatchSize  ) {
                        seedMapping.put(validatingCFG, newestMatch);
                        currentMatch = cfgToCompare;
                        currentSizeDiff = CFGsizeDifference(validatingCFG, cfgToCompare);
                    }
                    else if(newestMatch.size() == currentMatchSize && (CFGsizeDifference(cfgToCompare, validatingCFG) <= currentSizeDiff)){
                        if (findClosestMatchByNodes(validatingCFG, cfgToCompare, currentMatch).equals(cfgToCompare)) {
                            seedMapping.put(validatingCFG, newestMatch);
                            currentMatch = cfgToCompare;
                            currentSizeDiff = CFGsizeDifference(validatingCFG, cfgToCompare);
                        }
                    }
                }
                else {
                    seedMapping.put(validatingCFG, newestMatch);
                    currentMatch = cfgToCompare;
                    currentSizeDiff = CFGsizeDifference(validatingCFG, cfgToCompare);
                }
            }
        }
        return seedMapping;
    }

    public static HashMap<CFG, HashMap> cleanSeedMap(LinkedList<CFG> cleanCFGs, HashMap<CFG, HashMap> seedMap){
        HashMap<CFG, HashMap> cleanSeedMap = new HashMap<>();

        for (CFG cleanCFG : cleanCFGs) {
            LinkedList<CFGNode> allowedNodes = cleanCFG.getAllNodes();
            HashMap<CFGNode,CFGNode> checkedNodes = new HashMap<>();


            HashMap<CFGNode,CFGNode> seedMapKeys = seedMap.get(cleanCFG);
            for (CFGNode node : seedMapKeys.keySet()) {

                if (allowedNodes.contains(node)) {
                    checkedNodes.put(node, seedMapKeys.get(node));
                }
            }
            cleanSeedMap.put(cleanCFG, checkedNodes);
        }
        return cleanSeedMap;
    }

    private int CFGsizeDifference(CFG cfg1, CFG cfg2){
        int cfg1Size = cfg1.getAllNodes().size();
        int cfg2Size = cfg2.getAllNodes().size();

        return abs(cfg1Size-cfg2Size);
    }

    public HashMap compareCFGsTo(LinkedList<CFG> cfgsToCompare, boolean forMigration){
        HashMap bestMatches = new HashMap();
        while (originalCFGs.size() > 0){
            CFG validatingCFG = originalCFGs.pop();

            for (CFG cfgTocompare : cfgsToCompare){
                if (forMigration){
                    if (isCloseMatch(validatingCFG, cfgTocompare)){
                        bestMatches.put(validatingCFG, cfgsToCompare);
                    }
                }
                else{
                    if (isPerfectMatch(validatingCFG, cfgTocompare)){
                        bestMatches.put(validatingCFG, cfgsToCompare);
                    }
                }
            }
        }
        return bestMatches;
    }

    private HashMap bestMatchCompare(CFG cfg1, CFG cfg2){
        HashMap bestMatches = new HashMap();

        LinkedList<CFGNode> cfg1Nodes = cfg1.getAllNodes();

        //start at the beginning of the cfg and go down trying to find matches, storing the differences
        for(CFGNode nodeToCheck: cfg1Nodes){

            CFGNode mostSimilarNode = cfg2.getSimilarNode(nodeToCheck);
            if (mostSimilarNode != null) {
                bestMatches.put(nodeToCheck, mostSimilarNode);
            }
        }

        return bestMatches;
    }

    private static boolean isPerfectMatch(CFG originalCfg,CFG testCfg){
        boolean isPerfectMatch = false;

        // We start looking from the method invocations since that is what interests us
        LinkedList<CFGNode> invocationNodes = originalCfg.getNodesWithMethodInvocations();

        for (CFGNode originalNode : invocationNodes){
            if (exactNodeExists(originalNode, originalCfg, testCfg)){
                isPerfectMatch = true;
            }
            else {
                isPerfectMatch = false;
                break;
            }
        }

        return isPerfectMatch;
    }

    private static boolean isCloseMatch(CFG originalCfg,CFG testCfg){
        boolean isPerfectMatch = false;

        // We start looking from the method invocations since that is what interests us
        LinkedList<CFGNode> invocationNodes = originalCfg.getNodesWithMethodInvocations();

        for (CFGNode originalNode : invocationNodes){
            if (closeNodeExists(originalNode, originalCfg, testCfg)){
                isPerfectMatch = true;
            }
            else {
                isPerfectMatch = false;
                break;
            }
        }

        return isPerfectMatch;
    }

    private static CFG findClosestMatchByNodes(CFG original, CFG testCfg1, CFG testCfg2){
        int score;
        int test1Tally = 0;
        int test2Tally = 0;

        LinkedList<CFGNode> searchNodes = original.getAllNodes();
        searchNodes = trimStartEnd(searchNodes, original);

        for (CFGNode originalNode : searchNodes){
            score = 0;
            for (CFGNode testNode: testCfg1.getAllNodes()){
                int matchScore = originalNode.nodeSimilarity(testNode);
                if (matchScore > score){
                    score = matchScore;
                }
            }
            test1Tally += score;

            score = 0;
            for (CFGNode testNode: testCfg2.getAllNodes()){
                int matchScore = originalNode.nodeSimilarity(testNode);
                if (matchScore > score){
                    score = matchScore;
                }
            }
            test2Tally += score;
        }

        if (test1Tally > test2Tally){
            return testCfg1;
        }
        return testCfg2;

    }

    private static boolean exactNodeExists(CFGNode originalNode, CFG originalCFG, CFG testCfg){
        if (testCfg.containsExactSimpleMatch(originalNode)){

            LinkedList<CFGNode> searchNodes = originalCFG.getDataLinkedNodes(originalNode);
            searchNodes = trimStartEnd(searchNodes, originalCFG);

            for (CFGNode node : searchNodes){
                if (!testCfg.containsExactSimpleMatch(node)){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean closeNodeExists(CFGNode originalNode, CFG originalCFG, CFG testCfg){
        if (testCfg.containsCloseSimpleMatch(originalNode)){

            LinkedList<CFGNode> searchNodes = originalCFG.getDataLinkedNodes(originalNode);
            searchNodes = trimStartEnd(searchNodes, originalCFG);

            for (CFGNode node : searchNodes){
                if (!testCfg.containsCloseSimpleMatch(node)){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static LinkedList<CFGNode> trimStartEnd(LinkedList<CFGNode> nodeList, CFG cfg){
        nodeList.remove(cfg.getStartNode());
        List<CFGNode> exitNodes = cfg.getExitNodes();

        for (CFGNode node : exitNodes){
            nodeList.remove(node);
        }
        return nodeList;
    }

}
