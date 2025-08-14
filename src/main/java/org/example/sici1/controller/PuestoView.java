package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.sql.*;
import java.util.Optional;

public class PuestoView {

    @FXML private TableView<Puesto> tablePuestos;
    @FXML private TableColumn<Puesto, String> colNombre, colEstado;
    @FXML private TextField txtBuscar;
    @FXML private Button btnNuevo, btnBuscar, btnEditar;

    private final ObservableList<Puesto> puestos = FXCollections.observableArrayList();
    private final FilteredList<Puesto> puestosFiltrados = new FilteredList<>(puestos);

    // === PERMISOS ===
    private final String userRole = UserSession.getInstance().getRole();
    private final boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole); // <-- Solo admin modifica

    private static final String SCHEMA_OWNER = "ADMIN";

    @FXML
    public void initialize() {
        configurarTabla();
        configurarBuscador();

        // Visibilidad por rol
        btnNuevo.setVisible(isAdmin);
        btnEditar.setVisible(isAdmin);

        tablePuestos.setItems(puestosFiltrados);

        cargarPuestos();

        if (isAdmin) configurarAccionesAdmin();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());
    }

    private void configurarBuscador() {
        btnBuscar.setOnAction(e -> buscarPuestos());
        txtBuscar.setOnAction(e -> buscarPuestos());
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarPuestos());
    }

    private void configurarAccionesAdmin() {
        btnNuevo.setOnAction(e -> mostrarDialogoNuevo());

        // Acción principal del botón Editar = Editar seleccionado
        btnEditar.setOnAction(e -> editarSeleccionado());

        // Menú contextual en el botón Editar para Habilitar/Deshabilitar
        MenuItem miToggle = new MenuItem("Habilitar / Deshabilitar");
        miToggle.setOnAction(e -> toggleEstadoSeleccionado());

        ContextMenu menuEditar = new ContextMenu(miToggle);

        // Clic secundario (derecho) en el botón -> muestra menú
        btnEditar.setOnMousePressed(me -> {
            if (me.getButton() == MouseButton.SECONDARY) {
                menuEditar.show(btnEditar, me.getScreenX(), me.getScreenY());
                me.consume();
            } else {
                menuEditar.hide();
            }
        });
    }

    // ---- Acciones de UI ----

    private void editarSeleccionado() {
        Puesto seleccionado = getPuestoSeleccionado();
        if (seleccionado == null) return;
        mostrarDialogoEditar(seleccionado);
    }

    private void toggleEstadoSeleccionado() {
        Puesto seleccionado = getPuestoSeleccionado();
        if (seleccionado == null) return;

        // Alternar estado
        boolean activar = !"Activo".equalsIgnoreCase(seleccionado.getEstado());
        String nuevoEstadoTexto = activar ? "Activo" : "Inactivo";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar cambio de estado");
        confirm.setHeaderText(null);
        confirm.setContentText("¿Deseas marcar el puesto \"" + seleccionado.getNombre() + "\" como " + nuevoEstadoTexto + "?");

        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            seleccionado.setEstado(nuevoEstadoTexto);
            actualizarEstado(seleccionado); // Persiste S/N
            tablePuestos.refresh();
        }
    }

    private Puesto getPuestoSeleccionado() {
        Puesto p = tablePuestos.getSelectionModel().getSelectedItem();
        if (p == null) {
            mostrarAlerta("Atención", "Selecciona un puesto en la tabla.", Alert.AlertType.INFORMATION);
        }
        return p;
    }

    private void buscarPuestos() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase().trim();
        puestosFiltrados.setPredicate(p ->
                texto.isEmpty() || p.getNombre().toLowerCase().contains(texto)
        );
    }

    private void mostrarDialogoNuevo() {
        if (!isAdmin) { mostrarAlerta("Error", "No autorizado", Alert.AlertType.WARNING); return; }

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
            insertarPuesto(nombre);
        });
    }

    private void mostrarDialogoEditar(Puesto puesto) {
        if (!isAdmin) { mostrarAlerta("Error", "No autorizado", Alert.AlertType.WARNING); return; }

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
            if (!nombreNuevo.equals(puesto.getNombre()) && puestoExiste(nombreNuevo)) {
                mostrarAlerta("Error", "Ya existe un puesto con ese nombre", Alert.AlertType.ERROR);
                return;
            }
            actualizarPuesto(puesto.getNombre(), nombreNuevo);
        });
    }

    // ---- Datos ----

    private void cargarPuestos() {
        puestos.clear();
        String sql = "SELECT nombre, activo FROM puestos ORDER BY nombre";
        try (Connection cn = getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                puestos.add(new Puesto(
                        rs.getString("nombre"),
                        "S".equals(rs.getString("activo")) ? "Activo" : "Inactivo"
                ));
            }
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo cargar los puestos", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void insertarPuesto(String nombre) {
        if (!isAdmin) { mostrarAlerta("Error", "No autorizado", Alert.AlertType.WARNING); return; }

        String sql = "INSERT INTO puestos (nombre, activo) VALUES (?, 'S')";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            cargarPuestos();
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo insertar el puesto", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void actualizarPuesto(String nombreOriginal, String nuevoNombre) {
        if (!isAdmin) { mostrarAlerta("Error", "No autorizado", Alert.AlertType.WARNING); return; }

        String sql = "UPDATE puestos SET nombre = ?, actualizado_en = SYSTIMESTAMP WHERE nombre = ?";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nuevoNombre);
            ps.setString(2, nombreOriginal);
            ps.executeUpdate();
            cargarPuestos();
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo actualizar el puesto", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void actualizarEstado(Puesto puesto) {
        if (!isAdmin) { mostrarAlerta("Error", "No autorizado", Alert.AlertType.WARNING); return; }

        String sql = "UPDATE puestos SET activo = ?, actualizado_en = SYSTIMESTAMP WHERE nombre = ?";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, "Activo".equalsIgnoreCase(puesto.getEstado()) ? "S" : "N");
            ps.setString(2, puesto.getNombre());
            ps.executeUpdate();
        } catch (SQLException e) {
            mostrarAlerta("Error", "No se pudo cambiar el estado del puesto", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private boolean puestoExiste(String nombre) {
        String sql = "SELECT COUNT(*) FROM puestos WHERE UPPER(nombre) = UPPER(?)";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            mostrarAlerta("Error", "Error al verificar existencia del puesto", Alert.AlertType.ERROR);
            return false; // no bloquear por error de validación
        }
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

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ---- Modelo ----
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