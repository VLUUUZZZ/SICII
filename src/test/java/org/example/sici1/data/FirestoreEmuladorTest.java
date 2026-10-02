package org.example.sici1.data;

import org.example.sici1.model.Bien;
import org.example.sici1.model.Catalogo;
import org.example.sici1.model.Espacio;
import org.example.sici1.model.Inventario;
import org.example.sici1.model.Usuario;
import org.example.sici1.util.ReporteInventario;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas contra el emulador de Firestore. Solo se ejecutan si existe FIRESTORE_EMULATOR_HOST,
 * por ejemplo: firebase emulators:exec --only firestore "mvn test"
 */
@EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FirestoreEmuladorTest {

    @Test
    @Order(1)
    void creaAdministradorYDatosDeEjemploSoloUnaVez() {
        String contrasena = DatosIniciales.cargarSiEsNecesario();
        assertNotNull(contrasena);
        assertNull(DatosIniciales.cargarSiEsNecesario());
        assertFalse(CatalogoRepository.EDIFICIOS.listar().isEmpty());
        assertFalse(BienRepository.INSTANCIA.listar().isEmpty());
    }

    @Test
    @Order(2)
    void autenticaConHashYRespetaElEstado() {
        UsuarioRepository repo = UsuarioRepository.INSTANCIA;
        assertTrue(repo.autenticar("ADMIN", DatosIniciales.ADMIN_CONTRASENA_POR_DEFECTO).isPresent());
        assertTrue(repo.autenticar("admin", "incorrecta").isEmpty());

        repo.crear(new Usuario("prueba.uno", "Usuario de Prueba", Usuario.ROL_EMPLEADO, "Docente", true), "clave123");
        assertThrows(ValidacionException.class,
                () -> repo.crear(new Usuario("Prueba.Uno", "Otro", Usuario.ROL_EMPLEADO, null, true), "clave123"));
        assertThrows(ValidacionException.class,
                () -> repo.crear(new Usuario("corto", "X", Usuario.ROL_EMPLEADO, null, true), "123"));

        repo.cambiarActivo("prueba.uno", false);
        assertTrue(repo.autenticar("prueba.uno", "clave123").isEmpty());
        repo.actualizar(new Usuario("prueba.uno", "Usuario de Prueba", Usuario.ROL_EMPLEADO, "Docente", true), "nueva456");
        assertTrue(repo.autenticar("prueba.uno", "nueva456").isPresent());
    }

    @Test
    @Order(3)
    void noPermiteNombresDuplicadosEnCatalogos() {
        CatalogoRepository repo = CatalogoRepository.PUESTOS;
        Catalogo c = repo.guardar(new Catalogo(null, "Coordinador", true));
        assertThrows(ValidacionException.class, () -> repo.guardar(new Catalogo(null, "  coordinador ", true)));
        // Editar el mismo registro sin cambiar nombre sí se permite
        Catalogo editado = repo.guardar(new Catalogo(c.id(), "Coordinador", false));
        assertFalse(editado.activo());
        assertThrows(ValidacionException.class, () -> repo.guardar(new Catalogo(null, " ", true)));
    }

    @Test
    @Order(4)
    void asignaBienesAUnInventarioYGeneraElPdf(@TempDir Path tmp) throws Exception {
        Catalogo unidad = CatalogoRepository.UNIDADES_ADMINISTRATIVAS.listarActivos().get(0);
        Espacio espacio = EspacioRepository.INSTANCIA.listarActivos().get(0);
        assertNotEquals("(sin edificio)", espacio.edificioNombre());
        LocalDate hoy = LocalDate.now();

        InventarioRepository repo = InventarioRepository.INSTANCIA;
        assertTrue(repo.buscarActivo(unidad, espacio, hoy).isEmpty());
        Inventario inv = repo.crear(unidad, espacio, hoy, "admin");
        assertEquals(inv.id(), repo.buscarActivo(unidad, espacio, hoy).orElseThrow().id());

        Bien pc = repo.agregarBien(inv.id(), "pc-2020-041");
        assertEquals("PC-2020-041", pc.codigo());
        repo.agregarBien(inv.id(), "PRY-2019-007");
        assertThrows(ValidacionException.class, () -> repo.agregarBien(inv.id(), "PC-2020-041"));
        assertThrows(ValidacionException.class, () -> repo.agregarBien(inv.id(), "NO-EXISTE"));

        Bien baja = BienRepository.INSTANCIA.guardar(
                new Bien(null, "BAJA-001", "Silla rota", "", "", "", Bien.BAJA, null));
        assertThrows(ValidacionException.class, () -> repo.agregarBien(inv.id(), baja.codigo()));

        List<Bien> bienes = repo.bienes(inv.id());
        assertEquals(List.of("PC-2020-041", "PRY-2019-007"), bienes.stream().map(Bien::codigo).toList());

        File pdf = tmp.resolve("inventario.pdf").toFile();
        ReporteInventario.exportarPdf(inv, bienes, UsuarioRepository.INSTANCIA.buscar("admin").orElse(null), pdf);
        assertTrue(Files.size(pdf.toPath()) > 1000);
        assertEquals("%PDF", new String(Files.readAllBytes(pdf.toPath()), 0, 4));

        repo.quitarBien(inv.id(), pc.id());
        assertEquals(1, repo.bienes(inv.id()).size());
    }

    @Test
    @Order(5)
    void bienesConCodigoDuplicadoOImagenGrandeSeRechazan() {
        BienRepository repo = BienRepository.INSTANCIA;
        assertThrows(ValidacionException.class,
                () -> repo.guardar(new Bien(null, "pc-2020-041", "Copia", "", "", "", Bien.OPERATIVO, null)));
        byte[] grande = new byte[BienRepository.MAX_BYTES_IMAGEN + 1];
        assertThrows(ValidacionException.class,
                () -> repo.guardar(new Bien(null, "IMG-1", "Con imagen", "", "", "", Bien.OPERATIVO, grande)));
        Bien conImagen = repo.guardar(new Bien(null, "IMG-2", "Con imagen", "", "", "", Bien.OPERATIVO, new byte[]{1, 2, 3}));
        assertArrayEquals(new byte[]{1, 2, 3}, repo.buscarPorCodigo("img-2").orElseThrow().imagen());
        assertEquals(conImagen.id(), repo.buscarPorCodigo("IMG-2").orElseThrow().id());
    }
}
