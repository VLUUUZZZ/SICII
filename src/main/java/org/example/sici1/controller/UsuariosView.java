package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.util.*;

public class UsuariosView {

    @FXML private TableView<Usuario> tableUsuarios;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, Void> colSwitch;
    @FXML private TextField txtBuscar;
    @FXML private Button btnAgregar, btnBuscar, btnEditar;

    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();
    private final FilteredList<Usuario> usuariosFiltrados = new FilteredList<>(usuarios, p -> true);

    private final String userRole = UserSession.getInstance().getRole();
    private final boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
    private static final String SCHEMA_OWNER = "ADMIN";

    @FXML
    public void initialize() {
        colUsuario.setCellValueFactory(data -> data.getValue().usuarioProperty());
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colRol.setCellValueFactory(data -> data.getValue().rolProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());

        colSwitch.setVisible(isAdmin);
        colSwitch.setCellFactory(col -> new TableCell<>() {
            private final CheckBox check = new CheckBox();
            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                check.setDisable(!isAdmin);
                check.setOnAction(e -> {
                    if (!isAdmin) { mostrarAlerta("No autorizado", Alert.AlertType.WARNING); return; }
                    Usuario u = getTableView().getItems().get(getIndex());
                    String nuevo = check.isSelected() ? "Activo" : "Inactivo";
                    u.setEstado(nuevo);
                    actualizarActivo(u.getUsuario(), nuevo);
                    tableUsuarios.refresh();
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

        tableUsuarios.setItems(usuariosFiltrados);
        cargarUsuariosDB();

        btnBuscar.setOnAction(e -> buscarUsuarios());
        txtBuscar.setOnAction(e -> buscarUsuarios());
        txtBuscar.textProperty().addListener((obs, o, n) -> buscarUsuarios());

        btnAgregar.setOnAction(e -> {
            if (!isAdmin) { mostrarAlerta("No autorizado", Alert.AlertType.WARNING); return; }
            mostrarDialogoAgregarUsuario();
        });
        btnAgregar.setVisible(isAdmin);

        if (btnEditar != null) btnEditar.setVisible(false);
    }

    private void cargarUsuariosDB() {
        usuarios.clear();
        String sql = """
                SELECT u.username,
                       TRIM(NVL(e.nombre,'')||' '||NVL(e.apellido_p,'')||' '||NVL(e.apellido_m,'')) AS nombre,
                       NVL(MAX(r.nombre), 'USUARIO') AS rol,
                       u.activo
                  FROM usuarios u
                  LEFT JOIN empleados e ON e.id_empleado = u.id_empleado
                  LEFT JOIN usuario_rol ur ON ur.id_usuario = u.id_usuario
                  LEFT JOIN roles r ON r.id_rol = ur.id_rol
              GROUP BY u.username, u.activo, e.nombre, e.apellido_p, e.apellido_m
              ORDER BY u.username
                """;
        try (Connection cn = getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String username = rs.getString("username");
                String nombre = Optional.ofNullable(rs.getString("nombre")).orElse("").replaceAll("\\s+", " ").trim();
                String rol = rs.getString("rol");
                String estado = "S".equals(rs.getString("activo")) ? "Activo" : "Inactivo";
                usuarios.add(new Usuario(username, nombre, rol, estado));
            }
        } catch (SQLException e) {
            mostrarAlerta("Error al cargar usuarios desde BD.", Alert.AlertType.ERROR);
        }
    }

    private void mostrarDialogoAgregarUsuario() {
        Dialog<UsuarioDialogData> dialog = new Dialog<>();
        dialog.setTitle("Agregar Nuevo Usuario");
        dialog.setHeaderText("Complete los datos del nuevo usuario");

        ButtonType agregarButtonType = new ButtonType("Agregar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(agregarButtonType, ButtonType.CANCEL);

        TextField usuarioField = new TextField();
        usuarioField.setPromptText("Nombre de usuario");
        TextField nombreField = new TextField();
        nombreField.setPromptText("Nombre completo");
        ComboBox<String> rolCombo = new ComboBox<>();
        rolCombo.setPromptText("Seleccione un rol");
        cargarRoles(rolCombo);
        ComboBox<String> estadoCombo = new ComboBox<>();
        estadoCombo.getItems().addAll("Activo", "Inactivo");
        estadoCombo.setValue("Activo");

        VBox content = new VBox(10,
                new Label("Usuario:"), usuarioField,
                new Label("Nombre completo:"), nombreField,
                new Label("Rol:"), rolCombo,
                new Label("Estado:"), estadoCombo
        );
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == agregarButtonType) {
                return new UsuarioDialogData(
                        usuarioField.getText().trim(),
                        nombreField.getText().trim(),
                        rolCombo.getValue(),
                        estadoCombo.getValue()
                );
            }
            return null;
        });

        Optional<UsuarioDialogData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            if (data.username().isEmpty() || data.nombreCompleto().isEmpty() || data.rolNombre() == null || data.estadoUi() == null) {
                mostrarAlerta("Debes llenar todos los campos.", Alert.AlertType.WARNING);
                return;
            }
            if (existeUsuario(data.username())) {
                mostrarAlerta("Ese usuario ya existe.", Alert.AlertType.WARNING);
                return;
            }
            crearUsuarioEnBD(data);
        });
    }

    private void crearUsuarioEnBD(UsuarioDialogData data) {
        String estadoChar = "Activo".equalsIgnoreCase(data.estadoUi()) ? "S" : "N";
        String insertEmpleado = "INSERT INTO empleados (nombre) VALUES (?)";
        String insertUsuario = "INSERT INTO usuarios (id_empleado, username, hash_password, activo) VALUES (?, ?, ?, ?)";
        String insertUsuarioRol = "INSERT INTO usuario_rol (id_usuario, id_rol) VALUES (?, (SELECT id_rol FROM roles WHERE UPPER(nombre)=UPPER(?)))";
        try (Connection cn = getConnection()) {
            cn.setAutoCommit(false);
            Integer idEmpleado;
            try (PreparedStatement ps = cn.prepareStatement(insertEmpleado, new String[]{"ID_EMPLEADO"})) {
                ps.setString(1, data.nombreCompleto());
                ps.executeUpdate();
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (!gk.next()) throw new SQLException("No se obtuvo ID_EMPLEADO");
                    idEmpleado = gk.getInt(1);
                }
            }
            Integer idUsuario;
            try (PreparedStatement ps = cn.prepareStatement(insertUsuario, new String[]{"ID_USUARIO"})) {
                ps.setInt(1, idEmpleado);
                ps.setString(2, data.username());
                ps.setString(3, "123456");
                ps.setString(4, estadoChar);
                ps.executeUpdate();
                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (!gk.next()) throw new SQLException("No se obtuvo ID_USUARIO");
                    idUsuario = gk.getInt(1);
                }
            }
            try (PreparedStatement ps = cn.prepareStatement(insertUsuarioRol)) {
                ps.setInt(1, idUsuario);
                ps.setString(2, data.rolNombre());
                int n = ps.executeUpdate();
                if (n == 0) throw new SQLException("Rol no encontrado: " + data.rolNombre());
            }
            cn.commit();
            cargarUsuariosDB();
        } catch (SQLException e) {
            mostrarAlerta("No se pudo crear el usuario.", Alert.AlertType.ERROR);
        }
    }

    private void buscarUsuarios() {
        String textoBusqueda = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        usuariosFiltrados.setPredicate(u -> textoBusqueda.isEmpty() ||
                (u.getUsuario() != null && u.getUsuario().toLowerCase().contains(textoBusqueda)) ||
                (u.getNombre() != null && u.getNombre().toLowerCase().contains(textoBusqueda)));
    }

    private boolean existeUsuario(String username) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE UPPER(username) = UPPER(?)";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void actualizarActivo(String username, String estadoUi) {
        if (!isAdmin) { mostrarAlerta("No autorizado", Alert.AlertType.WARNING); return; }
        String sql = "UPDATE usuarios SET activo = ?, actualizado_en = SYSTIMESTAMP WHERE UPPER(username) = UPPER(?)";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, "Activo".equalsIgnoreCase(estadoUi) ? "S" : "N");
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            mostrarAlerta("No se pudo actualizar el estado.", Alert.AlertType.ERROR);
        }
    }

    private void cargarRoles(ComboBox<String> rolCombo) {
        rolCombo.getItems().clear();
        String sql = "SELECT nombre FROM roles ORDER BY nombre";
        try (Connection cn = getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) rolCombo.getItems().add(rs.getString("nombre"));
            if (!rolCombo.getItems().isEmpty()) rolCombo.setValue(rolCombo.getItems().get(0));
        } catch (SQLException e) {
            rolCombo.getItems().addAll("ADMIN", "USUARIO");
            rolCombo.setValue("USUARIO");
        }
    }

    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        mostrarAlerta("Gestión de Usuarios", mensaje, tipo);
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private Connection getConnection() throws SQLException {
        try {
            Connection cn = Conexion.conectar();
            try (Statement st = cn.createStatement()) {
                st.execute("ALTER SESSION SET CURRENT_SCHEMA=" + SCHEMA_OWNER);
            }
            return cn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver Oracle no encontrado", e);
        }
    }

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

    private record UsuarioDialogData(String username, String nombreCompleto, String rolNombre, String estadoUi) {}
}
