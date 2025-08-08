package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MovimientosView {

    @FXML private TableView<Movimiento> tableMovimientos;
    @FXML private TableColumn<Movimiento, String> colTipo, colActivo, colCantidad, colFecha, colResponsable, colMotivo;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private TextField txtActivo, txtCantidad, txtFecha, txtResponsable, txtMotivo;
    @FXML private Button btnAgregar, btnEliminar;

    private final ObservableList<Movimiento> movimientos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Opciones del ComboBox (solo en Java)
        cmbTipo.getItems().addAll("Entrada", "Salida");

        // Configuración columnas
        colTipo.setCellValueFactory(data -> data.getValue().tipoProperty());
        colActivo.setCellValueFactory(data -> data.getValue().activoProperty());
        colCantidad.setCellValueFactory(data -> data.getValue().cantidadProperty());
        colFecha.setCellValueFactory(data -> data.getValue().fechaProperty());
        colResponsable.setCellValueFactory(data -> data.getValue().responsableProperty());
        colMotivo.setCellValueFactory(data -> data.getValue().motivoProperty());

        // Datos simulados
        movimientos.addAll(
                new Movimiento("Entrada", "Laptop Dell", "2", "2024-06-15", "Juan Pérez", "Compra nueva"),
                new Movimiento("Salida", "Microscopio", "1", "2024-06-17", "Ana Torres", "Préstamo laboratorio"),
                new Movimiento("Entrada", "Silla ergonómica", "10", "2024-07-01", "Carlos Ruiz", "Donación")
        );
        tableMovimientos.setItems(movimientos);

        // Agregar movimiento
        btnAgregar.setOnAction(e -> {
            if (cmbTipo.getValue() != null && !txtActivo.getText().isEmpty() && !txtCantidad.getText().isEmpty()) {
                movimientos.add(new Movimiento(
                        cmbTipo.getValue(),
                        txtActivo.getText(),
                        txtCantidad.getText(),
                        txtFecha.getText(),
                        txtResponsable.getText(),
                        txtMotivo.getText()
                ));
                cmbTipo.setValue(null);
                txtActivo.clear();
                txtCantidad.clear();
                txtFecha.clear();
                txtResponsable.clear();
                txtMotivo.clear();
            }
        });

        // Eliminar movimiento seleccionado
        btnEliminar.setOnAction(e -> {
            Movimiento selected = tableMovimientos.getSelectionModel().getSelectedItem();
            if (selected != null) {
                movimientos.remove(selected);
            }
        });
    }

    // Clase interna de datos de Movimiento
    public static class Movimiento {
        private final SimpleStringProperty tipo, activo, cantidad, fecha, responsable, motivo;

        public Movimiento(String tipo, String activo, String cantidad, String fecha, String responsable, String motivo) {
            this.tipo = new SimpleStringProperty(tipo);
            this.activo = new SimpleStringProperty(activo);
            this.cantidad = new SimpleStringProperty(cantidad);
            this.fecha = new SimpleStringProperty(fecha);
            this.responsable = new SimpleStringProperty(responsable);
            this.motivo = new SimpleStringProperty(motivo);
        }

        public String getTipo() { return tipo.get(); }
        public String getActivo() { return activo.get(); }
        public String getCantidad() { return cantidad.get(); }
        public String getFecha() { return fecha.get(); }
        public String getResponsable() { return responsable.get(); }
        public String getMotivo() { return motivo.get(); }

        public SimpleStringProperty tipoProperty() { return tipo; }
        public SimpleStringProperty activoProperty() { return activo; }
        public SimpleStringProperty cantidadProperty() { return cantidad; }
        public SimpleStringProperty fechaProperty() { return fecha; }
        public SimpleStringProperty responsableProperty() { return responsable; }
        public SimpleStringProperty motivoProperty() { return motivo; }
    }
}
