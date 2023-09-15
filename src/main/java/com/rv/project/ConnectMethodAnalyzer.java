package com.rv.project;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectMethodAnalyzer {

    public static void main(String[] args) {
        String baseDirectory = "/home/gaian/snaplogic/Snap_v4";
        loadPomFiles(baseDirectory);
    }
    public static void loadPomFiles(String baseDirectory){
        File[] files = new File(baseDirectory).listFiles();

        if (files != null) {
            for (File file : files) {
                if(file.isDirectory()) {
                    loadPomFiles(file.getAbsolutePath());
                } else if (file.isFile() && file.getName().equals("pom.xml")) {
                    System.out.println("\n\n");
                    System.out.println(file.getAbsoluteFile());
                    System.out.println("---------------------------------------------------------");
                    extractClassNames(file);
                }
            }
        }
    }

    private static void extractClassNames(File pomFile){
        try {
            // Create a FileReader to read the file
            FileReader fileReader = new FileReader(pomFile);
            // Wrap the FileReader in a BufferedReader for efficient reading
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String accountClass;
            boolean classfound = false;

            // Read and print each line of the file
            while ((accountClass = bufferedReader.readLine()) != null) {
                if(accountClass.contains("<account.classes>") && accountClass.contains("</account.classes>")){
                    accountClass = accountClass.replace("<account.classes>", "")
                            .replace("</account.classes>", "").trim();
                    checkConnectMethod(accountClass, pomFile);
                    classfound = false;
                    continue;
                }
                if(accountClass.contains("<account.classes>")){
                    classfound = true;
                    continue;
                }
                if(accountClass.contains("</account.classes>")){
                    classfound = false;
                    continue;
                }
                if(classfound && !accountClass.contains("<!--")){
                    accountClass = accountClass.trim();
                    if(accountClass.endsWith(",")) {
                        accountClass = accountClass.replace(",", "");
                    }
                    checkConnectMethod(accountClass, pomFile);
                }
            }
            // Close the BufferedReader and FileReader when done
            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void checkConnectMethod(String accountClass, File pomFile) {
        StringBuffer buffer = new StringBuffer("");
        buffer.append(pomFile.getAbsolutePath().replace("pom.xml", "src/main/java/"));
        buffer.append(accountClass.replace(".", "/"));
        buffer.append(".java");
        File javaFile = new File(buffer.toString());
        System.out.println(javaFile.getAbsoluteFile());
        readMethod(javaFile);
    }

    public static void readMethod(File javaFile) {
        if (!javaFile.exists()) {
            System.err.println("java file not found : " + javaFile);
            return;
        }
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            DefaultPrinterConfiguration configuration = new DefaultPrinterConfiguration();
            AtomicBoolean isFoundAccountMethod = new AtomicBoolean(false);
            String methodName = "connect";
            cu.walk(Node.TreeTraversal.BREADTHFIRST, node -> {
                if (node instanceof MethodDeclaration) {
                    MethodDeclaration methodDeclaration = (MethodDeclaration) node;
                    if (methodDeclaration.getNameAsString().equals(methodName)) {
                        String methodCode = methodDeclaration.toString(configuration);
                        System.out.println("Method '" + "connect" + "':");
                        System.out.println(methodCode);
                        isFoundAccountMethod.set(true);
                    }
                }
            });

            if (!isFoundAccountMethod.get()) {
                String className = javaFile.getName().replace(".java", "");
                Optional<ClassOrInterfaceDeclaration> classOptional = cu.findFirst(ClassOrInterfaceDeclaration.class,
                        c -> c.getNameAsString().equals(className));

                if (classOptional.isPresent()) {
                    ClassOrInterfaceDeclaration clazz = classOptional.get();
                    // Check if the class extends another class
                    NodeList<ClassOrInterfaceType> extendedClassOptional = clazz.getExtendedTypes();
                    for (ClassOrInterfaceType classOrInterfaceType : extendedClassOptional) {
                        String parentName = classOrInterfaceType.getName().asString();
                        String parentJavaFileName = javaFile.getParent()+ File.separator + parentName + ".java";
                        File parentClassFile = new File(parentJavaFileName);
                        if (parentClassFile.exists()) {
                            System.out.println("1. found parent class: " + parentJavaFileName);
                            readMethod(parentClassFile);
                            return;
                        }
                        NodeList<ImportDeclaration> imports = cu.getImports();
                        for (ImportDeclaration anImport : imports) {
                            String importName = anImport.getName().toString();
                            if (importName.endsWith(parentName)) {
                                String pName = javaFile.getAbsolutePath().substring(0, javaFile.getAbsolutePath().indexOf("java/")+5) + importName.replace(".", File.separator) + ".java";
                                System.out.println("2. found parent class: " + pName);
                                readMethod(new File(pName));
                                return;
                            }
                        }

                        System.out.println(classOrInterfaceType);
                    }

                    /*if (extendedClassOptional.isPresent()) {
                        ClassOrInterfaceDeclaration extendedClass = extendedClassOptional.get();
                        System.out.println("Class '" + className + "' extends class: " + extendedClass.getName());
                    } else {
                        System.out.println("Class '" + className + "' does not extend another class.");
                    }*/
                } else {
                    System.out.println("Class '" + className + "' not found in the Java file.");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
