package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

import java.util.Optional;

public class ActivosView {

    @FXML private TableView<RegistroInventario> tableActivos;
    @FXML private TableColumn<RegistroInventario, String> colEmpleado, colEspacio, colFecha, colEstado;
    @FXML private TableColumn<RegistroInventario, Void> colSwitch, colFoto;
    @FXML private TextField txtBuscar;
    @FXML private Button btnRegistrar, btnBuscar;

    private final ObservableList<RegistroInventario> registros = FXCollections.observableArrayList();
    private final FilteredList<RegistroInventario> registrosFiltrados = new FilteredList<>(registros, p -> true);

    // Detecta si es EMPLEADO
    private final boolean esEmpleado = "EMPLEADO".equalsIgnoreCase(UserSession.getInstance().getRole());

    @FXML
    public void initialize() {
        configurarTabla();
        configurarBuscador();

        btnRegistrar.setVisible(!esEmpleado);
        colSwitch.setVisible(!esEmpleado);

        tableActivos.setItems(registrosFiltrados);

        if (!esEmpleado) {
            btnRegistrar.setOnAction(e -> mostrarDialogoRegistro());
        }

        cargarDatosEjemplo();
    }

    private void configurarTabla() {
        colEmpleado.setCellValueFactory(data -> data.getValue().empleadoProperty());
        colEspacio.setCellValueFactory(data -> data.getValue().espacioProperty());
        colFecha.setCellValueFactory(data -> data.getValue().fechaProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());

        // Switch de estado SOLO para admin
        colSwitch.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                checkBox.setDisable(esEmpleado);
                checkBox.setOnAction(e -> {
                    RegistroInventario reg = getTableView().getItems().get(getIndex());
                    reg.setEstado(checkBox.isSelected() ? "Activo" : "Inactivo");
                    tableActivos.refresh();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    RegistroInventario reg = getTableView().getItems().get(getIndex());
                    checkBox.setSelected("Activo".equalsIgnoreCase(reg.getEstado()));
                    setGraphic(checkBox);
                }
            }
        });

        // Botón de Foto
        colFoto.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("📷");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    RegistroInventario ri = getTableView().getItems().get(getIndex());
                    mostrarDialogoFoto(ri);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void configurarBuscador() {
        btnBuscar.setOnAction(e -> buscarRegistros());
        txtBuscar.setOnAction(e -> buscarRegistros());
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarRegistros());
    }

    private void buscarRegistros() {
        String textoBusqueda = txtBuscar.getText().toLowerCase().trim();
        registrosFiltrados.setPredicate(registro -> textoBusqueda.isEmpty() ||
                registro.getEmpleado().toLowerCase().contains(textoBusqueda));
    }

    private void mostrarDialogoRegistro() {
        Dialog<RegistroInventario> dialog = new Dialog<>();
        dialog.setTitle("Registrar Nuevo Activo");
        dialog.setHeaderText(null);

        ButtonType botonGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(botonGuardar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtEmpleado = new TextField();
        txtEmpleado.setPromptText("Nombre del empleado");
        TextField txtEspacio = new TextField();
        txtEspacio.setPromptText("Espacio físico");
        TextField txtFecha = new TextField();
        txtFecha.setPromptText("DD/MM/AAAA");

        grid.add(new Label("Empleado:"), 0, 0);
        grid.add(txtEmpleado, 1, 0);
        grid.add(new Label("Espacio:"), 0, 1);
        grid.add(txtEspacio, 1, 1);
        grid.add(new Label("Fecha:"), 0, 2);
        grid.add(txtFecha, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(boton -> {
            if (boton == botonGuardar) {
                return new RegistroInventario(
                        txtEmpleado.getText().trim(),
                        txtEspacio.getText().trim(),
                        txtFecha.getText().trim(),
                        "Activo"
                );
            }
            return null;
        });

        Optional<RegistroInventario> resultado = dialog.showAndWait();
        resultado.ifPresent(registro -> {
            if (registro.getEmpleado().isEmpty() || registro.getEspacio().isEmpty() || registro.getFecha().isEmpty()) {
                mostrarAlerta("Error", "Todos los campos son obligatorios", Alert.AlertType.ERROR);
                return;
            }
            registros.add(registro);
            buscarRegistros();
        });
    }

    private void mostrarDialogoFoto(RegistroInventario registro) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Foto del Activo");
        alert.setHeaderText("Foto para: " + registro.getEmpleado());
        alert.setContentText("Aquí iría la implementación para tomar/mostrar fotos");
        alert.showAndWait();
    }

    private void cargarDatosEjemplo() {
        registros.addAll(
                new RegistroInventario("Hugo Omar", "Aula 1", "25/08/2025", "Activo"),
                new RegistroInventario("Martha Fabiola", "Aula 2", "30/08/2025", "Activo")
        );
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static class RegistroInventario {
        private final SimpleStringProperty empleado, espacio, fecha, estado;

        public RegistroInventario(String empleado, String espacio, String fecha, String estado) {
            this.empleado = new SimpleStringProperty(empleado);
            this.espacio = new SimpleStringProperty(espacio);
            this.fecha = new SimpleStringProperty(fecha);
            this.estado = new SimpleStringProperty(estado);
        }

        public String getEmpleado() { return empleado.get(); }
        public String getEspacio() { return espacio.get(); }
        public String getFecha() { return fecha.get(); }
        public String getEstado() { return estado.get(); }

        public void setEstado(String estado) { this.estado.set(estado); }

        public SimpleStringProperty empleadoProperty() { return empleado; }
        public SimpleStringProperty espacioProperty() { return espacio; }
        public SimpleStringProperty fechaProperty() { return fecha; }
        public SimpleStringProperty estadoProperty() { return estado; }
    }
}
