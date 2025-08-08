package org.example.sici1.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import org.example.sici1.controller.UserSession;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ImageView logoImage;

    @FXML
    public void initialize() {
        if (logoImage != null) {
            double radius = Math.min(logoImage.getFitWidth(), logoImage.getFitHeight()) / 2;
            Circle clip = new Circle(radius, radius, radius);
            logoImage.setClip(clip);
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Usuario y contraseña son requeridos");
            return;
        }

        String role = authenticate(username, password);
        if (role == null) {
            showError("Credenciales incorrectas");
            passwordField.clear();
            return;
        }

        // Guardar usuario y rol en sesión
        UserSession.getInstance().setUser(username, role);

        redirectToDashboard(event);
    }

    // Simula autenticación y roles (reemplaza con tu lógica real)
    private String authenticate(String username, String password) {
        if ("admin".equals(username) && "admin123".equals(password)) {
            return "ADMIN";
        } else if ("empleado".equals(username) && "emp123".equals(password)) {
            return "EMPLEADO";
        }
        return null;
    }

    private void redirectToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/sici1/view/InventoryDashboard.fxml"));
            Parent root = loader.load();

            Stage dashboardStage = new Stage();
            dashboardStage.setTitle("Sistema de Inventario - SICI");
            dashboardStage.setScene(new Scene(root));
            dashboardStage.setMinWidth(1000);
            dashboardStage.setMinHeight(700);

            Image icon = new Image(getClass().getResourceAsStream("/org/example/sici1/1000371304.png"));
            dashboardStage.getIcons().add(icon);

            // Cerrar ventana login
            Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            dashboardStage.show();

        } catch (IOException e) {
            showError("Error al iniciar el sistema");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }
}
