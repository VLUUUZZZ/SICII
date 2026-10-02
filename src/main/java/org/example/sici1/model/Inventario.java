package org.example.sici1.model;

import java.time.LocalDate;

/**
 * Encabezado de un inventario: qué unidad administrativa tiene qué bienes en qué espacio.
 */
public record Inventario(String id, Catalogo unidadAdministrativa, Espacio espacio, LocalDate fecha,
                         String responsable, boolean activo) {
}
