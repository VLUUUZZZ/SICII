package org.example.sici1.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.*;

public class RespaldoView {

    @FXML private Button btnRespaldo;
    @FXML private Button btnRestaurar;

    @FXML
    public void initialize() {
        btnRespaldo.setOnAction(e -> crearRespaldo());
        btnRestaurar.setOnAction(e -> restaurarRespaldo());
    }

    // Demo: Respaldar archivo local (cambia por la ruta real de tu base de datos)
    private void crearRespaldo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Respaldo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo de respaldo (*.bak)", "*.bak"));
        File archivoRespaldo = fileChooser.showSaveDialog(null);

        if (archivoRespaldo != null) {
            File archivoBD = new File("datos-demo.db"); // Cambia por la ruta de tu BD real
            if (!archivoBD.exists()) {
                mostrarAlerta("No se encontró el archivo de base de datos (datos-demo.db).", Alert.AlertType.ERROR);
                return;
            }
            try (InputStream in = new FileInputStream(archivoBD);
                 OutputStream out = new FileOutputStream(archivoRespaldo)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                mostrarAlerta("Respaldo creado correctamente:\n" + archivoRespaldo.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException ex) {
                mostrarAlerta("Error al crear respaldo: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // Demo: Restaurar archivo local (cambia por la ruta real de tu base de datos)
    private void restaurarRespaldo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecciona respaldo para restaurar");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo de respaldo (*.bak)", "*.bak"));
        File archivoRespaldo = fileChooser.showOpenDialog(null);

        if (archivoRespaldo != null) {
            File archivoBD = new File("datos-demo.db"); // Cambia por la ruta de tu BD real
            try (InputStream in = new FileInputStream(archivoRespaldo);
                 OutputStream out = new FileOutputStream(archivoBD)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                mostrarAlerta("Base de datos restaurada correctamente.", Alert.AlertType.INFORMATION);
            } catch (IOException ex) {
                mostrarAlerta("Error al restaurar respaldo: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void mostrarAlerta(String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle("Respaldo");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
