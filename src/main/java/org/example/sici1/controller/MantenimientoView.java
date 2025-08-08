package org.example.sici1.controller;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;

public class MantenimientoView {

    @FXML private TableView<Mantenimiento> tablaMantenimientos;
    @FXML private TableColumn<Mantenimiento, String> colActivo, colTipo, colResponsable, colEstado, colNotas;
    @FXML private TableColumn<Mantenimiento, LocalDate> colFechaProgramada, colFechaRealizado;
    @FXML private TableColumn<Mantenimiento, Double> colCosto;

    @FXML private ComboBox<String> cmbActivo, cmbTipo, cmbEstado;
    @FXML private DatePicker dpFechaProgramada, dpFechaRealizado;
    @FXML private TextField txtResponsable, txtCosto;
    @FXML private TextArea txtNotas;
    @FXML private Button btnGuardar, btnEliminar;

    private final ObservableList<Mantenimiento> mantenimientos = FXCollections.observableArrayList();
    private Mantenimiento mantenimientoSeleccionado = null;

    @FXML
    public void initialize() {
        // Opciones demo (reemplaza por datos reales)
        cmbActivo.getItems().addAll("Laptop HP 01", "Proyector Epson", "Silla Oficina");
        cmbTipo.getItems().addAll("Preventivo", "Correctivo");
        cmbEstado.getItems().addAll("Pendiente", "Completado", "Cancelado");

        colActivo.setCellValueFactory(data -> data.getValue().activoProperty());
        colTipo.setCellValueFactory(data -> data.getValue().tipoProperty());
        colFechaProgramada.setCellValueFactory(data -> data.getValue().fechaProgramadaProperty());
        colFechaRealizado.setCellValueFactory(data -> data.getValue().fechaRealizadoProperty());
        colResponsable.setCellValueFactory(data -> data.getValue().responsableProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());
        colCosto.setCellValueFactory(data -> data.getValue().costoProperty().asObject());
        colNotas.setCellValueFactory(data -> data.getValue().notasProperty());

        tablaMantenimientos.setItems(mantenimientos);

        // Simulación de datos
        mantenimientos.addAll(
                new Mantenimiento("Laptop HP 01", "Preventivo", LocalDate.of(2024,7,1), null, "Carlos Ruiz", "Pendiente", 400.0, "Cambio de pasta térmica"),
                new Mantenimiento("Proyector Epson", "Correctivo", LocalDate.of(2024,6,12), LocalDate.of(2024,6,15), "Martha Torres", "Completado", 900.0, "Cambio de lámpara")
        );

        tablaMantenimientos.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) setFormularioParaEditar(newSel);
        });

        btnGuardar.setOnAction(e -> guardarMantenimiento());
        btnEliminar.setOnAction(e -> eliminarMantenimiento());
    }

    private void setFormularioParaEditar(Mantenimiento m) {
        mantenimientoSeleccionado = m;
        cmbActivo.setValue(m.getActivo());
        cmbTipo.setValue(m.getTipo());
        dpFechaProgramada.setValue(m.getFechaProgramada());
        dpFechaRealizado.setValue(m.getFechaRealizado());
        txtResponsable.setText(m.getResponsable());
        cmbEstado.setValue(m.getEstado());
        txtCosto.setText(m.getCosto() != null ? String.valueOf(m.getCosto()) : "");
        txtNotas.setText(m.getNotas());
    }

    private void limpiarFormulario() {
        mantenimientoSeleccionado = null;
        cmbActivo.setValue(null);
        cmbTipo.setValue(null);
        dpFechaProgramada.setValue(null);
        dpFechaRealizado.setValue(null);
        txtResponsable.clear();
        cmbEstado.setValue(null);
        txtCosto.clear();
        txtNotas.clear();
    }

    private void guardarMantenimiento() {
        String activo = cmbActivo.getValue();
        String tipo = cmbTipo.getValue();
        LocalDate fechaProg = dpFechaProgramada.getValue();
        LocalDate fechaReal = dpFechaRealizado.getValue();
        String responsable = txtResponsable.getText();
        String estado = cmbEstado.getValue();
        Double costo = null;
        try { if (!txtCosto.getText().isEmpty()) costo = Double.parseDouble(txtCosto.getText()); } catch(Exception ignored){}

        String notas = txtNotas.getText();

        if (activo == null || tipo == null || fechaProg == null || responsable.isEmpty() || estado == null) {
            mostrarAlerta("Completa todos los campos obligatorios.");
            return;
        }

        if (mantenimientoSeleccionado == null) {
            mantenimientos.add(new Mantenimiento(activo, tipo, fechaProg, fechaReal, responsable, estado, costo, notas));
        } else {
            mantenimientoSeleccionado.setActivo(activo);
            mantenimientoSeleccionado.setTipo(tipo);
            mantenimientoSeleccionado.setFechaProgramada(fechaProg);
            mantenimientoSeleccionado.setFechaRealizado(fechaReal);
            mantenimientoSeleccionado.setResponsable(responsable);
            mantenimientoSeleccionado.setEstado(estado);
            mantenimientoSeleccionado.setCosto(costo);
            mantenimientoSeleccionado.setNotas(notas);
            tablaMantenimientos.refresh();
        }
        limpiarFormulario();
        tablaMantenimientos.getSelectionModel().clearSelection();
    }

    private void eliminarMantenimiento() {
        Mantenimiento sel = tablaMantenimientos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Selecciona un mantenimiento para eliminar.");
            return;
        }
        mantenimientos.remove(sel);
        limpiarFormulario();
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static class Mantenimiento {
        private final SimpleStringProperty activo, tipo, responsable, estado, notas;
        private final ObjectProperty<LocalDate> fechaProgramada, fechaRealizado;
        private final SimpleDoubleProperty costo;

        public Mantenimiento(String activo, String tipo, LocalDate fechaProgramada, LocalDate fechaRealizado,
                             String responsable, String estado, Double costo, String notas) {
            this.activo = new SimpleStringProperty(activo);
            this.tipo = new SimpleStringProperty(tipo);
            this.fechaProgramada = new SimpleObjectProperty<>(fechaProgramada);
            this.fechaRealizado = new SimpleObjectProperty<>(fechaRealizado);
            this.responsable = new SimpleStringProperty(responsable);
            this.estado = new SimpleStringProperty(estado);
            this.costo = new SimpleDoubleProperty(costo != null ? costo : 0.0);
            this.notas = new SimpleStringProperty(notas);
        }
        public String getActivo() { return activo.get(); }
        public void setActivo(String v) { activo.set(v); }
        public SimpleStringProperty activoProperty() { return activo; }

        public String getTipo() { return tipo.get(); }
        public void setTipo(String v) { tipo.set(v); }
        public SimpleStringProperty tipoProperty() { return tipo; }

        public LocalDate getFechaProgramada() { return fechaProgramada.get(); }
        public void setFechaProgramada(LocalDate v) { fechaProgramada.set(v); }
        public ObjectProperty<LocalDate> fechaProgramadaProperty() { return fechaProgramada; }

        public LocalDate getFechaRealizado() { return fechaRealizado.get(); }
        public void setFechaRealizado(LocalDate v) { fechaRealizado.set(v); }
        public ObjectProperty<LocalDate> fechaRealizadoProperty() { return fechaRealizado; }

        public String getResponsable() { return responsable.get(); }
        public void setResponsable(String v) { responsable.set(v); }
        public SimpleStringProperty responsableProperty() { return responsable; }

        public String getEstado() { return estado.get(); }
        public void setEstado(String v) { estado.set(v); }
        public SimpleStringProperty estadoProperty() { return estado; }

        public Double getCosto() { return costo.get(); }
        public void setCosto(Double v) { costo.set(v != null ? v : 0.0); }
        public SimpleDoubleProperty costoProperty() { return costo; }

        public String getNotas() { return notas.get(); }
        public void setNotas(String v) { notas.set(v); }
        public SimpleStringProperty notasProperty() { return notas; }
    }
}
