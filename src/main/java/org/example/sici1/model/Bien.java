package org.example.sici1.model;

import java.util.List;

/**
 * Bien institucional identificado por su código de inventario.
 */
public record Bien(String id, String codigo, String descripcion, String marca, String modelo,
                   String numeroSerie, String estado, byte[] imagen) {

    public static final String OPERATIVO = "Operativo";
    public static final String MANTENIMIENTO = "Mantenimiento";
    public static final String BAJA = "Baja";
    public static final List<String> ESTADOS = List.of(OPERATIVO, MANTENIMIENTO, BAJA);

    /** Normaliza cualquier texto de estado a uno de los valores de {@link #ESTADOS}. */
    public static String normalizarEstado(String estado) {
        if (estado == null) return OPERATIVO;
        for (String e : ESTADOS) if (e.equalsIgnoreCase(estado.trim())) return e;
        return OPERATIVO;
    }
}
