package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class PrestamosView {

    @FXML private ComboBox<String> cmbTipoSolicitante;
    @FXML private TextField txtRfc, txtCodigoAcademia, txtNombre, txtResponsableAcademia, txtNumArticulo, txtArticulo;
    @FXML private DatePicker dpFechaPrestamo, dpFechaDevolucion;
    @FXML private Button btnRegistrar, btnDevolver, btnEliminar;
    @FXML private TableView<Prestamo> tablaPrestamos;
    @FXML private TableColumn<Prestamo, String> colTipoSolicitante, colRfc, colCodigoAcademia, colNombre, colResponsableAcademia, colNumArticulo, colArticulo, colEstatus;
    @FXML private TableColumn<Prestamo, LocalDate> colFechaPrestamo, colFechaDevolucion;

    private final ObservableList<Prestamo> prestamos = FXCollections.observableArrayList();

    private final Map<String, String> usuariosPorRfc = new HashMap<>();
    private final Map<String, AcademiaInfo> academiasPorCodigo = new HashMap<>();
    private final Map<String, String> articulosPorNumero = new HashMap<>();

    public static class AcademiaInfo {
        private final String nombre;
        private final String responsable;
        public AcademiaInfo(String nombre, String responsable) {
            this.nombre = nombre;
            this.responsable = responsable;
        }
        public String getNombre() { return nombre; }
        public String getResponsable() { return responsable; }
    }

    @FXML
    public void initialize() {
        // Tipos de solicitante
        cmbTipoSolicitante.getItems().addAll("Persona", "Academia");

        // Datos ejemplo
        usuariosPorRfc.put("GARA800101HDF", "Ana García");
        usuariosPorRfc.put("PELU900505MDF", "Luis Pérez");
        usuariosPorRfc.put("TOMA970712HDF", "Martha Torres");

        academiasPorCodigo.put("ACD001", new AcademiaInfo("Matemáticas", "Dr. Mario Ruiz"));
        academiasPorCodigo.put("ACD002", new AcademiaInfo("Física", "Dra. Laura Robles"));

        articulosPorNumero.put("A001", "Laptop HP 01");
        articulosPorNumero.put("A002", "Proyector Epson");
        articulosPorNumero.put("A003", "Silla Oficina");

        // Columnas
        colTipoSolicitante.setCellValueFactory(data -> data.getValue().tipoSolicitanteProperty());
        colRfc.setCellValueFactory(data -> data.getValue().rfcProperty());
        colCodigoAcademia.setCellValueFactory(data -> data.getValue().codigoAcademiaProperty());
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colResponsableAcademia.setCellValueFactory(data -> data.getValue().responsableAcademiaProperty());
        colNumArticulo.setCellValueFactory(data -> data.getValue().numArticuloProperty());
        colArticulo.setCellValueFactory(data -> data.getValue().articuloProperty());
        colFechaPrestamo.setCellValueFactory(data -> data.getValue().fechaPrestamoProperty());
        colFechaDevolucion.setCellValueFactory(data -> data.getValue().fechaDevolucionProperty());
        colEstatus.setCellValueFactory(data -> data.getValue().estatusProperty());

        tablaPrestamos.setItems(prestamos);

        btnRegistrar.setOnAction(e -> registrarPrestamo());
        btnDevolver.setOnAction(e -> registrarDevolucion());
        btnEliminar.setOnAction(e -> eliminarPrestamo());

        cmbTipoSolicitante.valueProperty().addListener((obs, oldVal, newVal) -> ajustarCamposPorTipo());

        txtRfc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && "Persona".equals(cmbTipoSolicitante.getValue())) {
                buscarYAsignarNombrePersona();
            }
        });

        txtCodigoAcademia.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && "Academia".equals(cmbTipoSolicitante.getValue())) {
                buscarYAsignarNombreAcademia();
            }
        });

        txtNumArticulo.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                buscarYAsignarArticulo();
            }
        });

        ajustarCamposPorTipo(); // para dejar todo en estado inicial
    }

    private void ajustarCamposPorTipo() {
        String tipo = cmbTipoSolicitante.getValue();

        if ("Persona".equals(tipo)) {
            txtRfc.setDisable(false);
            txtCodigoAcademia.setDisable(true);
            txtResponsableAcademia.setDisable(true);
            txtNombre.setDisable(false);

            txtCodigoAcademia.clear();
            txtResponsableAcademia.clear();
        } else if ("Academia".equals(tipo)) {
            txtRfc.setDisable(true);
            txtCodigoAcademia.setDisable(false);
            txtResponsableAcademia.setDisable(false);
            txtNombre.setDisable(false);

            txtRfc.clear();
        } else {
            // Nada seleccionado
            txtRfc.setDisable(true);
            txtCodigoAcademia.setDisable(true);
            txtResponsableAcademia.setDisable(true);
            txtNombre.setDisable(true);
            txtRfc.clear();
            txtCodigoAcademia.clear();
            txtResponsableAcademia.clear();
            txtNombre.clear();
        }
    }

    private void buscarYAsignarNombrePersona() {
        String rfc = txtRfc.getText();
        if (rfc == null || rfc.trim().isEmpty()) {
            txtNombre.setText("");
            mostrarAlerta("Debes ingresar un RFC.");
            return;
        }
        String nombre = usuariosPorRfc.get(rfc.trim().toUpperCase());
        if (nombre != null) {
            txtNombre.setText(nombre);
        } else {
            txtNombre.setText("");
            mostrarAlerta("El RFC '" + rfc.trim().toUpperCase() + "' no está registrado. Verifica el RFC del usuario.");
        }
    }

    private void buscarYAsignarNombreAcademia() {
        String codigo = txtCodigoAcademia.getText();
        if (codigo == null || codigo.trim().isEmpty()) {
            txtNombre.setText("");
            txtResponsableAcademia.setText("");
            mostrarAlerta("Debes ingresar el código de la academia.");
            return;
        }
        AcademiaInfo info = academiasPorCodigo.get(codigo.trim().toUpperCase());
        if (info != null) {
            txtNombre.setText(info.getNombre());
            txtResponsableAcademia.setText(info.getResponsable());
        } else {
            txtNombre.setText("");
            txtResponsableAcademia.setText("");
            mostrarAlerta("El código de academia '" + codigo.trim().toUpperCase() + "' no está registrado. Verifica el código.");
        }
    }

    private void buscarYAsignarArticulo() {
        String numArticulo = txtNumArticulo.getText();
        if (numArticulo == null || numArticulo.trim().isEmpty()) {
            txtArticulo.setText("");
            mostrarAlerta("Debes ingresar el número de artículo.");
            return;
        }
        String articulo = articulosPorNumero.get(numArticulo.trim().toUpperCase());
        if (articulo != null) {
            txtArticulo.setText(articulo);
        } else {
            txtArticulo.setText("");
            mostrarAlerta("El número de artículo '" + numArticulo.trim().toUpperCase() + "' no está registrado. Verifica el número del artículo.");
        }
    }

    private void registrarPrestamo() {
        String tipo = cmbTipoSolicitante.getValue();
        String rfc = txtRfc.getText();
        String codigoAcademia = txtCodigoAcademia.getText();
        String nombre = txtNombre.getText();
        String responsableAcademia = txtResponsableAcademia.getText();
        String numArticulo = txtNumArticulo.getText();
        String articulo = txtArticulo.getText();
        LocalDate fechaPrestamo = dpFechaPrestamo.getValue();
        LocalDate fechaDevolucion = dpFechaDevolucion.getValue();

        // Validaciones personalizadas
        if (tipo == null) {
            mostrarAlerta("Selecciona el tipo de solicitante (Persona o Academia).");
            return;
        }
        if ("Persona".equals(tipo)) {
            if (rfc == null || rfc.trim().isEmpty()) {
                mostrarAlerta("Debes ingresar el RFC del usuario.");
                return;
            }
            if (!usuariosPorRfc.containsKey(rfc.trim().toUpperCase())) {
                mostrarAlerta("El RFC '" + rfc.trim().toUpperCase() + "' no está registrado.");
                return;
            }
            if (nombre == null || nombre.trim().isEmpty()) {
                mostrarAlerta("El campo nombre está vacío. Ingresa un RFC válido y registrado.");
                return;
            }
        } else if ("Academia".equals(tipo)) {
            if (codigoAcademia == null || codigoAcademia.trim().isEmpty()) {
                mostrarAlerta("Debes ingresar el código de la academia.");
                return;
            }
            if (!academiasPorCodigo.containsKey(codigoAcademia.trim().toUpperCase())) {
                mostrarAlerta("El código de academia '" + codigoAcademia.trim().toUpperCase() + "' no está registrado.");
                return;
            }
            if (nombre == null || nombre.trim().isEmpty()) {
                mostrarAlerta("El campo nombre de la academia está vacío. Ingresa un código válido y registrado.");
                return;
            }
            if (responsableAcademia == null || responsableAcademia.trim().isEmpty()) {
                mostrarAlerta("El campo responsable está vacío. Ingresa un código de academia válido.");
                return;
            }
        }

        if (numArticulo == null || numArticulo.trim().isEmpty()) {
            mostrarAlerta("Debes ingresar el número de artículo.");
            return;
        }
        if (!articulosPorNumero.containsKey(numArticulo.trim().toUpperCase())) {
            mostrarAlerta("El número de artículo '" + numArticulo.trim().toUpperCase() + "' no está registrado.");
            return;
        }
        if (articulo == null || articulo.trim().isEmpty()) {
            mostrarAlerta("El campo artículo está vacío. Ingresa un número de artículo válido y registrado.");
            return;
        }
        if (fechaPrestamo == null) {
            mostrarAlerta("Debes seleccionar la fecha de inicio del préstamo.");
            return;
        }

        prestamos.add(new Prestamo(
                tipo,
                "Persona".equals(tipo) ? rfc.trim().toUpperCase() : "",
                "Academia".equals(tipo) ? codigoAcademia.trim().toUpperCase() : "",
                nombre,
                "Academia".equals(tipo) ? responsableAcademia : "",
                numArticulo.trim().toUpperCase(),
                articulo,
                fechaPrestamo,
                fechaDevolucion,
                "Prestado"
        ));

        // Limpiar campos
        cmbTipoSolicitante.setValue(null);
        txtRfc.clear();
        txtCodigoAcademia.clear();
        txtNombre.clear();
        txtResponsableAcademia.clear();
        txtNumArticulo.clear();
        txtArticulo.clear();
        dpFechaPrestamo.setValue(null);
        dpFechaDevolucion.setValue(null);
        ajustarCamposPorTipo();
    }

    private void registrarDevolucion() {
        Prestamo sel = tablaPrestamos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Selecciona un préstamo para marcar como devuelto.");
            return;
        }
        sel.setEstatus("Devuelto");
        tablaPrestamos.refresh();
    }

    private void eliminarPrestamo() {
        Prestamo sel = tablaPrestamos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            mostrarAlerta("Selecciona un préstamo para eliminar.");
            return;
        }
        prestamos.remove(sel);
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // Modelo de préstamo (interno)
    public static class Prestamo {
        private final SimpleStringProperty tipoSolicitante;
        private final SimpleStringProperty rfc;
        private final SimpleStringProperty codigoAcademia;
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty responsableAcademia;
        private final SimpleStringProperty numArticulo;
        private final SimpleStringProperty articulo;
        private final ObjectProperty<LocalDate> fechaPrestamo;
        private final ObjectProperty<LocalDate> fechaDevolucion;
        private final SimpleStringProperty estatus;

        public Prestamo(String tipoSolicitante, String rfc, String codigoAcademia, String nombre, String responsableAcademia,
                        String numArticulo, String articulo, LocalDate fechaPrestamo, LocalDate fechaDevolucion, String estatus) {
            this.tipoSolicitante = new SimpleStringProperty(tipoSolicitante);
            this.rfc = new SimpleStringProperty(rfc);
            this.codigoAcademia = new SimpleStringProperty(codigoAcademia);
            this.nombre = new SimpleStringProperty(nombre);
            this.responsableAcademia = new SimpleStringProperty(responsableAcademia);
            this.numArticulo = new SimpleStringProperty(numArticulo);
            this.articulo = new SimpleStringProperty(articulo);
            this.fechaPrestamo = new SimpleObjectProperty<>(fechaPrestamo);
            this.fechaDevolucion = new SimpleObjectProperty<>(fechaDevolucion);
            this.estatus = new SimpleStringProperty(estatus);
        }

        public String getTipoSolicitante() { return tipoSolicitante.get(); }
        public SimpleStringProperty tipoSolicitanteProperty() { return tipoSolicitante; }
        public String getRfc() { return rfc.get(); }
        public SimpleStringProperty rfcProperty() { return rfc; }
        public String getCodigoAcademia() { return codigoAcademia.get(); }
        public SimpleStringProperty codigoAcademiaProperty() { return codigoAcademia; }
        public String getNombre() { return nombre.get(); }
        public SimpleStringProperty nombreProperty() { return nombre; }
        public String getResponsableAcademia() { return responsableAcademia.get(); }
        public SimpleStringProperty responsableAcademiaProperty() { return responsableAcademia; }
        public String getNumArticulo() { return numArticulo.get(); }
        public SimpleStringProperty numArticuloProperty() { return numArticulo; }
        public String getArticulo() { return articulo.get(); }
        public SimpleStringProperty articuloProperty() { return articulo; }
        public LocalDate getFechaPrestamo() { return fechaPrestamo.get(); }
        public ObjectProperty<LocalDate> fechaPrestamoProperty() { return fechaPrestamo; }
        public LocalDate getFechaDevolucion() { return fechaDevolucion.get(); }
        public ObjectProperty<LocalDate> fechaDevolucionProperty() { return fechaDevolucion; }
        public String getEstatus() { return estatus.get(); }
        public SimpleStringProperty estatusProperty() { return estatus; }
        public void setEstatus(String estatus) { this.estatus.set(estatus); }
    }
}
