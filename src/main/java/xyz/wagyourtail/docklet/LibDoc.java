package xyz.wagyourtail.docklet;

import com.sun.javadoc.*;
import javafx.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class LibDoc {
    final ClassDoc clazz;
    final String libName;
    final String[] languages;
    
    public LibDoc(ClassDoc clazz, String libName, String[] languages) {
        this.clazz = clazz;
        this.libName = libName;
        this.languages = languages;
    }
    
    public String genTypeScript() {
        StringBuilder s = new StringBuilder("declare namespace ");
        s.append(libName);
        s.append(" {");
        Map<String, List<MethodDoc>> methods = new LinkedHashMap<>();
        for (MethodDoc method : clazz.methods()) {
            if (methods.containsKey(method.name())) {
                methods.get(method.name()).add(method);
            } else {
                List<MethodDoc> overloads = new LinkedList<>();
                overloads.add(method);
                TypeVariable[] types = method.typeParameters();
                List<String> genericParams = Arrays.stream(method.typeParameters()).map(e -> e.typeName()).collect(Collectors.toList());
                methods.put(method.name() + (genericParams.size() > 0 ? "<"+String.join(", ", genericParams)+">" : ""), overloads);
            }
        }
        for (Map.Entry<String, List<MethodDoc>> entry : methods.entrySet()) {
            s.append("\n\texport function ").append(entry.getKey());
            s.append(genMethodTypeScript(entry.getValue()));
        }
        s.append("\n}");
        return s.toString();
    }
    
    public static String genMethodTypeScript(List<MethodDoc> methods) {
        MethodDoc firstDoc = methods.get(0);
        StringBuilder s = new StringBuilder("(");
        boolean customParamsFlag = true;
        for (Tag ann : firstDoc.tags()) {
            if (ann.kind().equals("@custom.replaceParams")) {
                s.append(ann.text());
                customParamsFlag = false;
                break;
            }
        }
        if (customParamsFlag) {
            List<Pair<String, List<Parameter>>> params = new ArrayList<>();
            for (MethodDoc method : methods) {
                Parameter[] mparams = method.parameters();
                for (int i = 0; i < mparams.length; ++i) {
                    if (params.size() == i) {
                        params.add(new Pair<>(mparams[i].name(), new LinkedList<>()));
                    }
                    params.get(i).getValue().add(mparams[i]);
                }
            }
            List<String> paramvals = new LinkedList<>();
            for (Pair<String, List<Parameter>> pairs : params) {
                Set<String> types = new LinkedHashSet<>();
                for (Parameter p : pairs.getValue()) {
                    types.add(parseType(p.type()));
                }
                paramvals.add(pairs.getKey() + ": " + String.join(" | ", types));
            }
            s.append(String.join(", ", paramvals));
        }
        
        s.append("):").append(parseType(methods.get(0).returnType()));
        
        s.append(";");
        return s.toString();
    }
    
    public static String parseType(Type type) {
        ParameterizedType ptype = type.asParameterizedType();
        if (ptype != null) {
            List<String> types = Arrays.stream(ptype.typeArguments()).map(LibDoc::parseType).collect(Collectors.toList());
            return transformType(ptype.typeName()) + "<" + String.join(", ", types) + ">" + ptype.dimension();
        }
        return transformType(type.typeName()) + type.dimension();
    }
    
    public static String transformType(String type) {
        switch (type) {
            case "String":
                return "string";
            case "List":
                return "ArrayLike";
            case "int":
            case "Integer":
            case "float":
            case "Float":
            case "Long":
            case "long":
            case "short":
            case "Short":
            case "char":
            case "Character":
            case "byte":
            case "Byte":
            case "Double":
            case "double":
                return "number";
            case "Object":
                return "any";
            case "Boolean":
                return "boolean";
            default:
                return type;
        }
    }
}
