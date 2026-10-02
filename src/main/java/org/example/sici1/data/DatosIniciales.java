package org.example.sici1.data;

import org.example.sici1.model.Bien;
import org.example.sici1.model.Catalogo;
import org.example.sici1.model.Espacio;
import org.example.sici1.model.Usuario;

import java.util.List;

/**
 * La primera vez que se abre la aplicación con una base vacía crea el usuario
 * administrador y algunos datos de ejemplo para poder probarla de inmediato.
 */
public final class DatosIniciales {

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_CONTRASENA_POR_DEFECTO = "admin123";

    private DatosIniciales() {}

    /**
     * @return la contraseña del administrador si se acaba de crear, o null si ya había usuarios.
     */
    public static String cargarSiEsNecesario() {
        UsuarioRepository usuarios = UsuarioRepository.INSTANCIA;
        if (!usuarios.estaVacio()) return null;

        String contrasena = System.getenv("SICI_ADMIN_PASSWORD");
        if (contrasena == null || contrasena.length() < UsuarioRepository.MIN_CONTRASENA) {
            contrasena = ADMIN_CONTRASENA_POR_DEFECTO;
        }

        cargarEjemplos();
        usuarios.crear(new Usuario(ADMIN_USERNAME, "Administrador del sistema", Usuario.ROL_ADMIN, "Administrador", true),
                contrasena);
        return contrasena;
    }

    private static void cargarEjemplos() {
        if (CatalogoRepository.PUESTOS.estaVacio()) {
            for (String p : List.of("Administrador", "Docente", "Técnico de laboratorio", "Auxiliar administrativo")) {
                CatalogoRepository.PUESTOS.guardar(new Catalogo(null, p, true));
            }
        }
        if (CatalogoRepository.UNIDADES_ADMINISTRATIVAS.estaVacio()) {
            for (String u : List.of("Dirección General", "División Académica de Tecnologías", "Recursos Materiales")) {
                CatalogoRepository.UNIDADES_ADMINISTRATIVAS.guardar(new Catalogo(null, u, true));
            }
        }
        if (CatalogoRepository.EDIFICIOS.estaVacio() && EspacioRepository.INSTANCIA.estaVacio()) {
            Catalogo docencia = CatalogoRepository.EDIFICIOS.guardar(new Catalogo(null, "Docencia 1", true));
            Catalogo rectoria = CatalogoRepository.EDIFICIOS.guardar(new Catalogo(null, "Rectoría", true));
            EspacioRepository.INSTANCIA.guardar(new Espacio(null, "Laboratorio de cómputo 1", "LAB-01", docencia.id(), null, true));
            EspacioRepository.INSTANCIA.guardar(new Espacio(null, "Aula 101", "A-101", docencia.id(), null, true));
            EspacioRepository.INSTANCIA.guardar(new Espacio(null, "Oficina de dirección", "OF-DIR", rectoria.id(), null, true));
        }
        if (BienRepository.INSTANCIA.estaVacio()) {
            BienRepository.INSTANCIA.guardar(new Bien(null, "PC-2020-041", "Computadora de escritorio", "Dell", "OptiPlex 7080", "DL7080X41", Bien.OPERATIVO, null));
            BienRepository.INSTANCIA.guardar(new Bien(null, "PC-2020-042", "Computadora de escritorio", "Dell", "OptiPlex 7080", "DL7080X42", Bien.OPERATIVO, null));
            BienRepository.INSTANCIA.guardar(new Bien(null, "PRY-2019-007", "Proyector", "Epson", "PowerLite X41", "EPX41007", Bien.MANTENIMIENTO, null));
            BienRepository.INSTANCIA.guardar(new Bien(null, "MOB-2018-115", "Escritorio de oficina", "", "", "", Bien.OPERATIVO, null));
        }
    }
}
