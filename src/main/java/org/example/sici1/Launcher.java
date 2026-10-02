package org.example.sici1;

/**
 * Punto de entrada del .jar ejecutable. JavaFX no permite que la clase principal
 * del jar extienda Application cuando no se usan módulos, por eso existe esta clase.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        Main.main(args);
    }
}
