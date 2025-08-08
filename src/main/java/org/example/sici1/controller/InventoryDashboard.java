package org.example.sici1.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import org.example.sici1.controller.UserSession;

import java.io.IOException;
import java.util.*;

public class InventoryDashboard {

    private static final String VIEWS_PATH = "/org/example/sici1/view/";

    @FXML private BorderPane rootPane;
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private Label lblRol;

    @FXML private Button btnCerrarSesion, btnEdificios, btnEspacio, btnUnidadAdministrativa,
            btnPuesto, btnBienes, btnInventario, btnEmpleado;

    private Button currentSelected;
    private final Map<Button, String> buttonViewMap = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        initializeButtonMapping();
        applyRoleBasedAccess();
        setupMenuButtonActions();
        setupLogoutButton();
        showWelcomeView();
        rootPane.widthProperty().addListener((obs, oldVal, newVal) -> handleResponsiveSidebar(newVal.doubleValue()));
    }

    private void initializeButtonMapping() {
        buttonViewMap.put(btnEdificios, "EdificiosView");
        buttonViewMap.put(btnEspacio, "UbicacionesView");
        buttonViewMap.put(btnUnidadAdministrativa, "UnidadAdministrativaView");
        buttonViewMap.put(btnPuesto, "PuestoView");
        buttonViewMap.put(btnBienes, "BienesView");
        buttonViewMap.put(btnInventario, "AsignacionesView");
        buttonViewMap.put(btnEmpleado, "UsuariosView");
    }

    private void applyRoleBasedAccess() {
        String role = UserSession.getInstance().getRole();
        if (role == null) return;

        if (role.equalsIgnoreCase("EMPLEADO")) {
            btnEdificios.setVisible(true);
            btnEspacio.setVisible(true);
            btnUnidadAdministrativa.setVisible(true);
            btnPuesto.setVisible(true);
            btnBienes.setVisible(true);
            btnInventario.setVisible(true);
            btnEmpleado.setVisible(false); // restringido
            if (lblRol != null) lblRol.setText("Empleado");
        } else if (role.equalsIgnoreCase("ADMIN")) {
            btnEdificios.setVisible(true);
            btnEspacio.setVisible(true);
            btnUnidadAdministrativa.setVisible(true);
            btnPuesto.setVisible(true);
            btnBienes.setVisible(true);
            btnInventario.setVisible(true);
            btnEmpleado.setVisible(true);
            if (lblRol != null) lblRol.setText("Administrador");
        } else {
            if (lblRol != null) lblRol.setText("Usuario");
        }
    }

    private void setupMenuButtonActions() {
        buttonViewMap.forEach((button, viewName) -> {
            if (button != null) {
                button.setOnAction(event -> {
                    highlightSelectedButton(button);
                    switchView(viewName);
                });
            }
        });
    }

    private void highlightSelectedButton(Button selected) {
        if (currentSelected != null && currentSelected != selected) {
            currentSelected.getStyleClass().remove("selected");
        }
        if (!selected.getStyleClass().contains("selected")) {
            selected.getStyleClass().add("selected");
        }
        currentSelected = selected;
    }

    private void showWelcomeView() {
        VBox welcomeBox = new VBox(24);
        welcomeBox.setStyle("-fx-alignment: center; -fx-padding: 70 0 0 0;");
        Image logoImg = null;
        try {
            logoImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/org/example/sici1/1000371304.png")));
        } catch (Exception ignored) {}
        if (logoImg != null) {
            ImageView logo = new ImageView(logoImg);
            logo.setFitHeight(120);
            logo.setPreserveRatio(true);
            welcomeBox.getChildren().add(logo);
        }
        Label title = new Label("Sistema de Inventario Institucional");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #4361EE;");
        Label desc = new Label("Gestione los bienes y espacios de su institución de forma profesional, segura y eficiente.");
        desc.setStyle("-fx-font-size: 17px; -fx-text-fill: #4A5568; -fx-padding: 10 60 0 60;");
        welcomeBox.getChildren().addAll(title, desc);
        contentArea.getChildren().setAll(welcomeBox);
        if (currentSelected != null) {
            currentSelected.getStyleClass().remove("selected");
            currentSelected = null;
        }
    }

    private void switchView(String viewName) {
        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource(VIEWS_PATH + viewName + ".fxml")));
            contentArea.getChildren().setAll(view);
        } catch (IOException | NullPointerException e) {
            contentArea.getChildren().setAll(
                    new Label("No se pudo cargar la vista: " + viewName)
            );
        }
    }

    private void setupLogoutButton() {
        btnCerrarSesion.setOnAction(this::handleLogout);
    }

    private void handleLogout(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Está seguro que desea cerrar sesión?",
                ButtonType.YES, ButtonType.NO);

        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            UserSession.getInstance().clear();
            try {
                returnToLoginScreen(event);
            } catch (IOException ex) {
                contentArea.getChildren().setAll(new Label("Error al cerrar sesión"));
            }
        }
    }

    private void returnToLoginScreen(ActionEvent event) throws IOException {
        Stage currentStage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                getClass().getResource("/org/example/sici1/view/login.fxml")));
        Stage loginStage = new Stage();
        loginStage.setTitle("Inicio de Sesión");
        Scene scene = new Scene(root, 500, 620);
        loginStage.setScene(scene);
        loginStage.getIcons().add(new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/org/example/sici1/1000371304.png"))
        ));
        loginStage.setResizable(false);
        loginStage.setOnCloseRequest(ev -> javafx.application.Platform.exit());
        currentStage.close();
        loginStage.show();
    }

    private void handleResponsiveSidebar(double width) {
        if (sidebar == null) return;
        if (width < 800) {
            sidebar.setMinWidth(56);
            sidebar.setPrefWidth(56);
            sidebar.setMaxWidth(56);
        } else {
            sidebar.setMinWidth(260);
            sidebar.setPrefWidth(260);
            sidebar.setMaxWidth(260);
        }
    }
}
