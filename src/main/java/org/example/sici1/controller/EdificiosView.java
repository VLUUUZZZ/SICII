package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;

public class EdificiosView {

    @FXML private TableView<Edificio> tablaEdificios;
    @FXML private TableColumn<Edificio, String> colNombre;
    @FXML private TableColumn<Edificio, String> colEstado;
    @FXML private TableColumn<Edificio, Void> colSwitch;
    @FXML private TextField txtBuscarNombre;
    @FXML private Button btnAgregar, btnEditar, btnBuscarNombre;

    private final ObservableList<Edificio> edificios = FXCollections.observableArrayList();
    private final FilteredList<Edificio> edificiosFiltrados = new FilteredList<>(edificios, p -> true);

    // === PERMISOS CORRECTOS ===
    private final String userRole = UserSession.getInstance().getRole();
    private final boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);   // <-- SOLO ADMIN MODIFICA
    private static final String SCHEMA_OWNER = "ADMIN";

    @FXML
    public void initialize() {
        tablaEdificios.setItems(edificiosFiltrados);

        colNombre.setCellValueFactory(data -> data.getValue().nombreProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());

        // Visibilidad estricta por rol
        btnAgregar.setVisible(isAdmin);
        btnEditar.setVisible(isAdmin);
        colSwitch.setVisible(isAdmin);    // switch solo admin
        colEstado.setVisible(true);       // estado siempre visible

        // El switch SOLO se crea si es admin
        if (isAdmin) {
            colSwitch.setCellFactory(col -> new TableCell<>() {
                private final CheckBox check = new CheckBox();
                {
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    check.setOnAction(e -> {
                        Edificio edificio = getTableView().getItems().get(getIndex());
                        edificio.setEstado(check.isSelected() ? "Activo" : "Inactivo");
                        actualizarEstado(edificio); // aplica en BD
                        tablaEdificios.refresh();
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
        }

        // Buscador
        btnBuscarNombre.setOnAction(e -> buscarEdificio());
        txtBuscarNombre.textProperty().addListener((obs, oldVal, newVal) -> buscarEdificio());

        // Carga inicial
        cargarEdificios();

        // Acciones SOLO para admin
        if (isAdmin) {
            btnAgregar.setOnAction(e -> agregarEdificio());
            btnEditar.setOnAction(e -> editarEdificio());
        }
    }

    private void buscarEdificio() {
        String texto = txtBuscarNombre.getText() == null ? "" : txtBuscarNombre.getText().toLowerCase().trim();
        edificiosFiltrados.setPredicate(edificio ->
                texto.isEmpty() || edificio.getNombre().toLowerCase().contains(texto)
        );
    }

    private void cargarEdificios() {
        edificios.clear();
        String sql = "SELECT nombre, activo FROM edificios ORDER BY nombre";
        try (Connection cn = getConnection();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                String estado = "S".equals(rs.getString("activo")) ? "Activo" : "Inactivo";
                edificios.add(new Edificio(nombre, estado));
            }
        } catch (SQLException e) {
            mostrarAlerta("Error al cargar edificios.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void agregarEdificio() {
        // Guardia de seguridad: solo admin
        if (!isAdmin) { mostrarAlerta("No autorizado.", Alert.AlertType.WARNING); return; }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Agregar Edificio");
        dialog.setHeaderText(null);
        dialog.setContentText("Nombre del edificio:");

        dialog.showAndWait().ifPresent(nombre -> {
            String finalNombre = nombre.trim();
            if (finalNombre.isEmpty()) {
                mostrarAlerta("El nombre no puede estar vacío.", Alert.AlertType.WARNING);
                return;
            }
            if (existeEdificio(finalNombre)) {
                mostrarAlerta("Ya existe un edificio con ese nombre.", Alert.AlertType.WARNING);
                return;
            }

            String sql = "INSERT INTO edificios (nombre, activo) VALUES (?, 'S')";
            try (Connection cn = getConnection();
                 PreparedStatement ps = cn.prepareStatement(sql)) {

                ps.setString(1, finalNombre);
                ps.executeUpdate();

                edificios.add(new Edificio(finalNombre, "Activo"));
                buscarEdificio();
            } catch (SQLException e) {
                mostrarAlerta("Error al agregar el edificio.", Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        });
    }

    private void editarEdificio() {
        // Guardia de seguridad: solo admin
        if (!isAdmin) { mostrarAlerta("No autorizado.", Alert.AlertType.WARNING); return; }

        Edificio seleccionado = tablaEdificios.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Seleccione un edificio para editar.", Alert.AlertType.WARNING);
            return;
        }

        TextInputDialog dialog = new TextInputDialog(seleccionado.getNombre());
        dialog.setTitle("Editar Edificio");
        dialog.setHeaderText(null);
        dialog.setContentText("Nuevo nombre:");

        dialog.showAndWait().ifPresent(nuevo -> {
            String finalNombre = nuevo.trim();
            if (finalNombre.isEmpty()) {
                mostrarAlerta("El nombre no puede estar vacío.", Alert.AlertType.WARNING);
                return;
            }
            if (!finalNombre.equalsIgnoreCase(seleccionado.getNombre()) && existeEdificio(finalNombre)) {
                mostrarAlerta("Ya existe un edificio con ese nombre.", Alert.AlertType.WARNING);
                return;
            }

            String sql = "UPDATE edificios SET nombre = ?, actualizado_en = SYSTIMESTAMP WHERE nombre = ?";
            try (Connection cn = getConnection();
                 PreparedStatement ps = cn.prepareStatement(sql)) {

                ps.setString(1, finalNombre);
                ps.setString(2, seleccionado.getNombre());
                ps.executeUpdate();

                seleccionado.setNombre(finalNombre);
                tablaEdificios.refresh();
                buscarEdificio();
            } catch (SQLException e) {
                mostrarAlerta("Error al editar el edificio.", Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        });
    }

    private void actualizarEstado(Edificio edificio) {
        // Guardia de seguridad: solo admin
        if (!isAdmin) { mostrarAlerta("No autorizado.", Alert.AlertType.WARNING); return; }

        String sql = "UPDATE edificios SET activo = ?, actualizado_en = SYSTIMESTAMP WHERE nombre = ?";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, "Activo".equalsIgnoreCase(edificio.getEstado()) ? "S" : "N");
            ps.setString(2, edificio.getNombre());
            ps.executeUpdate();
        } catch (SQLException e) {
            mostrarAlerta("Error al actualizar el estado.", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private boolean existeEdificio(String nombre) {
        String sql = "SELECT COUNT(*) FROM edificios WHERE UPPER(nombre) = UPPER(?)";
        try (Connection cn = getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Edificios");
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
            throw new SQLException("No se pudo cargar el driver Oracle", e);
        }
    }

    /** Modelo */
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
