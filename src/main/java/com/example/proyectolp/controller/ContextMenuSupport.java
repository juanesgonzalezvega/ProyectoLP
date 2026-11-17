package com.example.proyectolp.controller;

import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Side;

import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class ContextMenuSupport {
    private ContextMenuSupport() {
    }

    public static <T> void installTableRows(TableView<T> table, ContextMenu rowMenu, ContextMenu emptyMenu) {
        table.setContextMenu(emptyMenu);
        table.setRowFactory(view -> {
            TableRow<T> row = new TableRow<>();
            row.itemProperty().addListener((observable, oldItem, item) ->
                    row.setContextMenu(item == null ? null : rowMenu));
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() != MouseButton.SECONDARY) return;
                if (row.isEmpty()) table.getSelectionModel().clearSelection();
                else table.getSelectionModel().select(row.getIndex());
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldItem, item) -> {
            if (item == null && rowMenu != null) rowMenu.hide();
        });
        installKeyboard(table, rowMenu, emptyMenu, () -> table.getSelectionModel().getSelectedItem() != null);
    }

    public static <T> void installListRows(ListView<T> list, ContextMenu rowMenu, ContextMenu emptyMenu) {
        var existingFactory = list.getCellFactory();
        list.setContextMenu(emptyMenu);
        list.setCellFactory(view -> {
            ListCell<T> cell = existingFactory == null ? new ListCell<>() : existingFactory.call(view);
            cell.itemProperty().addListener((observable, oldItem, item) ->
                    cell.setContextMenu(item == null ? null : rowMenu));
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() != MouseButton.SECONDARY) return;
                if (cell.isEmpty()) list.getSelectionModel().clearSelection();
                else list.getSelectionModel().select(cell.getIndex());
            });
            return cell;
        });
        list.getSelectionModel().selectedItemProperty().addListener((observable, oldItem, item) -> {
            if (item == null && rowMenu != null) rowMenu.hide();
        });
        installKeyboard(list, rowMenu, emptyMenu, () -> list.getSelectionModel().getSelectedItem() != null);
    }

    public static void registerNode(Node node, ContextMenu menu, Runnable selectNode) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(menu);
        node.setFocusTraversable(true);
        node.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY && selectNode != null) selectNode.run();
        });
        node.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event -> {
            if (selectNode != null) selectNode.run();
            menu.show(node, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        node.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isContextMenuKey(event)) {
                menu.show(node, Side.BOTTOM, 0, 0);
                event.consume();
            }
        });
    }

    public static void bindDisable(MenuItem item, Button button) {
        item.disableProperty().bind(button.disableProperty());
    }

    public static void bindDisable(MenuItem item, ObservableBooleanValue disabled) {
        item.disableProperty().bind(disabled);
    }

    public static void bindText(MenuItem item, Button button) {
        item.textProperty().bind(button.textProperty());
    }

    public static String habitCompletionLabel(boolean completedToday) {
        return completedToday ? "Desmarcar hoy" : "Marcar cumplido hoy";
    }

    public static void copyTextToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static <T> void installKeyboard(Control control, ContextMenu rowMenu, ContextMenu emptyMenu,
                                            BooleanSupplier hasSelection) {
        control.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!isContextMenuKey(event)) return;
            if (hasSelection.getAsBoolean() && rowMenu != null) {
                rowMenu.show(selectedCell(control), Side.BOTTOM, 0, 0);
            } else if (emptyMenu != null) {
                emptyMenu.show(control, Side.BOTTOM, 0, 0);
            }
            event.consume();
        });
    }

    private static Node selectedCell(Control control) {
        String selector = control instanceof TableView<?> ? ".table-row-cell:selected" : ".list-cell:selected";
        return control.lookupAll(selector).stream()
                .filter(IndexedCell.class::isInstance)
                .map(IndexedCell.class::cast)
                .filter(cell -> !cell.isEmpty())
                .map(cell -> (Node) cell)
                .findFirst()
                .orElse(control);
    }

    private static boolean isContextMenuKey(KeyEvent event) {
        return event.getCode() == KeyCode.CONTEXT_MENU
                || event.getCode() == KeyCode.F10 && event.isShiftDown();
    }
}
