package org.exparser.main.java;

import org.eclipse.jdt.core.dom.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ApiMethodObject {

    private String MethodPackage;
    private String ClassName;
    private String Modifiers;
    private String ReturnType;
    private String MethodName;
    private String Parameters;
    private String MethodExceptions;
    private String IsConstructor;


    public ApiMethodObject(MethodDeclaration methodDeclaration){
        IMethodBinding methodBinding = methodDeclaration.resolveBinding();

        this.Modifiers = getModifiers(methodDeclaration);
        this.MethodName = methodDeclaration.getName().getFullyQualifiedName();
        try {
            buildMethodObject(methodBinding);
        } catch (NullPointerException e){
            alternativeMethodBuilder(methodDeclaration);
        }

    }

    public ApiMethodObject(ResultSet rs) throws SQLException {

            this.MethodPackage = rs.getString("methodPackage");
            this.ClassName = rs.getString("apiClass");
            this.Modifiers = rs.getString("methodModifiers");
            this.ReturnType = rs.getString("returnType");
            this.MethodName = rs.getString("methodName");
            this.Parameters = rs.getString("methodArguments");
            this.MethodExceptions = rs.getString("methodExceptions");
            this.IsConstructor = rs.getString("isConstructor");

    }

    public ApiMethodObject(MethodInvocation methodInvocation) {
        IMethodBinding methodBinding = methodInvocation.resolveMethodBinding();

        this.MethodName = methodInvocation.getName().getFullyQualifiedName();
        this.Modifiers = null;

        if (methodBinding != null) {

            buildMethodObject(methodBinding);
        }
    }

    //experimental, not in use, could increase accuracy a little but not really worth it
    private void getInvocationParameterTypes(MethodInvocation methodInvocation){
        List<ASTNode> arguments = methodInvocation.arguments();

        for (ASTNode argument: arguments){
            int type = argument.getNodeType();
            if(argument.getNodeType() == 42){
                ITypeBinding argumentType = ((SimpleName) argument).resolveTypeBinding();
            }
        }


    }

    private void buildMethodObject(IMethodBinding methodBinding){
        try {

            this.MethodPackage = methodBinding.getDeclaringClass().getPackage().getName();
            this.ClassName = methodBinding.getDeclaringClass().getName();
            this.ReturnType = methodBinding.getReturnType().getQualifiedName();
            this.Parameters = getParameters(methodBinding);
            this.MethodExceptions = getException(methodBinding);
            this.IsConstructor = String.valueOf(methodBinding.isConstructor());
        } catch (NullPointerException e){
            throw e;
        }
    }

    private void alternativeMethodBuilder(MethodDeclaration methodDeclaration){
        this.MethodPackage = "Bindings Failed";
        this.ClassName = getClassName(methodDeclaration);
        this.ReturnType = getReturnType(methodDeclaration);
        this.Parameters = getParameters(methodDeclaration);
        this.MethodExceptions = getException(methodDeclaration);
        this.IsConstructor = String.valueOf(methodDeclaration.isConstructor());
    }

    private static String getClassName(MethodDeclaration method){
        ASTNode methodParent = method.getParent();
        if (methodParent.getNodeType() == 55) {

            String className = ((TypeDeclaration) methodParent).getName().toString() + ",";
            String classModifiers = getClassModifiers(((TypeDeclaration) methodParent));
            return classModifiers + className;
        }
        else if (methodParent.getNodeType() == 1){
            return "AnonymousClassDeclaration,";
        }
        else{
            return "unknown,";
        }
    }

    private static String getException(MethodDeclaration method){

        if (method.thrownExceptionTypes().size() > 0){
            StringBuilder exceptionString = new StringBuilder();
            for (Object item : method.thrownExceptionTypes()){
                exceptionString.append(item.toString()).append(";");
            }
            return exceptionString.toString();
        }
        return "None";
    }

    private static String getException(IMethodBinding method){

        if (method.getExceptionTypes().length > 0){
            String exceptionString = "";
            for (ITypeBinding item : method.getExceptionTypes()){
                exceptionString += item.getName() + ";";
            }
            return exceptionString;
        }
        return "None";
    }

    private static String getClassModifiers(TypeDeclaration classNode){
        String modifierString = "";


        if (classNode.modifiers().size() > 0){

            for (Object item : classNode.modifiers()){
                modifierString += item.toString() + ";";
            }
        }

        if (classNode.isInterface()){
            modifierString += "Interface;";
        }

        return modifierString;
    }

    private static String getModifiers(MethodDeclaration method) {

        if (method.modifiers().size() > 0){
            String modifierString = "";
            for (Object item : method.modifiers()){
                modifierString += item.toString() + ";";
            }
            return modifierString;
        }
        else{
            return "None";
        }
    }

    private static String getReturnType(MethodDeclaration method) {
        String returnTypeString = null;

        if (method.getReturnType2() != null){
            returnTypeString = method.getReturnType2().toString();
        }

        return returnTypeString;
    }

    private static String getParameters(MethodDeclaration method) {

        if (method.parameters().size() > 0) {
            String methodParameters = "";
            for (Object item : method.parameters()) {
                methodParameters += ((SingleVariableDeclaration)item).getType().toString().replaceAll(",",";") + ";";
            }
            return methodParameters;
        }
        else {
            return "None";
        }
    }

    private static String getParameters(IMethodBinding method) {

        if (method.getParameterTypes().length > 0) {
            String methodParameters = "";
            for (ITypeBinding item : method.getParameterTypes()) {
                methodParameters += item.getQualifiedName() +";";
            }
            return methodParameters;
        }
        else {
            return "None";
        }
    }

    private static String getPackage(CompilationUnit compilationUnit){
        String packageString = compilationUnit.getPackage().toString();

        //clean the packageString
        packageString.replaceAll("package ","");
        packageString.replaceAll("\n","");

        return packageString;
    }

    public String getMethodPackage() {
        return MethodPackage;
    }

    public String getClassName() {
        return ClassName;
    }

    public String getModifiers() {
        return Modifiers;
    }

    public String getReturnType() {
        return ReturnType;
    }

    public String getMethodName() {
        return MethodName;
    }

    public String getParameters() {
        return Parameters;
    }

    public String getMethodExceptions() {
        return MethodExceptions;
    }

    public String isConstructor() {
        return IsConstructor;
    }

    public String getStringOutput(){
        String outputString = "";
        outputString += this.MethodPackage + "." + this.ClassName + " ";
        outputString += this.ReturnType + " " +this.MethodName + "(" +this.Parameters + ")" + this.MethodExceptions;

        return outputString;
    }
}
