package org.example.sici1.util;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.example.sici1.model.Bien;
import org.example.sici1.model.Inventario;
import org.example.sici1.model.Usuario;

import java.io.File;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera el PDF del resguardo de inventario a partir de la plantilla Inventario.jrxml.
 */
public final class ReporteInventario {

    private static final String PLANTILLA = "/org/example/sici1/reportes/Inventario.jrxml";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static JasperReport compilado;

    private ReporteInventario() {}

    public static void exportarPdf(Inventario inv, List<Bien> bienes, Usuario responsable, File destino) throws JRException {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("INSTITUCION", institucion());
        parametros.put("FOLIO", inv.id());
        parametros.put("FECHA", inv.fecha().format(FECHA));
        parametros.put("UNIDAD_ADMINISTRATIVA", inv.unidadAdministrativa().nombre());
        parametros.put("ESPACIO", inv.espacio().nombre());
        parametros.put("EDIFICIO", inv.espacio().edificioNombre());
        parametros.put("RESPONSABLE", responsable != null ? responsable.nombre() : inv.responsable());
        parametros.put("PUESTO", responsable != null ? responsable.puesto() : "");

        Collection<Map<String, ?>> filas = new ArrayList<>();
        for (Bien b : bienes) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("CODIGO", b.codigo());
            fila.put("DESCRIPCION", b.descripcion());
            fila.put("MARCA", b.marca());
            fila.put("MODELO", b.modelo());
            fila.put("NUMERO_SERIE", b.numeroSerie());
            fila.put("ESTADO", b.estado());
            filas.add(fila);
        }

        JasperPrint impresion = JasperFillManager.fillReport(plantilla(), parametros, new JRMapCollectionDataSource(filas));
        JasperExportManager.exportReportToPdfFile(impresion, destino.getAbsolutePath());
    }

    private static synchronized JasperReport plantilla() throws JRException {
        if (compilado == null) {
            try (InputStream in = ReporteInventario.class.getResourceAsStream(PLANTILLA)) {
                if (in == null) throw new JRException("No se encontró la plantilla " + PLANTILLA);
                compilado = JasperCompileManager.compileReport(in);
            } catch (java.io.IOException e) {
                throw new JRException(e);
            }
        }
        return compilado;
    }

    /** Nombre que aparece en el encabezado. Se puede cambiar con la variable SICI_INSTITUCION. */
    private static String institucion() {
        String valor = System.getenv("SICI_INSTITUCION");
        return valor == null || valor.isBlank() ? "Sistema de Inventario Institucional" : valor.trim();
    }
}
