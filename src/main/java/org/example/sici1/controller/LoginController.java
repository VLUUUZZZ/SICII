package org.example.sici1.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.example.sici1.data.DatosIniciales;
import org.example.sici1.data.UsuarioRepository;
import org.example.sici1.util.Sesion;
import org.example.sici1.util.Tareas;
import org.example.sici1.util.Ventanas;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ImageView logoImage;
    @FXML private Button loginButton;

    @FXML
    public void initialize() {
        if (logoImage != null) {
            double radio = Math.min(logoImage.getFitWidth(), logoImage.getFitHeight()) / 2;
            logoImage.setClip(new Circle(radio, radio, radio));
        }
        prepararBaseDeDatos();
    }

    /** Conecta con Firebase y, si la base está vacía, crea el administrador y datos de ejemplo. */
    private void prepararBaseDeDatos() {
        ocupado(true, "Conectando con Firebase...");
        Tareas.ejecutar("Conexión", DatosIniciales::cargarSiEsNecesario,
                contrasenaAdmin -> {
                    ocupado(false, "");
                    if (contrasenaAdmin != null) {
                        mensaje("Base nueva. Entra con: " + DatosIniciales.ADMIN_USERNAME + " / " + contrasenaAdmin, false);
                    }
                },
                () -> ocupado(false, "Sin conexión con Firebase. Revisa la configuración e intenta de nuevo."));
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        // Enter en la contraseña también llega aquí: no intentar mientras se conecta o verifica.
        if (loginButton != null && loginButton.isDisabled()) return;

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String contrasena = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || contrasena.isEmpty()) {
            mensaje("Usuario y contraseña son requeridos", true);
            return;
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        ocupado(true, "Verificando...");
        Tareas.ejecutar("Inicio de sesión",
                () -> UsuarioRepository.INSTANCIA.autenticar(username, contrasena),
                usuario -> {
                    ocupado(false, "");
                    if (usuario.isEmpty()) {
                        mensaje("Credenciales incorrectas o usuario inactivo", true);
                        passwordField.clear();
                        return;
                    }
                    Sesion.iniciar(usuario.get());
                    Ventanas.mostrarPanel(stage);
                },
                () -> ocupado(false, ""));
    }

    private void ocupado(boolean ocupado, String texto) {
        if (loginButton != null) loginButton.setDisable(ocupado);
        mensaje(texto, false);
    }

    private void mensaje(String texto, boolean esError) {
        errorLabel.setStyle(esError ? "-fx-text-fill: red;" : "-fx-text-fill: #4A5568;");
        errorLabel.setText(texto);
    }
}
