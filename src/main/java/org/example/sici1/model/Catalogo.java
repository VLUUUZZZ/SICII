package org.example.sici1.model;

/**
 * Registro sencillo con nombre y estado: edificios, puestos y unidades administrativas.
 */
public record Catalogo(String id, String nombre, boolean activo) {

    @Override
    public String toString() { return nombre; }
}
