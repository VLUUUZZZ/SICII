package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.geometry.Insets;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


public class UbicacionesView {

    @FXML private TableView<Ubicacion> tablaUbicaciones;
    @FXML private TableColumn<Ubicacion, String> colNombre, colDescripcion, colEdificio, colEstado;
    @FXML private TextField txtBuscar;
    @FXML private Button btnAgregar, btnEditar, btnBuscar;

    private final ObservableList<Ubicacion> ubicaciones = FXCollections.observableArrayList();
    private final FilteredList<Ubicacion> ubicacionesFiltradas = new FilteredList<>(ubicaciones, p -> true);

    private static final String ARCHIVO_UBICACIONES =
            System.getProperty("user.home") + File.separator + "Documents" + File.separator + "ubicaciones.csv";
    private static final String ARCHIVO_EDIFICIOS =
            System.getProperty("user.home") + File.separator + "Documents" + File.separator + "edificios.csv";

    private final boolean esEmpleado = "EMPLEADO".equalsIgnoreCase(UserSession.getInstance().getRole());

    @FXML
    public void initialize() {
        configurarTabla();

        tablaUbicaciones.setItems(ubicacionesFiltradas);

        // Permisos según rol
        btnAgregar.setVisible(!esEmpleado);
        btnEditar.setVisible(!esEmpleado);
        colEstado.setVisible(true); // Estado siempre visible (cambiar si solo admin debe ver)

        // Buscador funcional
        btnBuscar.setOnAction(e -> buscarUbicaciones());
        txtBuscar.setOnAction(e -> buscarUbicaciones());
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarUbicaciones());

        // Admin: activar alta/edición
        if (!esEmpleado) {
            btnAgregar.setOnAction(e -> mostrarDialogoAgregar());
            btnEditar.setOnAction(e -> mostrarDialogoEditar());
        }

        cargarDatos();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colDescripcion.setCellValueFactory(data -> data.getValue().descripcionProperty());
        colEdificio.setCellValueFactory(data -> data.getValue().edificioProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());
    }

    private void buscarUbicaciones() {
        String textoBusqueda = txtBuscar.getText().toLowerCase().trim();

        ubicacionesFiltradas.setPredicate(ubicacion -> {
            if (textoBusqueda.isEmpty()) return true;
            return ubicacion.getNombre().toLowerCase().contains(textoBusqueda)
                    || ubicacion.getDescripcion().toLowerCase().contains(textoBusqueda)
                    || ubicacion.getEdificio().toLowerCase().contains(textoBusqueda);
        });
    }

    private void mostrarDialogoAgregar() {
        Dialog<Ubicacion> dialog = crearDialogoUbicacion("Agregar Ubicación", null);
        Optional<Ubicacion> resultado = dialog.showAndWait();

        resultado.ifPresent(nuevaUbicacion -> {
            if (ubicacionExiste(nuevaUbicacion.getNombre())) {
                mostrarAlerta("Error", "La ubicación ya existe", Alert.AlertType.ERROR);
                return;
            }
            ubicaciones.add(nuevaUbicacion);
            guardarDatos();
            buscarUbicaciones();
        });
    }

    private void mostrarDialogoEditar() {
        Ubicacion seleccionada = tablaUbicaciones.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Advertencia", "Seleccione una ubicación para editar", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Ubicacion> dialog = crearDialogoUbicacion("Editar Ubicación", seleccionada);
        Optional<Ubicacion> resultado = dialog.showAndWait();

        resultado.ifPresent(ubicacionEditada -> {
            // Si cambia el nombre, verificar duplicado
            if (!ubicacionEditada.getNombre().equalsIgnoreCase(seleccionada.getNombre())
                    && ubicacionExiste(ubicacionEditada.getNombre())) {
                mostrarAlerta("Error", "Ya existe una ubicación con ese nombre", Alert.AlertType.ERROR);
                return;
            }
            seleccionada.setNombre(ubicacionEditada.getNombre());
            seleccionada.setDescripcion(ubicacionEditada.getDescripcion());
            seleccionada.setEdificio(ubicacionEditada.getEdificio());
            seleccionada.setEstado(ubicacionEditada.getEstado());

            tablaUbicaciones.refresh();
            guardarDatos();
            buscarUbicaciones();
        });
    }

    private Dialog<Ubicacion> crearDialogoUbicacion(String titulo, Ubicacion ubicacionExistente) {
        Dialog<Ubicacion> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);

        ButtonType botonConfirmar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(botonConfirmar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField txtNombre = new TextField();
        TextField txtDescripcion = new TextField();
        ComboBox<String> cmbEdificio = new ComboBox<>();
        cmbEdificio.getItems().addAll(cargarEdificiosDisponibles());
        CheckBox chkActivo = new CheckBox("Activo");

        if (ubicacionExistente != null) {
            txtNombre.setText(ubicacionExistente.getNombre());
            txtDescripcion.setText(ubicacionExistente.getDescripcion());
            cmbEdificio.setValue(ubicacionExistente.getEdificio());
            chkActivo.setSelected("Activo".equalsIgnoreCase(ubicacionExistente.getEstado()));
        } else {
            chkActivo.setSelected(true);
        }

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(txtDescripcion, 1, 1);
        grid.add(new Label("Edificio:"), 0, 2);
        grid.add(cmbEdificio, 1, 2);
        grid.add(new Label("Estado:"), 0, 3);
        grid.add(chkActivo, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(boton -> {
            if (boton == botonConfirmar) {
                return new Ubicacion(
                        txtNombre.getText().trim(),
                        txtDescripcion.getText().trim(),
                        cmbEdificio.getValue(),
                        chkActivo.isSelected() ? "Activo" : "Inactivo"
                );
            }
            return null;
        });

        return dialog;
    }

    private boolean ubicacionExiste(String nombre) {
        return ubicaciones.stream()
                .anyMatch(u -> u.getNombre().equalsIgnoreCase(nombre));
    }

    private ObservableList<String> cargarEdificiosDisponibles() {
        ObservableList<String> edificios = FXCollections.observableArrayList();
        File archivo = new File(ARCHIVO_EDIFICIOS);
        if (!archivo.exists()) return edificios;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(",", 2);
                String nombre = desescapa(partes[0]);
                edificios.add(nombre);
            }
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo leer el archivo de edificios", Alert.AlertType.ERROR);
        }
        return edificios;
    }

    private void cargarDatos() {
        ubicaciones.clear();
        File archivo = new File(ARCHIVO_UBICACIONES);
        if (!archivo.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(",", 4);
                if (partes.length >= 3) {
                    String estado = partes.length == 4 ? desescapa(partes[3]) : "Activo";
                    ubicaciones.add(new Ubicacion(
                            desescapa(partes[0]),
                            desescapa(partes[1]),
                            desescapa(partes[2]),
                            estado
                    ));
                }
            }
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo leer el archivo de ubicaciones", Alert.AlertType.ERROR);
        }
    }

    private void guardarDatos() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ARCHIVO_UBICACIONES), StandardCharsets.UTF_8))) {
            for (Ubicacion u : ubicaciones) {
                writer.write(String.join(",",
                        escapa(u.getNombre()),
                        escapa(u.getDescripcion()),
                        escapa(u.getEdificio()),
                        escapa(u.getEstado())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            mostrarAlerta("Error", "No se pudo guardar el archivo de ubicaciones", Alert.AlertType.ERROR);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private String escapa(String valor) {
        if (valor == null) return "";
        String s = valor.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s + "\"";
        }
        return s;
    }

    private String desescapa(String valor) {
        if (valor == null) return "";
        if (valor.startsWith("\"") && valor.endsWith("\"")) {
            valor = valor.substring(1, valor.length()-1).replace("\"\"", "\"");
        }
        return valor;
    }

    public static class Ubicacion {
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty descripcion;
        private final SimpleStringProperty edificio;
        private final SimpleStringProperty estado;

        public Ubicacion(String nombre, String descripcion, String edificio, String estado) {
            this.nombre = new SimpleStringProperty(nombre);
            this.descripcion = new SimpleStringProperty(descripcion);
            this.edificio = new SimpleStringProperty(edificio);
            this.estado = new SimpleStringProperty(estado);
        }

        // Getters y setters
        public String getNombre() { return nombre.get(); }
        public void setNombre(String nombre) { this.nombre.set(nombre); }
        public String getDescripcion() { return descripcion.get(); }
        public void setDescripcion(String descripcion) { this.descripcion.set(descripcion); }
        public String getEdificio() { return edificio.get(); }
        public void setEdificio(String edificio) { this.edificio.set(edificio); }
        public String getEstado() { return estado.get(); }
        public void setEstado(String estado) { this.estado.set(estado); }

        // Property methods
        public SimpleStringProperty nombreProperty() { return nombre; }
        public SimpleStringProperty descripcionProperty() { return descripcion; }
        public SimpleStringProperty edificioProperty() { return edificio; }
        public SimpleStringProperty estadoProperty() { return estado; }
    }
}
