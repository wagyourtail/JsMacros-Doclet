package xyz.wagyourtail.docklet;
import com.sun.javadoc.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public class JsMacrosDocklet {
    public static File outDir;
    public static File versionOutDir;
    public static FileHandler outputTS;
    public static String version = null;
    
    public static List<LibDoc> libs = new LinkedList<>();
    public static List<String> classes = new LinkedList<>();
    public static List<EventDoc> events = new LinkedList<>();
    

    public static boolean start (RootDoc root) {
        versionOutDir = new File(outDir, version);
        outputTS = new FileHandler(new File(versionOutDir, "JsMacros.d.ts"));
        //clear version folder
        try {
            deleteFolder(versionOutDir);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (!versionOutDir.exists() && !versionOutDir.mkdirs()) {
            System.err.println("Failed to create version dir");
            return false;
        }
    
        for (ClassDoc clazz : root.classes()) {
            boolean flag = true;
            for (AnnotationDesc desc : clazz.annotations()) {
                if (desc.annotationType().name().equals("Library")) {
                    String value = null;
                    String[] langs = new String[]{};
                    for (AnnotationDesc.ElementValuePair val : desc.elementValues()) {
                        if (val.element().name().equals("value")) {
                            value = (String) val.value().value();
                        } else {
                            langs = Arrays.stream(((AnnotationValue[]) val.value().value())).map(e -> e.value()).toArray(String[]::new);
                        }
                    }
                    genLib(clazz, value, langs);
                    flag = false;
                    break;
                }
                if (desc.annotationType().name().equals("Event")) {
                    String name = null;
                    String oldName = null;
                    for (AnnotationDesc.ElementValuePair val : desc.elementValues()) {
                        if (val.element().name().equals("value")) {
                            name = (String) val.value().value();
                        } else {
                            oldName = (String)val.value().value();
                        }
                    }
                    genEvent(clazz, name, oldName);
                    flag = false;
                    break;
                }
            }
            if (flag) {
                genClass(clazz);
            }
        }
        
        for (LibDoc lib : libs) {
            try {
                outputTS.append("\n\n" + lib.genTypeScript());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        
        return true;
    }
    public static int optionLength(String var0) {
        if (var0.equals("-d")) return 2;
        else if (var0.equals("-v")) return 2;
        else return 0;
    }
    
    public static boolean validOptions(String[][] options, DocErrorReporter reporter) {
        for (String[] option : options) {
            if (option[0].equals("-d")) {
                if (outDir != null) {
                    reporter.printError("outdir set more than once");
                    return false;
                }
                outDir = new File(option[1]);
                if (outDir.exists()) {
                    if (!outDir.isDirectory()) {
                        reporter.printError("output is an existing file");
                        return false;
                    }
                } else {
                    return outDir.mkdirs();
                }
            } else if (option[0].equals("-v")) {
                if (version != null) {
                    reporter.printError("tried to set version more than once");
                    return false;
                }
                version = option[1];
            }
        }
        if (version == null) version = "1.0";
        return true;
    }
    
    public static void genLib(ClassDoc lib, String libName, String[] languages) {
        libs.add(new LibDoc(lib, libName, languages));
    }
    
    public static void genEvent(ClassDoc event, String name, String oldName) {
    
    }
    
    public static void genClass(ClassDoc clazz) {
    
    }
    
    public static void deleteFolder(File folder) throws IOException {
        if (!folder.exists()) return;
        if (!folder.isDirectory()) if (!folder.delete()) throw new IOException("failed to delete " + folder.getAbsolutePath());
        else {
            File[] files = folder.listFiles();
            if (files != null) { //some JVMs return null for empty dirs
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteFolder(f);
                    } else {
                        if (!f.delete()) throw new IOException("failed to delete " + f.getAbsolutePath());
                    }
                }
            }
            if (!folder.delete()) throw new IOException("failed to delete " + folder.getAbsolutePath());
        }
    }
    
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }
}
