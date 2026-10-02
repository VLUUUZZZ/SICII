package org.example.sici1.model;

import java.util.List;

/**
 * Usuario del sistema. La contraseña nunca se guarda aquí, solo su hash en Firebase.
 * Del puesto se guarda el id; el nombre se resuelve al leer para que renombrarlo no deje datos viejos.
 */
public record Usuario(String username, String nombre, String rol, String puestoId, String puesto, boolean activo) {

    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_EMPLEADO = "EMPLEADO";
    public static final List<String> ROLES = List.of(ROL_ADMIN, ROL_EMPLEADO);
}
