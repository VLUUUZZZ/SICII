package org.example.sici1;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Stage;
import org.example.sici1.util.Ventanas;

public class Main extends Application {

    private static HostServices servicios;

    /** Permite abrir archivos con el programa predeterminado del sistema. */
    public static HostServices servicios() { return servicios; }

    @Override
    public void start(Stage primaryStage) {
        servicios = getHostServices();
        Ventanas.mostrarLogin(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
