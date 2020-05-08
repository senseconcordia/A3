package org.exparser.util.fileHandler;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.exparser.util.CFG.*;
import org.exparser.util.GitUtilities.GitFileSaver;
import org.exparser.util.GitUtilities.GitSearchCommand;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileHandler {

    private ASTParser parser;
    private List<String> filesList;

    public FileHandler(File rootFolder, CompilationUnitRequest.ProcessType processType) {
        String projectRoot = rootFolder.getPath();
        this.parser = buildAstParser(rootFolder);
        this.filesList = getFilesFromFolder(projectRoot);

        int totalFiles = filesList.size();

        String[] javaFiles = filesList.toArray(new String[0]);

        FileASTRequestor fileASTRequestor = new FileASTRequestor() {
            @Override
            public void acceptAST(String sourceFilePath, CompilationUnit ast) {
                filesList.remove(sourceFilePath);
                printProgress(totalFiles - filesList.size(), totalFiles);

                String test = sourceFilePath;
                CompilationUnitRequest request = new CompilationUnitRequest(ast, processType, test);
                request.processCompilationUnit();
            }
        };
        try {
            this.parser.createASTs(javaFiles, null, new String[0], fileASTRequestor,null);
        } catch (UnsupportedOperationException e){
            System.out.println(e.getMessage());
        }
    }

    public static HashMap compareFiles(File firstFile, File secondFile) throws IOException {

        CFGComparer cfgComparer = new CFGComparer(extractCFGs(firstFile), extractCFGs(secondFile));
        HashMap similarCFGs = cfgComparer.compareCFGs();

        return similarCFGs;
    }

    public static void confirmExamples(File exampleSourceFolder){
        LinkedList<File> unconfirmedExampleFiles = getFilesFromFolder(exampleSourceFolder);

        for (File unconfirmedExampleFile : unconfirmedExampleFiles){
            try {
                CompilationUnit unit = parseAstFromSourceFile(unconfirmedExampleFile);
                CompilationUnitRequest request = new CompilationUnitRequest(unit, CompilationUnitRequest.ProcessType.API_USES, unconfirmedExampleFile.getAbsolutePath());
                request.processCompilationUnit();
            }
            catch (IOException e){
                System.out.println(e.getMessage());
            }
        }

    }

    public static HashMap<File, HashMap> searchExamples(File exampleFile, Path searchDirectory)throws IOException{

            FileSearcher searcher = new FileSearcher(searchDirectory);
            return searcher.searchDirectoryFor(extractCFGs(exampleFile));
    }

    public static void migrateExamples(File firstFile, File secondFile,Path searchDirectory) throws IOException{
        LinkedList<CFG> firstCFGs = extractCFGs(firstFile);
        LinkedList<CFG> secondCFGs = extractCFGs(secondFile);

        CFGComparer cfgComparer = new CFGComparer(firstCFGs, secondCFGs);
        FileSearcher searcher = new FileSearcher(searchDirectory);

        CFGMapper cfgMapper = new CFGMapper(cfgComparer.compareCFGs());
        cfgMapper.createMapping(searcher.searchDirectoryFor(firstCFGs));

    }

    public static void migrateExamples(File firstFile, File secondFile,Path searchDirectory, String targetAPIName) throws IOException{
        LinkedList<CFG> firstCFGs = extractRelevantCFGs(firstFile,targetAPIName);
        LinkedList<CFG> secondCFGs = extractCFGs(secondFile);

        CFGComparer cfgComparer = new CFGComparer(firstCFGs, secondCFGs);
        FileSearcher searcher = new FileSearcher(searchDirectory);

        HashMap<CFG, HashMap> seedMap = cfgComparer.compareCFGs();

        LinkedList<CFG> cleanFirstCFG = new LinkedList<>();
        for (CFG cfg : firstCFGs){
            cleanFirstCFG.add(CFGFactory.sliceCFG(cfg, targetAPIName));
        }


        HashMap<CFG, HashMap> cleanSeedMap = CFGComparer.cleanSeedMap(cleanFirstCFG, seedMap);
        CFGMapper cfgMapper = new CFGMapper(cleanSeedMap);
        cfgMapper.createMapping(searcher.searchDirectoryForCFGnInvocation(cleanFirstCFG,targetAPIName));

    }

    public static void generalMigration(Path filesToMigrate){
        System.out.println("Which method do you want to migrate?");
        Helper.showAvailableMigrationMethods();

        Helper.showAvailableMigrations(filesToMigrate);
    }

    public static LinkedList<CFG> extractCFGs(File file) throws IOException{
        if (file.getAbsolutePath().endsWith(".java")) {

            CompilationUnit originalCompilationUnit = parseAstFromSourceFile(file);
            CompilationUnitRequest newRequest = new CompilationUnitRequest(originalCompilationUnit, CompilationUnitRequest.ProcessType.CFG_MAPPING, file.getAbsolutePath());
            return newRequest.getCFGsForFile();
        }
        else{
            throw new IOException("Something is wrong with the input file");
        }
    }

    private static LinkedList<CFG> extractRelevantCFGs(File file, String apiName) throws IOException{
        if (file.getAbsolutePath().endsWith(".java")) {

            CompilationUnit originalCompilationUnit = parseAstFromSourceFile(file);
            CompilationUnitRequest newRequest = new CompilationUnitRequest(originalCompilationUnit, CompilationUnitRequest.ProcessType.CFG_MAPPING, file.getAbsolutePath());
            return newRequest.getRelevantCFGsForFile(apiName);
        }
        else{
            throw new IOException("Something is wrong with the input file");
        }
    }

    public static List<String> getFilesFromFolder(String folder){
        List<String> projectFiles = new ArrayList<>();

        File dir = new File(folder);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null){
            for(File child: directoryListing){
                if(child.isDirectory()){
                    projectFiles.addAll(getFilesFromFolder(child.getAbsolutePath()));
                }
                else if(child.getAbsolutePath().endsWith(".java")){
                    if (!child.getAbsolutePath().contains("apct-tests") &&
                            !child.getAbsolutePath().contains("legacy-test") &&
                            !child.getAbsolutePath().contains("tests")) {
                        projectFiles.add(child.getAbsolutePath());
                    }
                }
            }
        }

        return projectFiles;

    }

    public static LinkedList<File> getFilesFromFolder(File folder){
        LinkedList<File> projectFiles = new LinkedList<>();

        File[] directoryListing = folder.listFiles();
        if (directoryListing != null){
            for(File child: directoryListing){
                if(child.isDirectory()){
                    projectFiles.addAll(getFilesFromFolder(child));
                }
                else if(child.getAbsolutePath().endsWith(".java")){
                    projectFiles.add(child);
                }
            }
        }

        return projectFiles;

    }

    public static HashSet<File> getSubFolders(File folder){
        HashSet<File> subFolders = new HashSet<>();

        File[] directoryListing = folder.listFiles();
        if (directoryListing != null){
            for (File child : directoryListing){
                if (child.isDirectory()){
                    subFolders.add(child);
                }
            }
        }
        return subFolders;
    }

    private static ASTParser buildAstParser(File srcFolder) {

        ASTParser parser = ASTParser.newParser(AST.JLS9);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);

        Map options = JavaCore.getOptions();
        parser.setCompilerOptions(options);

        String unitName = srcFolder.getName();
        parser.setUnitName(unitName);

        parser.setEnvironment(new String[0], new String[]{srcFolder.getPath()}, null, true);
        return parser;
    }

    public static CompilationUnit parseAstFromString(String srcString){
        ASTParser parser = ASTParser.newParser(AST.JLS9);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(srcString.toCharArray());
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
    }

    public static CompilationUnit parseAstFromSourceFile(File srcFile) throws IOException{

        ASTParser parser = ASTParser.newParser(AST.JLS9);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);

        Map options = JavaCore.getOptions();
        parser.setCompilerOptions(options);

        String unitName = srcFile.getName();
        parser.setUnitName(unitName);

        String[] sources = {srcFile.getParent()};
        //TODO make a config file to set this up
        String[] classpath = {};

        parser.setEnvironment(classpath,sources,new String[]{ "UTF-8"}, true);
        parser.setSource(fileToCharArray(srcFile));

        return (CompilationUnit) parser.createAST(null); // parse
    }

    public static char[] fileToCharArray(File srcFile) throws IOException{
        String source = FileUtils.readFileToString(srcFile);
        return source.toCharArray();
    }


    public static void mineExamples(File examplesPath){
        DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
        LinkedHashMap<Integer, String> apiMethodsToStudy = databaseHandler.getAllAPIMethodNames();

        for (int invocationNumber : apiMethodsToStudy.keySet()){
            String invocationName = apiMethodsToStudy.get(invocationNumber);
            if (!stringInFile("Did "+invocationNumber)) {
                System.out.println(String.format("Looking at examples of %s", invocationName));
                getFilesFromGitRepos(examplesPath, invocationName, invocationNumber);

                writeToDoneFile("Did "+invocationNumber);
            }
        }
    }

    public static void writeToDoneFile(String stringToWrite){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get("done.txt").toFile(), true));
            writer.write(stringToWrite);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            System.out.println("couldn't write to done file, things might be redone if program fails");
        }
    }

    public static boolean stringInFile(String stringToFind){

        String line;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("done.txt"));
            try {
                while((line = reader.readLine()) != null){
                    if (line.contains(stringToFind)){
                        return true;
                    }
                }
            }catch (IOException e){
                System.out.println(e.getMessage());
            }

        }catch (FileNotFoundException e){
            System.out.println(e.getMessage());
        }

        return false;
    }

    private static void getFilesFromGitRepos(File path, String invocationString, int dbMethodId){
        for (File folder : getSubFolders(path)){


            String[] command = {"git", "log", "--reverse", "-S", invocationString, "--source", "--all" , "-p"};
            GitSearchCommand gitSearchCommand = new GitSearchCommand(folder, command);
            commitFileSaver(folder, gitSearchCommand.process(), dbMethodId, ExampleType.PRIMARY_EXAMPLE);
        }
    }

    public enum ExampleType {PRIMARY_EXAMPLE, MIGRATION_EXAMPLE}

    private static void commitFileSaver(File folder, LinkedHashMap<String, LinkedHashSet<String>> foundFiles, int dbMethodId, ExampleType exampleType){
        //TODO put this in a config file somewhere
        String savelocation = "";
        if (exampleType == ExampleType.PRIMARY_EXAMPLE){
            savelocation = "C:\\Users\\maxim\\Documents\\Results\\Summer2018\\ICSE_PROJECT\\exampleFiles";
        }
        else if (exampleType == ExampleType.MIGRATION_EXAMPLE){
            savelocation = "C:\\Users\\maxim\\Documents\\Results\\Summer2018\\ICSE_PROJECT\\migrationExampleFiles";
        }

        //String savelocation = "/home/user/Max_backup/FDroid2018/exampleFiles";

        //FIXME pass an actual invocationString
        String invocationString = "";
        if (!foundFiles.isEmpty()) {
            try {
                GitFileSaver fileSaver = new GitFileSaver(folder, createNewFolder(savelocation, folder.getName()));
                for (String commitId : foundFiles.keySet()) {
                    for (String fileToSave : foundFiles.get(commitId)) {
                        if (exampleType == ExampleType.PRIMARY_EXAMPLE){
                            fileSaver.saveAsPrimaryExample(fileToSave, commitId, dbMethodId, false, invocationString, folder);
                        }
                        else if (exampleType == ExampleType.MIGRATION_EXAMPLE){
                            fileSaver.saveAsMigrationExample(fileToSave, commitId, dbMethodId);
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println(String.format("Could not save Files for repo: %s because of %s", folder.getAbsolutePath(), ex.toString()));
            }
        }
    }

    private static File createNewFolder(String path, String folderName){

        File savePath = Paths.get(path + "\\" + folderName).toFile();
        savePath.mkdir();
        return savePath;
    }

    public static void mineMigrationExamples(){
        DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
        LinkedHashSet<PrimaryExample> originalsToMigrate = databaseHandler.getConfirmedPrimaryExamples();

        for (PrimaryExample primaryExample : originalsToMigrate){

            String invocationString = "\""+ cleanStringForRegex(primaryExample.getInvocationString()) + "\"";
            String timeBoundary = primaryExample.getCommitExtracted() + "..HEAD";
            String fileFound = primaryExample.getGitFilePath();


            String[] command = {"git", "log", "--reverse", "-G", invocationString, timeBoundary, "--source", "--all", "-p", "--", fileFound};
            GitSearchCommand gitSearchCommand = new GitSearchCommand(primaryExample.getProjectPath(), command);
            commitFileSaver(primaryExample.getProjectPath(), gitSearchCommand.process(), primaryExample.getExampleID(), ExampleType.MIGRATION_EXAMPLE);
        }
    }

    private static void printProgress(int currentItemsCompleted, int totalItems){
        int progress = (currentItemsCompleted*100/totalItems);
        int scale = totalItems/100;

        if(scale > 0 && (currentItemsCompleted % scale) == 0){

            System.out.printf("Currently %d%% done \n", progress);
        }
    }

    private static String cleanStringForRegex(String invocationString){

        String newString = invocationString.replaceAll("[^A-Za-z0-9]", ".");
        return newString.replaceAll(".(?!$)", "$0\\\\s*");
    }

}
