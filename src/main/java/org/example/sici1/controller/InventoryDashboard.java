package org.example.sici1.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.sici1.data.CatalogoRepository;
import org.example.sici1.model.Usuario;
import org.example.sici1.util.Alertas;
import org.example.sici1.util.Sesion;
import org.example.sici1.util.Ventanas;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class InventoryDashboard {

    @FXML private BorderPane rootPane;
    @FXML private StackPane contentArea;
    @FXML private VBox sidebar;
    @FXML private Label lblRol;

    @FXML private Button btnCerrarSesion, btnEdificios, btnEspacio, btnUnidadAdministrativa,
            btnPuesto, btnBienes, btnInventario, btnEmpleado;

    private Button seleccionado;
    private final Map<Button, Supplier<Parent>> vistas = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        vistas.put(btnEdificios, () -> catalogo("Edificios", "Edificio/Área", CatalogoRepository.EDIFICIOS));
        vistas.put(btnEspacio, () -> Ventanas.cargar("UbicacionesView", null));
        vistas.put(btnUnidadAdministrativa,
                () -> catalogo("Unidades Administrativas", "Nombre", CatalogoRepository.UNIDADES_ADMINISTRATIVAS));
        vistas.put(btnPuesto, () -> catalogo("Puestos", "Nombre", CatalogoRepository.PUESTOS));
        vistas.put(btnBienes, () -> Ventanas.cargar("BienesView", null));
        vistas.put(btnInventario, () -> Ventanas.cargar("AsignacionesView", null));
        vistas.put(btnEmpleado, () -> Ventanas.cargar("UsuariosView", null));

        aplicarPermisos();
        vistas.forEach((boton, vista) -> boton.setOnAction(e -> abrir(boton, vista)));
        btnCerrarSesion.setOnAction(this::cerrarSesion);
        mostrarBienvenida();

        rootPane.widthProperty().addListener((obs, a, ancho) -> ajustarMenu(ancho.doubleValue()));
    }

    private static Parent catalogo(String titulo, String columna, CatalogoRepository repo) {
        return Ventanas.cargar("CatalogoView", new CatalogoController(titulo, columna, repo));
    }

    private void aplicarPermisos() {
        boolean admin = Sesion.esAdmin();
        btnEmpleado.setVisible(admin);
        btnEmpleado.setManaged(admin);
        if (lblRol != null) {
            lblRol.setText(admin ? "Administrador"
                    : Usuario.ROL_EMPLEADO.equals(Sesion.rol()) ? "Empleado" : "Usuario");
        }
    }

    private void abrir(Button boton, Supplier<Parent> vista) {
        if (boton == btnEmpleado && !Sesion.esAdmin()) {
            Alertas.aviso("Acceso denegado", "No tienes permisos para acceder a esta sección.");
            return;
        }
        marcar(boton);
        try {
            contentArea.getChildren().setAll(vista.get());
        } catch (RuntimeException e) {
            e.printStackTrace();
            contentArea.getChildren().setAll(new Label("No se pudo cargar la vista: " + boton.getText()));
        }
    }

    private void marcar(Button boton) {
        if (seleccionado != null) seleccionado.getStyleClass().remove("selected");
        if (boton != null && !boton.getStyleClass().contains("selected")) boton.getStyleClass().add("selected");
        seleccionado = boton;
    }

    private void mostrarBienvenida() {
        VBox caja = new VBox(24);
        caja.setStyle("-fx-alignment: center; -fx-padding: 70 0 0 0;");

        var logo = getClass().getResourceAsStream("/org/example/sici1/1000371304.png");
        if (logo != null) {
            ImageView imagen = new ImageView(new Image(logo));
            imagen.setFitHeight(120);
            imagen.setPreserveRatio(true);
            caja.getChildren().add(imagen);
        }

        Label titulo = new Label("Sistema de Inventario Institucional");
        titulo.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #4361EE;");
        String nombre = Sesion.usuario() == null ? "" : ", " + Sesion.usuario().nombre();
        Label desc = new Label("Bienvenido" + nombre + ". Gestione los bienes y espacios de su institución.");
        desc.setStyle("-fx-font-size: 17px; -fx-text-fill: #4A5568; -fx-padding: 10 60 0 60;");

        caja.getChildren().addAll(titulo, desc);
        contentArea.getChildren().setAll(caja);
        marcar(null);
    }

    private void cerrarSesion(ActionEvent event) {
        if (!Alertas.confirmar("Cerrar sesión", "¿Está seguro que desea cerrar sesión?")) return;
        Sesion.cerrar();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Ventanas.mostrarLogin(stage);
    }

    private void ajustarMenu(double ancho) {
        if (sidebar == null) return;
        double w = ancho < 800 ? 56 : 260;
        sidebar.setMinWidth(w);
        sidebar.setPrefWidth(w);
        sidebar.setMaxWidth(w);
    }
}
