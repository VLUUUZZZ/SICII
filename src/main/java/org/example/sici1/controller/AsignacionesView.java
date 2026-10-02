package org.example.sici1.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.example.sici1.data.CatalogoRepository;
import org.example.sici1.data.EspacioRepository;
import org.example.sici1.data.InventarioRepository;
import org.example.sici1.data.UsuarioRepository;
import org.example.sici1.model.Bien;
import org.example.sici1.model.Catalogo;
import org.example.sici1.model.Espacio;
import org.example.sici1.model.Inventario;
import org.example.sici1.util.Alertas;
import org.example.sici1.util.ReporteInventario;
import org.example.sici1.util.Sesion;
import org.example.sici1.util.Tablas;
import org.example.sici1.util.Tareas;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

/**
 * Asignación de bienes: se crea un encabezado (unidad administrativa + espacio + fecha)
 * y después se agregan los bienes por su código.
 */
public class AsignacionesView {

    private static final String TITULO = "Inventario";

    @FXML private Label lblInventarioId, lblInfo;
    @FXML private TextField txtCodigoBien;
    @FXML private Button btnCrearInventario, btnAgregarBien, btnImprimir;
    @FXML private TableView<Bien> tablaDetalle;
    @FXML private TableColumn<Bien, String> colCodigo, colDescripcion, colEstado;
    @FXML private TableColumn<Bien, Void> colDesasignar;

    private final InventarioRepository repo = InventarioRepository.INSTANCIA;
    private final ObservableList<Bien> detalle = FXCollections.observableArrayList();
    private Inventario actual;

    /** Opciones para el formulario del encabezado. */
    private record Catalogos(List<Catalogo> unidades, List<Espacio> espacios) {}

    /** Lo que captura el formulario del encabezado. */
    private record Encabezado(Catalogo unidad, Espacio espacio, LocalDate fecha) {}

    @FXML
    public void initialize() {
        Tablas.texto(colCodigo, Bien::codigo);
        Tablas.texto(colDescripcion, Bien::descripcion);
        Tablas.texto(colEstado, Bien::estado);
        colDesasignar.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Desasignar");
            {
                btn.setStyle("-fx-background-color:#d32f2f; -fx-text-fill:white; -fx-font-size:12;");
                btn.setOnAction(e -> {
                    Bien b = getTableRow() == null ? null : getTableRow().getItem();
                    if (b != null) desasignar(b);
                });
            }

            @Override
            protected void updateItem(Void item, boolean vacio) {
                super.updateItem(item, vacio);
                setGraphic(vacio ? null : btn);
            }
        });
        tablaDetalle.setItems(detalle);
        tablaDetalle.setPlaceholder(new Label("Crea o abre un inventario para agregar bienes"));

        btnCrearInventario.setOnAction(e -> abrirEncabezado());
        btnAgregarBien.setOnAction(e -> agregarBien());
        txtCodigoBien.setOnAction(e -> agregarBien());
        btnImprimir.setOnAction(e -> imprimir());
    }

    // ===================== Encabezado =====================

    private void abrirEncabezado() {
        Tareas.ejecutar(TITULO,
                () -> new Catalogos(CatalogoRepository.UNIDADES_ADMINISTRATIVAS.listarActivos(),
                        EspacioRepository.INSTANCIA.listarActivos()),
                this::mostrarEncabezado);
    }

    private void mostrarEncabezado(Catalogos catalogos) {
        if (catalogos.unidades().isEmpty() || catalogos.espacios().isEmpty()) {
            Alertas.aviso(TITULO, "Primero registra al menos una unidad administrativa y un espacio activos.");
            return;
        }

        Dialog<Encabezado> dialog = new Dialog<>();
        dialog.setTitle("Encabezado de inventario");
        ButtonType ok = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        ComboBox<Catalogo> cmbUnidad = new ComboBox<>(FXCollections.observableArrayList(catalogos.unidades()));
        ComboBox<Espacio> cmbEspacio = new ComboBox<>(FXCollections.observableArrayList(catalogos.espacios()));
        cmbUnidad.setPrefWidth(260);
        cmbEspacio.setPrefWidth(260);
        DatePicker dpFecha = new DatePicker(LocalDate.now());

        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        grid.addRow(0, new Label("Unidad administrativa:"), cmbUnidad);
        grid.addRow(1, new Label("Espacio:"), cmbEspacio);
        grid.addRow(2, new Label("Fecha:"), dpFecha);
        dialog.getDialogPane().setContent(grid);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ok);
        Runnable validar = () -> btnOk.setDisable(
                cmbUnidad.getValue() == null || cmbEspacio.getValue() == null || dpFecha.getValue() == null);
        cmbUnidad.valueProperty().addListener((o, a, b) -> validar.run());
        cmbEspacio.valueProperty().addListener((o, a, b) -> validar.run());
        dpFecha.valueProperty().addListener((o, a, b) -> validar.run());
        validar.run();

        dialog.setResultConverter(bt -> bt == ok
                ? new Encabezado(cmbUnidad.getValue(), cmbEspacio.getValue(), dpFecha.getValue())
                : null);
        dialog.showAndWait().ifPresent(this::guardarEncabezado);
    }

    private void guardarEncabezado(Encabezado e) {
        Tareas.ejecutar(TITULO, () -> repo.buscarActivo(e.unidad(), e.espacio(), e.fecha()), existente -> {
            if (existente.isPresent()) {
                if (Alertas.confirmar("Inventario ya existe",
                        "Ya hay un inventario activo con esa unidad, espacio y fecha.\n¿Quieres abrirlo para continuar?")) {
                    abrir(existente.get());
                }
                return;
            }
            String responsable = Sesion.usuario() == null ? "" : Sesion.usuario().username();
            Tareas.ejecutar(TITULO, () -> repo.crear(e.unidad(), e.espacio(), e.fecha(), responsable), inv -> {
                abrir(inv);
                info("Encabezado guardado. Ahora agrega bienes por código.");
            });
        });
    }

    private void abrir(Inventario inv) {
        actual = inv;
        lblInventarioId.setText("Inventario: " + inv.unidadAdministrativa().nombre() + " · "
                + inv.espacio().nombre() + " · " + inv.fecha());
        detalle.clear();
        Tareas.ejecutar(TITULO, () -> repo.bienes(inv.id()), bienes -> {
            detalle.setAll(bienes);
            info(bienes.isEmpty() ? "Inventario abierto (sin bienes). Agrega por código."
                    : "Inventario abierto con " + bienes.size() + " bien(es).");
        });
    }

    // ===================== Bienes =====================

    private void agregarBien() {
        if (actual == null) {
            Alertas.info(TITULO, "Primero crea el encabezado.");
            return;
        }
        String codigo = txtCodigoBien.getText() == null ? "" : txtCodigoBien.getText().trim();
        if (codigo.isEmpty()) {
            Alertas.aviso(TITULO, "Ingresa un código de bien.");
            return;
        }
        String inventarioId = actual.id();
        Tareas.ejecutar(TITULO, () -> repo.agregarBien(inventarioId, codigo), bien -> {
            txtCodigoBien.clear();
            detalle.add(bien);
            info(detalle.size() + " bien(es) en este inventario.");
        });
    }

    private void desasignar(Bien bien) {
        if (!Alertas.confirmar("Desasignar bien", "¿Quitar el bien " + bien.codigo() + " de este inventario?")) return;
        String inventarioId = actual.id();
        Tareas.ejecutar(TITULO, () -> { repo.quitarBien(inventarioId, bien.id()); return null; }, r -> {
            detalle.remove(bien);
            info(detalle.size() + " bien(es) en este inventario.");
        });
    }

    // ===================== Impresión =====================

    private void imprimir() {
        if (actual == null) {
            Alertas.info(TITULO, "Crea o abre un inventario antes de imprimir.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar reporte");
        fc.setInitialFileName("inventario_" + actual.fecha() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File destino = fc.showSaveDialog(btnImprimir.getScene().getWindow());
        if (destino == null) return;

        Inventario inv = actual;
        Tareas.ejecutar(TITULO, () -> {
            List<Bien> bienes = repo.bienes(inv.id());
            ReporteInventario.exportarPdf(inv, bienes,
                    UsuarioRepository.INSTANCIA.buscar(inv.responsable()).orElse(null), destino);
            abrirArchivo(destino);
            return destino;
        }, archivo -> info("Reporte guardado en " + archivo.getAbsolutePath()));
    }

    private static void abrirArchivo(File archivo) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(archivo);
            }
        } catch (Exception ignorada) {
            // Si el sistema no puede abrirlo, el archivo de todas formas quedó guardado.
        }
    }

    private void info(String texto) {
        lblInfo.setText(texto);
    }
}
