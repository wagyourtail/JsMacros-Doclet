package xyz.wagyourtail.docklet.parsers;

import com.sun.javadoc.*;
import xyz.wagyourtail.docklet.AbstractParser;

import java.util.*;
import java.util.stream.Collectors;

public class LibraryParser extends AbstractParser {
    final String libName;
    
    public LibraryParser(ClassDoc clazz, String libName) {
        super ((clazz.subclassOf(clazz.findClass("xyz.wagyourtail.jsmacros.core.library.PerExecLanguageLibrary")) ||
            clazz.subclassOf(clazz.findClass("xyz.wagyourtail.jsmacros.core.library.PerLanguageLibrary"))) ?
            clazz.superclassType().asParameterizedType().typeArguments()[0].asClassDoc() : clazz);
        this.libName = libName;
    }
    
    public String genTypeScript() {
        StringBuilder s = new StringBuilder();
        Tag[] classtags = clazz.inlineTags();
        if (classtags.length > 0) s.append(AbstractParser.genCommentTypeScript(classtags, false, 0));
        s.append("declare namespace ").append(libName).append(" {\n");
        
        s.append(insertEachLine(genMethodTS(true), "\t"));
        s.append("\n}");
        return s.toString();
    }
    
}
