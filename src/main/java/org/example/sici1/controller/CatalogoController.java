package org.example.sici1.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.example.sici1.data.CatalogoRepository;
import org.example.sici1.model.Catalogo;
import org.example.sici1.util.Formato;
import org.example.sici1.util.Sesion;
import org.example.sici1.util.Tablas;
import org.example.sici1.util.Tareas;

/**
 * Pantalla para Edificios, Puestos y Unidades Administrativas.
 * Solo el administrador puede agregar, editar o cambiar el estado.
 */
public class CatalogoController {

    @FXML private Label lblTitulo, lblVacio;
    @FXML private TableView<Catalogo> tabla;
    @FXML private TableColumn<Catalogo, String> colNombre, colEstado;
    @FXML private TableColumn<Catalogo, Void> colActivo;
    @FXML private TextField txtBuscar;
    @FXML private Button btnAgregar, btnEditar, btnBuscar;

    private final String titulo;
    private final String encabezadoNombre;
    private final CatalogoRepository repo;
    private final boolean admin = Sesion.esAdmin();

    private final ObservableList<Catalogo> registros = FXCollections.observableArrayList();
    private final FilteredList<Catalogo> filtrados = new FilteredList<>(registros, c -> true);

    public CatalogoController(String titulo, String encabezadoNombre, CatalogoRepository repo) {
        this.titulo = titulo;
        this.encabezadoNombre = encabezadoNombre;
        this.repo = repo;
    }

    @FXML
    public void initialize() {
        lblTitulo.setText(titulo);
        colNombre.setText(encabezadoNombre);
        txtBuscar.setPromptText("Buscar " + repo.singular() + "...");

        Tablas.texto(colNombre, Catalogo::nombre);
        Tablas.texto(colEstado, c -> Formato.estado(c.activo()));
        Tablas.interruptor(colActivo, Catalogo::activo, this::cambiarActivo);
        tabla.setItems(filtrados);

        btnAgregar.setVisible(admin);
        btnEditar.setVisible(admin);
        colActivo.setVisible(admin);

        btnBuscar.setOnAction(e -> filtrar());
        txtBuscar.textProperty().addListener((obs, a, b) -> filtrar());

        if (admin) {
            btnAgregar.setOnAction(e -> mostrarDialogo(null));
            btnEditar.setOnAction(e -> {
                Catalogo c = Tablas.seleccionado(tabla, titulo);
                if (c != null) mostrarDialogo(c);
            });
            Tablas.dobleClic(tabla, this::mostrarDialogo);
        }

        cargar();
    }

    private void cargar() {
        Tareas.ejecutar(titulo, repo::listar, lista -> {
            registros.setAll(lista);
            lblVacio.setText("No hay registros");
        });
    }

    private void filtrar() {
        String texto = Formato.clave(txtBuscar.getText());
        filtrados.setPredicate(c -> texto.isEmpty() || Formato.clave(c.nombre()).contains(texto));
    }

    private void mostrarDialogo(Catalogo existente) {
        boolean nuevo = existente == null;
        Dialog<Catalogo> dialog = new Dialog<>();
        dialog.setTitle((nuevo ? "Agregar " : "Editar ") + repo.singular());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField txtNombre = new TextField(nuevo ? "" : existente.nombre());
        txtNombre.setPrefWidth(280);
        CheckBox chkActivo = new CheckBox("Activo");
        chkActivo.setSelected(nuevo || existente.activo());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.addRow(0, new Label("Nombre:"), txtNombre);
        grid.addRow(1, new Label("Estado:"), chkActivo);
        dialog.getDialogPane().setContent(grid);
        dialog.setOnShown(e -> txtNombre.requestFocus());

        dialog.setResultConverter(bt -> bt == ButtonType.OK
                ? new Catalogo(nuevo ? null : existente.id(), txtNombre.getText(), chkActivo.isSelected())
                : null);

        dialog.showAndWait().ifPresent(c -> Tareas.ejecutar(titulo, () -> repo.guardar(c), guardado -> cargar()));
    }

    private void cambiarActivo(Catalogo c, boolean activo) {
        Tareas.ejecutar(titulo, () -> { repo.cambiarActivo(c.id(), activo); return null; },
                r -> cargar(), this::cargar);
    }
}
