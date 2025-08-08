package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class EdificiosView {

    @FXML private TableView<Edificio> tablaEdificios;
    @FXML private TableColumn<Edificio, String> colNombre;
    @FXML private TableColumn<Edificio, String> colEstado;
    @FXML private TableColumn<Edificio, Void> colSwitch;
    @FXML private TextField txtBuscarNombre;
    @FXML private Button btnAgregar, btnEditar, btnBuscarNombre;

    private final ObservableList<Edificio> edificios = FXCollections.observableArrayList();
    private final FilteredList<Edificio> edificiosFiltrados = new FilteredList<>(edificios, p -> true);

    private final boolean esEmpleado = "EMPLEADO".equalsIgnoreCase(UserSession.getInstance().getRole());

    private static final String ARCHIVO_EDIFICIOS =
            System.getProperty("user.home") + File.separator + "Documents" + File.separator + "edificios.csv";

    @FXML
    public void initialize() {
        tablaEdificios.setItems(edificiosFiltrados);

        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());

        // Switch de estado SOLO para admin
        colSwitch.setCellFactory(col -> new TableCell<>() {
            private final CheckBox check = new CheckBox();
            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                check.setDisable(esEmpleado); // Solo admin puede activar/desactivar
                check.setOnAction(e -> {
                    Edificio edificio = getTableView().getItems().get(getIndex());
                    edificio.setEstado(check.isSelected() ? "Activo" : "Inactivo");
                    tablaEdificios.refresh();
                    guardarEdificios();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Edificio edificio = getTableView().getItems().get(getIndex());
                    check.setSelected("Activo".equalsIgnoreCase(edificio.getEstado()));
                    setGraphic(check);
                }
            }
        });

        colSwitch.setVisible(!esEmpleado);
        colEstado.setVisible(esEmpleado);
        btnAgregar.setVisible(!esEmpleado);
        btnEditar.setVisible(!esEmpleado);

        // --- BUSQUEDA ---
        btnBuscarNombre.setOnAction(e -> buscarEdificio());
        txtBuscarNombre.setOnAction(e -> buscarEdificio());
        txtBuscarNombre.textProperty().addListener((obs, oldVal, newVal) -> buscarEdificio());

        cargarEdificios();

        if (!esEmpleado) {
            btnAgregar.setOnAction(e -> agregarEdificio());
            btnEditar.setOnAction(e -> editarEdificio());
        }
    }

    private void buscarEdificio() {
        String textoBusqueda = txtBuscarNombre.getText().toLowerCase().trim();
        edificiosFiltrados.setPredicate(edificio ->
                textoBusqueda.isEmpty() ||
                        edificio.getNombre().toLowerCase().contains(textoBusqueda)
        );
    }

    private void agregarEdificio() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Agregar Edificio");
        dialog.setHeaderText(null);
        dialog.setContentText("Ingrese el nombre del nuevo edificio/área:");

        final String[] nombreFinal = new String[1]; // Usar array para variable 'final' en lambda
        dialog.showAndWait().ifPresent(nombre -> {
            nombreFinal[0] = nombre.trim();
            if (nombreFinal[0].isEmpty()) {
                mostrarAlerta("El nombre no puede estar vacío.", Alert.AlertType.WARNING);
                return;
            }
            if (edificios.stream().anyMatch(e -> e.getNombre().equalsIgnoreCase(nombreFinal[0]))) {
                mostrarAlerta("El edificio/área ya existe.", Alert.AlertType.WARNING);
                return;
            }
            edificios.add(new Edificio(nombreFinal[0], "Activo"));
            guardarEdificios();
            buscarEdificio();
        });
    }

    private void editarEdificio() {
        Edificio seleccionado = tablaEdificios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Seleccione un edificio/área para editar.", Alert.AlertType.WARNING);
            return;
        }

        TextInputDialog dialog = new TextInputDialog(seleccionado.getNombre());
        dialog.setTitle("Editar Edificio");
        dialog.setHeaderText(null);
        dialog.setContentText("Modifique el nombre del edificio/área:");

        final String[] nuevoNombre = new String[1];
        dialog.showAndWait().ifPresent(nombre -> {
            nuevoNombre[0] = nombre.trim();
            if (nuevoNombre[0].isEmpty()) {
                mostrarAlerta("El nombre no puede estar vacío.", Alert.AlertType.WARNING);
                return;
            }
            if (edificios.stream()
                    .filter(e -> e != seleccionado)
                    .anyMatch(e -> e.getNombre().equalsIgnoreCase(nuevoNombre[0]))) {
                mostrarAlerta("El edificio/área ya existe.", Alert.AlertType.WARNING);
                return;
            }
            seleccionado.setNombre(nuevoNombre[0]);
            tablaEdificios.refresh();
            guardarEdificios();
            buscarEdificio();
        });
    }

    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Gestión de Edificios");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /** ==== Persistencia en archivo CSV ==== */
    private void guardarEdificios() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ARCHIVO_EDIFICIOS), StandardCharsets.UTF_8))) {
            for (Edificio e : edificios) {
                writer.write(escapa(e.getNombre()) + "," + escapa(e.getEstado()));
                writer.newLine();
            }
        } catch (IOException ex) {
            mostrarAlerta("No se pudo guardar el archivo de edificios.", Alert.AlertType.ERROR);
        }
    }

    private void cargarEdificios() {
        edificios.clear();
        File archivo = new File(ARCHIVO_EDIFICIOS);
        if (!archivo.exists()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = linea.split(",", 2);
                String nombre = desescapa(partes[0]);
                String estado = (partes.length > 1) ? desescapa(partes[1]) : "Activo";
                edificios.add(new Edificio(nombre, estado));
            }
        } catch (IOException ex) {
            mostrarAlerta("No se pudo leer el archivo de edificios.", Alert.AlertType.ERROR);
        }
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
        if (valor.startsWith("\"") && valor.endsWith("\"")) {
            valor = valor.substring(1, valor.length()-1).replace("\"\"", "\"");
        }
        return valor;
    }

    // Modelo con estado
    public static class Edificio {
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty estado;
        public Edificio(String nombre, String estado) {
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

    public ObservableList<Edificio> getEdificiosList() {
        return edificios;
    }
}
