package org.example.sici1.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Carga de vistas FXML y cambio entre la pantalla de login y el panel principal.
 */
public final class Ventanas {

    private static final String VISTAS = "/org/example/sici1/view/";
    private static final String ICONO_LOGIN = "/org/example/sici1/1000371305.jpg";
    private static final String ICONO_PANEL = "/org/example/sici1/1000371304.png";

    private Ventanas() {}

    public static void mostrarLogin(Stage stage) {
        Parent root = cargar("Login", null);
        stage.setTitle("Inicio de Sesión - SICI");
        cambiarIcono(stage, ICONO_LOGIN);
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setScene(new Scene(root, 500, 620));
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    public static void mostrarPanel(Stage stage) {
        Parent root = cargar("InventoryDashboard", null);
        stage.setTitle("Sistema de Inventario - SICI");
        cambiarIcono(stage, ICONO_PANEL);
        stage.setScene(new Scene(root));
        stage.setResizable(true);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Carga una vista por nombre (sin ".fxml"). Si se pasa un controlador se usa ese
     * en lugar del indicado en el FXML.
     */
    public static Parent cargar(String vista, Object controlador) {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                Ventanas.class.getResource(VISTAS + vista + ".fxml"), "No existe la vista " + vista));
        if (controlador != null) loader.setController(controlador);
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la vista " + vista, e);
        }
    }

    private static void cambiarIcono(Stage stage, String recurso) {
        stage.getIcons().setAll(new Image(Objects.requireNonNull(Ventanas.class.getResourceAsStream(recurso))));
    }
}
