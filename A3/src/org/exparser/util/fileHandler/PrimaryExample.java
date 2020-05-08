package org.exparser.util.fileHandler;

import java.io.File;

public class PrimaryExample {

    private File fileOnDisk;
    private File projectPath;
    private String commitExtracted;
    private String gitFilePath;
    private boolean confirmed;
    private int apiId;
    private String invocationString;
    private int exampleID = 0;

    public PrimaryExample(int apiId, String gitFilePath, String commitExtracted, File fileOnDisk, File projectPath, boolean confirmed, String invocationString){
        this.apiId =apiId;
        this.fileOnDisk = fileOnDisk;
        this.projectPath = projectPath;
        this.commitExtracted = commitExtracted;
        this.gitFilePath = gitFilePath;
        this.confirmed = confirmed;
        this.invocationString = invocationString;
    }

    public File getFileOnDisk() {
        return fileOnDisk;
    }

    public File getProjectPath() { return projectPath; }

    public String getCommitExtracted() {
        return commitExtracted;
    }

    public String getGitFilePath() {
        return gitFilePath;
    }

    public int getApiId() {
        return apiId;
    }

    public int getConfirmed(){
        if (confirmed){
            return 1;
        }
        else {
            return 0;
        }
    }

    public String getInvocationString(){ return invocationString; }

    public void setExampleID(int Id){ this.exampleID = Id; }

    public int getExampleID(){ return exampleID; }

    public void saveToDb(){
        DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
        databaseHandler.saveToDb(this);
    }
}
