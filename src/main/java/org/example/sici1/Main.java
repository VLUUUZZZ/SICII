package org.example.sici1;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.sici1.util.Ventanas;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        Ventanas.mostrarLogin(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
