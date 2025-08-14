package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

import java.sql.*;
import java.util.Optional;

public class UbicacionesView {

    // ===== FXML =====
    @FXML private TableView<Ubicacion> tablaUbicaciones;
    @FXML private TableColumn<Ubicacion, String> colNombre, colDescripcion, colEdificio, colEstado;
    @FXML private TextField txtBuscar;
    @FXML private Button btnAgregar, btnEditar, btnBuscar;

    // ===== Datos en memoria + filtro =====
    private final ObservableList<Ubicacion> ubicaciones = FXCollections.observableArrayList();
    private final FilteredList<Ubicacion> ubicacionesFiltradas = new FilteredList<>(ubicaciones, p -> true);

    // ===== Permisos (MISMA LÓGICA QUE EN EdificiosView) =====
    // Solo ADMIN puede modificar; USUARIO (u otros) solo consulta
    private final String userRole = UserSession.getInstance().getRole();
    private final boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);

    // Esquema dueño de tablas
    private static final String SCHEMA_OWNER = "ADMIN";

    // ===== Ciclo de vida =====
    @FXML
    public void initialize() {
        // Tabla y columnas
        configurarTabla();
        tablaUbicaciones.setItems(ubicacionesFiltradas);

        // Visibilidad estricta por rol (igual que en EdificiosView)
        btnAgregar.setVisible(isAdmin);
        btnEditar.setVisible(isAdmin);

        // Buscador (disponible para todos)
        btnBuscar.setOnAction(e -> buscarUbicaciones());
        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarUbicaciones());

        // Acciones SOLO para admin
        if (isAdmin) {
            btnAgregar.setOnAction(e -> mostrarDialogoAgregar());
            btnEditar.setOnAction(e -> mostrarDialogoEditar());
        }

        // Carga inicial desde BD
        cargarDatos();
    }

    /** Configura columnas de la tabla */
    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        // OJO: en BD la columna es UNIDADES.CODIGO, aquí la mostramos como "Descripción"
        colDescripcion.setCellValueFactory(data -> data.getValue().descripcionProperty());
        colEdificio.setCellValueFactory(data -> data.getValue().edificioProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());
    }

    /** Filtro por texto para nombre/código/edificio */
    private void buscarUbicaciones() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase().trim();
        ubicacionesFiltradas.setPredicate(ubicacion ->
                texto.isEmpty()
                        || ubicacion.getNombre().toLowerCase().contains(texto)
                        || ubicacion.getDescripcion().toLowerCase().contains(texto)
                        || ubicacion.getEdificio().toLowerCase().contains(texto)
        );
    }

    // ===== Carga de datos =====
    private void cargarDatos() {
        ubicaciones.clear();

        // SQL CORRECTO: UNIDADES tiene (nombre, codigo, id_edificio, activo, ...)
        final String sql =
                "SELECT u.nombre, u.codigo AS descripcion, e.nombre AS edificio, u.activo " +
                        "FROM unidades u " +
                        "JOIN edificios e ON u.id_edificio = e.id_edificio " +
                        "ORDER BY u.nombre";

        try (Connection cn = getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                ubicaciones.add(new Ubicacion(
                        rs.getString("nombre"),
                        rs.getString("descripcion") == null ? "" : rs.getString("descripcion"),
                        rs.getString("edificio"),
                        "S".equals(rs.getString("activo")) ? "Activo" : "Inactivo"
                ));
            }
        } catch (SQLException e) {
            mostrarAlerta("Error al cargar ubicaciones.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // ===== Alta/edición (SOLO ADMIN) =====
    private void mostrarDialogoAgregar() {
        // Guardia de seguridad: solo admin
        if (!isAdmin) { mostrarAlerta("No autorizado.", Alert.AlertType.WARNING); return; }

        Dialog<Ubicacion> dialog = crearDialogoUbicacion("Agregar Ubicación", null);
        dialog.showAndWait().ifPresent(nueva -> {
            if (nueva.getNombre().isBlank()) {
                mostrarAlerta("El nombre no puede estar vacío.", Alert.AlertType.WARNING);
                return;
            }
            if (ubicacionExiste(nueva.getNombre())) {
                mostrarAlerta("La ubicación ya existe.", Alert.AlertType.WARNING);
                return;
            }
            insertarUbicacion(nueva);
        });
    }

    private void mostrarDialogoEditar() {
        // Guardia de seguridad: solo admin
        if (!isAdmin) { mostrarAlerta("No autorizado.", Alert.AlertType.WARNING); return; }

        Ubicacion seleccionada = tablaUbicaciones.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarAlerta("Seleccione una ubicación.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Ubicacion> dialog = crearDialogoUbicacion("Editar Ubicación", seleccionada);
        dialog.showAndWait().ifPresent(editada -> {
            // Si cambia el nombre, valida duplicado
            if (!editada.getNombre().equalsIgnoreCase(seleccionada.getNombre())
                    && ubicacionExiste(editada.getNombre())) {
                mostrarAlerta("Nombre duplicado.", Alert.AlertType.WARNING);
                return;
            }
            actualizarUbicacion(seleccionada, editada);
        });
    }

    /** Construye el diálogo de alta/edición con ComboBox de edificios (por nombre) */
    private Dialog<Ubicacion> crearDialogoUbicacion(String titulo, Ubicacion existente) {
        Dialog<Ubicacion> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);

        ButtonType guardarBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        // Formulario
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField txtNombre = new TextField();           // -> UNIDADES.NOMBRE
        TextField txtDescripcion = new TextField();      // -> UNIDADES.CODIGO
        ComboBox<String> cmbEdificio = new ComboBox<>(cargarEdificios()); // nombres de edificios
        CheckBox chkActivo = new CheckBox("Activo");     // -> UNIDADES.ACTIVO 'S'/'N'

        if (existente != null) {
            txtNombre.setText(existente.getNombre());
            txtDescripcion.setText(existente.getDescripcion());
            cmbEdificio.setValue(existente.getEdificio());
            chkActivo.setSelected("Activo".equalsIgnoreCase(existente.getEstado()));
        } else {
            chkActivo.setSelected(true);
        }

        grid.addRow(0, new Label("Nombre:"), txtNombre);
        grid.addRow(1, new Label("Descripción/Código:"), txtDescripcion);
        grid.addRow(2, new Label("Edificio:"), cmbEdificio);
        grid.addRow(3, new Label("Estado:"), chkActivo);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(boton -> {
            if (boton == guardarBtn) {
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

    private void insertarUbicacion(Ubicacion u) {
        // Guardia de seguridad: solo admin
        if (!isAdmin) { mostrarAlerta("No autorizado.", Alert.AlertType.WARNING); return; }

        // SQL CORRECTO: la columna se llama CODIGO (no "descripcion")
        final String sql =
                "INSERT INTO unidades (nombre, codigo, id_edificio, activo) " +
                        "VALUES (?, ?, (SELECT id_edificio FROM edificios WHERE nombre = ?), ?)";

        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getDescripcion()); // -> CODIGO
            ps.setString(3, u.getEdificio());
            ps.setString(4, "Activo".equalsIgnoreCase(u.getEstado()) ? "S" : "N");
            ps.executeUpdate();
            cargarDatos(); // recarga tabla
        } catch (SQLException e) {
            mostrarAlerta("No se pudo agregar la ubicación.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void actualizarUbicacion(Ubicacion original, Ubicacion nueva) {
        // Guardia de seguridad: solo admin
        if (!isAdmin) { mostrarAlerta("No autorizado.", Alert.AlertType.WARNING); return; }

        // Actualiza por nombre (si tienes UNIQUE mejor actualizar por una PK/ID)
        final String sql =
                "UPDATE unidades " +
                        "   SET nombre = ?, " +
                        "       codigo = ?, " +
                        "       id_edificio = (SELECT id_edificio FROM edificios WHERE nombre = ?), " +
                        "       activo = ?, " +
                        "       actualizado_en = SYSTIMESTAMP " +
                        " WHERE nombre = ?";

        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nueva.getNombre());
            ps.setString(2, nueva.getDescripcion()); // -> CODIGO
            ps.setString(3, nueva.getEdificio());
            ps.setString(4, "Activo".equalsIgnoreCase(nueva.getEstado()) ? "S" : "N");
            ps.setString(5, original.getNombre());
            ps.executeUpdate();
            cargarDatos(); // recarga tabla
        } catch (SQLException e) {
            mostrarAlerta("No se pudo actualizar la ubicación.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    // ===== Utilidades =====
    private boolean ubicacionExiste(String nombre) {
        final String sql = "SELECT COUNT(*) FROM unidades WHERE UPPER(nombre) = UPPER(?)";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            // Ante error al validar, no bloquear (retorna false)
            return false;
        }
    }

    /** Carga nombres de edificios (puedes filtrar a activos si prefieres) */
    private ObservableList<String> cargarEdificios() {
        ObservableList<String> edificios = FXCollections.observableArrayList();
        final String sql = "SELECT nombre FROM edificios ORDER BY nombre";
        try (Connection cn = getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) edificios.add(rs.getString("nombre"));
        } catch (SQLException e) {
            mostrarAlerta("Error al cargar edificios.", Alert.AlertType.ERROR);
        }
        return edificios;
    }

    // ===== Infra de conexión (mismo patrón que el login) =====
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

    // ===== Alertas (MISMA FIRMA QUE EN EdificiosView: 2 PARÁMETROS) =====
    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Ubicaciones");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // ===== Modelo =====
    public static class Ubicacion {
        private final SimpleStringProperty nombre, descripcion, edificio, estado;
        public Ubicacion(String nombre, String descripcion, String edificio, String estado) {
            this.nombre = new SimpleStringProperty(nombre);           // UNIDADES.NOMBRE
            this.descripcion = new SimpleStringProperty(descripcion); // UNIDADES.CODIGO (mostrado como "Descripción")
            this.edificio = new SimpleStringProperty(edificio);       // EDIFICIOS.NOMBRE (se resuelve a ID en SQL)
            this.estado = new SimpleStringProperty(estado);           // 'Activo'/'Inactivo' (mapeo S/N)
        }
        public String getNombre() { return nombre.get(); }
        public void setNombre(String n) { nombre.set(n); }
        public String getDescripcion() { return descripcion.get(); }
        public void setDescripcion(String d) { descripcion.set(d); }
        public String getEdificio() { return edificio.get(); }
        public void setEdificio(String e) { edificio.set(e); }
        public String getEstado() { return estado.get(); }
        public void setEstado(String est) { estado.set(est); }
        public SimpleStringProperty nombreProperty() { return nombre; }
        public SimpleStringProperty descripcionProperty() { return descripcion; }
        public SimpleStringProperty edificioProperty() { return edificio; }
        public SimpleStringProperty estadoProperty() { return estado; }
    }
}
