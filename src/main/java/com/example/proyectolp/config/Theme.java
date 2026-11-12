package com.example.proyectolp.config;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.paint.Color;

/** Utilidades compartidas para el tema oscuro. */
public final class Theme {
    private static final String CSS = Theme.class.getResource("/com/example/proyectolp/css/theme.css").toExternalForm();
    private Theme() {}
    public static void apply(Scene scene) { if (!scene.getStylesheets().contains(CSS)) scene.getStylesheets().add(CSS); scene.setFill(Color.web("#141416")); }
    public static void apply(DialogPane pane) { if (!pane.getStylesheets().contains(CSS)) pane.getStylesheets().add(CSS); }
    public static void apply(Dialog<?> dialog) { apply(dialog.getDialogPane()); }
}
