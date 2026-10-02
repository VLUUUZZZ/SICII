package org.example.sici1.data;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.SetOptions;
import org.example.sici1.model.Catalogo;
import org.example.sici1.util.Formato;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a las colecciones que solo tienen nombre y estado.
 * Antes había una clase casi idéntica por cada tabla; ahora es una sola.
 */
public class CatalogoRepository extends Repositorio {

    public static final CatalogoRepository EDIFICIOS = new CatalogoRepository("edificios", "edificio");
    public static final CatalogoRepository PUESTOS = new CatalogoRepository("puestos", "puesto");
    public static final CatalogoRepository UNIDADES_ADMINISTRATIVAS =
            new CatalogoRepository("unidadesAdministrativas", "unidad administrativa");

    private final String nombreColeccion;
    private final String singular;

    private CatalogoRepository(String nombreColeccion, String singular) {
        this.nombreColeccion = nombreColeccion;
        this.singular = singular;
    }

    public String singular() { return singular; }

    public List<Catalogo> listar() {
        return Firebase.esperar(col().get()).getDocuments().stream()
                .map(CatalogoRepository::aModelo)
                .sorted(Comparator.comparing(c -> Formato.clave(c.nombre())))
                .toList();
    }

    public List<Catalogo> listarActivos() {
        return listar().stream().filter(Catalogo::activo).toList();
    }

    public Map<String, Catalogo> porId() {
        Map<String, Catalogo> mapa = new HashMap<>();
        for (Catalogo c : listar()) mapa.put(c.id(), c);
        return mapa;
    }

    public boolean estaVacio() {
        return Firebase.esperar(col().limit(1).get()).isEmpty();
    }

    /** Crea el registro si no tiene id o lo actualiza si ya existe. */
    public Catalogo guardar(Catalogo c) {
        String nombre = Formato.texto(c.nombre()).replaceAll("\\s+", " ");
        if (nombre.isEmpty()) throw new ValidacionException("El nombre no puede estar vacío.");
        String clave = Formato.clave(nombre);
        if (existeOtro(col(), "clave", clave, c.id())) {
            throw new ValidacionException("Ya existe un(a) " + singular + " con ese nombre.");
        }

        boolean nuevo = c.id() == null;
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", nombre);
        datos.put("clave", clave);
        datos.put("activo", c.activo());

        DocumentReference ref = nuevo ? col().document() : col().document(c.id());
        Firebase.esperar(ref.set(conFechas(datos, nuevo), SetOptions.merge()));
        return new Catalogo(ref.getId(), nombre, c.activo());
    }

    public void cambiarActivo(String id, boolean activo) {
        Firebase.esperar(col().document(id).update("activo", activo, "actualizadoEn", FieldValue.serverTimestamp()));
    }

    private CollectionReference col() { return coleccion(nombreColeccion); }

    private static Catalogo aModelo(DocumentSnapshot d) {
        return new Catalogo(d.getId(), d.getString("nombre"), bool(d.getBoolean("activo")));
    }
}
