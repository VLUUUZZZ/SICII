package org.example.sici1.util;

public final class Formato {

    private Formato() {}

    public static String estado(boolean activo) { return activo ? "Activo" : "Inactivo"; }

    public static String texto(String s) { return s == null ? "" : s.trim(); }

    public static String vacioANull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    /** Para comparar nombres sin importar mayúsculas ni espacios de más. */
    public static String clave(String s) { return texto(s).replaceAll("\\s+", " ").toLowerCase(); }
}
