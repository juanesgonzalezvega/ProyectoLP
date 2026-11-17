package com.example.proyectolp;

import com.example.proyectolp.controller.ContextMenuSupport;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ContextMenusConsistencyTest {
    private static final Path UI = Path.of("src/main/resources/com/example/proyectolp/ui");
    private static final Path THEME = Path.of("src/main/resources/com/example/proyectolp/css/theme.css");

    @Test
    void menuActionsResolveToFxmlControllerMethods() throws Exception {
        try (Stream<Path> files = Files.list(UI)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".fxml")).toList()) {
                var document = parse(file);
                Element root = document.getDocumentElement();
                String controllerName = root.getAttribute("fx:controller");
                if (controllerName.isBlank()) continue;
                Class<?> controller = Class.forName(controllerName);
                NodeList menuItems = document.getElementsByTagName("MenuItem");
                for (int i = 0; i < menuItems.getLength(); i++) {
                    Element item = (Element) menuItems.item(i);
                    String action = item.getAttribute("onAction");
                    if (action.isBlank()) continue;
                    assertTrue(action.startsWith("#"), file + " usa un manejador FXML inválido: " + action);
                    String methodName = action.substring(1);
                    assertNotNull(findNoArgumentMethod(controller, methodName),
                            file + " no encuentra " + controllerName + "." + methodName + "()");
                }
            }
        }
    }

    @Test
    void deleteMenuItemsAreLastAndSeparated() throws Exception {
        try (Stream<Path> files = Files.list(UI)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".fxml")).toList()) {
                NodeList menus = parse(file).getElementsByTagName("ContextMenu");
                for (int i = 0; i < menus.getLength(); i++) {
                    Element contextMenu = (Element) menus.item(i);
                    Element items = firstChildElement(contextMenu, "items");
                    if (items == null) continue;
                    var children = elementChildren(items);
                    for (int itemIndex = 0; itemIndex < children.size(); itemIndex++) {
                        Element item = children.get(itemIndex);
                        if (!"Eliminar".equals(item.getAttribute("text"))) continue;
                        assertEquals(itemIndex, children.size() - 1,
                                file + " tiene acciones después de Eliminar");
                        assertTrue(itemIndex > 0 && "SeparatorMenuItem".equals(children.get(itemIndex - 1).getTagName()),
                                file + " no separa Eliminar de las otras acciones");
                    }
                }
            }
        }
    }

    @Test
    void fxmlMenusHaveNoInlineStylesAndUseThemeClasses() throws Exception {
        String theme = Files.readString(THEME);
        Set<String> selectors = new HashSet<>();
        Matcher selectorMatcher = Pattern.compile("\\.([A-Za-z][A-Za-z0-9_-]*)").matcher(theme);
        while (selectorMatcher.find()) selectors.add(selectorMatcher.group(1));

        try (Stream<Path> files = Files.list(UI)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".fxml")).toList()) {
                var document = parse(file);
                NodeList elements = document.getElementsByTagName("*");
                for (int i = 0; i < elements.getLength(); i++) {
                    Element element = (Element) elements.item(i);
                    assertFalse(element.hasAttribute("style"), file + " usa estilos en línea");
                    if (!Set.of("ContextMenu", "Menu", "MenuItem", "SeparatorMenuItem").contains(element.getTagName())) continue;
                    String classes = element.getAttribute("styleClass");
                    for (String styleClass : classes.split("\\s+")) {
                        if (!styleClass.isBlank()) {
                            assertTrue(selectors.contains(styleClass),
                                    file + " usa una clase de menú sin estilo en theme.css: " + styleClass);
                        }
                    }
                }
            }
        }
    }

    @Test
    void habitCompletionMenuLabelReflectsTodayState() {
        assertEquals("Marcar cumplido hoy", ContextMenuSupport.habitCompletionLabel(false));
        assertEquals("Desmarcar hoy", ContextMenuSupport.habitCompletionLabel(true));
    }

    private static java.lang.reflect.Method findNoArgumentMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                // Continúa con la clase base por si el controlador hereda el manejador.
            }
        }
        return null;
    }

    private static org.w3c.dom.Document parse(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(file.toFile());
    }

    private static Element firstChildElement(Element parent, String name) {
        for (Element child : elementChildren(parent)) {
            if (name.equals(child.getTagName())) return child;
        }
        return null;
    }

    private static java.util.List<Element> elementChildren(Element parent) {
        java.util.List<Element> result = new java.util.ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element) result.add(element);
        }
        return result;
    }
}
