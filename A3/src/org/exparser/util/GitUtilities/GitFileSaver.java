package org.exparser.util.GitUtilities;

import org.exparser.util.fileHandler.MigrationExample;
import org.exparser.util.fileHandler.PrimaryExample;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitFileSaver {

    private File repositoryFile;
    private File saveLocation;

    public GitFileSaver(File repositoryFile, File saveLocation){
        this.repositoryFile = repositoryFile;
        this.saveLocation = saveLocation;
    }

    private boolean saveFile(String repoFileName, String commitId, File savePathOnDisk) throws IOException, InterruptedException {
        boolean fileSaved;
        if (!savePathOnDisk.exists()) {

            String[] command = {"git", "show", composeFileAddress(commitId, repoFileName)};

            ProcessBuilder gitFileSaver = new ProcessBuilder(repositoryFile.getAbsolutePath());
            gitFileSaver.directory(repositoryFile.getAbsoluteFile());
            gitFileSaver.command(command);
            gitFileSaver.redirectOutput(ProcessBuilder.Redirect.appendTo(savePathOnDisk));

            Process gitSave = gitFileSaver.start();
            gitSave.waitFor(100, TimeUnit.MILLISECONDS);
            if (gitSave.exitValue() > 0){
                destroyErrorneousFile(savePathOnDisk);
                fileSaved = false;
            } else {
                fileSaved = true;
            }
            gitSave.destroy();
        } else {
            fileSaved = true;
        }
        return fileSaved;
    }

    public void saveAsPrimaryExample(String repoFileName, String commitId, int dbMethodId, boolean confirmed, String invocationString, File projectPath) throws IOException, InterruptedException {
        File saveLocation = composeSaveLocation(commitId, repoFileName);

        if(saveFile(repoFileName, commitId, saveLocation)) {
            PrimaryExample primaryExample = new PrimaryExample(dbMethodId, repoFileName, commitId, saveLocation, projectPath, confirmed, invocationString);
            primaryExample.saveToDb();
        }
    }

    public void saveAsMigrationExample(String repoFileName, String commitId, int dbPrimaryExample) throws IOException, InterruptedException {
        File saveLocation = composeSaveLocation(commitId, repoFileName);

        if(saveFile(repoFileName, commitId, saveLocation)) {
            MigrationExample migrationExample = new MigrationExample(dbPrimaryExample, repoFileName, commitId, saveLocation);
            migrationExample.saveToDb();
        }
    }

    private static String composeFileAddress(String commitId, String repoFileName){
        String composedAddress = String.format("%s:%s", commitId, repoFileName);

        return composedAddress;
    }

    private static void destroyErrorneousFile(File filePath){
        filePath.delete();
    }

    private File composeSaveLocation(String commitId, String repoFileName){
        String identifyingFileName = commitId + "_" + getFileName(repoFileName);

        Path savePath = Paths.get(this.saveLocation.getAbsolutePath() + "\\" + identifyingFileName);

        return savePath.toFile();
    }

    private static String getFileName(String line){
        String pattern = "[^/]+$";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(line);
        if (m.find()){
            return m.group(0);
        }
        else {
            return null;
        }
    }
}
