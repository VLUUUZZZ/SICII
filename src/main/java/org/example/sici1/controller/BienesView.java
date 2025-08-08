package org.example.sici1.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class BienesView {

    @FXML private TableView<Bien> tableBienes;
    @FXML private TableColumn<Bien, String> colCodigo;
    @FXML private TableColumn<Bien, String> colDescripcion;
    @FXML private TableColumn<Bien, String> colEstado;
    @FXML private TableColumn<Bien, Void> colSwitch;
    @FXML private TableColumn<Bien, Void> colVer;
    @FXML private TableColumn<Bien, Void> colEditar;
    @FXML private Button btnNuevo;
    @FXML private TextField txtBuscarCodigo;
    @FXML private Button btnBuscarCodigo;

    private final ObservableList<Bien> bienes = FXCollections.observableArrayList();

    // Detecta si es EMPLEADO
    private final boolean esEmpleado = "EMPLEADO".equalsIgnoreCase(UserSession.getInstance().getRole());

    // RUTA CSV (ajusta la ruta según tu equipo)
    private static final String ARCHIVO_BIENES =
            "C:\\Users\\VICTOR UZZIEL\\Documents\\bienes.csv";

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(data -> data.getValue().codigoProperty());
        colDescripcion.setCellValueFactory(data -> data.getValue().descripcionProperty());
        colEstado.setCellValueFactory(data -> data.getValue().estadoProperty());

        // Ocultar el botón Nuevo si es empleado
        btnNuevo.setVisible(!esEmpleado);

        // Ocultar columna editar si es empleado
        colEditar.setVisible(!esEmpleado);

        // Ocultar columna de switch (activo/inactivo) si es empleado
        colSwitch.setVisible(!esEmpleado);

        // Switch: ahora FUNCIONAL para admin
        colSwitch.setCellFactory(col -> new TableCell<>() {
            private final CheckBox check = new CheckBox();
            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                check.setDisable(esEmpleado); // Solo admin puede usarlo

                check.setOnAction(e -> {
                    Bien bien = getTableView().getItems().get(getIndex());
                    if (check.isSelected()) {
                        bien.setEstado("Activo");
                        mostrarAlerta("Cambio de Estado", "Bien activado correctamente.", Alert.AlertType.INFORMATION);
                    } else {
                        bien.setEstado("Inactivo");
                        mostrarAlerta("Cambio de Estado", "Bien desactivado correctamente.", Alert.AlertType.INFORMATION);
                    }
                    tableBienes.refresh();
                    guardarBienes();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    Bien bien = getTableView().getItems().get(getIndex());
                    check.setSelected("Activo".equalsIgnoreCase(bien.getEstado()));
                    setGraphic(check);
                }
            }
        });

        // Botón ver (ojo)
        colVer.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("👁");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-font-size: 15;");
                btn.setOnAction(e -> {
                    Bien bien = getTableView().getItems().get(getIndex());
                    Dialog<Void> dialog = new Dialog<>();
                    dialog.setTitle("Ver Bien");
                    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

                    String info = "Código: " + bien.getCodigo() + "\n"
                            + "Descripción: " + bien.getDescripcion() + "\n"
                            + "Estado: " + bien.getEstado();

                    VBox content = new VBox(10);
                    content.getChildren().add(new Label(info));

                    if (bien.getImagenPath() != null && !bien.getImagenPath().isEmpty()) {
                        File imgFile = new File(bien.getImagenPath());
                        if (imgFile.exists()) {
                            ImageView img = new ImageView(new Image(imgFile.toURI().toString()));
                            img.setFitWidth(220);
                            img.setFitHeight(220);
                            img.setPreserveRatio(true);
                            content.getChildren().add(img);
                        } else {
                            content.getChildren().add(new Label("Imagen no encontrada en la ruta."));
                        }
                    }
                    dialog.getDialogPane().setContent(content);
                    dialog.showAndWait();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // Botón editar (lápiz)
        colEditar.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✎");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-font-size: 15; -fx-text-fill: #1976d2;");
                btn.setOnAction(e -> {
                    Bien bien = getTableView().getItems().get(getIndex());
                    mostrarDialogoBien(bien, false);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || esEmpleado) ? null : btn);
            }
        });

        // Buscar por código
        btnBuscarCodigo.setOnAction(e -> {
            String codigo = txtBuscarCodigo.getText().trim().toLowerCase();
            if (codigo.isEmpty()) {
                tableBienes.setItems(bienes);
            } else {
                ObservableList<Bien> filtrados = FXCollections.observableArrayList();
                for (Bien b : bienes) {
                    if (b.getCodigo().toLowerCase().contains(codigo)) {
                        filtrados.add(b);
                    }
                }
                if (filtrados.isEmpty()) {
                    mostrarAlerta("Sin resultados", "No se encontró ningún bien con ese código.", Alert.AlertType.INFORMATION);
                }
                tableBienes.setItems(filtrados);
            }
        });

        tableBienes.setItems(bienes);
        cargarBienes();

        btnNuevo.setOnAction(e -> mostrarDialogoBien(null, true));
    }

    // =================== Diálogo para nuevo o editar ===================
    private void mostrarDialogoBien(Bien bien, boolean esNuevo) {
        Dialog<Bien> dialog = new Dialog<>();
        dialog.setTitle(esNuevo ? "Nuevo bien" : "Editar bien");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Campos del formulario
        TextField txtCodigo = new TextField();
        txtCodigo.setPromptText("Código bien");
        if (!esNuevo && bien != null) txtCodigo.setText(bien.getCodigo());

        TextField txtDescripcion = new TextField();
        txtDescripcion.setPromptText("Descripción");
        if (!esNuevo && bien != null) txtDescripcion.setText(bien.getDescripcion());

        ComboBox<String> cmbEstado = new ComboBox<>();
        cmbEstado.getItems().addAll("Activo", "Inactivo", "En reparación", "Baja");
        cmbEstado.setValue((!esNuevo && bien != null) ? bien.getEstado() : "Activo");

        // Campo imagen
        Label lblImagen = new Label("Imagen:");
        ImageView imgView = new ImageView();
        imgView.setFitWidth(110);
        imgView.setFitHeight(110);
        imgView.setPreserveRatio(true);

        Button btnSeleccionarImagen = new Button("Seleccionar Imagen");
        btnSeleccionarImagen.setDisable(esEmpleado);

        String[] imagenPath = {""};
        if (!esNuevo && bien != null && bien.getImagenPath() != null && !bien.getImagenPath().isEmpty()) {
            File imgFile = new File(bien.getImagenPath());
            if (imgFile.exists()) {
                imgView.setImage(new Image(imgFile.toURI().toString()));
                imagenPath[0] = bien.getImagenPath();
            }
        }

        btnSeleccionarImagen.setOnAction(ev -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar Imagen");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.png", "*.jpeg")
            );
            File file = fileChooser.showOpenDialog(btnSeleccionarImagen.getScene().getWindow());
            if (file != null) {
                imagenPath[0] = file.getAbsolutePath();
                imgView.setImage(new Image(file.toURI().toString()));
            }
        });

        GridPane grid = new GridPane();
        grid.setVgap(12); grid.setHgap(10);
        grid.add(new Label("Código:"), 0, 0);
        grid.add(txtCodigo, 1, 0);
        grid.add(new Label("Descripción:"), 0, 1);
        grid.add(txtDescripcion, 1, 1);
        grid.add(new Label("Estado:"), 0, 2);
        grid.add(cmbEstado, 1, 2);
        grid.add(lblImagen, 0, 3);
        grid.add(imgView, 1, 3);
        if (!esEmpleado) grid.add(btnSeleccionarImagen, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Validación al presionar OK
        final Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtCodigo.getText().trim().isEmpty() ||
                    txtDescripcion.getText().trim().isEmpty() ||
                    cmbEstado.getValue() == null) {
                mostrarAlerta("Llena todos los campos.", "No puedes dejar campos vacíos.", Alert.AlertType.WARNING);
                event.consume();
            } else if (esNuevo && existeCodigo(txtCodigo.getText().trim())) {
                mostrarAlerta("El código ya existe.", "Ese código de bien ya está registrado.", Alert.AlertType.WARNING);
                event.consume();
            } else if (!esNuevo && bien != null && !bien.getCodigo().equals(txtCodigo.getText().trim())
                    && existeCodigo(txtCodigo.getText().trim())) {
                mostrarAlerta("El código ya existe.", "Ese código de bien ya está registrado.", Alert.AlertType.WARNING);
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return new Bien(
                        txtCodigo.getText().trim(),
                        txtDescripcion.getText().trim(),
                        cmbEstado.getValue(),
                        imagenPath[0]
                );
            }
            return null;
        });

        Optional<Bien> resultado = dialog.showAndWait();
        resultado.ifPresent(nuevoBien -> {
            if (esNuevo) {
                bienes.add(nuevoBien);
            } else if (bien != null) {
                bien.setCodigo(nuevoBien.getCodigo());
                bien.setDescripcion(nuevoBien.getDescripcion());
                bien.setEstado(nuevoBien.getEstado());
                bien.setImagenPath(nuevoBien.getImagenPath());
                tableBienes.refresh();
            }
            guardarBienes();
        });
    }

    private boolean existeCodigo(String codigo) {
        for (Bien b : bienes) {
            if (b.getCodigo().equalsIgnoreCase(codigo)) return true;
        }
        return false;
    }

    private void guardarBienes() {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(ARCHIVO_BIENES), StandardCharsets.UTF_8))) {
            for (Bien b : bienes) {
                writer.write(escapa(b.getCodigo()) + "," +
                        escapa(b.getDescripcion()) + "," +
                        escapa(b.getEstado()) + "," +
                        escapa(b.getImagenPath() == null ? "" : b.getImagenPath()));
                writer.newLine();
            }
        } catch (IOException ex) {
            mostrarAlerta("Error", "No se pudo guardar el archivo de bienes.", Alert.AlertType.ERROR);
        }
    }

    private void cargarBienes() {
        bienes.clear();
        File archivo = new File(ARCHIVO_BIENES);
        if (!archivo.exists()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                String[] partes = desescapaCSV(linea);
                if (partes.length >= 4) {
                    bienes.add(new Bien(partes[0], partes[1], partes[2], partes[3]));
                } else if (partes.length >= 3) { // Para compatibilidad antigua
                    bienes.add(new Bien(partes[0], partes[1], partes[2], ""));
                }
            }
        } catch (IOException ex) {
            mostrarAlerta("Error", "No se pudo leer el archivo de bienes.", Alert.AlertType.ERROR);
        }
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
        java.util.List<String> list = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : linea.toCharArray()) {
            if (c == '"') inQuotes = !inQuotes;
            else if (c == ',' && !inQuotes) {
                list.add(sb.toString());
                sb.setLength(0);
            } else sb.append(c);
        }
        list.add(sb.toString());
        return list.toArray(new String[0]);
    }

    public static class Bien {
        private final SimpleStringProperty codigo, descripcion, estado, imagenPath;
        public Bien(String codigo, String descripcion, String estado, String imagenPath) {
            this.codigo = new SimpleStringProperty(codigo);
            this.descripcion = new SimpleStringProperty(descripcion);
            this.estado = new SimpleStringProperty(estado);
            this.imagenPath = new SimpleStringProperty(imagenPath);
        }
        public String getCodigo() { return codigo.get(); }
        public void setCodigo(String c) { this.codigo.set(c); }
        public String getDescripcion() { return descripcion.get(); }
        public void setDescripcion(String d) { this.descripcion.set(d); }
        public String getEstado() { return estado.get(); }
        public void setEstado(String e) { this.estado.set(e); }
        public String getImagenPath() { return imagenPath.get(); }
        public void setImagenPath(String i) { this.imagenPath.set(i); }
        public SimpleStringProperty codigoProperty() { return codigo; }
        public SimpleStringProperty descripcionProperty() { return descripcion; }
        public SimpleStringProperty estadoProperty() { return estado; }
        public SimpleStringProperty imagenPathProperty() { return imagenPath; }
    }

    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
