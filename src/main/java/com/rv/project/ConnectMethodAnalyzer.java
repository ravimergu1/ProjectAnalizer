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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConnectMethodAnalyzer {

    static Map<String, List<String>> data;

    public static void main(String[] args) {
        data = new LinkedHashMap<>();
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
                    wait1Sec();
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
            List<String> accountClassList = new ArrayList<>();
            while ((accountClass = bufferedReader.readLine()) != null) {
                if(accountClass.contains("<account.classes>") && accountClass.contains("</account.classes>")){
                    accountClass = accountClass.replace("<account.classes>", "")
                            .replace("</account.classes>", "").trim();
                    accountClassList.add(accountClass);
                    //checkConnectMethod(accountClass, pomFile);
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
                    accountClassList.add(accountClass);
                    //checkConnectMethod(accountClass, pomFile);
                }
            }
            // Close the BufferedReader and FileReader when done
            bufferedReader.close();
            fileReader.close();
            if (!accountClassList.isEmpty()) {
                System.out.println("\n\n");
                System.out.println(pomFile.getAbsoluteFile());
                System.out.println("---------------------------------------------------------");
                for (String accountClassName : accountClassList) {
                    checkConnectMethod(accountClassName, pomFile);
                }
            }
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
        wait1Sec();
        System.out.println(javaFile.getAbsoluteFile());
        List<String> exceptions = readMethod(javaFile);
        System.out.println("account class : " + javaFile.getAbsolutePath());
        System.out.println("Exceptions are " + exceptions);

    }

    public static List<String> readMethod(File javaFile) {
        AtomicReference<List<String>> list = new AtomicReference<>(new ArrayList<>());
        if (!javaFile.exists()) {
            wait1Sec();
            System.err.println("java file not found : " + javaFile);
            return null;
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
                        wait1Sec();
                        list.set(findExceptions(methodCode));
                        //System.out.println("Method '" + "connect" + "':");
                        //System.out.println(methodCode);
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
                        if (className.equals(parentName)) {
                            System.out.println("found same class name with parent " + classOrInterfaceType.getNameWithScope());
                            continue;
                        }

                        String parentJavaFileName = javaFile.getParent()+ File.separator + parentName + ".java";
                        File parentClassFile = new File(parentJavaFileName);
                        if (parentClassFile.exists()) {
                            wait1Sec();
                            System.out.println("1. found parent class: " + parentJavaFileName);
                            return readMethod(parentClassFile);
                        }
                        NodeList<ImportDeclaration> imports = cu.getImports();
                        for (ImportDeclaration anImport : imports) {
                            String importName = anImport.getName().toString();
                            if (importName.endsWith(parentName)) {
                                String pName = javaFile.getAbsolutePath().substring(0, javaFile.getAbsolutePath().indexOf("java/")+5) + importName.replace(".", File.separator) + ".java";
                                wait1Sec();
                                System.out.println("2. found parent class: " + pName);
                                return readMethod(new File(pName));
                            }
                        }
                        wait1Sec();
                        System.out.println(classOrInterfaceType);
                    }
                } else {
                    wait1Sec();
                    System.out.println("Class '" + className + "' not found in the Java file.");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return list.get();
    }

    private static List<String> findExceptions(String code) {
        List<String> list = new ArrayList<>();
        // Define a regular expression pattern to match "throws" clauses
        Pattern pattern = Pattern.compile("throws\\s+(\\w+(\\.\\w+)?(,\\s*\\w+(\\.\\w+)?)*)");

        // Create a matcher for the code string
        Matcher matcher = pattern.matcher(code);

        // Find and print matching "throws" clauses
        while (matcher.find()) {
            String exceptions = matcher.group(1);
            String[] exceptionArray = exceptions.split(",\\s*");
            for (String exception : exceptionArray) {
               list.add("throws : " + exception);
            }
        }

        // Define a regular expression pattern to match "throws" clauses
        Pattern pattern1 = Pattern.compile("throw\\s+new\\s+(\\w+(\\.\\w+)?(,\\s*\\w+(\\.\\w+)?)*)");

        // Create a matcher for the code string
        Matcher matcher1 = pattern1.matcher(code);

        // Find and print matching "throws" clauses
        while (matcher1.find()) {
            String exceptions = matcher1.group(1);
            String[] exceptionArray = exceptions.split(",\\s*");
            for (String exception : exceptionArray) {
                list.add("throw new : " + exception);
            }
        }

        return list;
    }

    private static void wait1Sec() {

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }
}
