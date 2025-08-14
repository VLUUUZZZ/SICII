package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.print.PrinterJob;
import javafx.stage.FileChooser;

import java.io.*;
import java.sql.*;
import java.util.Arrays;
import java.util.Optional;

public class BienesView {

    // --- Controles con los mismos fx:id del FXML ---
    @FXML private TableView<Bien> tableBienes;
    @FXML private TableColumn<Bien, String> colCodigo, colDescripcion, colMarca, colModelo, colSerie, colEstado;
    @FXML private Button btnNuevo, btnImprimir, btnBuscarCodigo;
    @FXML private TextField txtBuscarCodigo;

    private final ObservableList<Bien> bienes = FXCollections.observableArrayList();
    private final FilteredList<Bien> bienesFiltrados = new FilteredList<>(bienes, p -> true);

    private final String userRole = UserSession.getInstance().getRole();
    private final boolean isAdmin = "ADMIN".equalsIgnoreCase(userRole);
    private static final String SCHEMA_OWNER = "ADMIN";

    // Posibles nombres de columnas en BD (ajústalos si tu esquema usa otros)
    private static final String[] COLS_CODIGO = {"codigo_inventario", "codigo", "id_bien"};
    private static final String[] COLS_DESC   = {"descripcion", "descripcion_bien"};
    private static final String[] COLS_MARCA  = {"marca"};
    private static final String[] COLS_MODELO = {"modelo"};
    private static final String[] COLS_SERIE  = {"numero_serie", "n_serie", "no_serie", "num_serie"};
    private static final String[] COLS_ESTADO = {"estado"};
    private static final String[] COLS_IMAGEN = {"imagen"};

    @FXML
    public void initialize() {
        // Mapeo columnas -> propiedades
        colCodigo.setCellValueFactory(d -> d.getValue().codigoProperty());
        colDescripcion.setCellValueFactory(d -> d.getValue().descripcionProperty());
        colMarca.setCellValueFactory(d -> d.getValue().marcaProperty());
        colModelo.setCellValueFactory(d -> d.getValue().modeloProperty());
        colSerie.setCellValueFactory(d -> d.getValue().numeroSerieProperty());
        colEstado.setCellValueFactory(d -> d.getValue().estadoProperty());

        // Permisos por rol
        btnNuevo.setVisible(isAdmin);

        // Acciones
        btnBuscarCodigo.setOnAction(e -> buscarPorCodigo());
        txtBuscarCodigo.setOnAction(e -> buscarPorCodigo());
        if (isAdmin) {
            btnNuevo.setOnAction(e -> mostrarDialogoBien(null, true));
        }

        // Doble clic: ver (usuario) / editar (admin)
        tableBienes.setRowFactory(tv -> {
            TableRow<Bien> row = new TableRow<>();
            row.setOnMouseClicked(evt -> {
                if (evt.getClickCount() == 2 && !row.isEmpty()) {
                    Bien b = row.getItem();
                    if (isAdmin) mostrarDialogoBien(b, false);
                    else verBien(b);
                }
            });
            return row;
        });

        tableBienes.setItems(bienesFiltrados);
        cargarBienes();
    }

    // --- Buscar por código ---
    private void buscarPorCodigo() {
        String codigo = txtBuscarCodigo.getText() == null ? "" : txtBuscarCodigo.getText().trim().toLowerCase();
        bienesFiltrados.setPredicate(b -> codigo.isEmpty() || b.getCodigo().toLowerCase().contains(codigo));
    }

    // --- Ver detalle simple ---
    private void verBien(Bien bien) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ver Bien");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        String info = "Código: " + safe(bien.getCodigo()) + "\n"
                + "Descripción: " + safe(bien.getDescripcion()) + "\n"
                + "Marca: " + safe(bien.getMarca()) + "\n"
                + "Modelo: " + safe(bien.getModelo()) + "\n"
                + "N.Serie: " + safe(bien.getNumeroSerie()) + "\n"
                + "Estado: " + safe(bien.getEstado());

        VBox content = new VBox(10);
        content.getChildren().add(new Label(info));

        if (bien.getImagen() != null) {
            Image img = new Image(new ByteArrayInputStream(bien.getImagen()));
            ImageView imgView = new ImageView(img);
            imgView.setFitWidth(220);
            imgView.setFitHeight(220);
            imgView.setPreserveRatio(true);
            content.getChildren().add(imgView);
        }

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    // --- Formulario crear/editar ---
    private void mostrarDialogoBien(Bien bien, boolean esNuevo) {
        if (!isAdmin) { mostrarAlerta("No autorizado", Alert.AlertType.WARNING); return; }

        Dialog<Bien> dialog = new Dialog<>();
        dialog.setTitle(esNuevo ? "Nuevo Bien" : "Editar Bien");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtCodigo = new TextField(esNuevo ? "" : bien.getCodigo());
        TextField txtDescripcion = new TextField(esNuevo ? "" : bien.getDescripcion());
        TextField txtMarca = new TextField(esNuevo ? "" : bien.getMarca());
        TextField txtModelo = new TextField(esNuevo ? "" : bien.getModelo());
        TextField txtSerie = new TextField(esNuevo ? "" : bien.getNumeroSerie());

        ComboBox<String> cmbEstado = new ComboBox<>();
        cmbEstado.getItems().addAll("Operativo", "Mantenimiento", "Baja");
        cmbEstado.setValue(esNuevo ? "Operativo" : bien.getEstado());

        // Imagen
        Label lblImagen = new Label("Imagen:");
        ImageView imgView = new ImageView();
        imgView.setFitWidth(110); imgView.setFitHeight(110); imgView.setPreserveRatio(true);
        final byte[][] imagenBytes = {null};
        if (!esNuevo && bien.getImagen() != null) {
            imgView.setImage(new Image(new ByteArrayInputStream(bien.getImagen())));
            imagenBytes[0] = bien.getImagen();
        }
        Button btnSeleccionarImagen = new Button("Seleccionar Imagen");
        btnSeleccionarImagen.setOnAction(ev -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar Imagen");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"));
            File file = fc.showOpenDialog(btnSeleccionarImagen.getScene().getWindow());
            if (file != null) {
                try {
                    imagenBytes[0] = leerArchivoComoBytes(file);
                    imgView.setImage(new Image(new ByteArrayInputStream(imagenBytes[0])));
                } catch (IOException ex) {
                    mostrarAlerta("No se pudo leer la imagen", Alert.AlertType.ERROR);
                }
            }
        });

        GridPane grid = new GridPane();
        grid.setVgap(12); grid.setHgap(10);
        int r = 0;
        grid.add(new Label("Código:"), 0, r); grid.add(txtCodigo, 1, r++);
        grid.add(new Label("Descripción:"), 0, r); grid.add(txtDescripcion, 1, r++);
        grid.add(new Label("Marca:"), 0, r); grid.add(txtMarca, 1, r++);
        grid.add(new Label("Modelo:"), 0, r); grid.add(txtModelo, 1, r++);
        grid.add(new Label("N.Serie:"), 0, r); grid.add(txtSerie, 1, r++);
        grid.add(new Label("Estado:"), 0, r); grid.add(cmbEstado, 1, r++);
        grid.add(lblImagen, 0, r); grid.add(imgView, 1, r++);
        grid.add(btnSeleccionarImagen, 1, r);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new Bien(
                        txtCodigo.getText().trim(),
                        txtDescripcion.getText().trim(),
                        txtMarca.getText().trim(),
                        txtModelo.getText().trim(),
                        txtSerie.getText().trim(),
                        cmbEstado.getValue(),
                        imagenBytes[0]
                );
            }
            return null;
        });

        Optional<Bien> res = dialog.showAndWait();
        res.ifPresent(nuevo -> {
            if (esNuevo) insertarBien(nuevo);
            else actualizarBien(bien.getCodigo(), nuevo);
        });
    }

    // --- Capa de datos ---
    private void cargarBienes() {
        bienes.clear();
        String sql = "SELECT * FROM bienes ORDER BY " + preferido(COLS_CODIGO, "codigo_inventario");
        try (Connection cn = getConnection();
             Statement st = cn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int iCodigo = findIndex(md, COLS_CODIGO, true);
            int iDesc   = findIndex(md, COLS_DESC, true);
            int iMarca  = findIndex(md, COLS_MARCA, false);
            int iModelo = findIndex(md, COLS_MODELO, false);
            int iSerie  = findIndex(md, COLS_SERIE, false);
            int iEstado = findIndex(md, COLS_ESTADO, true);
            int iImagen = findIndex(md, COLS_IMAGEN, false);

            while (rs.next()) {
                bienes.add(new Bien(
                        rs.getString(iCodigo),
                        rs.getString(iDesc),
                        iMarca  > 0 ? rs.getString(iMarca)  : "",
                        iModelo > 0 ? rs.getString(iModelo) : "",
                        iSerie  > 0 ? rs.getString(iSerie)  : "",
                        mapEstadoDbToUi(rs.getString(iEstado)),
                        iImagen > 0 ? rs.getBytes(iImagen)  : null
                ));
            }
        } catch (SQLException e) {
            mostrarAlerta("Error al cargar bienes", Alert.AlertType.ERROR);
        }
    }

    private void insertarBien(Bien b) {
        String sql = "INSERT INTO bienes (" +
                preferido(COLS_CODIGO, "codigo_inventario") + "," +
                preferido(COLS_DESC, "descripcion") + "," +
                preferido(COLS_MARCA, "marca") + "," +
                preferido(COLS_MODELO, "modelo") + "," +
                preferido(COLS_SERIE, "numero_serie") + "," +
                preferido(COLS_ESTADO, "estado") + "," +
                preferido(COLS_IMAGEN, "imagen") +
                ") VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, b.getCodigo());
            ps.setString(2, b.getDescripcion());
            ps.setString(3, emptyToNull(b.getMarca()));
            ps.setString(4, emptyToNull(b.getModelo()));
            ps.setString(5, emptyToNull(b.getNumeroSerie()));
            ps.setString(6, mapEstadoUiToDb(b.getEstado()));
            if (b.getImagen() != null) {
                ps.setBinaryStream(7, new ByteArrayInputStream(b.getImagen()), b.getImagen().length);
            } else {
                ps.setNull(7, Types.BLOB);
            }
            ps.executeUpdate();
            cargarBienes();
        } catch (SQLException e) {
            mostrarAlerta("Error al insertar bien", Alert.AlertType.ERROR);
        }
    }

    private void actualizarBien(String codigoOriginal, Bien b) {
        String colCod = preferido(COLS_CODIGO, "codigo_inventario");
        String colDesc = preferido(COLS_DESC, "descripcion");
        String colMarca = preferido(COLS_MARCA, "marca");
        String colModelo = preferido(COLS_MODELO, "modelo");
        String colSerie = preferido(COLS_SERIE, "numero_serie");
        String colEstado = preferido(COLS_ESTADO, "estado");
        String colImagen = preferido(COLS_IMAGEN, "imagen");

        String sql = "UPDATE bienes SET " + colCod + "=?, " + colDesc + "=?, " + colMarca + "=?, " + colModelo + "=?, " +
                colSerie + "=?, " + colEstado + "=?, " + colImagen + "=?, actualizado_en = SYSTIMESTAMP WHERE " + colCod + "=?";
        try (Connection cn = getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, b.getCodigo());
            ps.setString(2, b.getDescripcion());
            ps.setString(3, emptyToNull(b.getMarca()));
            ps.setString(4, emptyToNull(b.getModelo()));
            ps.setString(5, emptyToNull(b.getNumeroSerie()));
            ps.setString(6, mapEstadoUiToDb(b.getEstado()));
            if (b.getImagen() != null) {
                ps.setBinaryStream(7, new ByteArrayInputStream(b.getImagen()), b.getImagen().length);
            } else {
                ps.setNull(7, Types.BLOB);
            }
            ps.setString(8, codigoOriginal);
            ps.executeUpdate();
            cargarBienes();
        } catch (SQLException e) {
            mostrarAlerta("Error al actualizar bien", Alert.AlertType.ERROR);
        }
    }

    // --- Estados ---
    private String mapEstadoUiToDb(String ui) {
        if (ui == null) return "OPERATIVO";
        switch (ui.toUpperCase()) {
            case "MANTENIMIENTO": return "MANTENIMIENTO";
            case "BAJA": return "BAJA";
            default: return "OPERATIVO";
        }
    }
    private String mapEstadoDbToUi(String db) {
        if (db == null) return "Operativo";
        switch (db.toUpperCase()) {
            case "MANTENIMIENTO": return "Mantenimiento";
            case "BAJA": return "Baja";
            default: return "Operativo";
        }
    }

    // --- Modelo ---
    public static class Bien {
        private final SimpleStringProperty codigo, descripcion, marca, modelo, numeroSerie, estado;
        private final byte[] imagen;

        public Bien(String codigo, String descripcion, String marca, String modelo, String numeroSerie, String estado, byte[] imagen) {
            this.codigo = new SimpleStringProperty(safe(codigo));
            this.descripcion = new SimpleStringProperty(safe(descripcion));
            this.marca = new SimpleStringProperty(safe(marca));
            this.modelo = new SimpleStringProperty(safe(modelo));
            this.numeroSerie = new SimpleStringProperty(safe(numeroSerie));
            this.estado = new SimpleStringProperty(safe(estado));
            this.imagen = imagen;
        }
        public String getCodigo() { return codigo.get(); }
        public String getDescripcion() { return descripcion.get(); }
        public String getMarca() { return marca.get(); }
        public String getModelo() { return modelo.get(); }
        public String getNumeroSerie() { return numeroSerie.get(); }
        public String getEstado() { return estado.get(); }
        public byte[] getImagen() { return imagen; }

        public void setEstado(String e) { estado.set(safe(e)); }

        public SimpleStringProperty codigoProperty() { return codigo; }
        public SimpleStringProperty descripcionProperty() { return descripcion; }
        public SimpleStringProperty marcaProperty() { return marca; }
        public SimpleStringProperty modeloProperty() { return modelo; }
        public SimpleStringProperty numeroSerieProperty() { return numeroSerie; }
        public SimpleStringProperty estadoProperty() { return estado; }
    }

    // --- Utilidades ---
    private static String safe(String s) { return s == null ? "" : s; }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    private static String preferido(String[] candidatos, String fallback) {
        return (candidatos != null && candidatos.length > 0) ? candidatos[0] : fallback;
    }

    private static int findIndex(ResultSetMetaData md, String[] posibles, boolean obligatorio) throws SQLException {
        int cols = md.getColumnCount();
        for (String nombre : posibles) {
            for (int i = 1; i <= cols; i++) {
                if (nombre.equalsIgnoreCase(md.getColumnName(i))) {
                    return i;
                }
            }
        }
        if (obligatorio) throw new SQLException("No se encontró columna obligatoria: " + Arrays.toString(posibles));
        return -1;
    }

    private byte[] leerArchivoComoBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        mostrarAlerta("Bienes", mensaje, tipo);
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
            throw new SQLException("No se pudo cargar el driver Oracle", e);
        }
    }
}
