package org.example.sici1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/sici1/view/login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 500, 620); // Tamaño sugerido para login, ajusta a tu gusto

        primaryStage.setTitle("Inicio de Sesión - Sistema de Inventario");
        primaryStage.setScene(scene);

        // ICONO personalizado
        Image icon = new Image(getClass().getResourceAsStream("/org/example/sici1/1000371304.png"));
        primaryStage.getIcons().add(icon);

        primaryStage.setResizable(false); // Opcional, para evitar que lo hagan más chico
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
