package org.exparser.main.java;

import org.exparser.util.fileHandler.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class Exparser {

    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            throw argumentException();
        }

        final String option = args[0];
        if (option.equalsIgnoreCase("-h")) {
            printHelp();
        } else if (option.equalsIgnoreCase("-a")) {
            analyseFolder(args);
        } else if(option.equalsIgnoreCase("-getAPIUsage")){
            String[] apiMethodSetup = {"-a","C:\\Users\\maxim\\Documents\\Results\\Summer2018\\FDroidApps\\TEST_SET","-a"};
            analyseFolder(apiMethodSetup);
        } else if (option.equalsIgnoreCase("-e")){
            //parseExamples(args);
            findExampleByFolders();
        } else if (option.equalsIgnoreCase("-f")){
            findExamples(args);
        } else if (option.equalsIgnoreCase("-m")){
            migrateExamples(args);
        } else if (option.equalsIgnoreCase("-migrateGeneral")) {
            generalMigration();
        } else if (option.equalsIgnoreCase("-populateDb")){
            String[] apiMethodSetup = {"-a","C:\\Users\\maxim\\Desktop\\test_version1\\setUPFolder","-b"};

            //analyseFolder(apiMethodSetup);

            //firstStepSearchAndRescue();

            //searchAndRescueConfirmation();

            searchAndRescueMigrationMine();
        }
    }

    private static void searchAndRescueMigrationMine(){
        loadDB();

        System.out.println("Finding migrations of previously confirmed API examples");

        FileHandler.mineMigrationExamples();
    }

    private static void searchAndRescueConfirmation(){
        //FIXME this is temporary, will need something to feed it later
        // Currently require -a to a file that has a use of the method you want to search, can be modified at a later date
        loadDB();

        System.out.println("Confirming API examples using best available AST information");

        String savelocation = "C:\\Users\\maxim\\Documents\\Results\\Summer2018\\exampleFiles\\.git";
        //String savelocation = "/home/user/Max_backup/FDroid2018/exampleFiles";
        File saveLocation = Paths.get(savelocation).toFile();
        FileHandler fileHandler = new FileHandler(saveLocation, CompilationUnitRequest.ProcessType.API_USES);

    }

    private static void firstStepSearchAndRescue(){
        loadDB();

        System.out.println("Gathering examples of API uses from example files");

        //String examplesFolder = "C:\\Users\\maxim\\Documents\\Results\\Summer2018\\exampleRepos";
        String examplesFolder = "C:\\Users\\maxim\\Desktop\\testProject\\survey_originals\\asgit";
        File examplesFile = Paths.get(examplesFolder).toFile();
        FileHandler.mineExamples(examplesFile);
    }

    private static void loadDB(){
        String folder = "C:\\Users\\maxim\\Desktop\\testProject\\survey_originals\\experimentsetup";
        //String folder = "/home/user/Max_backup/FDroid2018/remoteRunner";
        Path folderPath = Paths.get(folder);
        Path saveFilePath = Paths.get(folderPath.toString(), "API_uses.db");

        DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
        databaseHandler.setDatabasePath(saveFilePath);
    }

    private static void findExamples(String[] args) throws Exception{
        if (args.length != 3){
            throw argumentException();
        }

        Path examplePath = Paths.get(args[1]);
        Path searchDirectory = Paths.get(args[2]);

        HashMap<File, HashMap> examples = FileHandler.searchExamples(examplePath.toFile(), searchDirectory);
        System.out.println("Done");

    }

    private static void migrateExamples(String[] args) throws Exception{

        Path firstExamplePath = Paths.get("C:\\Users\\maxim\\Desktop\\testProject\\original_example");
        Path secondExamplePath = Paths.get("C:\\Users\\maxim\\Desktop\\testProject\\migration_example");
        Path searchDirectory = Paths.get("C:\\Users\\maxim\\Desktop\\testProject\\file_to_migrate");
        String invocation = "vibrate";

        FileHandler.migrateExamples(getFileInFolder(firstExamplePath),getFileInFolder(secondExamplePath),searchDirectory, invocation);

    }

    //TODO remove this, this is a hack to speed up testing
    private static File getFileInFolder(Path folderPath){
        File[] files = folderPath.toFile().listFiles();

        return files[0];
    }

    private static void generalMigration() throws Exception{
        loadDB();

        Path migrationCandidatePath = Paths.get("C:\\Users\\maxim\\Desktop\\test_version1");
        FileHandler.generalMigration(migrationCandidatePath);
    }


    private static void parseExamples(String[] args) throws Exception{
        if (args.length != 3){
            throw argumentException();
        }
        String firstExamplePath = args[1];
        String secondExamplePath = args[2];

        Path e1Path = Paths.get(firstExamplePath);
        Path e2Path = Paths.get(secondExamplePath);

        FileHandler.compareFiles(e1Path.toFile(), e2Path.toFile());

    }

    private static void findExampleByFolders(){
        Path originalFolder = Paths.get("C:\\Users\\maxim\\Desktop\\testProject\\survey_originals");
        Path migratedFolder = Paths.get("C:\\Users\\maxim\\Desktop\\testProject\\surveymaps");

        ExampleFileFinder fileFinder = new ExampleFileFinder(originalFolder, migratedFolder);

    }

    private static void analyseFolder(String[] args){
        if (args.length > 4){
            throw argumentException();
        }

        String folder = args[1];
        CompilationUnitRequest.ProcessType processType = determineProcessType(args);



            if(processType == CompilationUnitRequest.ProcessType.API_BUILDER) {
                Path folderPath = Paths.get(folder);
                Path saveFilePath = Paths.get(folderPath.toString(), "API_uses.db");

                DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
                databaseHandler.setDatabasePath(saveFilePath);

                System.out.println("Building DB list of API methods from API files");
                FileHandler fileHandler = new FileHandler(folderPath.toFile(), processType);
            }
            else if(processType == CompilationUnitRequest.ProcessType.API_USES || processType == CompilationUnitRequest.ProcessType.API_USAGE){
                loadDB();
                Path analysisPath = Paths.get(args[1]);

                File[] directoryListing = analysisPath.toFile().listFiles();
                if (directoryListing != null) {
                    for (File child : directoryListing) {
                        if (child.isDirectory() && !FileHandler.stringInFile(child.toString())) {
                            System.out.printf("Currently looking at %s \n", child.getAbsolutePath());
                            FileHandler fileHandler = new FileHandler(child, processType);
                            FileHandler.writeToDoneFile(child.toString());
                        }
                    }
                }
            }
    }

    private static CompilationUnitRequest.ProcessType determineProcessType(String[] args){
        String arg3 = args[2];

        if (arg3.equalsIgnoreCase("-b")){
            return CompilationUnitRequest.ProcessType.API_BUILDER;
        }
        else if (arg3.equalsIgnoreCase("-u")){

            return CompilationUnitRequest.ProcessType.API_USES;
        }
        else if (arg3.equalsIgnoreCase("-a")){

            return CompilationUnitRequest.ProcessType.API_USAGE;
        }
        else{
            throw argumentException();
        }
    }

    private static IllegalArgumentException argumentException() {
        return new IllegalArgumentException("Type 'Exparser -h' for help.");
    }

    private static void printHelp(){
        System.out.println("-h\t\t\t\t\t\t\tShow tips");
        System.out.println("-a <folder> -b\t\t\t\tBuild all API methods in <folder>.");
        System.out.println("-a <folder> -u <folder2>\tDetect all API <folder> uses in <folder2>.");
        System.out.println("-e <example1> <example2>\tCreate mapping between example 1 and example 2, paths must be given.");
        System.out.println("-f <example> <folder>\tSearch folder for instances of the example, paths must be given.");
        System.out.println("-m <example1> <example2> <folder>\tCreate mapping between example 1 and example 2, and apply to all relevant sources in folder paths must be given.");
    }

    private static void setUpSaveFile(String folderArg){
        try {
            Path folderPath = Paths.get(folderArg);
            Path saveFilePath = Paths.get(folderPath.toString(), "API_uses.csv");
            Files.deleteIfExists(saveFilePath);

            DataToCSV dataToCSV = DataToCSV.getInstance();
            dataToCSV.setSavePath(saveFilePath);
            dataToCSV.buildHeaders();

        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
