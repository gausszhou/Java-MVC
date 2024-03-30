package com.example.mvc.framework.util;

//import com.github.javaparser.JavaParser;
import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileUtil {

    private static String parseClassname(File filename) throws IOException {
        try (FileInputStream fin = new FileInputStream(filename)) {
            CompilationUnit cu = StaticJavaParser.parse(fin);
            Optional<PackageDeclaration> packageDeclaration = cu.getPackageDeclaration();
            String packageName = packageDeclaration.get().getName().asString();

            String className = cu.findFirst(ClassOrInterfaceDeclaration.class)
                    .map(c -> c.getNameAsString())
                    .orElse("Unknown");
            String fullName = packageName + "." + className;
            return fullName;

        }
    }

    public static String getClassNameFromFile(String filePath) throws IOException {
        String className = parseClassname(new File(filePath));
        return className;
    }

    public static List<String> getAllClassFileClassNames(String folderPath) throws IOException {
        List<String> filePaths = getAllJavaFilePaths(folderPath);
        List<String> classNames = new ArrayList<>();
        for (String filePath : filePaths) {
            String className = getClassNameFromFile(filePath);
            if (className != null) {
                classNames.add(className);
            }
        }
        return classNames;
    }

    public static List<String> getAllJavaFilePaths(String folderPath) {
        List<String> javaFilePaths = new ArrayList<>();
        File folder = new File(folderPath);
        getAllJavaFiles(folder, javaFilePaths);
        return javaFilePaths;
    }

    public static void getAllJavaFiles(File folder, List<String> javaFilePaths) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".java")) {
                    String path = file.getAbsolutePath();
                    javaFilePaths.add(path);
                } else if (file.isDirectory()) {
                    getAllJavaFiles(file, javaFilePaths); // 递归调用，处理子文件夹
                }
            }
        }
    }
}
