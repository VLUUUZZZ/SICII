package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox; // <-- AGREGA ESTA LÍNEA
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class UsuariosView {

    // ==== FXML Elements ====
    @FXML private TableView<Usuario> tableUsuarios;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, Void> colSwitch;
    @FXML private TextField txtBuscar;
    @FXML private Button btnAgregar, btnBuscar, btnEditar;

    // ==== Data ====
    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();
    private final FilteredList<Usuario> usuariosFiltrados = new FilteredList<>(usuarios, p -> true);

    private static final String USUARIOS_CSV = "C:\\Users\\VICTOR UZZIEL\\Documents\\usuarios.csv";
    private final boolean esEmpleado = "EMPLEADO".equalsIgnoreCase(UserSession.getInstance().getRole());

    @FXML
    public void initialize() {
        // Configura las columnas
        colUsuario.setCellValueFactory(data -> data.getValue().usuarioProperty());
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colRol.setCellValueFactory(data -> data.getValue().rolProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());

        // Switch visual solo para admin
        colSwitch.setVisible(!esEmpleado);
        colSwitch.setCellFactory(col -> new TableCell<>() {
            private final CheckBox check = new CheckBox();
            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                check.setDisable(esEmpleado);
                check.setOnAction(e -> {
                    Usuario u = getTableView().getItems().get(getIndex());
                    u.setEstado(check.isSelected() ? "Activo" : "Inactivo");
                    tableUsuarios.refresh();
                    guardarUsuariosCSV();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Usuario u = getTableView().getItems().get(getIndex());
                    check.setSelected("Activo".equalsIgnoreCase(u.getEstado()));
                    setGraphic(check);
                }
            }
        });

        // Asigna la lista filtrada
        tableUsuarios.setItems(usuariosFiltrados);

        // Carga datos
        cargarUsuariosCSV();

        // Listeners para el buscador
        btnBuscar.setOnAction(e -> buscarUsuarios());
        txtBuscar.setOnAction(e -> buscarUsuarios());
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarUsuarios());

        // Botón agregar (muestra diálogo)
        btnAgregar.setOnAction(e -> mostrarDialogoAgregarUsuario());
        btnAgregar.setVisible(!esEmpleado);

        // Botón editar (opcional, desactivado)
        if (btnEditar != null) btnEditar.setVisible(false);
    }

    // === Diálogo para agregar usuario ===
    private void mostrarDialogoAgregarUsuario() {
        Dialog<Usuario> dialog = new Dialog<>();
        dialog.setTitle("Agregar Nuevo Usuario");
        dialog.setHeaderText("Complete los datos del nuevo usuario");

        // Botón Agregar
        ButtonType agregarButtonType = new ButtonType("Agregar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(agregarButtonType, ButtonType.CANCEL);

        // Campos del formulario
        TextField usuarioField = new TextField();
        usuarioField.setPromptText("Nombre de usuario");
        TextField nombreField = new TextField();
        nombreField.setPromptText("Nombre completo");
        ComboBox<String> rolCombo = new ComboBox<>();
        rolCombo.getItems().addAll("Administrador", "Encargado", "Visualizador");
        rolCombo.setPromptText("Seleccione un rol");
        ComboBox<String> estadoCombo = new ComboBox<>();
        estadoCombo.getItems().addAll("Activo", "Inactivo");
        estadoCombo.setPromptText("Seleccione estado");

        // Layout vertical
        VBox content = new VBox(10,
                new Label("Usuario:"), usuarioField,
                new Label("Nombre:"), nombreField,
                new Label("Rol:"), rolCombo,
                new Label("Estado:"), estadoCombo
        );
        dialog.getDialogPane().setContent(content);

        // Convertir resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == agregarButtonType) {
                return new Usuario(
                        usuarioField.getText().trim(),
                        nombreField.getText().trim(),
                        rolCombo.getValue(),
                        estadoCombo.getValue()
                );
            }
            return null;
        });

        Optional<Usuario> result = dialog.showAndWait();

        result.ifPresent(usuario -> {
            // Validar campos
            if (usuario.getUsuario().isEmpty() || usuario.getNombre().isEmpty() ||
                    usuario.getRol() == null || usuario.getEstado() == null) {
                showAlert("Debes llenar todos los campos.", Alert.AlertType.WARNING);
                return;
            }

            // Evitar duplicados
            for (Usuario u : usuarios) {
                if (u.getUsuario().equalsIgnoreCase(usuario.getUsuario())) {
                    showAlert("Ese usuario ya existe.", Alert.AlertType.WARNING);
                    return;
                }
            }

            usuarios.add(usuario);
            guardarUsuariosCSV();
        });
    }

    // === Buscador ===
    private void buscarUsuarios() {
        String textoBusqueda = txtBuscar.getText().trim().toLowerCase();
        usuariosFiltrados.setPredicate(usuario -> {
            if (textoBusqueda.isEmpty()) return true;
            return usuario.getUsuario().toLowerCase().contains(textoBusqueda) ||
                    usuario.getNombre().toLowerCase().contains(textoBusqueda);
        });
    }

    // === Mensaje de alerta ===
    private void showAlert(String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Gestión de Usuarios");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // === Guardar en CSV ===
    private void guardarUsuariosCSV() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(USUARIOS_CSV), StandardCharsets.UTF_8))) {
            for (Usuario u : usuarios) {
                writer.write(u.getUsuario() + "," + u.getNombre() + "," + u.getRol() + "," + u.getEstado());
                writer.newLine();
            }
        } catch (IOException e) {
            showAlert("Error al guardar usuarios.", Alert.AlertType.ERROR);
        }
    }

    // === Cargar desde CSV ===
    private void cargarUsuariosCSV() {
        usuarios.clear();
        File f = new File(USUARIOS_CSV);
        if (!f.exists()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] p = linea.split(",", -1);
                if (p.length >= 4) {
                    usuarios.add(new Usuario(p[0], p[1], p[2], p[3]));
                }
            }
        } catch (IOException e) {
            showAlert("Error al cargar usuarios.", Alert.AlertType.ERROR);
        }
    }

    // === Clase modelo Usuario ===
    public static class Usuario {
        private final SimpleStringProperty usuario, nombre, rol, estado;

        public Usuario(String usuario, String nombre, String rol, String estado) {
            this.usuario = new SimpleStringProperty(usuario);
            this.nombre = new SimpleStringProperty(nombre);
            this.rol = new SimpleStringProperty(rol);
            this.estado = new SimpleStringProperty(estado);
        }

        public String getUsuario() { return usuario.get(); }
        public String getNombre() { return nombre.get(); }
        public String getRol() { return rol.get(); }
        public String getEstado() { return estado.get(); }
        public void setEstado(String estado) { this.estado.set(estado); }

        public SimpleStringProperty usuarioProperty() { return usuario; }
        public SimpleStringProperty nombreProperty() { return nombre; }
        public SimpleStringProperty rolProperty() { return rol; }
        public SimpleStringProperty estadoProperty() { return estado; }
    }
}
