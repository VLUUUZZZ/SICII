package org.example.sici1.util;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Ayudas para configurar tablas que se repetían en todas las vistas.
 */
public final class Tablas {

    private Tablas() {}

    /** Columna de solo lectura que muestra el texto que regresa la función. */
    public static <T> void texto(TableColumn<T, String> columna, Function<T, String> valor) {
        columna.setCellValueFactory(d -> new ReadOnlyStringWrapper(valor.apply(d.getValue())));
    }

    /** Columna con una casilla para activar/desactivar el registro. */
    public static <T> void interruptor(TableColumn<T, Void> columna, Predicate<T> activo, BiConsumer<T, Boolean> alCambiar) {
        columna.setSortable(false);
        columna.setCellFactory(col -> new TableCell<>() {
            private final CheckBox check = new CheckBox();
            {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                check.setOnAction(e -> {
                    T item = getTableRow() == null ? null : getTableRow().getItem();
                    if (item != null) alCambiar.accept(item, check.isSelected());
                });
            }

            @Override
            protected void updateItem(Void v, boolean vacio) {
                super.updateItem(v, vacio);
                T item = getTableRow() == null ? null : getTableRow().getItem();
                if (vacio || item == null) {
                    setGraphic(null);
                } else {
                    check.setSelected(activo.test(item));
                    setGraphic(check);
                }
            }
        });
    }

    /** Ejecuta la acción al hacer doble clic sobre una fila. */
    public static <T> void dobleClic(TableView<T> tabla, Consumer<T> accion) {
        tabla.setRowFactory(tv -> {
            TableRow<T> fila = new TableRow<>();
            fila.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !fila.isEmpty()) accion.accept(fila.getItem());
            });
            return fila;
        });
    }

    /** Devuelve la fila seleccionada o avisa al usuario si no hay ninguna. */
    public static <T> T seleccionado(TableView<T> tabla, String titulo) {
        T item = tabla.getSelectionModel().getSelectedItem();
        if (item == null) Alertas.aviso(titulo, "Selecciona un registro de la tabla.");
        return item;
    }
}
