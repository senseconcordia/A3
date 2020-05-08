package org.exparser.util.fileHandler;

import org.eclipse.jdt.core.dom.*;
import org.exparser.main.java.ApiMethodObject;
import org.exparser.util.CFG.*;
import org.exparser.util.scrapeVisitors.ApiClassVisitor;
import org.exparser.util.scrapeVisitors.ApiUsageVisitor;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;

public class CompilationUnitRequest {

    public enum ProcessType {API_BUILDER, API_USES, API_USAGE, CFG_MAPPING}


    private CompilationUnit unit;
    private ProcessType processType;
    private LinkedList<MethodDeclaration> apiMethods = new LinkedList<>();
    private String sourceFilePath;


    public CompilationUnitRequest(CompilationUnit unit, ProcessType processType, String sourceFilePath){
        this.unit = unit;
        this.processType = processType;
        this.sourceFilePath = sourceFilePath;
    }

    protected void processCompilationUnit(){
        switch (this.processType){
            case API_BUILDER:
                buildApiEntries();
                break;
            case API_USES:
                getMethodInvocations();
                break;
            case API_USAGE:
                getMethodInvocations();
                break;
            case CFG_MAPPING:
                getCFGsForFile();
                break;
        }


    }

    public LinkedList<CFG> getCFGsForFile(){
        CFGFactory cfgFactory = new CFGFactory();
        LinkedList<CFG> cfgList = cfgFactory.createCFG(unit);

        return cfgList;

    }

    public LinkedList<CFG> getRelevantCFGsForFile(LinkedList<MethodInvocation> knownInvocations){
        CFGFactory cfgFactory = new CFGFactory();
        LinkedList<CFG> cfgList = cfgFactory.createRelevantCFG(unit,knownInvocations);

        return cfgList;
    }

    public LinkedList<CFG> getRelevantCFGsForFile(String knownInvocation){
        CFGFactory cfgFactory = new CFGFactory();
        LinkedList<CFG> cfgList = cfgFactory.createRelevantCFG(unit,knownInvocation);

        return cfgList;
    }

    private void getMethodInvocations(){
        ApiUsageVisitor apiUsageVisitor = new ApiUsageVisitor();
        unit.accept(apiUsageVisitor);

        DatabaseHandler databaseHandler= DatabaseHandler.getInstance();

        for (MethodInvocation method : apiUsageVisitor.getMethodsInvoked()) {
            //check if invoked method is part of the API methods in the saved DB
            try {
                ApiMethodObject apiMethodObject = new ApiMethodObject(method);
                 int matchedItem = databaseHandler.contains(apiMethodObject);
                if (matchedItem >= 0){
                    switch (this.processType){
                        case API_USES:
                            confirmMethodUse(method, databaseHandler, matchedItem);
                            break;
                        case API_USAGE:
                            detectUsage(method, databaseHandler, matchedItem);
                            break;
                    }
                }
            } catch (NullPointerException e){
                System.out.printf("Could not get bindings for method %s because of %s after attempting to resolve bindings\n", method.getName() ,e.getMessage());
            }


        }
    }

    private void confirmMethodUse(MethodInvocation method, DatabaseHandler databaseHandler, int matchedItem) throws NullPointerException{

        //FIXME Need to find a way to get a file location for this. this isn't working
        databaseHandler.setPrimaryExampleAsConfirmed( this.sourceFilePath, matchedItem);
        databaseHandler.setInvocationString(method.toString(), this.sourceFilePath, matchedItem);

    }

    private void detectUsage(MethodInvocation methodInvocation, DatabaseHandler databaseHandler, int matchedItem){
        File nullFile = new File("");
        ApiMethodObject apiCall = databaseHandler.getAPICall(matchedItem);

        if (pseudoMatches(apiCall, methodInvocation)){
            PrimaryExample primaryExample = new PrimaryExample(matchedItem,"0","0",Paths.get(this.sourceFilePath).toFile(),nullFile,false,methodInvocation.toString());
            databaseHandler.saveToDb(primaryExample);
        }
    }

    private boolean pseudoMatches(ApiMethodObject originalApiCall, MethodInvocation invocation){
        if (sameNumberParameters(originalApiCall, invocation)){
            for (Object importedPackage : this.unit.imports()){
                if (importedPackage.toString().contains(originalApiCall.getMethodPackage())){
                    return true;
                }
            }
        }

        return false;
    }

    private boolean sameNumberParameters(ApiMethodObject originalApiCall, MethodInvocation invocation){
        int invocationParameters = invocation.arguments().size();

        String[] apiCallParameters = originalApiCall.getParameters().split(";");
        int apiCallParametersCount = apiCallParameters.length;

        if (apiCallParametersCount == 1){
            for (String callParameter : apiCallParameters) {
                if (callParameter.equalsIgnoreCase("None")) {
                    apiCallParametersCount = 0;
                    break;
                }
            }
        }
        return apiCallParametersCount == invocationParameters;
    }

    private void buildApiEntries(){
        ApiClassVisitor apiClassVisitor = new ApiClassVisitor();
        unit.accept(apiClassVisitor);

        for (MethodDeclaration method : apiClassVisitor.getMethods()) {
            //if a method exists, then add it to the list
            if(method != null){
                apiMethods.add(method);
            }
        }

        saveAPIMethods();
    }

    private void saveAPIMethods(){
        DatabaseHandler databaseHandler = DatabaseHandler.getInstance();

        int currentSavedMethodCount = 0;
        //System.out.printf("There are a total of %d methods to save.\n", apiMethods.size());
        for (MethodDeclaration method : apiMethods){

            ApiMethodObject apiMethodObject = new ApiMethodObject(method);

            if (!apiMethodObject.getMethodPackage().toLowerCase().contains("test")) {
                databaseHandler.saveToDb(apiMethodObject);
            }

            //printProgress(currentSavedMethodCount, apiMethods.size());
        }
    }

    public static void printSimilarCFG(LinkedList<HashMap> similarCFGs) {

        for (HashMap item : similarCFGs) {
            for (Object key : item.keySet()) {
                printNodeDifference((CFGNode)key, (CFGNode)item.get(key));
            }
        }
    }

    public static void printNodeDifference(CFGNode node1,CFGNode node2){

            String node1Text = "\t";
            String node2Text = "\t";

            if (node1 != null) {
                node1Text = node1.getAstNode().toString().replace("\n", "");
            }
            if (node2 != null) {
                node2Text =node2.getAstNode().toString().replace("\n", "");
            }


            System.out.println(node1Text + " >>>>>>>>>>> " + node2Text);
    }

}
