package com.example.proyectolp;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

class ThemeConsistencyTest {
    private final Path ui=Path.of("src/main/resources/com/example/proyectolp/ui");
    private final Path css=Path.of("src/main/resources/com/example/proyectolp/css/theme.css");
    @Test void fxmlHasNoInlineStylesAndRootsLoadTheme() throws Exception {
        String theme=Files.readString(css); Pattern root=Pattern.compile("<(?:VBox|BorderPane|GridPane|StackPane|DialogPane|AnchorPane|HBox|TabPane)\\b([^>]*)>");
        for(Path file: (Iterable<Path>)Files.list(ui)::iterator){if(!file.toString().endsWith(".fxml"))continue;String xml=Files.readString(file);assertFalse(Pattern.compile("\\sstyle\\s*=").matcher(xml).find(),file+" tiene estilo en l?nea");if(!xml.contains("<fx:include")){Matcher m=root.matcher(xml);assertTrue(m.find(),file+" no declara ra?z soportada");assertTrue(m.group(1).contains("@../css/theme.css"),file+" no carga el tema");}}
    }
    @Test void allFxmlStyleClassesExistInTheme() throws Exception {
        String theme=Files.readString(css);Set<String> selectors=new HashSet<>();Matcher cssClass=Pattern.compile("\\.([A-Za-z][A-Za-z0-9_-]*)").matcher(theme);while(cssClass.find())selectors.add(cssClass.group(1));Pattern attrs=Pattern.compile("styleClass=\"([^\"]+)\"");
        for(Path file:(Iterable<Path>)Files.list(ui)::iterator){if(!file.toString().endsWith(".fxml"))continue;Matcher m=attrs.matcher(Files.readString(file));while(m.find())for(String name:m.group(1).split("\\s+"))assertTrue(selectors.contains(name),"Selector inexistente: "+name+" en "+file);}
    }
    @Test void javaHasNoInlineStyleCalls() throws Exception {
        try(var paths=Files.walk(Path.of("src/main/java"))){for(Path file:(Iterable<Path>)paths.filter(p->p.toString().endsWith(".java"))::iterator)assertFalse(Files.readString(file).contains("setStyle("),file+" contiene setStyle");}
    }
}
