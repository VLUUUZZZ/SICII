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

import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ImageView logoImage;

    // Dueño real de las tablas (USUARIOS, ROLES, USUARIO_ROL, etc.)
    private static final String SCHEMA_OWNER = "ADMIN"; // TODO: cambia si el owner no es ADMIN

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
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().trim() : "";

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

        // Guarda el usuario y su rol en una sesión simple
        UserSession.getInstance().setUser(username, role);
        redirectToDashboard(event);
    }

    private String authenticate(String username, String plainPassword) {
        final String sql =
                "SELECT u.id_usuario, u.hash_password, COALESCE(MAX(r.nombre), 'USUARIO') AS rol " +
                        "FROM " + SCHEMA_OWNER + ".usuarios u " +
                        "LEFT JOIN " + SCHEMA_OWNER + ".usuario_rol ur ON ur.id_usuario = u.id_usuario " +
                        "LEFT JOIN " + SCHEMA_OWNER + ".roles r ON r.id_rol = ur.id_rol " +
                        "WHERE UPPER(u.username) = UPPER(?) AND u.activo = 'S' " +
                        "GROUP BY u.id_usuario, u.hash_password";

        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String stored = rs.getString("hash_password");
                String role   = rs.getString("rol");

                if (!isPasswordValid(plainPassword, stored)) return null;
                return role != null ? role : "USUARIO";
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showError("Error de conexión con la base de datos");
            return null;
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            Connection cn = Conexion.conectar();
            try (Statement st = cn.createStatement()) {
                st.execute("ALTER SESSION SET CURRENT_SCHEMA=" + SCHEMA_OWNER);
            }
            return cn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver Oracle no encontrado", e);
        }
    }

    private boolean isPasswordValid(String plain, String stored) {
        if (stored == null) return false;

        return plain.equals(stored);
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

            // Cerrar ventana de login
            Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            currentStage.close();

            dashboardStage.show();

        } catch (IOException e) {
            showError("Error al iniciar el sistema");
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        if (errorLabel != null) errorLabel.setText(message);
        else System.err.println(message);
    }

    // ====== Utilidad de depuración (opcional) ======
    private void debugConnection(Connection cn) {
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT SYS_CONTEXT('USERENV','SERVICE_NAME') svc, " +
                             "       SYS_CONTEXT('USERENV','CURRENT_SCHEMA') sch " +
                             "FROM dual")) {
            if (rs.next()) {
                System.out.println("Servicio: " + rs.getString("svc") +
                        " | Esquema: " + rs.getString("sch"));
            }
        } catch (SQLException ignore) {}
    }
}