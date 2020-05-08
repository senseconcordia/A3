package org.exparser.util.GitUtilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GitSearchCommand {

    private File filePath;
    private String[] command;

    public GitSearchCommand(File filePath, String[] command) {
        this.command = command;
        this.filePath = filePath;
    }

    public LinkedHashMap<String, LinkedHashSet<String>> process() {
        LinkedHashMap<String, LinkedHashSet<String>> commitList = new LinkedHashMap<>();
        String currentCommitNumber = null;
        try {

            //System.out.println("Creating Process");
            ProcessBuilder gitSearchBuilder = new ProcessBuilder(filePath.getAbsolutePath());
            gitSearchBuilder.directory(filePath.getAbsoluteFile());
            gitSearchBuilder.command(command);

            Process gitSearch = gitSearchBuilder.start();

            InputStreamReader inputStreamReader = new InputStreamReader(gitSearch.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
                String commitNumber = getCommitNumber(currentLine);
                if (commitNumber != null) {
                    currentCommitNumber = commitNumber;
                }
                //if we didn't get a commit number from this, then we can look for files
                if (currentCommitNumber != null) {
                    LinkedHashSet<String> files = getFileName(currentLine);
                    if (files.size() > 0) {
                        if (commitList.containsKey(currentCommitNumber)){
                            commitList.get(currentCommitNumber).addAll(files);
                        }
                        else {
                            commitList.put(currentCommitNumber, files);
                        }
                    }
                }
            }
            gitSearch.destroy();
            bufferedReader.close();
            inputStreamReader.close();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        return commitList;
    }

    public static String getCommitNumber(String line){
        String pattern = "commit (?s)(\\S*)";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(line);
        if (m.find()){
            return m.group(1);
        }
        else {
            return null;
        }
    }

    public static LinkedHashSet<String> getFileName(String line){
        String pattern = "diff --git a\\/(\\S*) b\\/(?s)(\\S*\\.java)";
        LinkedHashSet<String> modifiedFiles = new LinkedHashSet<>();

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(line);
        if (m.find()){
            modifiedFiles.add(m.group(1));
            modifiedFiles.add(m.group(2));
        }
        return modifiedFiles;
    }

    public static String functionNameToRegEx(String callString){
        String literalCall = callString
                .replace("(","\\(")
                .replace(")","\\)")
                .replace(".","\\.")
                .replace("?", "\\?")
                .replace("+", "\\+");
        String regEx = "(\\s)*" + literalCall + "(\\s)*";
        return regEx;
    }
}
