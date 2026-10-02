package org.example.sici1.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.example.sici1.data.CatalogoRepository;
import org.example.sici1.data.UsuarioRepository;
import org.example.sici1.model.Catalogo;
import org.example.sici1.model.Usuario;
import org.example.sici1.util.Alertas;
import org.example.sici1.util.Formato;
import org.example.sici1.util.Sesion;
import org.example.sici1.util.Tablas;
import org.example.sici1.util.Tareas;

import java.util.List;

/**
 * Administración de empleados con acceso al sistema. Solo visible para el administrador.
 */
public class UsuariosView {

    private static final String TITULO = "Empleados";

    @FXML private TableView<Usuario> tableUsuarios;
    @FXML private TableColumn<Usuario, String> colUsuario, colNombre, colRol, colPuesto, colEstado;
    @FXML private TableColumn<Usuario, Void> colSwitch;
    @FXML private TextField txtBuscar;
    @FXML private Button btnAgregar, btnBuscar, btnEditar;
    @FXML private Label lblVacio;

    private final UsuarioRepository repo = UsuarioRepository.INSTANCIA;
    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();
    private final FilteredList<Usuario> filtrados = new FilteredList<>(usuarios, u -> true);

    /** Datos que regresa el formulario. */
    private record Formulario(Usuario usuario, String contrasena) {}

    @FXML
    public void initialize() {
        Tablas.texto(colUsuario, Usuario::username);
        Tablas.texto(colNombre, Usuario::nombre);
        Tablas.texto(colRol, Usuario::rol);
        Tablas.texto(colPuesto, Usuario::puesto);
        Tablas.texto(colEstado, u -> Formato.estado(u.activo()));
        Tablas.interruptor(colSwitch, Usuario::activo, this::cambiarActivo);
        tableUsuarios.setItems(filtrados);

        btnBuscar.setOnAction(e -> filtrar());
        txtBuscar.setOnAction(e -> filtrar());
        txtBuscar.textProperty().addListener((obs, a, b) -> filtrar());

        // La pantalla solo se abre para administradores, pero se valida de nuevo por seguridad.
        boolean admin = Sesion.esAdmin();
        btnAgregar.setVisible(admin);
        btnEditar.setVisible(admin);
        colSwitch.setVisible(admin);
        if (!admin) return;

        btnAgregar.setOnAction(e -> abrirFormulario(null));
        btnEditar.setOnAction(e -> {
            Usuario sel = Tablas.seleccionado(tableUsuarios, TITULO);
            if (sel != null) abrirFormulario(sel);
        });
        Tablas.dobleClic(tableUsuarios, this::abrirFormulario);

        cargar();
    }

    private void cargar() {
        Tareas.ejecutar(TITULO, repo::listar, lista -> {
            usuarios.setAll(lista);
            lblVacio.setText("No hay empleados registrados");
        });
    }

    private void filtrar() {
        String texto = Formato.clave(txtBuscar.getText());
        filtrados.setPredicate(u -> texto.isEmpty()
                || Formato.clave(u.username()).contains(texto)
                || Formato.clave(u.nombre()).contains(texto));
    }

    private boolean esUsuarioActual(Usuario u) {
        return Sesion.usuario() != null && Formato.clave(Sesion.usuario().username()).equals(Formato.clave(u.username()));
    }

    private void cambiarActivo(Usuario u, boolean activo) {
        if (!activo && esUsuarioActual(u)) {
            Alertas.aviso(TITULO, "No puedes desactivar tu propio usuario.");
            tableUsuarios.refresh();
            return;
        }
        Tareas.ejecutar(TITULO, () -> { repo.cambiarActivo(u.username(), activo); return null; },
                r -> cargar(), this::cargar);
    }

    /** Trae los puestos para el combo y abre el formulario. */
    private void abrirFormulario(Usuario existente) {
        Tareas.ejecutar(TITULO, CatalogoRepository.PUESTOS::listar, puestos -> mostrarFormulario(existente, puestos));
    }

    private void mostrarFormulario(Usuario existente, List<Catalogo> puestos) {
        boolean nuevo = existente == null;
        boolean propio = !nuevo && esUsuarioActual(existente);

        Dialog<Formulario> dialog = new Dialog<>();
        dialog.setTitle(nuevo ? "Agregar empleado" : "Editar empleado");
        dialog.setHeaderText(nuevo ? "Complete los datos del nuevo empleado" : "Usuario: " + existente.username());
        ButtonType guardar = new ButtonType(nuevo ? "Agregar" : "Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        TextField txtUsuario = new TextField(nuevo ? "" : existente.username());
        txtUsuario.setPromptText("Nombre de usuario");
        txtUsuario.setDisable(!nuevo);
        TextField txtNombre = new TextField(nuevo ? "" : existente.nombre());
        txtNombre.setPromptText("Nombre completo");

        ComboBox<String> cmbRol = new ComboBox<>(FXCollections.observableArrayList(Usuario.ROLES));
        cmbRol.setValue(nuevo ? Usuario.ROL_EMPLEADO : existente.rol());
        cmbRol.setDisable(propio); // evita que el administrador se quite el permiso a sí mismo

        // Puestos activos y, si se está editando, también el actual aunque esté inactivo.
        ComboBox<Catalogo> cmbPuesto = new ComboBox<>(FXCollections.observableArrayList(puestos.stream()
                .filter(p -> p.activo() || (!nuevo && p.id().equals(existente.puestoId()))).toList()));
        cmbPuesto.setPromptText("Sin puesto");
        if (!nuevo && existente.puestoId() != null) {
            cmbPuesto.getItems().stream().filter(p -> p.id().equals(existente.puestoId()))
                    .findFirst().ifPresent(cmbPuesto::setValue);
        }

        PasswordField txtContrasena = new PasswordField();
        txtContrasena.setPromptText(nuevo ? "Mínimo " + UsuarioRepository.MIN_CONTRASENA + " caracteres"
                : "Dejar vacío para no cambiarla");
        PasswordField txtConfirmar = new PasswordField();

        CheckBox chkActivo = new CheckBox("Activo");
        chkActivo.setSelected(nuevo || existente.activo());
        chkActivo.setDisable(propio);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        int r = 0;
        grid.addRow(r++, new Label("Usuario:"), txtUsuario);
        grid.addRow(r++, new Label("Nombre completo:"), txtNombre);
        grid.addRow(r++, new Label("Rol:"), cmbRol);
        grid.addRow(r++, new Label("Puesto:"), cmbPuesto);
        grid.addRow(r++, new Label(nuevo ? "Contraseña:" : "Nueva contraseña:"), txtContrasena);
        grid.addRow(r++, new Label("Confirmar:"), txtConfirmar);
        grid.addRow(r, new Label("Estado:"), chkActivo);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> bt == guardar
                ? new Formulario(new Usuario(txtUsuario.getText(), txtNombre.getText(), cmbRol.getValue(),
                        cmbPuesto.getValue() == null ? null : cmbPuesto.getValue().id(), null,
                        chkActivo.isSelected()), txtContrasena.getText())
                : null);

        var resultado = dialog.showAndWait();
        if (resultado.isEmpty()) return;
        Formulario f = resultado.get();

        if (!f.contrasena().equals(txtConfirmar.getText())) {
            Alertas.aviso(TITULO, "Las contraseñas no coinciden.");
            return;
        }
        Tareas.ejecutar(TITULO, () -> {
            if (nuevo) repo.crear(f.usuario(), f.contrasena());
            else repo.actualizar(f.usuario(), f.contrasena());
            return null;
        }, listo -> cargar());
    }
}
