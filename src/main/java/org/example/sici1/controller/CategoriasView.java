package org.example.sici1.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class CategoriasView {

    @FXML private TableView<Categoria> tableCategorias;
    @FXML private TableColumn<Categoria, Integer> colId;
    @FXML private TableColumn<Categoria, String> colNombre;
    @FXML private TableColumn<Categoria, String> colDescripcion;
    @FXML private TableColumn<Categoria, Void> colAcciones;

    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private Button btnNuevaCategoria;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private Button btnEliminar;

    private final ObservableList<Categoria> categorias = FXCollections.observableArrayList();
    private Categoria categoriaSeleccionada = null;
    private int idAutoIncrement = 1;

    @FXML
    public void initialize() {
        // Inicializar columnas
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colDescripcion.setCellValueFactory(data -> data.getValue().descripcionProperty());

        // Columna de acciones con botones Editar y Borrar
        colAcciones.setCellFactory(tc -> new TableCell<>() {
            final Button btnEditar = new Button("Editar");
            final Button btnBorrar = new Button("Borrar");
            final HBox actionBox = new HBox(8, btnEditar, btnBorrar);
            {
                btnEditar.getStyleClass().add("table-edit-button");
                btnBorrar.getStyleClass().add("table-delete-button");

                btnEditar.setOnAction(e -> {
                    Categoria cat = getTableView().getItems().get(getIndex());
                    setFormularioParaEditar(cat);
                });

                btnBorrar.setOnAction(e -> {
                    Categoria cat = getTableView().getItems().get(getIndex());
                    categorias.remove(cat);
                    if (cat == categoriaSeleccionada) {
                        limpiarFormulario();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });

        // Datos simulados
        categorias.addAll(
                new Categoria(idAutoIncrement++, "Tecnología", "Dispositivos electrónicos, computadoras, etc."),
                new Categoria(idAutoIncrement++, "Mobiliario", "Escritorios, sillas, pizarras, etc."),
                new Categoria(idAutoIncrement++, "Laboratorio", "Microscopios, material de laboratorio."),
                new Categoria(idAutoIncrement++, "Biblioteca", "Libros, revistas, material bibliográfico.")
        );
        tableCategorias.setItems(categorias);

        // Selección en la tabla para edición rápida
        tableCategorias.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                setFormularioParaEditar(newSel);
            }
        });

        btnNuevaCategoria.setOnAction(e -> {
            limpiarFormulario();
            tableCategorias.getSelectionModel().clearSelection();
        });

        btnGuardar.setOnAction(e -> guardarCategoria());

        btnEliminar.setOnAction(e -> {
            if (categoriaSeleccionada != null) {
                categorias.remove(categoriaSeleccionada);
                limpiarFormulario();
                tableCategorias.getSelectionModel().clearSelection();
            }
        });

        btnCancelar.setOnAction(e -> {
            limpiarFormulario();
            tableCategorias.getSelectionModel().clearSelection();
        });

        limpiarFormulario();
    }

    private void setFormularioParaEditar(Categoria categoria) {
        if (categoria == null) return;
        categoriaSeleccionada = categoria;
        txtNombre.setText(categoria.getNombre());
        txtDescripcion.setText(categoria.getDescripcion());
        btnGuardar.setText("Actualizar");
        btnEliminar.setDisable(false);
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtDescripcion.clear();
        btnGuardar.setText("Guardar");
        btnEliminar.setDisable(true);
        categoriaSeleccionada = null;
    }

    private void guardarCategoria() {
        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();

        if (nombre.isEmpty() || descripcion.isEmpty()) {
            showAlert("Debe llenar ambos campos.");
            return;
        }

        if (categoriaSeleccionada == null) {
            // Nueva categoría
            categorias.add(new Categoria(idAutoIncrement++, nombre, descripcion));
        } else {
            // Editar existente
            categoriaSeleccionada.setNombre(nombre);
            categoriaSeleccionada.setDescripcion(descripcion);
            tableCategorias.refresh();
        }
        limpiarFormulario();
        tableCategorias.getSelectionModel().clearSelection();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // --- Clase interna para la tabla ---
    public static class Categoria {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty descripcion;

        public Categoria(int id, String nombre, String descripcion) {
            this.id = new SimpleIntegerProperty(id);
            this.nombre = new SimpleStringProperty(nombre);
            this.descripcion = new SimpleStringProperty(descripcion);
        }

        public int getId() { return id.get(); }
        public SimpleIntegerProperty idProperty() { return id; }

        public String getNombre() { return nombre.get(); }
        public void setNombre(String nombre) { this.nombre.set(nombre); }
        public SimpleStringProperty nombreProperty() { return nombre; }

        public String getDescripcion() { return descripcion.get(); }
        public void setDescripcion(String descripcion) { this.descripcion.set(descripcion); }
        public SimpleStringProperty descripcionProperty() { return descripcion; }
    }
}
