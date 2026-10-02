package org.example.sici1.model;

/**
 * Espacio físico (aula, oficina, laboratorio...) dentro de un edificio.
 */
public record Espacio(String id, String nombre, String codigo, String edificioId, String edificioNombre, boolean activo) {

    @Override
    public String toString() { return nombre; }
}
