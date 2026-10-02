package org.example.sici1.data;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.example.sici1.model.Bien;
import org.example.sici1.model.Catalogo;
import org.example.sici1.model.Espacio;
import org.example.sici1.model.Inventario;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Inventarios (asignaciones). Cada inventario tiene una subcolección "bienes"
 * cuyo id de documento es el id del bien, para que no se pueda agregar dos veces.
 */
public class InventarioRepository extends Repositorio {

    public static final InventarioRepository INSTANCIA = new InventarioRepository();

    private InventarioRepository() {}

    /** Busca un inventario activo con la misma unidad, espacio y fecha. */
    public Optional<Inventario> buscarActivo(Catalogo unidad, Espacio espacio, LocalDate fecha) {
        return Firebase.esperar(col()
                        .whereEqualTo("unidadAdministrativaId", unidad.id())
                        .whereEqualTo("espacioId", espacio.id())
                        .whereEqualTo("fecha", fecha.toString())
                        .whereEqualTo("activo", true)
                        .limit(1).get())
                .getDocuments().stream().findFirst()
                .map(d -> new Inventario(d.getId(), unidad, espacio, fecha, d.getString("responsable"), true));
    }

    public Inventario crear(Catalogo unidad, Espacio espacio, LocalDate fecha, String responsable) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("unidadAdministrativaId", unidad.id());
        datos.put("espacioId", espacio.id());
        datos.put("fecha", fecha.toString());
        datos.put("responsable", responsable);
        datos.put("activo", true);
        DocumentReference ref = col().document();
        Firebase.esperar(ref.set(conFechas(datos, true)));
        return new Inventario(ref.getId(), unidad, espacio, fecha, responsable, true);
    }

    public List<Bien> bienes(String inventarioId) {
        List<String> ids = Firebase.esperar(detalle(inventarioId).orderBy("agregadoEn", Query.Direction.ASCENDING).get())
                .getDocuments().stream().map(QueryDocumentSnapshot::getId).toList();
        return BienRepository.INSTANCIA.buscarVarios(ids);
    }

    /** Agrega un bien por su código. Devuelve el bien agregado. */
    public Bien agregarBien(String inventarioId, String codigo) {
        Bien bien = BienRepository.INSTANCIA.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ValidacionException("No existe un bien con el código " + codigo + "."));
        if (Bien.BAJA.equals(bien.estado())) {
            throw new ValidacionException("El bien " + bien.codigo() + " está dado de baja y no se puede asignar.");
        }

        DocumentReference ref = detalle(inventarioId).document(bien.id());
        Firebase.esperar(db().runTransaction(tx -> {
            if (tx.get(ref).get().exists()) {
                throw new ValidacionException("Ese bien ya está agregado a este inventario.");
            }
            tx.create(ref, Map.of("codigo", bien.codigo(), "agregadoEn", FieldValue.serverTimestamp()));
            return null;
        }));
        return bien;
    }

    public void quitarBien(String inventarioId, String bienId) {
        Firebase.esperar(detalle(inventarioId).document(bienId).delete());
    }

    private CollectionReference col() { return coleccion("inventarios"); }

    private CollectionReference detalle(String inventarioId) {
        return col().document(inventarioId).collection("bienes");
    }
}
