package org.exparser.util.fileHandler;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.exparser.main.java.ApiMethodObject;

public class DatabaseHandler {
    private static DatabaseHandler single_instance=null;

    private Path databasePath;
    private String dbURL;

    private DatabaseHandler(){
        // Exists only to defeat instantiation as part of Singleton pattern.
    }

    public static DatabaseHandler getInstance(){
        if (single_instance == null){
            single_instance = new DatabaseHandler();
        }
        return single_instance;
    }

    public void setDatabasePath(Path absFilePath){
        this.databasePath = absFilePath;
        this.dbURL = "jdbc:sqlite:" + databasePath.toString();

        if (!Files.exists(databasePath)){
            createNewDatabase(dbURL);
        }
    }

    private static void createNewDatabase(String url){

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");

                populateDatabase(url);
                createMethodPrimaryExampleTable(url);
                createMethodMigrationExampleTable(url);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void populateDatabase(String dbURL){
        String sql = "CREATE TABLE IF NOT EXISTS api_uses (\n"
                + "	id integer PRIMARY KEY,\n"
                + "	methodPackage text NOT NULL,\n"
                + "	apiClass text NOT NULL,\n"
                + "	methodModifiers text NOT NULL,\n"
                + "	returnType text,\n"
                + "	methodName text NOT NULL,\n"
                + "	methodArguments text NOT NULL,\n"
                + "	methodExceptions text NOT NULL,\n"
                + "	isConstructor text NOT NULL,\n"
                + " UNIQUE(methodPackage, apiClass, methodModifiers, returnType, methodName, methodArguments, methodExceptions, isConstructor)"
                + ");"
                ;

        try (Connection conn = DriverManager.getConnection(dbURL);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void createMethodPrimaryExampleTable(String dbURL){

        String sql = "CREATE TABLE IF NOT EXISTS api_primary_examples (\n"
                + " example_id integer PRIMARY KEY,\n"
                + " project_path text NOT NULL,\n"
                + " fileName text NOT NULL,\n"
                + " fileLocationOnDisk  text NOT NULL,\n"
                + " commit_number text NOT NULL,\n"
                + " confirmed integer NOT NULL,\n"
                + " api_method_id integer NOT NULL,\n"
                + " invocation_string text NOT NULL,\n"
                + " FOREIGN KEY (api_method_id) REFERENCES api_uses(id),\n"
                + " UNIQUE(project_path, fileName, fileLocationOnDisk, commit_number, confirmed, api_method_id, invocation_string)"
                + ");"
                ;

        try (Connection conn = DriverManager.getConnection(dbURL);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void createMethodMigrationExampleTable(String dbURL){

        String sql = "CREATE TABLE IF NOT EXISTS api_migration_examples (\n"
                + " example_id integer PRIMARY KEY,\n"
                + " fileName text NOT NULL,\n"
                + " fileLocationOnDisk  text NOT NULL,\n"
                + " commit_number text NOT NULL,\n"
                + " original_example_id integer NOT NULL,\n"
                + " FOREIGN KEY (original_example_id) REFERENCES api_primary_examples(example_id),\n"
                + " UNIQUE(fileName, fileLocationOnDisk, commit_number, original_example_id)"
                + ");"
                ;

        try (Connection conn = DriverManager.getConnection(dbURL);
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private Connection connect(){
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(this.dbURL);
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void saveToDb(ApiMethodObject apiMethodObject){
        String sql = "INSERT OR IGNORE INTO api_uses(apiClass,methodModifiers,returnType,methodName,methodArguments,methodExceptions,methodPackage,isConstructor) VALUES(?,?,?,?,?,?,?,?)";

        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, apiMethodObject.getClassName());
            pstmt.setString(2, apiMethodObject.getModifiers());
            pstmt.setString(3, apiMethodObject.getReturnType());
            pstmt.setString(4, apiMethodObject.getMethodName());
            pstmt.setString(5, apiMethodObject.getParameters());
            pstmt.setString(6, apiMethodObject.getMethodExceptions());
            pstmt.setString(7, apiMethodObject.getMethodPackage());
            pstmt.setString(8, apiMethodObject.isConstructor());
            pstmt.executeUpdate();
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void saveToDb(PrimaryExample primaryExample){
        String sql = "INSERT OR IGNORE INTO api_primary_examples(fileName,fileLocationOnDisk,commit_number,confirmed,api_method_id,invocation_string,project_path) VALUES(?,?,?,?,?,?,?)";

        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, primaryExample.getGitFilePath());
            pstmt.setString(2, primaryExample.getFileOnDisk().getAbsolutePath());
            pstmt.setString(3, primaryExample.getCommitExtracted());
            pstmt.setInt(4, primaryExample.getConfirmed());
            pstmt.setInt(5, primaryExample.getApiId());
            pstmt.setString(6, primaryExample.getInvocationString());
            pstmt.setString(7, primaryExample.getProjectPath().getAbsolutePath());
            pstmt.executeUpdate();
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void saveToDb(MigrationExample migrationExample){
        String sql = "INSERT OR IGNORE INTO api_migration_examples(fileName,fileLocationOnDisk,commit_number,original_example_id) VALUES(?,?,?,?)";

        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, migrationExample.getGitFilePath());
            pstmt.setString(2, migrationExample.getFileOnDisk().getAbsolutePath());
            pstmt.setString(3, migrationExample.getCommitExtracted());
            pstmt.setInt(4, migrationExample.getOriginalExampleId());
            pstmt.executeUpdate();
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    private int apiMethodObjectNoBindings(ApiMethodObject apiMethodObject){
        int contains = -1;

        String sql = "SELECT * FROM api_uses WHERE "
                + "methodName LIKE ?";

        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, apiMethodObject.getMethodName());


            contains = checkIfFound(pstmt);

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }

        return contains;

    }

    public int contains(ApiMethodObject apiMethodObject){
        if (apiMethodObject.getMethodPackage() == null){
            return apiMethodObjectNoBindings(apiMethodObject);
        } else {
            return apiMethodObjectHasBindings(apiMethodObject);
        }
    }

    private int apiMethodObjectHasBindings(ApiMethodObject apiMethodObject){
        int contains = -1;

        String sql = "SELECT * FROM api_uses WHERE "
                + "apiClass LIKE? AND "
                + "returnType LIKE ? AND "
                + "methodName LIKE ? AND "
                + "methodArguments LIKE ? AND "
                + "methodExceptions LIKE ? AND "
                + "methodPackage=? AND "
                + "isConstructor LIKE ?";

        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, apiMethodObject.getClassName());
            pstmt.setString(2, apiMethodObject.getReturnType());
            pstmt.setString(3, apiMethodObject.getMethodName());
            pstmt.setString(4, apiMethodObject.getParameters());
            pstmt.setString(5, apiMethodObject.getMethodExceptions());
            pstmt.setString(6, apiMethodObject.getMethodPackage());
            pstmt.setString(7, apiMethodObject.isConstructor());


            contains = checkIfFound(pstmt);

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }

        return contains;
    }

    private int checkIfFound(PreparedStatement pstmt){
        try (ResultSet rs = pstmt.executeQuery()){

            if (rs.next()) {
                Statement item = rs.getStatement();
                int found = rs.getInt("id"); // "found" column
                return found;
            }
        }
        catch (SQLException ex){
            ex.printStackTrace();
        }
        return -1;
    }

    public ApiMethodObject getAPICall(int APIid){

        String sql = "SELECT * FROM api_uses WHERE "
                + "id=?";

        ApiMethodObject apiMethodObject = null;
        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, APIid);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()){
                apiMethodObject = new ApiMethodObject(rs);
            }

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return apiMethodObject;
    }

    public LinkedHashMap<Integer, String> getAllAPIMethodNames(){
        LinkedHashMap<Integer, String> apiMethodNames = new LinkedHashMap<>();

        String sql = "SELECT id,methodName FROM api_uses";

        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(sql)){

            while (resultSet.next()){
                apiMethodNames.put(resultSet.getInt("id"), resultSet.getString("methodName"));
            }

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }

        return apiMethodNames;
    }

    public void setPrimaryExampleAsConfirmed(String fileLocationOnDisk, Integer api_method_id) {
        //fileLocationOnDisk = fileLocationOnDisk.replace("\\", "\\\\");

        String sql = "UPDATE api_primary_examples SET confirmed = 1 WHERE fileLocationOnDisk LIKE ? AND api_method_id = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileLocationOnDisk);
            pstmt.setInt(2, api_method_id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void setInvocationString(String invocationString, String fileLocationOnDisk, Integer api_method_id) {
        //fileLocationOnDisk = fileLocationOnDisk.replace("\\", "\\\\");

        String sql = "UPDATE api_primary_examples SET invocation_string = ? WHERE fileLocationOnDisk LIKE ? AND api_method_id = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, invocationString);
            pstmt.setString(2, fileLocationOnDisk);
            pstmt.setInt(3, api_method_id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public LinkedHashSet<PrimaryExample> getConfirmedPrimaryExamples(){
        LinkedHashSet<PrimaryExample> confirmedPrimaryExamples = new LinkedHashSet<>();

        String sql = "SELECT example_id,project_path,fileName,fileLocationOnDisk,commit_number,confirmed,api_method_id,invocation_string FROM api_primary_examples WHERE confirmed = 1";

        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(sql)){

            while (resultSet.next()){
                PrimaryExample primaryExample = fromResultBuildPrimaryExample(resultSet);
                confirmedPrimaryExamples.add(primaryExample);
            }

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }

        return confirmedPrimaryExamples;
    }

    public LinkedHashSet<PrimaryExample> getConfirmedPrimaryExamplesForMethod(Integer methodID){
        LinkedHashSet<PrimaryExample> confirmedPrimaryExamples = new LinkedHashSet<>();

        String sql = "SELECT example_id,project_path,fileName,fileLocationOnDisk,commit_number,confirmed,api_method_id,invocation_string FROM api_primary_examples WHERE confirmed = 1 AND api_method_id = ?";

        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, methodID);

            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                PrimaryExample primaryExample = fromResultBuildPrimaryExample(resultSet);
                confirmedPrimaryExamples.add(primaryExample);
            }

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }

        return confirmedPrimaryExamples;
    }

    private PrimaryExample fromResultBuildPrimaryExample(ResultSet resultsSet) throws SQLException {

        int apiId = resultsSet.getInt("api_method_id");
        String gitFilePath = resultsSet.getString("fileName");
        String commitExtracted = resultsSet.getString("commit_number");
        File fileOnDisk = Paths.get(resultsSet.getString("fileLocationOnDisk")).toFile();
        File projectPath = Paths.get(resultsSet.getString("project_path")).toFile();
        boolean confirmed = (resultsSet.getInt("confirmed") == 1);
        String invocationString = resultsSet.getString("invocation_string");
        PrimaryExample primaryExample = new PrimaryExample(apiId, gitFilePath, commitExtracted, fileOnDisk, projectPath, confirmed, invocationString);
        primaryExample.setExampleID(resultsSet.getInt("example_id"));

        return primaryExample;
    }

    private MigrationExample fromResultBuildMigrationExample(ResultSet resultsSet) throws SQLException {

        int apiId = resultsSet.getInt("original_example_id");
        String gitFilePath = resultsSet.getString("fileName");
        String commitExtracted = resultsSet.getString("commit_number");
        File fileOnDisk = Paths.get(resultsSet.getString("fileLocationOnDisk")).toFile();
        MigrationExample migrationExample = new MigrationExample(apiId, gitFilePath, commitExtracted, fileOnDisk);

        return migrationExample;
    }

    public MigrationExample fromPrimaryGetMigration(PrimaryExample primaryExample){
        MigrationExample migrationExample = null;

        String sql = "SELECT example_id,fileName,fileLocationOnDisk,commit_number,original_example_id FROM api_migration_examples WHERE original_example_id = ?";

        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, primaryExample.getExampleID());

            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                migrationExample = fromResultBuildMigrationExample(resultSet);
            }

        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
        return migrationExample;
    }
}


