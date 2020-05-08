package org.exparser.util.fileHandler;

import org.exparser.main.java.ApiMethodObject;

import java.io.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Scanner;

public class Helper {

    public static void showAvailableMigrationMethods(){
        LinkedHashMap<Integer, String> migrateableMethods = getMigratableMethods();

        for (int apiMethod : getMigratableMethods().keySet()){
            System.out.println(String.format("%d) %s", apiMethod, migrateableMethods.get(apiMethod)));
        }
    }

    private static LinkedHashMap<Integer, String> getMigratableMethods() {

        DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
        LinkedHashMap<Integer, String> apiMethodsToStudy = databaseHandler.getAllAPIMethodNames();

        return apiMethodsToStudy;
    }

    public static int getMigrationMethodUserInput(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your selection integer now: ");
        while(!scanner.hasNextInt()) scanner.next();
        int i = scanner.nextInt();
        scanner.close();

        return i;
    }

    public static LinkedHashSet<PrimaryExample> getExampleFromSelection(int selection){
        LinkedHashSet<PrimaryExample> relatedPrimaryExamples = DatabaseHandler.getInstance().getConfirmedPrimaryExamplesForMethod(selection);
        return relatedPrimaryExamples;
    }

    public static void showAvailableMigrations(Path filesToMigrate){
        int userSelection = getMigrationMethodUserInput();
        ApiMethodObject apiMethod = DatabaseHandler.getInstance().getAPICall(userSelection);

        for (PrimaryExample example : getExampleFromSelection(userSelection)) {
            MigrationExample migrationExample = DatabaseHandler.getInstance().fromPrimaryGetMigration(example);

            if (migrationExample != null){
                System.out.println(String.format("Using examples %s & %s", example.getFileOnDisk().getAbsolutePath(), migrationExample.getFileOnDisk().getAbsolutePath()));

                try {
                    FileHandler.migrateExamples(example.getFileOnDisk(), migrationExample.getFileOnDisk(), filesToMigrate, apiMethod.getMethodName());
                } catch (IOException e) {
                    System.out.println(String.format("Error finding migration using example %d", example.getApiId()));
                }

            }
        }
    }


    public static boolean stringInFile(String stringToFind, File file){

        String line;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
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


}
