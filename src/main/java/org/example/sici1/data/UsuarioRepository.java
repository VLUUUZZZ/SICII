package org.example.sici1.data;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import org.example.sici1.model.Usuario;
import org.example.sici1.util.Contrasenas;
import org.example.sici1.util.Formato;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Usuarios del sistema. El id del documento es el nombre de usuario en minúsculas,
 * así Firestore garantiza que no se repita.
 */
public class UsuarioRepository extends Repositorio {

    public static final UsuarioRepository INSTANCIA = new UsuarioRepository();
    public static final int MIN_CONTRASENA = 6;

    private static final Pattern USERNAME_VALIDO = Pattern.compile("[a-zA-Z0-9._-]{3,30}");

    private UsuarioRepository() {}

    /** Devuelve el usuario si existe, está activo y la contraseña coincide. */
    public Optional<Usuario> autenticar(String username, String contrasena) {
        DocumentSnapshot d = Firebase.esperar(col().document(Formato.clave(username)).get());
        if (!d.exists() || !bool(d.getBoolean("activo"))) return Optional.empty();
        if (!Contrasenas.verificar(contrasena, d.getString("passwordHash"))) return Optional.empty();
        return Optional.of(aModelo(d));
    }

    public Optional<Usuario> buscar(String username) {
        if (username == null) return Optional.empty();
        DocumentSnapshot d = Firebase.esperar(col().document(Formato.clave(username)).get());
        return d.exists() ? Optional.of(aModelo(d)) : Optional.empty();
    }

    public List<Usuario> listar() {
        return Firebase.esperar(col().get()).getDocuments().stream()
                .map(UsuarioRepository::aModelo)
                .sorted(Comparator.comparing(u -> Formato.clave(u.username())))
                .toList();
    }

    public boolean estaVacio() {
        return Firebase.esperar(col().limit(1).get()).isEmpty();
    }

    public void crear(Usuario u, String contrasena) {
        String username = Formato.texto(u.username());
        if (!USERNAME_VALIDO.matcher(username).matches()) {
            throw new ValidacionException("El usuario debe tener de 3 a 30 caracteres: letras, números, punto, guion o guion bajo.");
        }
        validarDatos(u);
        validarContrasena(contrasena);

        Map<String, Object> datos = datosBase(u);
        datos.put("username", username);
        datos.put("passwordHash", Contrasenas.hash(contrasena));
        conFechas(datos, true);

        DocumentReference ref = col().document(Formato.clave(username));
        Firebase.esperar(db().runTransaction(tx -> {
            if (tx.get(ref).get().exists()) throw new ValidacionException("Ese usuario ya existe.");
            tx.create(ref, datos);
            return null;
        }));
    }

    /** Actualiza nombre, rol, puesto y estado. Si nuevaContrasena no está vacía también la cambia. */
    public void actualizar(Usuario u, String nuevaContrasena) {
        validarDatos(u);
        Map<String, Object> datos = datosBase(u);
        if (nuevaContrasena != null && !nuevaContrasena.isEmpty()) {
            validarContrasena(nuevaContrasena);
            datos.put("passwordHash", Contrasenas.hash(nuevaContrasena));
        }
        Firebase.esperar(col().document(Formato.clave(u.username())).update(conFechas(datos, false)));
    }

    public void cambiarActivo(String username, boolean activo) {
        Firebase.esperar(col().document(Formato.clave(username))
                .update("activo", activo, "actualizadoEn", FieldValue.serverTimestamp()));
    }

    private static void validarDatos(Usuario u) {
        if (Formato.texto(u.nombre()).isEmpty()) throw new ValidacionException("El nombre completo es obligatorio.");
        if (!Usuario.ROLES.contains(u.rol())) throw new ValidacionException("Selecciona un rol válido.");
    }

    private static void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < MIN_CONTRASENA) {
            throw new ValidacionException("La contraseña debe tener al menos " + MIN_CONTRASENA + " caracteres.");
        }
    }

    private static Map<String, Object> datosBase(Usuario u) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", Formato.texto(u.nombre()));
        datos.put("rol", u.rol());
        datos.put("puesto", Formato.vacioANull(u.puesto()));
        datos.put("activo", u.activo());
        return datos;
    }

    private CollectionReference col() { return coleccion("usuarios"); }

    private static Usuario aModelo(DocumentSnapshot d) {
        return new Usuario(d.getString("username"), Formato.texto(d.getString("nombre")),
                d.getString("rol"), Formato.texto(d.getString("puesto")), bool(d.getBoolean("activo")));
    }
}
