package org.example.sici1.util;

import javafx.application.Platform;
import org.example.sici1.data.ValidacionException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Ejecuta las consultas a Firebase fuera del hilo de la interfaz para que
 * la ventana no se congele, y regresa el resultado al hilo de JavaFX.
 */
public final class Tareas {

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "sici-datos");
        t.setDaemon(true);
        return t;
    });

    private Tareas() {}

    public static <T> void ejecutar(String titulo, Callable<T> trabajo, Consumer<T> alTerminar) {
        ejecutar(titulo, trabajo, alTerminar, null);
    }

    /**
     * @param alFallar se ejecuta en el hilo de JavaFX si hubo error (además de mostrar la alerta). Puede ser null.
     */
    public static <T> void ejecutar(String titulo, Callable<T> trabajo, Consumer<T> alTerminar, Runnable alFallar) {
        POOL.execute(() -> {
            try {
                T resultado = trabajo.call();
                if (alTerminar != null) Platform.runLater(() -> alTerminar.accept(resultado));
            } catch (ValidacionException e) {
                Alertas.aviso(titulo, e.getMessage());
                if (alFallar != null) Platform.runLater(alFallar);
            } catch (Exception e) {
                e.printStackTrace();
                Alertas.error(titulo, "Ocurrió un error al comunicarse con la base de datos.\n" + mensajeRaiz(e));
                if (alFallar != null) Platform.runLater(alFallar);
            }
        });
    }

    private static String mensajeRaiz(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
}
