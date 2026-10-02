package org.example.sici1.data;

/**
 * Error de una regla del negocio (dato duplicado, campo vacío...). Su mensaje se muestra tal cual al usuario.
 */
public class ValidacionException extends RuntimeException {
    public ValidacionException(String mensaje) { super(mensaje); }
}
