package org.example.sici1.data;

import com.google.cloud.firestore.Blob;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.SetOptions;
import org.example.sici1.model.Bien;
import org.example.sici1.util.Formato;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BienRepository extends Repositorio {

    public static final BienRepository INSTANCIA = new BienRepository();

    /** Firestore permite documentos de hasta 1 MB; dejamos margen para el resto de campos. */
    public static final int MAX_BYTES_IMAGEN = 700 * 1024;

    private BienRepository() {}

    public List<Bien> listar() {
        return Firebase.esperar(col().get()).getDocuments().stream()
                .map(BienRepository::aModelo)
                .sorted(Comparator.comparing(b -> Formato.clave(b.codigo())))
                .toList();
    }

    public boolean estaVacio() {
        return Firebase.esperar(col().limit(1).get()).isEmpty();
    }

    public Optional<Bien> buscarPorCodigo(String codigo) {
        return Firebase.esperar(col().whereEqualTo("clave", Formato.clave(codigo)).limit(1).get())
                .getDocuments().stream().findFirst().map(BienRepository::aModelo);
    }

    /** Lee varios bienes por id, en el mismo orden recibido. Los que ya no existan se omiten. */
    public List<Bien> buscarVarios(List<String> ids) {
        if (ids.isEmpty()) return List.of();
        DocumentReference[] refs = ids.stream().map(col()::document).toArray(DocumentReference[]::new);
        List<Bien> bienes = new ArrayList<>();
        for (DocumentSnapshot d : Firebase.esperar(db().getAll(refs))) {
            if (d.exists()) bienes.add(aModelo(d));
        }
        return bienes;
    }

    public Bien guardar(Bien b) {
        String codigo = Formato.texto(b.codigo());
        if (codigo.isEmpty()) throw new ValidacionException("El código de inventario es obligatorio.");
        if (Formato.texto(b.descripcion()).isEmpty()) throw new ValidacionException("La descripción es obligatoria.");
        if (b.imagen() != null && b.imagen().length > MAX_BYTES_IMAGEN) {
            throw new ValidacionException("La imagen pesa demasiado (máximo " + (MAX_BYTES_IMAGEN / 1024) + " KB).");
        }
        String clave = Formato.clave(codigo);
        if (existeOtro(col(), "clave", clave, b.id())) {
            throw new ValidacionException("Ya existe un bien con el código " + codigo + ".");
        }

        boolean nuevo = b.id() == null;
        Map<String, Object> datos = new HashMap<>();
        datos.put("codigo", codigo);
        datos.put("clave", clave);
        datos.put("descripcion", Formato.texto(b.descripcion()));
        datos.put("marca", Formato.vacioANull(b.marca()));
        datos.put("modelo", Formato.vacioANull(b.modelo()));
        datos.put("numeroSerie", Formato.vacioANull(b.numeroSerie()));
        datos.put("estado", Bien.normalizarEstado(b.estado()));
        datos.put("imagen", b.imagen() == null ? null : Blob.fromBytes(b.imagen()));

        DocumentReference ref = nuevo ? col().document() : col().document(b.id());
        Firebase.esperar(ref.set(conFechas(datos, nuevo), SetOptions.merge()));
        return new Bien(ref.getId(), codigo, b.descripcion(), b.marca(), b.modelo(), b.numeroSerie(),
                Bien.normalizarEstado(b.estado()), b.imagen());
    }

    private CollectionReference col() { return coleccion("bienes"); }

    private static Bien aModelo(DocumentSnapshot d) {
        Blob imagen = d.getBlob("imagen");
        return new Bien(d.getId(), d.getString("codigo"), d.getString("descripcion"),
                Formato.texto(d.getString("marca")), Formato.texto(d.getString("modelo")),
                Formato.texto(d.getString("numeroSerie")), Bien.normalizarEstado(d.getString("estado")),
                imagen == null ? null : imagen.toBytes());
    }
}
