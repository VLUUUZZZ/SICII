package org.example.sici1.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.*;
import java.util.Optional;

public class UnidadAdministrativaView {

    // === FXML: coincide con el FXML proporcionado ===
    @FXML private TableView<Unidad> tableUnidades;
    @FXML private TableColumn<Unidad, String> colNombre;
    @FXML private TableColumn<Unidad, String> colEstado;
    @FXML private TextField txtBuscar;
    @FXML private Button btnAgregar;
    @FXML private Button btnEditar;
    @FXML private Button btnBuscar;



    // === Datos + filtro ===
    private final ObservableList<Unidad> unidades = FXCollections.observableArrayList();
    private final FilteredList<Unidad> unidadesFiltradas = new FilteredList<>(unidades, p -> true);

    // === Permisos ===
    private final String userRole = safe(UserSession.getInstance() != null ? UserSession.getInstance().getRole() : "USER");
    private final boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);

    // === Config BD ===
    private static final String SCHEMA_OWNER = "ADMIN";
    private static final String TBL = "UNIDADES_ADMINISTRATIVAS";

    @FXML
    public void initialize() {
        configurarTabla();
        configurarBuscador();

        // Si por algún motivo el FXML cambiara y no hay botón, evitamos NPE
        if (btnAgregar != null) btnAgregar.setVisible(isAdmin);
        if (btnEditar  != null) btnEditar.setVisible(isAdmin);

        tableUnidades.setItems(unidadesFiltradas);
        cargarUnidades();

        if (isAdmin) {
            if (btnAgregar != null) {
                btnAgregar.setOnAction(e -> mostrarDialogoNuevo());
            }
            if (btnEditar != null) {
                btnEditar.setOnAction(e -> {
                    Unidad sel = tableUnidades.getSelectionModel().getSelectedItem();
                    if (sel == null) {
                        mostrarAlerta("Selecciona una fila", "Elige una unidad para editar.", Alert.AlertType.INFORMATION);
                        return;
                    }
                    mostrarDialogoEditar(sel);
                });
            }
        }

        // Doble clic: abre editar si eres admin
        tableUnidades.setRowFactory(tv -> {
            TableRow<Unidad> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2 && !row.isEmpty() && isAdmin) {
                    mostrarDialogoEditar(row.getItem());
                }
            });
            return row;
        });
    }

    private void configurarTabla() {
        // Si usas las property() está bien; PropertyValueFactory también funciona y es claro con FXML
        if (colNombre != null) {
            colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
            // Alternativa equivalente:
            // colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        }
        if (colEstado != null) {
            colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());
            // Alternativa:
            // colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        }
    }

    private void configurarBuscador() {
        if (btnBuscar != null) btnBuscar.setOnAction(e -> buscarUnidades());
        if (txtBuscar != null) {
            txtBuscar.setOnAction(e -> buscarUnidades());
            txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> buscarUnidades());
        }
    }

    private void buscarUnidades() {
        String texto = (txtBuscar == null || txtBuscar.getText() == null) ? "" : txtBuscar.getText().toLowerCase().trim();
        unidadesFiltradas.setPredicate(u ->
                texto.isEmpty() ||
                        (u.getNombre() != null && u.getNombre().toLowerCase().contains(texto)) ||
                        (u.getEstado() != null && u.getEstado().toLowerCase().contains(texto))
        );
    }

    // === Diálogo: NUEVO (nombre + estado) ===
    private void mostrarDialogoNuevo() {
        if (!isAdmin) { mostrarAlerta("No autorizado", "Solo un administrador puede crear unidades.", Alert.AlertType.WARNING); return; }

        Dialog<Unidad> dialog = new Dialog<>();
        dialog.setTitle("Nueva Unidad Administrativa");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ejemplo: División Académica...");
        ComboBox<String> cmbEstado = new ComboBox<>();
        cmbEstado.getItems().addAll("Activo", "Inactivo");
        cmbEstado.setValue("Activo");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(12);
        grid.add(new Label("Nombre:"), 0, 0); grid.add(txtNombre, 1, 0);
        grid.add(new Label("Estado:"), 0, 1); grid.add(cmbEstado, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String nombre = safe(txtNombre.getText()).trim();
                if (nombre.isEmpty()) {
                    mostrarAlerta("Error", "El nombre no puede estar vacío.", Alert.AlertType.ERROR);
                    return null;
                }
                return new Unidad(0, nombre, cmbEstado.getValue());
            }
            return null;
        });

        Optional<Unidad> res = dialog.showAndWait();
        res.ifPresent(u -> {
            if (unidadExiste(u.getNombre())) {
                mostrarAlerta("Error", "Ya existe una unidad con ese nombre.", Alert.AlertType.ERROR);
                return;
            }
            insertarUnidad(u.getNombre(), "Activo".equalsIgnoreCase(u.getEstado()));
        });
    }

    // === Diálogo: EDITAR (nombre + estado) ===
    private void mostrarDialogoEditar(Unidad unidad) {
        if (!isAdmin) { mostrarAlerta("No autorizado", "Solo un administrador puede editar unidades.", Alert.AlertType.WARNING); return; }

        Dialog<Unidad> dialog = new Dialog<>();
        dialog.setTitle("Editar Unidad Administrativa");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtNombre = new TextField(unidad.getNombre());
        ComboBox<String> cmbEstado = new ComboBox<>();
        cmbEstado.getItems().addAll("Activo", "Inactivo");
        cmbEstado.setValue(unidad.getEstado() == null || unidad.getEstado().isBlank() ? "Activo" : unidad.getEstado());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(12);
        grid.add(new Label("Nombre:"), 0, 0); grid.add(txtNombre, 1, 0);
        grid.add(new Label("Estado:"), 0, 1); grid.add(cmbEstado, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String nuevoNombre = safe(txtNombre.getText()).trim();
                if (nuevoNombre.isEmpty()) {
                    mostrarAlerta("Error", "El nombre no puede estar vacío.", Alert.AlertType.ERROR);
                    return null;
                }
                if (!nuevoNombre.equalsIgnoreCase(unidad.getNombre()) && unidadExiste(nuevoNombre)) {
                    mostrarAlerta("Error", "Ya existe una unidad con ese nombre.", Alert.AlertType.ERROR);
                    return null;
                }
                return new Unidad(unidad.getId(), nuevoNombre, cmbEstado.getValue());
            }
            return null;
        });

        Optional<Unidad> res = dialog.showAndWait();
        res.ifPresent(u -> actualizarUnidad(unidad, u.getNombre(), u.getEstado()));
    }

    // === BD ===
    private boolean unidadExiste(String nombre) {
        String sql = "SELECT COUNT(*) FROM " + TBL + " WHERE UPPER(NOMBRE) = UPPER(?)";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            mostrarAlerta("Error", "Error al verificar existencia.", Alert.AlertType.ERROR);
            return false;
        }
    }

    private void cargarUnidades() {
        unidades.clear();
        String sql = "SELECT ID, NOMBRE, ACTIVO FROM " + TBL + " ORDER BY NOMBRE";
        try (Connection cn = getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String nombre = rs.getString("NOMBRE");
                String estado = "S".equalsIgnoreCase(rs.getString("ACTIVO")) ? "Activo" : "Inactivo";
                unidades.add(new Unidad(id, nombre, estado));
            }
        } catch (SQLException ex) {
            mostrarAlerta("Error", "No se pudo cargar unidades.", Alert.AlertType.ERROR);
        }
    }

    private void insertarUnidad(String nombre, boolean activo) {
        if (!isAdmin) { mostrarAlerta("No autorizado", "Solo un administrador puede crear unidades.", Alert.AlertType.WARNING); return; }
        String sql = "INSERT INTO " + TBL + " (NOMBRE, ACTIVO) VALUES (?, ?)";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setString(2, activo ? "S" : "N");
            ps.executeUpdate();
            cargarUnidades();
        } catch (SQLException ex) {
            mostrarAlerta("Error", "No se pudo guardar la unidad.", Alert.AlertType.ERROR);
        }
    }

    private void actualizarUnidad(Unidad unidad, String nuevoNombre, String nuevoEstado) {
        if (!isAdmin) { mostrarAlerta("No autorizado", "Solo un administrador puede editar unidades.", Alert.AlertType.WARNING); return; }
        String sql = "UPDATE " + TBL + " SET NOMBRE = ?, ACTIVO = ?, ACTUALIZADO_EN = SYSTIMESTAMP WHERE ID = ?";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nuevoNombre);
            ps.setString(2, "Activo".equalsIgnoreCase(nuevoEstado) ? "S" : "N");
            ps.setInt(3, unidad.getId());
            ps.executeUpdate();

            // Refresca modelo local
            unidad.setNombre(nuevoNombre);
            unidad.setEstado(nuevoEstado);
            tableUnidades.refresh();
        } catch (SQLException ex) {
            mostrarAlerta("Error", "No se pudo actualizar la unidad.", Alert.AlertType.ERROR);
        }
    }

    // Mantengo por si lo llamas desde otro flujo
    private void actualizarEstadoUnidad(Unidad unidad) {
        if (!isAdmin) { mostrarAlerta("No autorizado", "Solo un administrador puede cambiar el estado.", Alert.AlertType.WARNING); return; }
        String sql = "UPDATE " + TBL + " SET ACTIVO = ?, ACTUALIZADO_EN = SYSTIMESTAMP WHERE ID = ?";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, "Activo".equalsIgnoreCase(unidad.getEstado()) ? "S" : "N");
            ps.setInt(2, unidad.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            mostrarAlerta("Error", "No se pudo cambiar el estado.", Alert.AlertType.ERROR);
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

    // === Modelo ===
    public static class Unidad {
        private final SimpleIntegerProperty id;        // PK
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty estado;

        public Unidad(int id, String nombre, String estado) {
            this.id = new SimpleIntegerProperty(id);
            this.nombre = new SimpleStringProperty(nombre);
            this.estado = new SimpleStringProperty(estado);
        }
        public int getId() { return id.get(); }
        public SimpleIntegerProperty idProperty() { return id; }

        public String getNombre() { return nombre.get(); }
        public void setNombre(String nombre) { this.nombre.set(nombre); }
        public SimpleStringProperty nombreProperty() { return nombre; }

        public String getEstado() { return estado.get(); }
        public void setEstado(String estado) { this.estado.set(estado); }
        public SimpleStringProperty estadoProperty() { return estado; }
    }

    // Util
    private static String safe(String s) { return s == null ? "" : s; }
}
