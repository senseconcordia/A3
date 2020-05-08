package org.exparser.util.fileHandler;

import org.eclipse.jdt.core.dom.MethodInvocation;
import org.exparser.util.CFG.CFG;
import org.exparser.util.CFG.CFGComparer;
import org.exparser.util.CFG.CFGNode;
import org.exparser.util.scrapeVisitors.MethodInvocationVisitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class FileSearcher {

    private LinkedList<File> searchFiles;


    public FileSearcher(Path searchDirectory){
        Path searchDirectory1 = searchDirectory;
        this.searchFiles = FileHandler.getFilesFromFolder(searchDirectory.toFile());

    }

    public FileSearcher(File searchFile){
        this.searchFiles = new LinkedList<File>();
        this.searchFiles.add(searchFile);
    }


    public  HashMap<File, HashMap> searchDirectoryFor(LinkedList<CFG> cfgs){
        LinkedList<MethodInvocation> knownInvocations = getKnownInvocations(cfgs);

        // Get a preliminary list of files that use the invocations from the CFG, this is done with string matching to be more efficient
        LinkedList<File> preliminaryFilesToMigrate = getFilesWithKnownInvocations(knownInvocations);

        // Now we attempt to match the pattern, this requires building the AST so we want to do as few times as possible.
        HashMap<File, HashMap> filesToMigrate = getFilesWithPatternMatch(preliminaryFilesToMigrate, cfgs, knownInvocations);

        return filesToMigrate;
    }

    public  HashMap<File, HashMap> searchDirectoryForCFGnInvocation(LinkedList<CFG> cfgs, String invocation){
        LinkedList<MethodInvocation> knownInvocations = getKnownInvocations(cfgs);

        LinkedList<MethodInvocation> desiredInvocations = new LinkedList<>();
        for (MethodInvocation invoked : knownInvocations){
            if (invoked.toString().contains(invocation)){
                desiredInvocations.add(invoked);
            }
        }

        // Get a preliminary list of files that use the invocations from the CFG, this is done with string matching to be more efficient
        LinkedList<File> preliminaryFilesToMigrate = getFilesWithKnownInvocations(desiredInvocations);

        // Now we attempt to match the pattern, this requires building the AST so we want to do as few times as possible.
        HashMap<File, HashMap> filesToMigrate = getFilesWithPatternMatch(preliminaryFilesToMigrate, cfgs, desiredInvocations);

        return filesToMigrate;
    }

    private HashMap<File, HashMap> getFilesWithPatternMatch(LinkedList<File> prefilteredFiles, LinkedList<CFG> cfgs, LinkedList<MethodInvocation> knownInvocations){
        HashMap<File, HashMap> filesToMigrate = new HashMap<>();

        for (File file : prefilteredFiles){
            LinkedList<CFG> fileCfgs = parseToCFG(file, knownInvocations);

            CFGComparer cfgComparer = new CFGComparer(cfgs);
            HashMap matches = cfgComparer.compareCFGsTo(fileCfgs, true);
            if (matches.size() > 0){
                filesToMigrate.put(file, matches);
            }
        }
        return filesToMigrate;
    }

    private LinkedList<CFG> parseToCFG(File file, LinkedList<MethodInvocation> knownInvocations){
        LinkedList<CFG> cfgs = new LinkedList<>();
        try {
            CompilationUnitRequest compilationUnitRequest = new CompilationUnitRequest(FileHandler.parseAstFromSourceFile(file), CompilationUnitRequest.ProcessType.CFG_MAPPING, file.getAbsolutePath());
            cfgs.addAll(compilationUnitRequest.getRelevantCFGsForFile(knownInvocations));
        }
        catch (IOException e){
            System.out.println("ERROR: Could not parse AST for: " + file.getAbsolutePath());
        }
        return cfgs;
    }

    private LinkedList<MethodInvocation> getKnownInvocations(LinkedList<CFG> cfgs){
        LinkedList<MethodInvocation> knownInvocations = new LinkedList<>();

        for (CFG cfg : cfgs){
            for (CFGNode node : cfg.getAllNodes()){
                MethodInvocationVisitor methodInvocationVisitor = new MethodInvocationVisitor();
                node.getAstNode().accept(methodInvocationVisitor);
                knownInvocations.addAll(methodInvocationVisitor.getAPImethodInvocations());
            }
        }
        return knownInvocations;
    }


    private LinkedList<File> getFilesWithKnownInvocations(LinkedList<MethodInvocation> knownInvocations){
        LinkedList<File> filesToMigrate = new LinkedList<>();

        for (File file : searchFiles){
            if (fileContainsInvocation(file, knownInvocations)){
                filesToMigrate.add(file);
            }
        }
        return filesToMigrate;
    }

    private boolean fileContainsInvocation(File file, LinkedList<MethodInvocation> knownInvocations){
        try {
            Scanner scan = new Scanner(file);
            while (scan.hasNext()){
                String line = scan.nextLine();
                if (stringContainsInvocation(line, knownInvocations)){
                    return true;
                }
            }
        }
        catch (FileNotFoundException e){
            System.out.println("Could not find file: " + file.getAbsolutePath());
        }
        return false;
    }

    public static boolean stringContainsInvocation(String line, LinkedList<MethodInvocation> knownInvocations){
        for (MethodInvocation methodInvocation : knownInvocations){
            if (line.contains(methodInvocation.getName().getIdentifier())){
                return true;
            }
        }
        return false;
    }

    public static boolean stringContainsName(String line, String name){
            if (line.contains(name)){
                return true;
            }
        return false;
    }
}
