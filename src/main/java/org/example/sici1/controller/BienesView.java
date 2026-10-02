package org.example.sici1.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.sici1.data.BienRepository;
import org.example.sici1.model.Bien;
import org.example.sici1.util.Alertas;
import org.example.sici1.util.Formato;
import org.example.sici1.util.Sesion;
import org.example.sici1.util.Tablas;
import org.example.sici1.util.Tareas;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Catálogo de bienes. El administrador puede dar de alta y editar; los demás solo consultar.
 */
public class BienesView {

    private static final String TITULO = "Bienes";

    @FXML private TableView<Bien> tableBienes;
    @FXML private TableColumn<Bien, String> colCodigo, colDescripcion, colMarca, colModelo, colSerie, colEstado;
    @FXML private Button btnNuevo, btnBuscarCodigo;
    @FXML private TextField txtBuscarCodigo;
    @FXML private Label lblVacio;

    private final boolean admin = Sesion.esAdmin();
    private final BienRepository repo = BienRepository.INSTANCIA;
    private final ObservableList<Bien> bienes = FXCollections.observableArrayList();
    private final FilteredList<Bien> filtrados = new FilteredList<>(bienes, b -> true);

    @FXML
    public void initialize() {
        Tablas.texto(colCodigo, Bien::codigo);
        Tablas.texto(colDescripcion, Bien::descripcion);
        Tablas.texto(colMarca, Bien::marca);
        Tablas.texto(colModelo, Bien::modelo);
        Tablas.texto(colSerie, Bien::numeroSerie);
        Tablas.texto(colEstado, Bien::estado);
        tableBienes.setItems(filtrados);

        btnNuevo.setVisible(admin);
        if (admin) btnNuevo.setOnAction(e -> mostrarFormulario(null));

        btnBuscarCodigo.setOnAction(e -> filtrar());
        txtBuscarCodigo.textProperty().addListener((obs, a, b) -> filtrar());

        // Doble clic: el administrador edita, los demás solo ven el detalle
        Tablas.dobleClic(tableBienes, b -> { if (admin) mostrarFormulario(b); else verDetalle(b); });

        cargar();
    }

    private void cargar() {
        Tareas.ejecutar(TITULO, repo::listar, lista -> {
            bienes.setAll(lista);
            lblVacio.setText("No hay bienes registrados");
        });
    }

    private void filtrar() {
        String texto = Formato.clave(txtBuscarCodigo.getText());
        filtrados.setPredicate(b -> texto.isEmpty() || Formato.clave(b.codigo()).contains(texto));
    }

    private void verDetalle(Bien b) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ver bien");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox contenido = new VBox(10, new Label(
                "Código: " + b.codigo() + "\n"
                        + "Descripción: " + b.descripcion() + "\n"
                        + "Marca: " + b.marca() + "\n"
                        + "Modelo: " + b.modelo() + "\n"
                        + "N. Serie: " + b.numeroSerie() + "\n"
                        + "Estado: " + b.estado()));
        if (b.imagen() != null) contenido.getChildren().add(vistaImagen(b.imagen(), 220));

        dialog.getDialogPane().setContent(contenido);
        dialog.showAndWait();
    }

    private void mostrarFormulario(Bien existente) {
        boolean nuevo = existente == null;
        Dialog<Bien> dialog = new Dialog<>();
        dialog.setTitle(nuevo ? "Nuevo bien" : "Editar bien");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtCodigo = new TextField(nuevo ? "" : existente.codigo());
        TextField txtDescripcion = new TextField(nuevo ? "" : existente.descripcion());
        TextField txtMarca = new TextField(nuevo ? "" : existente.marca());
        TextField txtModelo = new TextField(nuevo ? "" : existente.modelo());
        TextField txtSerie = new TextField(nuevo ? "" : existente.numeroSerie());
        ComboBox<String> cmbEstado = new ComboBox<>(FXCollections.observableArrayList(Bien.ESTADOS));
        cmbEstado.setValue(nuevo ? Bien.OPERATIVO : existente.estado());

        byte[][] imagen = {nuevo ? null : existente.imagen()};
        ImageView vista = vistaImagen(imagen[0], 110);
        Button btnImagen = new Button("Seleccionar imagen");
        Button btnQuitar = new Button("Quitar");
        btnImagen.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar imagen");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"));
            File archivo = fc.showOpenDialog(btnImagen.getScene().getWindow());
            if (archivo == null) return;
            if (archivo.length() > BienRepository.MAX_BYTES_IMAGEN) {
                Alertas.aviso(TITULO, "La imagen pesa demasiado (máximo " + BienRepository.MAX_BYTES_IMAGEN / 1024 + " KB).");
                return;
            }
            try {
                imagen[0] = Files.readAllBytes(archivo.toPath());
                vista.setImage(new Image(new ByteArrayInputStream(imagen[0])));
            } catch (IOException ex) {
                Alertas.error(TITULO, "No se pudo leer la imagen.");
            }
        });
        btnQuitar.setOnAction(ev -> { imagen[0] = null; vista.setImage(null); });

        GridPane grid = new GridPane();
        grid.setVgap(12);
        grid.setHgap(10);
        int r = 0;
        grid.addRow(r++, new Label("Código:"), txtCodigo);
        grid.addRow(r++, new Label("Descripción:"), txtDescripcion);
        grid.addRow(r++, new Label("Marca:"), txtMarca);
        grid.addRow(r++, new Label("Modelo:"), txtModelo);
        grid.addRow(r++, new Label("N. Serie:"), txtSerie);
        grid.addRow(r++, new Label("Estado:"), cmbEstado);
        grid.addRow(r++, new Label("Imagen:"), vista);
        grid.add(new HBox(8, btnImagen, btnQuitar), 1, r);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> bt == ButtonType.OK
                ? new Bien(nuevo ? null : existente.id(), txtCodigo.getText(), txtDescripcion.getText(),
                        txtMarca.getText(), txtModelo.getText(), txtSerie.getText(), cmbEstado.getValue(), imagen[0])
                : null);

        dialog.showAndWait().ifPresent(b -> Tareas.ejecutar(TITULO, () -> repo.guardar(b), guardado -> cargar()));
    }

    private static ImageView vistaImagen(byte[] bytes, double tamano) {
        ImageView vista = new ImageView();
        vista.setFitWidth(tamano);
        vista.setFitHeight(tamano);
        vista.setPreserveRatio(true);
        if (bytes != null) vista.setImage(new Image(new ByteArrayInputStream(bytes)));
        return vista;
    }
}
