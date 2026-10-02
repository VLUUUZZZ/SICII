package org.example.sici1.data;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.SetOptions;
import org.example.sici1.model.Catalogo;
import org.example.sici1.model.Espacio;
import org.example.sici1.util.Formato;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Espacios físicos. Solo se guarda el id del edificio; el nombre se resuelve al leer,
 * así renombrar un edificio no deja datos viejos.
 */
public class EspacioRepository extends Repositorio {

    public static final EspacioRepository INSTANCIA = new EspacioRepository();

    private EspacioRepository() {}

    public List<Espacio> listar() {
        Map<String, Catalogo> edificios = CatalogoRepository.EDIFICIOS.porId();
        return Firebase.esperar(col().get()).getDocuments().stream()
                .map(d -> aModelo(d, edificios))
                .sorted(Comparator.comparing(e -> Formato.clave(e.nombre())))
                .toList();
    }

    public List<Espacio> listarActivos() {
        return listar().stream().filter(Espacio::activo).toList();
    }

    public Map<String, Espacio> porId() {
        Map<String, Espacio> mapa = new HashMap<>();
        for (Espacio e : listar()) mapa.put(e.id(), e);
        return mapa;
    }

    public boolean estaVacio() {
        return Firebase.esperar(col().limit(1).get()).isEmpty();
    }

    public Espacio guardar(Espacio e) {
        String nombre = Formato.texto(e.nombre());
        if (nombre.isEmpty()) throw new ValidacionException("El nombre no puede estar vacío.");
        if (e.edificioId() == null) throw new ValidacionException("Selecciona el edificio al que pertenece.");
        String clave = Formato.clave(nombre);
        if (existeOtro(col(), "clave", clave, e.id())) {
            throw new ValidacionException("Ya existe un espacio con ese nombre.");
        }

        boolean nuevo = e.id() == null;
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("clave", clave);
        datos.put("codigo", Formato.texto(e.codigo()));
        datos.put("edificioId", e.edificioId());
        datos.put("activo", e.activo());

        DocumentReference ref = nuevo ? col().document() : col().document(e.id());
        Firebase.esperar(ref.set(conFechas(datos, nuevo), SetOptions.merge()));
        return new Espacio(ref.getId(), nombre, e.codigo(), e.edificioId(), e.edificioNombre(), e.activo());
    }

    private CollectionReference col() { return coleccion("espacios"); }

    private static Espacio aModelo(DocumentSnapshot d, Map<String, Catalogo> edificios) {
        String edificioId = d.getString("edificioId");
        Catalogo edificio = edificios.get(edificioId);
        return new Espacio(d.getId(), d.getString("nombre"), Formato.texto(d.getString("codigo")),
                edificioId, edificio == null ? "(sin edificio)" : edificio.nombre(), bool(d.getBoolean("activo")));
    }
}
