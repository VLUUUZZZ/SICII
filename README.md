Sistema de Gestión de Inventario de Bienes Institucionales

---

 Descripción General

El **Sistema de Gestión de Inventario de Bienes Institucionales** es un Sistema que centraliza la administración de **edificios, espacios, unidades administrativas, puestos, empleados, bienes e inventarios**.  
Su propósito principal es **agregar, actualizar, eliminar y visualizar** los bienes a lo largo de su ciclo de vida.

---

Componentes Principales

- **Edificios y Espacios**: Registro detallado de ubicaciones físicas (oficinas, laboratorios, almacenes, etc.).
- **Unidades Administrativas**: Áreas organizativas asociadas a espacios físicos.
- **Puestos y Empleados**: Administración de cargos y personal.
- **Bienes**: Alta de activos con código único, descripción, estado y ubicación.
- **Inventarios y Movimientos**: Control de asignaciones, bajas.

---

🔄 Flujo de Operación General

1. **Configuración inicial**: Registrar edificios, espacios, unidades administrativas, puestos y empleados.
2. **Alta de bienes**: Ingresar los activos en el sistema con estado **Disponible**.
3. **Movimientos**: Realizar asignaciones, bajas, devoluciones o envíos a mantenimiento.

---

🎯 Flujo Específico: Asignación de un Bien

**Objetivo**: Asignar un bien a un empleado previamente registrado, indicando **nombre del empleado**, **espacio**, **unidad administrativa** y **fecha**, vinculándolo con un bien existente en el módulo *Bienes*.

 ✅ Precondiciones
- Empleado registrado y activo.
- Espacio y unidad administrativa existentes y activos.
- Bien registrado con estado **Disponible**.

📝 Pasos
1. Localizar al empleado por nombre.
2. Seleccionar el espacio físico correspondiente.
3. Seleccionar la unidad administrativa.
4. Definir la fecha de asignación (por defecto: fecha actual).
5. Seleccionar el bien mediante su código de inventario.
6. Confirmar la asignación para generar el registro de movimiento y actualizar el estado del bien.

 🧩 Resultados
- Creación de un registro con vínculos a `empleado`, `espacio`, `unidad_administrativa`, `bien` y `fecha`.
- Actualización del estado del bien a **Asignado** y ubicación al espacio seleccionado.

🔐 Validaciones
- Solo bienes con estado `Disponible` pueden ser asignados.
- El empleado debe estar activo.
- Espacio y unidad administrativa deben estar activos.

---

 🧱 Modelo de Datos (Referencial)

- `empleados(id, nombre, RFC, unidad_id, espacio_id, estatus)`
- `espacios(id, edificio_id, nombre, estatus)`
- `unidades(id, nombre)`
- `bienes(codigo_inventario, descripcion, marca, modelo, numero, estado)`
- `inventario(nombre_empleado, RFC_empleado, unidad_id, puesto_id, fecha)`


---
Ejemplo de API

**Body**
```json
{
  "empleado_nombre": "Victoria",
  "espacio_id": 22,
  "unidad_id": 15,
  "fecha": "2025-01-15",
  "bien_codigo_inventario": "PC-2020-041"
}

