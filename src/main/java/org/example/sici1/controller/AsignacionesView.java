package org.example.sici1.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TableCell;
import javafx.scene.layout.GridPane;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// JasperReports
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;

public class AsignacionesView {

    // ===================== Configuración de Conexión =====================
    private static final String TNS_ADMIN_PATH = "C:/Users/VICTOR UZZIEL/IdeaProjects/SICI1/src/main/resources/org/example/sici1/Wallet_CN4PI23N1E6J6TZS";
    private static final String JDBC_URL = "jdbc:oracle:thin:@cn4pi23n1e6j6tzs_high";
    private static final String DB_USER = "ADMIN";
    private static final String DB_PASS = "Knoxotics_Kashima50";

    private static DataSource dataSource;
    private static final ExecutorService IO_POOL = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() / 2)
    );

    static {
        System.setProperty("oracle.net.tns_admin", TNS_ADMIN_PATH);
        initializeDataSource();
    }

    private static void initializeDataSource() {
        try {
            PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
            pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
            pds.setURL(JDBC_URL);
            pds.setUser(DB_USER);
            pds.setPassword(DB_PASS);
            pds.setInitialPoolSize(3);
            pds.setMinPoolSize(3);
            pds.setMaxPoolSize(15);
            pds.setAbandonedConnectionTimeout(60);
            pds.setInactiveConnectionTimeout(120);
            pds.setTimeoutCheckInterval(30);
            pds.setValidateConnectionOnBorrow(true);
            pds.setConnectionWaitTimeout(60);
            dataSource = pds;
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo inicializar el pool UCP", e);
        }
    }

    // ============================= Componentes FXML =============================
    @FXML private Label lblInventarioId, lblInfo;
    @FXML private TextField txtCodigoBien;
    @FXML private Button btnCrearInventario, btnAgregarBien, btnImprimir;

    @FXML private TableView<DetalleRow> tablaDetalle;
    @FXML private TableColumn<DetalleRow, String> colCodigo, colDescripcion, colEstado;
    @FXML private TableColumn<DetalleRow, Void> colDesasignar;

    // ============================= Estado =============================
    private final ObservableList<DetalleRow> detalles = FXCollections.observableArrayList();
    private Long idInventarioActual = null;

    // Empleado que crea el inventario (ajústalo desde tu flujo de login)
    private Long idEmpleadoActual = 1L;
    public void setIdEmpleadoActual(Long id) { this.idEmpleadoActual = id; }

    // Caches para combos
    private final ObservableList<Item> cacheUnidadesAdmin = FXCollections.observableArrayList(); // UNIDADES_ADMINISTRATIVAS
    private final ObservableList<Item> cacheEspacios = FXCollections.observableArrayList();      // UNIDADES (espacios)

    // ============================= Ciclo de vida =============================
    @FXML
    public void initialize() {
        configurarTabla();
        configurarBotones();
        precargarCacheCombos();
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(d -> d.getValue().codigoProperty());
        colDescripcion.setCellValueFactory(d -> d.getValue().descripcionProperty());
        colEstado.setCellValueFactory(d -> d.getValue().estadoProperty());
        tablaDetalle.setItems(detalles);

        colDesasignar.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Desasignar");
            {
                btn.setStyle("-fx-background-color:#d32f2f; -fx-text-fill:white; -fx-font-size:12;");
                btn.setOnAction(e -> {
                    DetalleRow r = getTableView().getItems().get(getIndex());
                    confirmarYDesasignar(r);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void configurarBotones() {
        btnCrearInventario.setOnAction(e -> abrirDialogoEncabezado());
        btnAgregarBien.setOnAction(e -> onAgregarBien());
        btnImprimir.setOnAction(e -> imprimir());
    }

    private void precargarCacheCombos() {
        IO_POOL.execute(() -> {
            try {
                // Cargar en background…
                ObservableList<Item> uas = cargarItems(
                        "SELECT id, nombre FROM ID_UNIDADES_ADMINISTRATIVAS WHERE ACTIVO = 'S' ORDER BY nombre");
                ObservableList<Item> espacios = cargarItems(
                        "SELECT id_unidad, nombre FROM UNIDADES WHERE ACTIVO = 'S' ORDER BY nombre");
                // …y aplicar en el hilo FX
                Platform.runLater(() -> {
                    cacheUnidadesAdmin.setAll(uas);
                    cacheEspacios.setAll(espacios);
                });
            } catch (SQLException e) {
                Platform.runLater(() ->
                        showAlert("Error al cargar catálogos: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    // ========================== Encabezado de Inventario ==========================
    private void abrirDialogoEncabezado() {
        Dialog<HeaderData> dialog = new Dialog<>();
        dialog.setTitle("Encabezado de Inventario");
        ButtonType ok = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        ComboBox<Item> cmbUnidadAdmin = new ComboBox<>(cacheUnidadesAdmin);
        ComboBox<Item> cmbEspacio = new ComboBox<>(cacheEspacios);
        DatePicker dpFecha = new DatePicker(LocalDate.now());

        GridPane grid = new GridPane();
        grid.setVgap(10); grid.setHgap(10);
        grid.add(new Label("Unidad administrativa:"), 0, 0); grid.add(cmbUnidadAdmin, 1, 0);
        grid.add(new Label("Espacio (UNIDADES):"), 0, 1);    grid.add(cmbEspacio, 1, 1);
        grid.add(new Label("Fecha:"), 0, 2);                 grid.add(dpFecha, 1, 2);
        dialog.getDialogPane().setContent(grid);

        final Button okBtn = (Button) dialog.getDialogPane().lookupButton(ok);
        Runnable validate = () -> okBtn.setDisable(
                cmbUnidadAdmin.getValue() == null || cmbEspacio.getValue() == null || dpFecha.getValue() == null
        );
        cmbUnidadAdmin.valueProperty().addListener((a,b,c)->validate.run());
        cmbEspacio.valueProperty().addListener((a,b,c)->validate.run());
        dpFecha.valueProperty().addListener((a,b,c)->validate.run());
        validate.run();

        dialog.setResultConverter(bt -> bt == ok ? new HeaderData(cmbUnidadAdmin.getValue(), cmbEspacio.getValue(), dpFecha.getValue()) : null);
        dialog.showAndWait().ifPresent(this::guardarEncabezado);
    }

    private void guardarEncabezado(HeaderData hd) {
        IO_POOL.execute(() -> {
            try (Connection cn = dataSource.getConnection()) {

                final String sqlExists = """
                    SELECT ID_INVENTARIO
                    FROM INVENTARIO
                    WHERE ID_UNIDAD_ADMINISTRATIVA = ?
                      AND ID_UNIDAD = ?
                      AND FECHA = ?
                      AND ACTIVO = 'S'
                """;
                Long idExistente = null;
                try (PreparedStatement ps = cn.prepareStatement(sqlExists)) {
                    ps.setLong(1, hd.unidadAdmin().id());
                    ps.setLong(2, hd.espacio().id());
                    ps.setDate(3, Date.valueOf(hd.fecha()));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) idExistente = rs.getLong(1);
                    }
                }
                if (idExistente != null) {
                    final Long toOpen = idExistente;
                    Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                        a.setTitle("Inventario ya existe");
                        a.setHeaderText("Ya hay un inventario con esa UA + Espacio + Fecha (activo).");
                        a.setContentText("¿Quieres abrirlo para continuar?");
                        ButtonType abrir = new ButtonType("Abrir existente", ButtonBar.ButtonData.OK_DONE);
                        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                        a.getButtonTypes().setAll(abrir, cancelar);
                        a.showAndWait().ifPresent(bt -> {
                            if (bt == abrir) {
                                idInventarioActual = toOpen;
                                lblInventarioId.setText("Inventario: " + idInventarioActual);
                                actualizarInfo("Inventario existente abierto. Puedes agregar/quitar bienes.");
                                IO_POOL.execute(() -> cargarDetalleInventario(idInventarioActual));
                            }
                        });
                    });
                    return;
                }


                final String sqlInsert = """
                    INSERT INTO INVENTARIO
                        (ID_UNIDAD_ADMINISTRATIVA, ID_UNIDAD, ID_EMPLEADO, FECHA, ACTIVO, CREADO_EN, ACTUALIZADO_EN)
                    VALUES
                        (?, ?, ?, ?, 'S', SYSTIMESTAMP, SYSTIMESTAMP)
                """;

                try (PreparedStatement ps = cn.prepareStatement(sqlInsert, new String[]{"ID_INVENTARIO"})) {
                    ps.setLong(1, hd.unidadAdmin().id());     // ID_UNIDAD_ADMINISTRATIVA
                    ps.setLong(2, hd.espacio().id());         // ID_UNIDAD
                    ps.setLong(3, idEmpleadoActual);          // ID_EMPLEADO
                    ps.setDate(4, Date.valueOf(hd.fecha()));  // FECHA
                    ps.executeUpdate();
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        if (gk.next()) idInventarioActual = gk.getLong(1);
                    }
                }

                Platform.runLater(() -> {
                    lblInventarioId.setText("Inventario: " + idInventarioActual);
                    actualizarInfo("Encabezado guardado. Ahora agrega bienes por código.");
                    detalles.clear();
                });

            } catch (SQLException ex) {
                Platform.runLater(() ->
                        showAlert("No se pudo guardar el encabezado: " + ex.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    private void cargarDetalleInventario(long idInventario) {
        final String q = """
            SELECT d.ID_DETALLE, b.CODIGO_INVENTARIO, b.DESCRIPCION, b.ESTADO
            FROM DETALLE_INVENTARIO d
            JOIN BIENES b ON b.ID_BIEN = d.ID_BIEN
            WHERE d.ID_INVENTARIO = ?
            ORDER BY d.ID_DETALLE
            """;
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(q)) {
            ps.setLong(1, idInventario);
            try (ResultSet rs = ps.executeQuery()) {
                ObservableList<DetalleRow> rows = FXCollections.observableArrayList();
                while (rs.next()) {
                    rows.add(new DetalleRow(
                            rs.getLong("ID_DETALLE"),
                            rs.getString("CODIGO_INVENTARIO"),
                            rs.getString("DESCRIPCION"),
                            rs.getString("ESTADO")
                    ));
                }
                Platform.runLater(() -> {
                    detalles.setAll(rows);
                    if (rows.isEmpty()) actualizarInfo("Inventario abierto (sin bienes). Agrega por código.");
                    else actualizarInfo("Inventario abierto con " + rows.size() + " bien(es).");
                });
            }
        } catch (SQLException ex) {
            Platform.runLater(() ->
                    showAlert("Error al cargar el detalle: " + ex.getMessage(), Alert.AlertType.ERROR));
        }
    }

    // ====================== Gestión de Bienes ======================
    private void onAgregarBien() {
        if (idInventarioActual == null) {
            showAlert("Primero crea el encabezado.", Alert.AlertType.INFORMATION);
            return;
        }
        String codigo = txtCodigoBien.getText().trim();
        if (codigo.isEmpty()) {
            showAlert("Ingresa un código de bien.", Alert.AlertType.WARNING);
            return;
        }
        txtCodigoBien.clear();
        IO_POOL.execute(() -> insertarDetallePorCodigo(codigo));
    }

    private void insertarDetallePorCodigo(String codigo) {
        final String query = """
            SELECT b.id_bien, b.descripcion, b.estado, b.codigo_inventario
            FROM bienes b
            WHERE UPPER(b.codigo_inventario) = UPPER(?)
            """;
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(query)) {

            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    Platform.runLater(() ->
                            showAlert("No existe un bien con ese código.", Alert.AlertType.WARNING));
                    return;
                }

                long idBien = rs.getLong("id_bien");
                String desc = rs.getString("descripcion");
                String est = rs.getString("estado");
                String cod = rs.getString("codigo_inventario");

                if (existeDetalle(cn, idInventarioActual, idBien)) {
                    Platform.runLater(() ->
                            showAlert("Ese bien ya está agregado a este inventario.", Alert.AlertType.WARNING));
                    return;
                }

                final String ins = "INSERT INTO DETALLE_INVENTARIO (ID_INVENTARIO, ID_BIEN, ACTIVO) VALUES (?, ?, 'S')";
                try (PreparedStatement psi = cn.prepareStatement(ins, new String[]{"ID_DETALLE"})) {
                    psi.setLong(1, idInventarioActual);
                    psi.setLong(2, idBien);
                    psi.executeUpdate();

                    long idDetalle = -1L;
                    try (ResultSet gk = psi.getGeneratedKeys()) {
                        if (gk.next()) idDetalle = gk.getLong(1);
                    }

                    DetalleRow row = new DetalleRow(idDetalle, cod, desc, est);
                    Platform.runLater(() -> detalles.add(row));
                }
            }
        } catch (SQLException ex) {
            if (isUniqueViolation(ex)) {
                Platform.runLater(() ->
                        showAlert("Ese bien ya está agregado a este inventario.", Alert.AlertType.WARNING));
            } else {
                Platform.runLater(() ->
                        showAlert("Error al agregar bien: " + ex.getMessage(), Alert.AlertType.ERROR));
            }
        }
    }

    private boolean existeDetalle(Connection cn, long idInventario, long idBien) throws SQLException {
        final String sql = "SELECT 1 FROM DETALLE_INVENTARIO WHERE ID_INVENTARIO = ? AND ID_BIEN = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idInventario);
            ps.setLong(2, idBien);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ============================ Desasignar Bienes ============================
    private void confirmarYDesasignar(DetalleRow row) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Desasignar bien");
        a.setHeaderText("Quitar del inventario actual");
        a.setContentText("¿Desasignar el bien " + row.getCodigo() + " de este inventario?");
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) IO_POOL.execute(() -> desasignar(row));
        });
    }

    private void desasignar(DetalleRow row) {
        final String sql = "DELETE FROM DETALLE_INVENTARIO WHERE ID_DETALLE = ?";
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, row.getIdDetalle());
            int n = ps.executeUpdate();
            Platform.runLater(() -> {
                if (n > 0) detalles.remove(row);
                else showAlert("No se encontró el registro a desasignar.", Alert.AlertType.WARNING);
            });
        } catch (SQLException ex) {
            Platform.runLater(() ->
                    showAlert("Error al desasignar: " + ex.getMessage(), Alert.AlertType.ERROR));
        }
    }

    // ============================== Impresión ==============================
    private void imprimir() {
        if (idInventarioActual == null) {
            showAlert("Crea el encabezado antes de imprimir.", Alert.AlertType.INFORMATION);
            return;
        }
        IO_POOL.execute(() -> {
            try (Connection conexion = dataSource.getConnection()) {
                // Cargar el archivo .jasper desde resources
                InputStream input = getClass().getResourceAsStream("/Inventario.jasper");
                if (input == null) {
                    Platform.runLater(() -> showAlert("No se encontró el recurso Inventario.jasper", Alert.AlertType.ERROR));
                    return;
                }
                JasperReport reporte = (JasperReport) JRLoader.loadObject(input);

                Map<String, Object> parametros = new HashMap<>();
                // Ajusta a los parámetros reales de tu reporte
                parametros.put("p_id_inventario", idInventarioActual);
                parametros.put("fecha", Date.valueOf(LocalDate.now()));

                JasperPrint jasperPrint = JasperFillManager.fillReport(reporte, parametros, conexion);

                JasperViewer.viewReport(jasperPrint, false);
                JasperExportManager.exportReportToPdfFile(jasperPrint, "reporte_inventario_" + idInventarioActual + ".pdf");

                Platform.runLater(() ->
                        showAlert("Reporte generado correctamente.", Alert.AlertType.INFORMATION));
            } catch (Exception e) {
                Platform.runLater(() ->
                        showAlert("Error al generar el reporte: " + e.getMessage(), Alert.AlertType.ERROR));
            }
        });
    }

    // ==================== Utilidades ====================
    private ObservableList<Item> cargarItems(String sql, Object... params) throws SQLException {
        ObservableList<Item> items = FXCollections.observableArrayList();
        try (Connection cn = dataSource.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(new Item(rs.getLong(1), rs.getString(2)));
            }
        }
        return items;
    }

    private void actualizarInfo(String txt) {
        lblInfo.setText(txt == null ? "" : txt);
    }

    private void showAlert(String msg, Alert.AlertType tipo) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(tipo);
            alert.setTitle("Inventario");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> showAlert(msg, tipo));
        }
    }

    private boolean isUniqueViolation(SQLException ex) {
        // ORA-00001: unique constraint violated
        return ex != null && ("23000".equals(ex.getSQLState()) ||
                (ex.getMessage() != null && ex.getMessage().contains("ORA-00001")));
    }

    // ==================== Clases auxiliares ====================
    public static final class Item {
        private final long id;
        private final String nombre;
        public Item(long id, String nombre) { this.id = id; this.nombre = nombre; }
        public long id() { return id; }
        @Override public String toString() { return nombre; }
    }

    public static final class DetalleRow {
        private final SimpleLongProperty idDetalle = new SimpleLongProperty();
        private final SimpleStringProperty codigo = new SimpleStringProperty();
        private final SimpleStringProperty descripcion = new SimpleStringProperty();
        private final SimpleStringProperty estado = new SimpleStringProperty();

        public DetalleRow(long idDetalle, String codigo, String descripcion, String estado) {
            this.idDetalle.set(idDetalle);
            this.codigo.set(codigo);
            this.descripcion.set(descripcion);
            this.estado.set(estado);
        }
        public long getIdDetalle() { return idDetalle.get(); }
        public String getCodigo() { return codigo.get(); }
        public String getDescripcion() { return descripcion.get(); }
        public String getEstado() { return estado.get(); }

        public SimpleStringProperty codigoProperty() { return codigo; }
        public SimpleStringProperty descripcionProperty() { return descripcion; }
        public SimpleStringProperty estadoProperty() { return estado; }
    }

    private record HeaderData(Item unidadAdmin, Item espacio, LocalDate fecha) {}
}
