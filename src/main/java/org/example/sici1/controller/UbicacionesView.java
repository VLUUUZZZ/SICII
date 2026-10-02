package org.example.sici1.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.example.sici1.data.CatalogoRepository;
import org.example.sici1.data.EspacioRepository;
import org.example.sici1.model.Catalogo;
import org.example.sici1.model.Espacio;
import org.example.sici1.util.Formato;
import org.example.sici1.util.Sesion;
import org.example.sici1.util.Tablas;
import org.example.sici1.util.Tareas;

import java.util.List;

/**
 * Espacios físicos (aulas, oficinas, laboratorios) y el edificio donde están.
 */
public class UbicacionesView {

    private static final String TITULO = "Espacios";

    @FXML private TableView<Espacio> tablaUbicaciones;
    @FXML private TableColumn<Espacio, String> colNombre, colDescripcion, colEdificio, colEstado;
    @FXML private TextField txtBuscar;
    @FXML private Button btnAgregar, btnEditar, btnBuscar;
    @FXML private Label lblVacio;

    private final boolean admin = Sesion.esAdmin();
    private final EspacioRepository repo = EspacioRepository.INSTANCIA;
    private final ObservableList<Espacio> espacios = FXCollections.observableArrayList();
    private final FilteredList<Espacio> filtrados = new FilteredList<>(espacios, e -> true);

    @FXML
    public void initialize() {
        Tablas.texto(colNombre, Espacio::nombre);
        Tablas.texto(colDescripcion, Espacio::codigo);
        Tablas.texto(colEdificio, Espacio::edificioNombre);
        Tablas.texto(colEstado, e -> Formato.estado(e.activo()));
        tablaUbicaciones.setItems(filtrados);

        btnAgregar.setVisible(admin);
        btnEditar.setVisible(admin);

        btnBuscar.setOnAction(e -> filtrar());
        txtBuscar.textProperty().addListener((obs, a, b) -> filtrar());

        if (admin) {
            btnAgregar.setOnAction(e -> abrirDialogo(null));
            btnEditar.setOnAction(e -> {
                Espacio sel = Tablas.seleccionado(tablaUbicaciones, TITULO);
                if (sel != null) abrirDialogo(sel);
            });
            Tablas.dobleClic(tablaUbicaciones, this::abrirDialogo);
        }

        cargar();
    }

    private void cargar() {
        Tareas.ejecutar(TITULO, repo::listar, lista -> {
            espacios.setAll(lista);
            lblVacio.setText("No hay espacios registrados");
        });
    }

    private void filtrar() {
        String texto = Formato.clave(txtBuscar.getText());
        filtrados.setPredicate(e -> texto.isEmpty()
                || Formato.clave(e.nombre()).contains(texto)
                || Formato.clave(e.codigo()).contains(texto)
                || Formato.clave(e.edificioNombre()).contains(texto));
    }

    /** Primero trae los edificios para el combo y después abre el formulario. */
    private void abrirDialogo(Espacio existente) {
        Tareas.ejecutar(TITULO, CatalogoRepository.EDIFICIOS::listar, edificios -> mostrarDialogo(existente, edificios));
    }

    private void mostrarDialogo(Espacio existente, List<Catalogo> edificios) {
        boolean nuevo = existente == null;
        Dialog<Espacio> dialog = new Dialog<>();
        dialog.setTitle(nuevo ? "Agregar espacio" : "Editar espacio");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtNombre = new TextField(nuevo ? "" : existente.nombre());
        TextField txtCodigo = new TextField(nuevo ? "" : existente.codigo());
        // Se ofrecen los edificios activos y, si se está editando, también el actual aunque esté inactivo.
        ComboBox<Catalogo> cmbEdificio = new ComboBox<>(FXCollections.observableArrayList(
                edificios.stream().filter(ed -> ed.activo() || (!nuevo && ed.id().equals(existente.edificioId()))).toList()));
        cmbEdificio.setPrefWidth(250);
        if (!nuevo) {
            cmbEdificio.getItems().stream().filter(ed -> ed.id().equals(existente.edificioId()))
                    .findFirst().ifPresent(cmbEdificio::setValue);
        }
        CheckBox chkActivo = new CheckBox("Activo");
        chkActivo.setSelected(nuevo || existente.activo());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Nombre:"), txtNombre);
        grid.addRow(1, new Label("Código:"), txtCodigo);
        grid.addRow(2, new Label("Edificio:"), cmbEdificio);
        grid.addRow(3, new Label("Estado:"), chkActivo);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            Catalogo edificio = cmbEdificio.getValue();
            return new Espacio(nuevo ? null : existente.id(), txtNombre.getText(), txtCodigo.getText(),
                    edificio == null ? null : edificio.id(), edificio == null ? null : edificio.nombre(),
                    chkActivo.isSelected());
        });

        dialog.showAndWait().ifPresent(e -> Tareas.ejecutar(TITULO, () -> repo.guardar(e), guardado -> cargar()));
    }
}
