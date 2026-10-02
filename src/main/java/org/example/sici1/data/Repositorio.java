package org.example.sici1.data;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;

import java.util.Map;

/**
 * Funciones comunes a todos los repositorios.
 */
abstract class Repositorio {

    protected final Firestore db() { return Firebase.db(); }

    protected final CollectionReference coleccion(String nombre) { return db().collection(nombre); }

    /** true si otro documento (distinto de idExcluido) ya tiene ese valor en el campo. */
    protected final boolean existeOtro(CollectionReference col, String campo, Object valor, String idExcluido) {
        for (QueryDocumentSnapshot doc : Firebase.esperar(col.whereEqualTo(campo, valor).get()).getDocuments()) {
            if (!doc.getId().equals(idExcluido)) return true;
        }
        return false;
    }

    /** Agrega las marcas de tiempo de creación/actualización al documento. */
    protected static Map<String, Object> conFechas(Map<String, Object> datos, boolean nuevo) {
        datos.put("actualizadoEn", FieldValue.serverTimestamp());
        if (nuevo) datos.put("creadoEn", FieldValue.serverTimestamp());
        return datos;
    }

    protected static boolean bool(Boolean b) { return b != null && b; }
}
