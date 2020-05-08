package org.exparser.util.fileHandler;

import java.io.File;

public class MigrationExample {

    private File fileOnDisk;
    private String commitExtracted;
    private String gitFilePath;
    private int originalExampleId;

    public MigrationExample(int originalExampleId, String gitFilePath, String commitExtracted, File fileOnDisk){
        this.originalExampleId = originalExampleId;
        this.fileOnDisk = fileOnDisk;
        this.commitExtracted = commitExtracted;
        this.gitFilePath = gitFilePath;
    }

    public File getFileOnDisk() {
        return fileOnDisk;
    }

    public String getCommitExtracted() {
        return commitExtracted;
    }

    public String getGitFilePath() {
        return gitFilePath;
    }

    public int getOriginalExampleId() {
        return originalExampleId;
    }

    public void saveToDb(){
        DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
        databaseHandler.saveToDb(this);
    }
}
