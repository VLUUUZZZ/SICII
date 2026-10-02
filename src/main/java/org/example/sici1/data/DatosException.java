package org.example.sici1.data;

/**
 * Error técnico al leer o escribir en Firebase.
 */
public class DatosException extends RuntimeException {
    public DatosException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
