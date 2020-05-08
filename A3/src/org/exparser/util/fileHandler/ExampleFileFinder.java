package org.exparser.util.fileHandler;

import org.exparser.util.CFG.CFG;
import org.exparser.util.CFG.CFGComparer;
import org.exparser.util.CFG.CFGNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class ExampleFileFinder {
    File[] originalExamples;
    File[] migrateExamples;
    HashMap<File, File> similarFiles = new HashMap<>();

    public ExampleFileFinder(Path originalExamplesFolder, Path migratedExamplesFolder){
        this.originalExamples = originalExamplesFolder.toFile().listFiles();
        this.migrateExamples = migratedExamplesFolder.toFile().listFiles();

        try {
            getMatchedExamples();
            printMatchedExamples();
        } catch (IOException e){
            e.getMessage();
        }
    }

    private void getMatchedExamples() throws IOException{
        HashMap<CFG, File> fileCFG = new HashMap<>();
        LinkedList<CFG> cfgs = new LinkedList<>();
        for (File migrateExample : migrateExamples){
            LinkedList<CFG> currentCfg =  FileHandler.extractCFGs(migrateExample);
            cfgs.addAll(currentCfg);
            fileCFG.put(currentCfg.get(0), migrateExample);
        }

        for (File originalExample : originalExamples){
            LinkedList<CFG> originalCFGs = FileHandler.extractCFGs(originalExample);

            CFGComparer comparer = new CFGComparer(originalCFGs, cfgs);
            HashMap<CFG, HashMap> stuff = comparer.compareCFGs();
            System.out.println("File " + originalExample.getAbsolutePath() + "looks like:");

            for (CFG cfg : stuff.keySet()){
                HashMap comparedValues = stuff.get(cfg);
                for (Object node: comparedValues.keySet()){
                    String migrated = ((CFGNode)comparedValues.get(node)).getAstNode().toString().replaceAll("(\\r|\\n)", "");
                    String original = ((CFGNode)node).getAstNode().toString().replaceAll("(\\r|\\n)", "");
                    System.out.println(String.format("Original: %s  === %s :Migrated", original, migrated));
                }
            }

            System.out.println("<==============================================>");
        }
    }

    private void printMatchedExamples(){
        for (File file : similarFiles.keySet()){
            System.out.println(String.format("The following files are similar <%s> & <%s>", file.getAbsolutePath().toString(), similarFiles.get(file).getAbsolutePath().toString()));
        }
    }

}
