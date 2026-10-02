package org.example.sici1.data;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Punto único de conexión a Cloud Firestore.
 *
 * Busca la llave de la cuenta de servicio en este orden:
 *   1. Variable de entorno SICI_FIREBASE_CREDENTIALS
 *   2. Variable de entorno GOOGLE_APPLICATION_CREDENTIALS
 *   3. Archivo firebase-credentials.json en la carpeta desde donde se ejecuta
 *   4. Archivo ~/.sici/firebase-credentials.json
 *
 * Si existe FIRESTORE_EMULATOR_HOST se usa el emulador local y no se necesita llave.
 */
public final class Firebase {

    private static final String ARCHIVO_CREDENCIALES = "firebase-credentials.json";
    private static final long SEGUNDOS_ESPERA = 30;

    private static Firestore db;

    private Firebase() {}

    public static synchronized Firestore db() {
        if (db == null) db = conectar();
        return db;
    }

    /** Espera el resultado de una operación de Firestore y traduce sus errores. */
    public static <T> T esperar(ApiFuture<T> futuro) {
        try {
            return futuro.get(SEGUNDOS_ESPERA, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DatosException("Operación interrumpida", e);
        } catch (ExecutionException e) {
            for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
                if (t instanceof ValidacionException v) throw v;
            }
            throw new DatosException("Error de Firestore", e.getCause());
        } catch (TimeoutException e) {
            throw new DatosException("Firebase no respondió a tiempo. Revisa tu conexión a internet.", e);
        }
    }

    private static Firestore conectar() {
        String proyecto = variable("SICI_FIREBASE_PROJECT_ID");
        String emulador = System.getenv("FIRESTORE_EMULATOR_HOST");

        if (emulador != null && !emulador.isBlank()) {
            // La librería detecta el emulador por la variable de entorno y no pide credenciales.
            return FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId(proyecto != null ? proyecto : "sici-local")
                    .build()
                    .getService();
        }

        Path credenciales = ubicarCredenciales();
        try (InputStream in = Files.newInputStream(credenciales)) {
            FirebaseOptions.Builder opciones = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in));
            if (proyecto != null) opciones.setProjectId(proyecto);
            FirebaseApp app = FirebaseApp.initializeApp(opciones.build());
            return FirestoreClient.getFirestore(app);
        } catch (IOException e) {
            throw new DatosException("No se pudo leer la llave de Firebase: " + credenciales, e);
        }
    }

    private static Path ubicarCredenciales() {
        List<Path> candidatos = new ArrayList<>();
        String porVariable = variable("SICI_FIREBASE_CREDENTIALS");
        if (porVariable != null) candidatos.add(Path.of(porVariable));
        String google = variable("GOOGLE_APPLICATION_CREDENTIALS");
        if (google != null) candidatos.add(Path.of(google));
        candidatos.add(Path.of(ARCHIVO_CREDENCIALES));
        candidatos.add(Path.of(System.getProperty("user.home"), ".sici", ARCHIVO_CREDENCIALES));

        for (Path p : candidatos) {
            if (Files.isRegularFile(p)) return p;
        }
        throw new DatosException("No se encontró la llave de Firebase. Descárgala desde la consola de Firebase "
                + "(Configuración del proyecto > Cuentas de servicio) y guárdala como " + ARCHIVO_CREDENCIALES
                + " junto al proyecto, o indica su ruta en la variable SICI_FIREBASE_CREDENTIALS.", null);
    }

    private static String variable(String nombre) {
        String valor = System.getenv(nombre);
        if (valor == null || valor.isBlank()) valor = System.getProperty(nombre);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
