package org.exparser.util.fileHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class DataToCSV {
    private static DataToCSV single_instance=null;

    private Path savePath;

    private DataToCSV(){
        // Exists only to defeat instantiation as part of Singleton pattern.
    }

    public static DataToCSV getInstance(){
        if (single_instance == null){
            single_instance = new DataToCSV();
        }
        return single_instance;
    }

    public void saveResultToFile(String methodID, String methodLocation) {
        String content = methodID + "," + methodLocation;
        saveToFile(content);
    }

    private void saveToFile(String content) {
        Path path = savePath;
        byte[] contentBytes = (content + System.lineSeparator()).getBytes();
        try {
            Files.write(path, contentBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void buildHeaders(){
        String resultHeader = "Found Method API Invocation,Found In";
        saveToFile(resultHeader);
    }

    public void setSavePath(Path absFilePath){
        this.savePath = absFilePath;
    }
}