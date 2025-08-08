package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AsignacionesView {

    @FXML private ComboBox<String> cmbActivo, cmbUsuario, cmbEdificio, cmbUbicacion;
    @FXML private Button btnAsignar, btnBuscar;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Asignacion> tablaAsignaciones;
    @FXML private TableColumn<Asignacion, String> colActivo, colUsuario, colEdificio, colUbicacion, colEstado;
    @FXML private TableColumn<Asignacion, Void> colDesasignar;
    @FXML private HBox hboxCombos;

    private final ObservableList<Asignacion> asignaciones = FXCollections.observableArrayList();
    private final FilteredList<Asignacion> asignacionesFiltradas = new FilteredList<>(asignaciones, p -> true);

    private static final String BIENES_CSV = "C:\\Users\\VICTOR UZZIEL\\Documents\\bienes.csv";
    private static final String USUARIOS_CSV = "C:\\Users\\VICTOR UZZIEL\\Documents\\usuarios.csv";
    private static final String ASIGNACIONES_CSV = "C:\\Users\\VICTOR UZZIEL\\Documents\\asignaciones.csv";

    private final Map<String, String[]> bienesMap = new LinkedHashMap<>();
    private final Map<String, String[]> usuariosMap = new LinkedHashMap<>();
    private final Map<String, List<String>> edificioEspacios = new LinkedHashMap<>();

    private final boolean esEmpleado = "EMPLEADO".equalsIgnoreCase(UserSession.getInstance().getRole());

    @FXML
    public void initialize() {
        configurarInterfaz();
        cargarDatos();
        configurarTabla();
        configurarBuscador();

        if (!esEmpleado) {
            configurarAccionesAdmin();
        }
    }

    private void configurarInterfaz() {
        btnAsignar.setVisible(!esEmpleado);
        hboxCombos.setVisible(!esEmpleado);
    }

    private void cargarDatos() {
        cargarBienes();
        cargarUsuarios();
        configurarEdificios();
        cargarAsignaciones();
    }

    private void configurarTabla() {
        colActivo.setCellValueFactory(data -> data.getValue().activoProperty());
        colUsuario.setCellValueFactory(data -> data.getValue().usuarioProperty());
        colEdificio.setCellValueFactory(data -> data.getValue().edificioProperty());
        colUbicacion.setCellValueFactory(data -> data.getValue().ubicacionProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());

        tablaAsignaciones.setItems(asignacionesFiltradas);

        tablaAsignaciones.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // Columna Desasignar SOLO para admin
        colDesasignar.setVisible(!esEmpleado);
        colDesasignar.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Desasignar");
            {
                btn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 13; -fx-background-radius: 7;");
                btn.setOnAction(e -> {
                    Asignacion a = getTableView().getItems().get(getIndex());
                    if ("Desasignado".equalsIgnoreCase(a.getEstado())) {
                        mostrarAlerta("Aviso", "Este activo ya está desasignado.", Alert.AlertType.INFORMATION);
                        return;
                    }
                    a.setEstado("Desasignado");
                    tablaAsignaciones.refresh();
                    guardarAsignaciones();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || esEmpleado) ? null : btn);
            }
        });
    }

    private void configurarBuscador() {
        btnBuscar.setOnAction(e -> buscarAsignaciones());
        txtBuscar.setOnAction(e -> buscarAsignaciones());
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarAsignaciones());
    }

    private void configurarAccionesAdmin() {
        btnAsignar.setOnAction(e -> asignarActivo());

        cmbEdificio.setOnAction(e -> {
            String edif = cmbEdificio.getValue();
            cmbUbicacion.getItems().clear();
            if (edif != null && edificioEspacios.containsKey(edif)) {
                cmbUbicacion.getItems().addAll(edificioEspacios.get(edif));
            }
        });
    }

    private void buscarAsignaciones() {
        String textoBusqueda = txtBuscar.getText().toLowerCase().trim();

        asignacionesFiltradas.setPredicate(asignacion -> {
            if (textoBusqueda.isEmpty()) return true;
            return asignacion.getUsuario().toLowerCase().contains(textoBusqueda);
        });
    }

    private void cargarBienes() {
        cmbActivo.getItems().clear();
        bienesMap.clear();
        File f = new File(BIENES_CSV);
        if (!f.exists()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] p = splitCsv(linea);
                if (p.length >= 2) {
                    bienesMap.put(p[0], new String[]{p[1], (p.length > 2 ? p[2] : "")});
                    cmbActivo.getItems().add(p[0] + " - " + p[1]);
                }
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo leer el archivo de bienes", Alert.AlertType.ERROR);
        }
    }

    private void cargarUsuarios() {
        cmbUsuario.getItems().clear();
        usuariosMap.clear();
        File f = new File(USUARIOS_CSV);
        if (!f.exists()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] p = splitCsv(linea);
                if (p.length >= 4) {
                    usuariosMap.put(p[0], new String[]{p[1], p[2], p[3]});
                    cmbUsuario.getItems().add(p[1]);
                }
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo leer el archivo de usuarios", Alert.AlertType.ERROR);
        }
    }

    private void configurarEdificios() {
        cmbEdificio.getItems().clear();
        cmbEdificio.getItems().addAll("Docencia", "Laboratorio", "Biblioteca", "Edificio A");

        edificioEspacios.clear();
        edificioEspacios.put("Docencia", Arrays.asList("Aula 101", "Aula 102", "Aula 103"));
        edificioEspacios.put("Laboratorio", Arrays.asList("Lab Química", "Lab Física", "Lab Computo"));
        edificioEspacios.put("Biblioteca", Arrays.asList("Sala General", "Cubículos", "Recepción"));
        edificioEspacios.put("Edificio A", Arrays.asList("Oficina 1", "Oficina 2", "Oficina 3"));
    }

    private void cargarAsignaciones() {
        asignaciones.clear();
        File f = new File(ASIGNACIONES_CSV);
        if (!f.exists()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] p = splitCsv(linea);
                if (p.length >= 5) {
                    asignaciones.add(new Asignacion(p[0], p[1], p[2], p[3], p[4]));
                } else if (p.length >= 4) {
                    asignaciones.add(new Asignacion(p[0], p[1], p[2], p[3], "Activo"));
                }
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo leer el archivo de asignaciones", Alert.AlertType.ERROR);
        }
    }

    private void guardarAsignaciones() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ASIGNACIONES_CSV), StandardCharsets.UTF_8))) {
            for (Asignacion a : asignaciones) {
                writer.write(String.join(",",
                        a.getActivo(),
                        a.getUsuario(),
                        a.getEdificio(),
                        a.getUbicacion(),
                        a.getEstado()
                ));
                writer.newLine();
            }
        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo guardar las asignaciones", Alert.AlertType.ERROR);
        }
    }

    private void asignarActivo() {
        String activo = cmbActivo.getValue();
        String usuario = cmbUsuario.getValue();
        String edificio = cmbEdificio.getValue();
        String ubicacion = cmbUbicacion.getValue();

        if (activo == null || usuario == null || edificio == null || ubicacion == null) {
            mostrarAlerta("Error", "Selecciona todos los campos requeridos", Alert.AlertType.WARNING);
            return;
        }

        // Verificar si ya existe la asignación
        for (Asignacion a : asignaciones) {
            if (a.getActivo().equals(activo) && a.getUsuario().equals(usuario)
                    && a.getEdificio().equals(edificio) && a.getUbicacion().equals(ubicacion)) {
                mostrarAlerta("Advertencia", "Ya existe esta asignación", Alert.AlertType.WARNING);
                return;
            }
        }

        asignaciones.add(new Asignacion(activo, usuario, edificio, ubicacion, "Activo"));
        guardarAsignaciones();

        cmbActivo.setValue(null);
        cmbUsuario.setValue(null);
        cmbEdificio.setValue(null);
        cmbUbicacion.getItems().clear();
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private String[] splitCsv(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') inQuotes = !inQuotes;
            else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else sb.append(c);
        }
        result.add(sb.toString());
        return result.toArray(new String[0]);
    }

    public static class Asignacion {
        private final SimpleStringProperty activo, usuario, edificio, ubicacion, estado;

        public Asignacion(String activo, String usuario, String edificio, String ubicacion, String estado) {
            this.activo = new SimpleStringProperty(activo);
            this.usuario = new SimpleStringProperty(usuario);
            this.edificio = new SimpleStringProperty(edificio);
            this.ubicacion = new SimpleStringProperty(ubicacion);
            this.estado = new SimpleStringProperty(estado);
        }

        public String getActivo() { return activo.get(); }
        public void setActivo(String activo) { this.activo.set(activo); }
        public SimpleStringProperty activoProperty() { return activo; }

        public String getUsuario() { return usuario.get(); }
        public void setUsuario(String usuario) { this.usuario.set(usuario); }
        public SimpleStringProperty usuarioProperty() { return usuario; }

        public String getEdificio() { return edificio.get(); }
        public void setEdificio(String edificio) { this.edificio.set(edificio); }
        public SimpleStringProperty edificioProperty() { return edificio; }

        public String getUbicacion() { return ubicacion.get(); }
        public void setUbicacion(String ubicacion) { this.ubicacion.set(ubicacion); }
        public SimpleStringProperty ubicacionProperty() { return ubicacion; }

        public String getEstado() { return estado.get(); }
        public void setEstado(String estado) { this.estado.set(estado); }
        public SimpleStringProperty estadoProperty() { return estado; }
    }
}