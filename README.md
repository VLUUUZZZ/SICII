# SICI · Sistema de Inventario de Bienes Institucionales

Aplicación de escritorio (JavaFX) para administrar **edificios, espacios, unidades administrativas, puestos, empleados y bienes**, y para generar el **resguardo de inventario** en PDF. Los datos se guardan en **Cloud Firestore (Firebase)**.

> Proyecto académico desarrollado en la Universidad Tecnológica Emiliano Zapata (UTEZ) como réplica de un sistema institucional real, con fines de aprendizaje.

---

## Funciones

| Módulo | Qué hace | Quién |
|---|---|---|
| **Edificios / Puestos / Unidades administrativas** | Alta, edición, búsqueda y activación/desactivación | Admin edita · todos consultan |
| **Espacios** | Aulas, oficinas, laboratorios y el edificio al que pertenecen | Admin edita · todos consultan |
| **Bienes** | Código de inventario único, descripción, marca, modelo, serie, estado e imagen | Admin edita · todos consultan |
| **Inventario** | Encabezado (unidad + espacio + fecha), asignar/desasignar bienes por código y exportar el resguardo en PDF | Todos |
| **Empleados** | Usuarios, rol (ADMIN / EMPLEADO), puesto, contraseña y estado | Solo admin |

Reglas que valida el sistema:
- No se repiten nombres en catálogos, códigos de bien ni nombres de usuario (sin importar mayúsculas).
- Un bien en estado **Baja** no se puede asignar, y un bien no se puede agregar dos veces al mismo inventario.
- Solo hay un inventario activo por unidad administrativa + espacio + fecha (si ya existe, se ofrece abrirlo).
- Las contraseñas se guardan con hash **PBKDF2** (nunca en texto plano). Un usuario inactivo no puede iniciar sesión.

## Tecnologías

- Java 17+ · JavaFX 21 (FXML)
- Firebase Admin SDK · Cloud Firestore
- JasperReports (plantilla `.jrxml` → PDF)
- Maven · JUnit 5

---

## Cómo ejecutarlo

### Opción A: con tu proyecto de Firebase

1. En la [consola de Firebase](https://console.firebase.google.com/) crea un proyecto y activa **Firestore Database**.
2. Ve a **Configuración del proyecto → Cuentas de servicio → Generar nueva clave privada**.
3. Guarda el archivo descargado como `firebase-credentials.json` en la raíz del proyecto (ya está en `.gitignore`).
   También puedes dejarlo en otro lugar e indicar su ruta con la variable `SICI_FIREBASE_CREDENTIALS`.
4. (Recomendado) Publica las reglas de `firestore.rules`, que bloquean el acceso directo desde fuera de la aplicación:
   `firebase deploy --only firestore:rules --project <id-de-tu-proyecto>`
5. Ejecuta:
   ```bash
   ./mvnw javafx:run
   ```

### Opción B: sin cuenta, con el emulador local

Necesitas [Firebase CLI](https://firebase.google.com/docs/cli) (`npm i -g firebase-tools`).

```bash
firebase emulators:start --only firestore          # en una terminal
FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 ./mvnw javafx:run   # en otra
```

En Windows (PowerShell): `$env:FIRESTORE_EMULATOR_HOST="127.0.0.1:8085"; .\mvnw javafx:run`

### Primer inicio

Si la base está vacía, la aplicación crea automáticamente datos de ejemplo y el usuario administrador:

| Usuario | Contraseña |
|---|---|
| `admin` | `admin123` (o el valor de la variable `SICI_ADMIN_PASSWORD`) |

Cambia la contraseña desde **Empleados → Editar** después de entrar.

### Variables de entorno opcionales

| Variable | Uso |
|---|---|
| `SICI_FIREBASE_CREDENTIALS` | Ruta a la llave de la cuenta de servicio |
| `SICI_FIREBASE_PROJECT_ID` | Id del proyecto (si no viene en la llave) |
| `FIRESTORE_EMULATOR_HOST` | Usa el emulador local en lugar de la nube |
| `SICI_ADMIN_PASSWORD` | Contraseña del admin que se crea en el primer inicio |
| `SICI_INSTITUCION` | Nombre que aparece en el encabezado del PDF |

### Generar el ejecutable

```bash
./mvnw package
java -jar target/SICI1-1.0-SNAPSHOT.jar
```

El `.jar` incluye JavaFX para el sistema operativo donde se compiló (compílalo en Windows para usarlo en Windows).

### Pruebas

```bash
./mvnw test                                                # pruebas unitarias
firebase emulators:exec --only firestore "./mvnw test"     # también las de Firestore
```

---

## Estructura

```
src/main/java/org/example/sici1/
├── Main.java / Launcher.java      Arranque de la aplicación
├── controller/                    Controladores de cada pantalla (FXML)
├── data/                          Conexión a Firebase y repositorios por colección
├── model/                         Edificio/Puesto/Unidad (Catalogo), Espacio, Bien, Usuario, Inventario
└── util/                          Sesión, alertas, tareas en segundo plano, hash de contraseñas, reporte PDF
src/main/resources/org/example/sici1/
├── view/                          Pantallas .fxml
└── reportes/Inventario.jrxml      Plantilla del resguardo
```

## Modelo de datos (Firestore)

```
edificios/{id}                 nombre, activo
puestos/{id}                   nombre, activo
unidadesAdministrativas/{id}   nombre, activo
espacios/{id}                  nombre, codigo, edificioId, activo
bienes/{id}                    codigo, descripcion, marca, modelo, numeroSerie, estado, imagen
usuarios/{username}            nombre, rol, puesto, activo, passwordHash
inventarios/{id}               unidadAdministrativaId, espacioId, fecha, responsable, activo
  └── bienes/{bienId}          codigo, agregadoEn
```

Cada documento guarda además `clave` (nombre en minúsculas para evitar duplicados) y las fechas `creadoEn` / `actualizadoEn`.
