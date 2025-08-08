package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PuestoView {

    @FXML private TableView<Puesto> tablePuestos;
    @FXML private TableColumn<Puesto, String> colNombre, colEstado, colSwitch, colEditar;
    @FXML private TextField txtBuscar;
    @FXML private Button btnNuevo, btnBuscar;

    private final ObservableList<Puesto> puestos = FXCollections.observableArrayList();
    private final FilteredList<Puesto> puestosFiltrados = new FilteredList<>(puestos);

    private static final String ARCHIVO_PUESTOS =
            System.getProperty("user.home") + File.separator + "Documents" + File.separator + "puestos.csv";

    // Detecta si es EMPLEADO
    private final boolean esEmpleado = "EMPLEADO".equalsIgnoreCase(UserSession.getInstance().getRole());

    @FXML
    public void initialize() {
        configurarTabla();
        configurarBuscador();
        configurarVisibilidadSegunRol();
        cargarPuestos();

        if (!esEmpleado) {
            configurarAccionesAdmin();
        }
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());

        // Switch de estado (solo admin puede cambiar)
        colSwitch.setCellFactory(col -> new TableCell<>() {
            private final CheckBox check = new CheckBox();
            {
                check.setDisable(esEmpleado);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                check.setStyle("-fx-scale-x:1.3; -fx-scale-y:1.3; -fx-border-color: #57ba47; -fx-border-radius: 15;");
                check.setOnAction(e -> {
                    Puesto p = getTableView().getItems().get(getIndex());
                    p.setEstado(check.isSelected() ? "Activo" : "Inactivo");
                    tablePuestos.refresh();
                    guardarPuestos();
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Puesto p = getTableView().getItems().get(getIndex());
                    check.setSelected("Activo".equalsIgnoreCase(p.getEstado()));
                    setGraphic(check);
                }
            }
        });

        // Columna Editar (solo admin)
        colEditar.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✎");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-font-size: 16; -fx-text-fill: #1976d2;");
                btn.setDisable(esEmpleado);
                btn.setOnAction(e -> {
                    Puesto p = getTableView().getItems().get(getIndex());
                    mostrarDialogoEditar(p);
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tablePuestos.setItems(puestosFiltrados);
    }

    private void configurarBuscador() {
        btnBuscar.setOnAction(e -> buscarPuestos());
        txtBuscar.setOnAction(e -> buscarPuestos());
    }

    private void configurarVisibilidadSegunRol() {
        btnNuevo.setVisible(!esEmpleado);
        colSwitch.setVisible(!esEmpleado);
        colEditar.setVisible(!esEmpleado);
    }

    private void configurarAccionesAdmin() {
        btnNuevo.setOnAction(e -> mostrarDialogoNuevo());
    }

    private void buscarPuestos() {
        String textoBusqueda = txtBuscar.getText().toLowerCase().trim();

        puestosFiltrados.setPredicate(puesto -> {
            if (textoBusqueda.isEmpty()) return true;
            return puesto.getNombre().toLowerCase().contains(textoBusqueda);
        });
    }

    private void mostrarDialogoNuevo() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuevo Puesto");
        dialog.setHeaderText("Crear nuevo puesto");
        dialog.setContentText("Nombre del puesto:");
        dialog.getEditor().setPromptText("Ejemplo: Director");
        dialog.getDialogPane().setPrefWidth(400);

        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(nombre -> {
            nombre = nombre.trim();
            if (nombre.isEmpty()) {
                mostrarAlerta("Error", "El nombre no puede estar vacío", Alert.AlertType.ERROR);
                return;
            }

            if (puestoExiste(nombre)) {
                mostrarAlerta("Error", "Ya existe un puesto con ese nombre", Alert.AlertType.ERROR);
                return;
            }

            puestos.add(new Puesto(nombre, "Activo"));
            guardarPuestos();
            buscarPuestos();
        });
    }

    private void mostrarDialogoEditar(Puesto puesto) {
        TextInputDialog dialog = new TextInputDialog(puesto.getNombre());
        dialog.setTitle("Editar Puesto");
        dialog.setHeaderText("Modificar nombre del puesto");
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

            if (!nombreNuevo.equals(puesto.getNombre())) {
                if (puestoExiste(nombreNuevo)) {
                    mostrarAlerta("Error", "Ya existe un puesto con ese nombre", Alert.AlertType.ERROR);
                    return;
                }
            }

            puesto.setNombre(nombreNuevo);
            tablePuestos.refresh();
            guardarPuestos();
            buscarPuestos();
        });
    }

    private boolean puestoExiste(String nombre) {
        return puestos.stream()
                .anyMatch(p -> p.getNombre().equalsIgnoreCase(nombre));
    }

    private void cargarPuestos() {
        puestos.clear();
        File archivo = new File(ARCHIVO_PUESTOS);
        if (!archivo.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = desescapaCSV(linea);
                if (partes.length >= 2) {
                    puestos.add(new Puesto(partes[0], partes[1]));
                }
            }
        } catch (IOException ex) {
            mostrarAlerta("Error", "No se pudo leer el archivo de puestos", Alert.AlertType.ERROR);
        }
    }

    private void guardarPuestos() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ARCHIVO_PUESTOS), StandardCharsets.UTF_8))) {
            for (Puesto p : puestos) {
                writer.write(escapa(p.getNombre()) + "," + escapa(p.getEstado()));
                writer.newLine();
            }
        } catch (IOException ex) {
            mostrarAlerta("Error", "No se pudo guardar el archivo de puestos", Alert.AlertType.ERROR);
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
        java.util.StringTokenizer tok = new java.util.StringTokenizer(linea, ",", true);
        String[] arr = new String[2];
        int i = 0;
        String actual = "";
        boolean enComillas = false;
        while (tok.hasMoreTokens() && i < 2) {
            String token = tok.nextToken();
            if (token.equals(",")) {
                if (!enComillas) {
                    arr[i++] = actual;
                    actual = "";
                } else {
                    actual += ",";
                }
            } else if (token.startsWith("\"")) {
                enComillas = true;
                actual += token.substring(1);
            } else if (token.endsWith("\"")) {
                enComillas = false;
                actual += token.substring(0, token.length() - 1);
            } else {
                actual += token;
            }
        }
        if (i < 2) arr[i] = actual;
        return arr;
    }

    public static class Puesto {
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty estado;

        public Puesto(String nombre, String estado) {
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