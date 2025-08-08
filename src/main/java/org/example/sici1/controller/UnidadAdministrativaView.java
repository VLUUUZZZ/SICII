package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class UnidadAdministrativaView {

    @FXML private TableView<Unidad> tableUnidades;
    @FXML private TableColumn<Unidad, String> colNombre;
    @FXML private TableColumn<Unidad, String> colEstado;
    @FXML private TableColumn<Unidad, Void> colSwitch;
    @FXML private TableColumn<Unidad, Void> colEditar;
    @FXML private TextField txtBuscar;
    @FXML private Button btnNuevo, btnBuscar;

    private final ObservableList<Unidad> unidades = FXCollections.observableArrayList();
    private final FilteredList<Unidad> unidadesFiltradas = new FilteredList<>(unidades, p -> true);

    private static final String ARCHIVO_UNIDADES =
            System.getProperty("user.home") + File.separator + "Documents" + File.separator + "unidades_adm.csv";

    private final boolean esEmpleado = "EMPLEADO".equalsIgnoreCase(UserSession.getInstance().getRole());

    @FXML
    public void initialize() {
        configurarTabla();
        configurarBuscador();

        btnNuevo.setVisible(!esEmpleado);
        colSwitch.setVisible(!esEmpleado);
        colEditar.setVisible(!esEmpleado);

        tableUnidades.setItems(unidadesFiltradas);

        cargarUnidades();

        if (!esEmpleado) {
            btnNuevo.setOnAction(e -> mostrarDialogoNuevo());
        }
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());

        // Switch de estado SOLO para admin
        colSwitch.setCellFactory(col -> new TableCell<>() {
            private final CheckBox check = new CheckBox();
            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                check.setDisable(esEmpleado);
                check.setOnAction(e -> {
                    Unidad u = getTableView().getItems().get(getIndex());
                    u.setEstado(check.isSelected() ? "Activo" : "Inactivo");
                    tableUnidades.refresh();
                    guardarUnidades();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Unidad u = getTableView().getItems().get(getIndex());
                    check.setSelected("Activo".equalsIgnoreCase(u.getEstado()));
                    setGraphic(check);
                }
            }
        });

        // Columna Editar SOLO para admin
        colEditar.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✎");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-font-size: 16; -fx-text-fill: #1976d2;");
                btn.setOnAction(e -> {
                    Unidad u = getTableView().getItems().get(getIndex());
                    mostrarDialogoEditar(u);
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
        btnBuscar.setOnAction(e -> buscarUnidades());
        txtBuscar.setOnAction(e -> buscarUnidades());
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarUnidades());
    }

    private void buscarUnidades() {
        String textoBusqueda = txtBuscar.getText().toLowerCase().trim();
        unidadesFiltradas.setPredicate(unidad -> textoBusqueda.isEmpty() ||
                unidad.getNombre().toLowerCase().contains(textoBusqueda));
    }

    private void mostrarDialogoNuevo() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nueva Unidad Administrativa");
        dialog.setHeaderText("Crear nueva unidad administrativa");
        dialog.setContentText("Nombre de la unidad:");
        dialog.getEditor().setPromptText("Ejemplo: División Académica...");
        dialog.getDialogPane().setPrefWidth(400);

        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(nombre -> {
            nombre = nombre.trim();
            if (nombre.isEmpty()) {
                mostrarAlerta("Error", "El nombre no puede estar vacío", Alert.AlertType.ERROR);
                return;
            }

            if (unidadExiste(nombre)) {
                mostrarAlerta("Error", "Ya existe una unidad con ese nombre", Alert.AlertType.ERROR);
                return;
            }

            unidades.add(new Unidad(nombre, "Activo"));
            guardarUnidades();
            buscarUnidades();
        });
    }

    private void mostrarDialogoEditar(Unidad unidad) {
        TextInputDialog dialog = new TextInputDialog(unidad.getNombre());
        dialog.setTitle("Editar Unidad Administrativa");
        dialog.setHeaderText("Modificar nombre de la unidad");
        dialog.setContentText("Nuevo nombre:");
        dialog.getEditor().setPromptText("Nombre actualizado");
        dialog.getDialogPane().setPrefWidth(400);

        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(nombreNuevo -> {
            nombreNuevo = nombreNuevo.trim();
            if (nombreNuevo.isEmpty()) {
                mostrarAlerta("Error", "El nombre no puede estar vacío", Alert.AlertType.ERROR);
                return;
            }

            if (!nombreNuevo.equals(unidad.getNombre()) && unidadExiste(nombreNuevo)) {
                mostrarAlerta("Error", "Ya existe una unidad con ese nombre", Alert.AlertType.ERROR);
                return;
            }

            unidad.setNombre(nombreNuevo);
            tableUnidades.refresh();
            guardarUnidades();
            buscarUnidades();
        });
    }

    private boolean unidadExiste(String nombre) {
        return unidades.stream().anyMatch(u -> u.getNombre().equalsIgnoreCase(nombre));
    }

    private void cargarUnidades() {
        unidades.clear();
        File archivo = new File(ARCHIVO_UNIDADES);
        if (!archivo.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = desescapaCSV(linea);
                if (partes.length >= 2) {
                    unidades.add(new Unidad(partes[0], partes[1]));
                }
            }
        } catch (IOException ex) {
            mostrarAlerta("Error", "No se pudo leer el archivo de unidades", Alert.AlertType.ERROR);
        }
    }

    private void guardarUnidades() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ARCHIVO_UNIDADES), StandardCharsets.UTF_8))) {
            for (Unidad u : unidades) {
                writer.write(escapa(u.getNombre()) + "," + escapa(u.getEstado()));
                writer.newLine();
            }
        } catch (IOException ex) {
            mostrarAlerta("Error", "No se pudo guardar el archivo de unidades", Alert.AlertType.ERROR);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private String escapa(String campo) {
        if (campo == null) return "";
        String s = campo.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s + "\"";
        }
        return s;
    }

    private String[] desescapaCSV(String linea) {
        // Split por comas, soportando comillas
        java.util.List<String> partes = new java.util.ArrayList<>();
        boolean enComillas = false;
        StringBuilder actual = new StringBuilder();
        for (char c : linea.toCharArray()) {
            if (c == '"') enComillas = !enComillas;
            else if (c == ',' && !enComillas) {
                partes.add(actual.toString());
                actual.setLength(0);
            } else actual.append(c);
        }
        partes.add(actual.toString());
        return partes.toArray(new String[0]);
    }

    public static class Unidad {
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty estado;

        public Unidad(String nombre, String estado) {
            this.nombre = new SimpleStringProperty(nombre);
            this.estado = new SimpleStringProperty(estado);
        }
        public String getNombre() { return nombre.get(); }
        public void setNombre(String nombre) { this.nombre.set(nombre); }
        public String getEstado() { return estado.get(); }
        public void setEstado(String estado) { this.estado.set(estado); }
        public SimpleStringProperty nombreProperty() { return nombre; }
        public SimpleStringProperty estadoProperty() { return estado; }
    }
}
