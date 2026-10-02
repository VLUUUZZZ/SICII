package org.example.sici1.util;

import org.example.sici1.model.Usuario;

/**
 * Usuario que inició sesión en la aplicación.
 */
public final class Sesion {

    private static Usuario usuario;

    private Sesion() {}

    public static void iniciar(Usuario u) { usuario = u; }

    public static void cerrar() { usuario = null; }

    public static Usuario usuario() { return usuario; }

    public static String rol() {
        return usuario == null || usuario.rol() == null ? "" : usuario.rol().trim().toUpperCase();
    }

    public static boolean esAdmin() { return Usuario.ROL_ADMIN.equals(rol()); }
}
