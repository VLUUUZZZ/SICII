package org.example.sici1.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContrasenasTest {

    @Test
    void verificaLaContrasenaCorrecta() {
        String hash = Contrasenas.hash("secreta123");
        assertTrue(Contrasenas.verificar("secreta123", hash));
        assertFalse(Contrasenas.verificar("otra", hash));
    }

    @Test
    void elHashNoContieneLaContrasenaYCambiaCadaVez() {
        String a = Contrasenas.hash("secreta123");
        String b = Contrasenas.hash("secreta123");
        assertNotEquals(a, b);
        assertFalse(a.contains("secreta123"));
    }

    @Test
    void rechazaValoresInvalidos() {
        assertFalse(Contrasenas.verificar("x", null));
        assertFalse(Contrasenas.verificar(null, Contrasenas.hash("x")));
        assertFalse(Contrasenas.verificar("123456", "123456"));
        assertFalse(Contrasenas.verificar("x", "pbkdf2$no-numero$a$b"));
    }
}
