# Documentación de AddressBlockApp

## Resumen del Proyecto

AddressBlockApp es una aplicación Android de gestión de contactos que utiliza una arquitectura clásica basada en Fragments, ContentProvider y SQLite. La aplicación permite crear, leer, actualizar y eliminar (CRUD) contactos con información de dirección completa.

### Stack Tecnológico
- **Lenguaje:** Java
- **Arquitectura:** Fragments + ContentProvider + SQLite
- **UI:** RecyclerView, Material Design (FAB, Snackbar, TextInputLayout)
- **Persistencia:** SQLite vía ContentProvider
- **Carga de datos:** CursorLoader (LoaderManager)
- **Mínimo SDK:** 25 (Android 7.0)
- **Target SDK:** 36

---

## Arquitectura General

```
┌─────────────────────────────────────────────────────────┐
│                      MainActivity                        │
│  (Coordinador de navegación y comunicación entre Fragments)│
└────────────┬──────────────────────────────┬─────────────┘
             │                              │
    ┌────────▼────────┐           ┌────────▼────────┐
    │ ContactsFragment │           │ DetailFragment   │
    │  (Lista contactos)│           │  (Ver detalles)  │
    └────────┬────────┘           └────────┬────────┘
             │                              │
             │                ┌─────────────▼─────────────┐
             │                │   AddEditFragment        │
             │                │  (Crear/Editar contacto)  │
             │                └─────────────┬─────────────┘
             │                              │
             └──────────────┬───────────────┘
                            │
                    ┌───────▼────────┐
                    │ ContactsAdapter │
                    │ (RecyclerView) │
                    └───────┬────────┘
                            │
                    ┌───────▼──────────────────────────────┐
                    │   AddressBookContentProvider          │
                    │   (Acceso a datos via ContentResolver)│
                    └───────┬──────────────────────────────┘
                            │
                    ┌───────▼────────┐
                    │ SQLite Database │
                    │ (AddressBook.db)│
                    └────────────────┘
```

---

## Componentes Principales

### 1. MainActivity
**Ruta:** `app/src/main/java/fisei/uta/edu/ec/addressblockapp/MainActivity.java`

**Responsabilidades:**
- Punto de entrada de la aplicación (Activity launcher)
- Coordinador de navegación entre Fragments
- Implementa 3 interfaces listener para comunicarse con los Fragments
- Detecta layout de tablet vs teléfono para comportamiento dual-pane

**Métodos clave:**

- `onCreate()`: Inicializa la Toolbar y carga `ContactsFragment` al inicio
- `isTabletLayout()`: Detecta si el dispositivo tiene layout de tablet (busca `R.id.rightPaneContainer`)
- `displayFragment(Fragment, boolean)`: Muestra un fragmento reemplazando el contenido actual
- `displayDetailFragment(Uri)`: Crea `DetailFragment` con la URI del contacto y lo muestra
- `displayAddEditFragment(Uri)`: Crea `AddEditFragment` en modo nuevo (null) o edición (con URI)

**Implementación de Listeners:**

- `onContactSelected(Uri)`: Llamado desde `ContactsFragment` → muestra detalles
- `onAddContact()`: Llamado desde `ContactsFragment` → muestra formulario nuevo
- `onContactDeleted()`: Llamado desde `DetailFragment` → hace popBackStack en teléfono
- `onEditContact(Uri)`: Llamado desde `DetailFragment` → muestra formulario edición
- `onBackFromDetails()`: Llamado desde `DetailFragment` → vuelve a lista en teléfono
- `onAddEditCompleted(Uri)`: Llamado desde `AddEditFragment` → cierra formulario y actualiza detalle en tablet

**Constante importante:**
- `CONTACT_URI = "CONTACT_URI"`: Key usada en Bundle para pasar URIs entre Fragments

---

### 2. ContactsFragment
**Ruta:** `app/src/main/java/fisei/uta/edu/ec/addressblockapp/ContactsFragment.java`

**Responsabilidades:**
- Muestra la lista de contactos en un RecyclerView
- Permite seleccionar un contacto para ver detalles
- Permite agregar un nuevo contacto vía FAB
- Carga datos automáticamente usando CursorLoader

**Componentes:**
- `RecyclerView`: Lista de contactos
- `ContactsAdapter`: Adaptador para el RecyclerView
- `FloatingActionButton`: Botón para agregar contacto
- `ItemDivider`: Decoración para separar items

**Métodos clave:**

- `onCreateView()`: Infla layout, configura RecyclerView con LinearLayoutManager, inicializa FAB
- `onActivityCreated()`: Inicializa el LoaderManager
- `onCreateLoader()`: Crea CursorLoader para cargar todos los contactos ordenados por nombre (case-insensitive)
- `onLoadFinished()`: Actualiza el adapter con el nuevo cursor
- `onLoaderReset()`: Limpia el cursor del adapter

**Query ejecutada:**
```sql
SELECT * FROM contacts ORDER BY name COLLATE NOCASE ASC
```

**Interface:**
- `ContactsFragmentListener`: Define callbacks para `onContactSelected(Uri)` y `onAddContact()`

---

### 3. AddEditFragment
**Ruta:** `app/src/main/java/fisei/uta/edu/ec/addressblockapp/AddEditFragment.java`

**Responsabilidades:**
- Formulario para crear o editar contactos
- Validación de campos antes de guardar
- Persistencia vía ContentResolver (insert/update)
- Feedback al usuario vía Snackbar

**Modos de operación:**
- **Modo nuevo:** `addingNewContact = true`, no hay `contactUri`
- **Modo edición:** `addingNewContact = false`, `contactUri` recibida por Bundle

**Campos del formulario:**
- Nombre (obligatorio)
- Teléfono (obligatorio, exactamente 10 dígitos numéricos)
- Email (opcional, formato válido si se ingresa)
- Calle (opcional)
- Ciudad (opcional)
- Estado (opcional)
- ZIP (obligatorio, exactamente 5 dígitos numéricos)

**Validaciones implementadas:**

```java
validateForm() {
    - Nombre: No puede estar vacío ni contener solo espacios
    - Email: Si se ingresa, debe tener formato válido (Patterns.EMAIL_ADDRESS)
    - Teléfono: Obligatorio, solo números, exactamente 10 dígitos (\\d{10})
    - ZIP: Obligatorio, solo números, exactamente 5 dígitos (\\d{5})
}
```

**Métodos clave:**

- `onCreateView()`: Infla layout, vincula campos, configura FAB de guardar
- `updateSaveButtonFAB()`: Mantiene el FAB siempre visible y habilitado
- `validateForm()`: Valida todos los campos y muestra errores en TextInputLayout
- `saveContact()`: Ejecuta validación, construye ContentValues, llama insert/update
- `onCreateLoader()`: Carga datos del contacto a editar (solo en modo edición)
- `onLoadFinished()`: Llena campos con datos del contacto (modo edición)

**Flujo de guardado:**

1. Usuario presiona FAB "Guardar"
2. Se oculta teclado virtual
3. Se llama `validateForm()`
4. Si hay errores: se muestra Snackbar "Revisa los campos marcados"
5. Si válido:
   - Se construye `ContentValues` con los datos
   - Si es nuevo: `ContentResolver.insert(Contact.CONTENT_URI, values)`
   - Si es edición: `ContentResolver.update(contactUri, values, null, null)`
   - Se muestra Snackbar de éxito
   - Se llama `listener.onAddEditCompleted(uri)`

---

### 4. DetailFragment
**Ruta:** `app/src/main/java/fisei/uta/edu/ec/addressblockapp/DetailFragment.java`

**Responsabilidades:**
- Muestra los detalles de un contacto seleccionado
- Permite editar el contacto
- Permite eliminar el contacto con confirmación
- Muestra Snackbar de eliminación con acción DESHACER

**Componentes:**
- TextViews para cada campo (nombre, teléfono, email, dirección completa)
- Menú con acciones: Back, Edit, Delete

**Métodos clave:**

- `onCreateView()`: Infla layout, obtiene `contactUri` del Bundle, inicializa Loader
- `onCreateOptionsMenu()`: Infla menú `fragment_details_menu.xml`
- `onOptionsItemSelected()`: Maneja acciones del menú
- `confirmDelete()`: Muestra AlertDialog de confirmación y ejecuta eliminación
- `onCreateLoader()`: Carga datos del contacto específico
- `onLoadFinished()`: Muestra datos en TextViews y guarda copia en `lastLoadedContactValues`

**Flujo de eliminación con DESHACER:**

1. Usuario selecciona "Delete" en el menú
2. Se muestra AlertDialog: "¿Estás seguro? Esto eliminará permanentemente el contacto"
3. Si confirma:
   - Se capturan los valores del contacto en `deletedValues` (para posible deshacer)
   - Se captura un `Context` seguro desde `coordinatorLayout.getContext()` (evita crash si el Fragment es detachado)
   - Se elimina: `ContentResolver.delete(contactUri, null, null)`
   - Se muestra Snackbar: "Contacto eliminado" con acción "DESHACER"
   - Se llama `listener.onContactDeleted()` (hace popBackStack en teléfono)
4. Si usuario presiona "DESHACER":
   - Se reinserta el contacto: `ContentResolver.insert(Contact.CONTENT_URI, deletedValues)`
   - El ContentProvider notifica el cambio → Loader se refresca → Lista se actualiza

**Importante:** El Context capturado desde `coordinatorLayout.getContext()` es de la Activity, no del Fragment. Esto evita el crash `IllegalStateException: Fragment not attached to a context` cuando el Snackbar se muestra después de que el Fragment fue removido del back stack.

---

### 5. ContactsAdapter
**Ruta:** `app/src/main/java/fisei/uta/edu/ec/addressblockapp/ContactsAdapter.java`

**Responsabilidades:**
- Adaptador para RecyclerView que muestra la lista de contactos
- Maneja clicks en items y notifica al listener

**Componentes:**
- `ViewHolder`: Contiene TextView para mostrar el nombre
- `Cursor`: Cursor con los datos de contactos
- `ContactClickListener`: Interface para callback al hacer click

**Métodos clave:**

- `onCreateViewHolder()`: Infla layout simple_list_item_1
- `onBindViewHolder()`: Mueve cursor a posición, obtiene ID y nombre, actualiza ViewHolder
- `getItemCount()`: Retorna cantidad de items en el cursor
- `swapCursor()`: Reemplaza cursor y notifica cambios

**Click en item:**
- Al hacer click, se construye URI del contacto: `DatabaseDescription.Contact.buildContactUri(rowID)`
- Se llama `clickListener.onClick(contactUri)`

---

### 6. AddressBookContentProvider
**Ruta:** `app/src/main/java/fisei/uta/edu/ec/addressblockapp/data/AddressBookContentProvider.java`

**Responsabilidades:**
- Implementa ContentProvider para acceso a datos
- Intermediario entre ContentResolver y SQLite
- Maneja operaciones CRUD (query, insert, update, delete)
- Notifica cambios para que los Loaders se refresquen

**URIs soportadas:**
- `content://fisei.uta.edu.ec.addressblockapp.data/contacts` → CONTACTS (todos los contactos)
- `content://fisei.uta.edu.ec.addressblockapp.data/contacts/#` → CONTACT_ID (contacto específico)

**UriMatcher:**
- `CONTACTS = 1`: URI para lista completa
- `CONTACT_ID = 2`: URI para contacto individual

**Métodos clave:**

- `onCreate()`: Inicializa `AddressBookDatabaseHelper`
- `query()`: 
  - CONTACTS: retorna todos los contactos con orden especificado
  - CONTACT_ID: retorna contacto específico filtrando por `_ID`
  - Establece `setNotificationUri()` para que el Loader observe cambios
- `insert()`: Solo acepta CONTACTS, inserta fila, retorna nueva URI, notifica cambio
- `update()`: Solo acepta CONTACT_ID, actualiza fila específica, notifica cambio
- `delete()`: Solo acepta CONTACT_ID, elimina fila específica, notifica cambio
- `notifyChange()`: Notifica a ContentResolver que la URI cambió → Loader se recarga automáticamente

**Notificación de cambios:**
```java
cursor.setNotificationUri(getContext().getContentResolver(), uri);
getContext().getContentResolver().notifyChange(uri, null);
```
Esto es crucial: cuando se inserta/update/delete, los CursorLoaders que observan esa URI reciben automáticamente los nuevos datos.

---

### 7. DatabaseDescription
**Ruta:** `app/src/main/java/fisei/uta/edu/ec/addressblockapp/data/DatabaseDescription.java`

**Responsabilidades:**
- Define el contrato de la base de datos
- Constantes para nombres de tabla, columnas y URIs
- Constructor privado para evitar instanciación

**Constantes:**

- `AUTHORITY = "fisei.uta.edu.ec.addressblockapp.data"`
- `BASE_CONTENT_URI = "content://fisei.uta.edu.ec.addressblockapp.data"`

**Clase interna Contact:**
- `TABLE_NAME = "contacts"`
- `CONTENT_URI = "content://fisei.uta.edu.ec.addressblockapp.data/contacts"`
- Columnas: `_ID`, `name`, `phone`, `email`, `street`, `city`, `state`, `zip`
- `buildContactUri(long id)`: Construye URI para contacto específico

---

### 8. AddressBookDatabaseHelper
**Ruta:** `app/src/main/java/fisei/uta/edu/ec/addressblockapp/data/AddressBookDatabaseHelper.java`

**Responsabilidades:**
- Administra la base de datos SQLite
- Crea tabla en `onCreate()`
- Maneja migraciones en `onUpgrade()` (actualmente no-op para versión 1)

**Configuración:**
- `DATABASE_NAME = "AddressBook.db"`
- `DATABASE_VERSION = 1`

**Estructura de tabla:**
```sql
CREATE TABLE contacts (
    _ID INTEGER PRIMARY KEY,
    name TEXT,
    phone TEXT,
    email TEXT,
    street TEXT,
    city TEXT,
    state TEXT,
    zip TEXT
);
```

---

## Flujo de Navegación

### Inicio de la aplicación
1. MainActivity.onCreate() se ejecuta
2. Se carga ContactsFragment en `fragmentContainer`
3. ContactsFragment inicializa CursorLoader
4. CursorLoader consulta ContentProvider → SQLite
5. ContentProvider retorna Cursor con todos los contactos
6. ContactsFragment.onLoadFinished() actualiza el adapter
7. RecyclerView muestra la lista de contactos

### Ver detalles de un contacto
1. Usuario hace click en un item del RecyclerView
2. ContactsAdapter llama `listener.onClick(contactUri)`
3. ContactsFragment llama `listener.onContactSelected(contactUri)`
4. MainActivity.onContactSelected() ejecuta `displayDetailFragment(contactUri)`
5. MainActivity crea DetailFragment, pasa URI por Bundle
6. DetailFragment se muestra en teléfono: reemplaza ContactsFragment (con addToBackStack)
7. DetailFragment se muestra en tablet: se coloca en `rightPaneContainer` (sin addToBackStack)
8. DetailFragment inicializa CursorLoader para cargar datos del contacto específico
9. DetailFragment muestra datos en TextViews

### Crear nuevo contacto
1. Usuario presiona FAB "+" en ContactsFragment
2. ContactsFragment llama `listener.onAddContact()`
3. MainActivity.onAddContact() ejecuta `displayAddEditFragment(null)`
4. MainActivity crea AddEditFragment sin URI (modo nuevo)
5. AddEditFragment se muestra con campos vacíos
6. Usuario completa campos
7. Usuario presiona FAB "Guardar"
8. AddEditFragment valida campos
9. Si válido: ContentResolver.insert(Contact.CONTENT_URI, values)
10. ContentProvider inserta en SQLite y notifica cambio
11. AddEditFragment llama `listener.onAddEditCompleted(newUri)`
12. MainActivity hace popBackStack → vuelve a lista
13. En tablet: también muestra detalle del nuevo contacto

### Editar contacto existente
1. Desde DetailFragment, usuario presiona "Edit" en menú
2. DetailFragment llama `listener.onEditContact(contactUri)`
3. MainActivity.onEditContact() ejecuta `displayAddEditFragment(contactUri)`
4. MainActivity crea AddEditFragment con URI (modo edición)
5. AddEditFragment carga datos del contacto vía CursorLoader
6. AddEditFragment llena campos con datos existentes
7. Usuario modifica campos
8. Usuario presiona FAB "Guardar"
9. AddEditFragment valida campos
10. Si válido: ContentResolver.update(contactUri, values, null, null)
11. ContentProvider actualiza en SQLite y notifica cambio
12. AddEditFragment llama `listener.onAddEditCompleted(contactUri)`
13. MainActivity hace popBackStack → vuelve a detalle
14. DetailFragment se recarga automáticamente (Loader observa cambios)

### Eliminar contacto
1. Desde DetailFragment, usuario presiona "Delete" en menú
2. DetailFragment llama `confirmDelete()`
3. AlertDialog de confirmación aparece
4. Usuario confirma "Delete"
5. DetailFragment captura valores del contacto en `lastLoadedContactValues`
6. DetailFragment captura Context seguro desde Activity
7. DetailFragment llama ContentResolver.delete(contactUri, null, null)
8. ContentProvider elimina de SQLite y notifica cambio
9. DetailFragment muestra Snackbar "Contacto eliminado" con acción "DESHACER"
10. DetailFragment llama `listener.onContactDeleted()`
11. MainActivity.onContactDeleted() hace popBackStack en teléfono
12. ContactsFragment se recarga automáticamente (Loader observa cambios)

### Deshacer eliminación
1. Mientras Snackbar está visible, usuario presiona "DESHACER"
2. Snackbar callback ejecuta ContentResolver.insert(CONTENT_URI, deletedValues)
3. ContentProvider inserta fila nueva con los datos del contacto eliminado
4. ContentProvider notifica cambio
5. ContactsFragment se recarga automáticamente
6. El contacto reaparece en la lista (con nuevo ID)

---

## Validaciones de Formulario

### Reglas de validación (AddEditFragment)

| Campo | Obligatorio | Formato | Mensaje de error |
|-------|-------------|---------|------------------|
| Nombre | Sí | No vacío, no solo espacios | "El nombre es obligatorio" |
| Email | No | Si se ingresa, formato válido (Patterns.EMAIL_ADDRESS) | "El correo no tiene un formato válido" |
| Teléfono | Sí | Solo números, exactamente 10 dígitos | "El teléfono es obligatorio" / "El teléfono debe tener exactamente 10 dígitos" |
| ZIP | Sí | Solo números, exactamente 5 dígitos | "El ZIP es obligatorio" / "El ZIP debe tener exactamente 5 dígitos" |
| Calle | No | Cualquier texto | - |
| Ciudad | No | Cualquier texto | - |
| Estado | No | Cualquier texto | - |

### Implementación
- Validación se ejecuta en `validateForm()` antes de guardar
- Errores se muestran en TextInputLayout usando `setError()`
- Si hay errores, se muestra Snackbar "Revisa los campos marcados"
- Botón de guardar está siempre habilitado, pero la validación bloquea el guardado

---

## Soporte Tablet vs Teléfono

### Detección de layout
```java
private boolean isTabletLayout() {
    return findViewById(R.id.rightPaneContainer) != null;
}
```

### Comportamiento en teléfono
- Solo un contenedor: `fragmentContainer`
- Navegación usa back stack
- Al ver detalles: ContactsFragment → DetailFragment (addToBackStack = true)
- Al borrar: popBackStack() para volver a lista
- Al guardar edición: popBackStack() para volver a detalle

### Comportamiento en tablet
- Dos contenedores: `fragmentContainer` (izquierda) + `rightPaneContainer` (derecha)
- ContactsFragment siempre visible en panel izquierdo
- Detalles se muestran en panel derecho (sin addToBackStack)
- Al borrar: no hace popBackStack, el detalle simplemente desaparece
- Al guardar edición: se actualiza el panel derecho con el nuevo detalle

---

## Archivos de Recursos

### Layouts
- `activity_main.xml`: CoordinatorLayout con AppBarLayout (Toolbar) + include content_main
- `content_main.xml`: FrameLayout `fragmentContainer` para teléfono (layout base)
- `fragment_contacts.xml`: RecyclerView + FAB de agregar
- `fragment_add_edit.xml`: ScrollView con LinearLayout de TextInputLayouts + FAB de guardar
- `fragment_detail.xml`: ScrollView con GridLayout para mostrar datos en pares label-valor

### Menú
- `fragment_details_menu.xml`: 3 items (Back, Edit, Delete) siempre visibles como acciones

### Strings (nuevas agregadas en mantenimiento)
- `error_name_required`: "El nombre es obligatorio"
- `error_email_invalid`: "El correo no tiene un formato válido"
- `error_phone_required`: "El teléfono es obligatorio"
- `error_phone_10_digits`: "El teléfono debe tener exactamente 10 dígitos"
- `error_zip_required`: "El ZIP es obligatorio"
- `error_zip_5_digits`: "El ZIP debe tener exactamente 5 dígitos"
- `form_has_errors`: "Revisa los campos marcados"
- `snackbar_contact_deleted`: "Contacto eliminado"
- `snackbar_undo`: "DESHACER"

---

## AndroidManifest.xml

```xml
<application>
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    
    <provider
        android:name=".data.AddressBookContentProvider"
        android:authorities="fisei.uta.edu.ec.addressblockapp.data"
        android:exported="false" />
</application>
```

**Puntos importantes:**
- Solo una Activity: MainActivity
- ContentProvider no exportado (acceso solo interno a la app)
- Autoridad del provider coincide con `DatabaseDescription.AUTHORITY`

---

## Cambios Aplicados en Mantenimiento

### 1. Validaciones en formulario (AddEditFragment)
- Se agregó método `validateForm()` con validaciones para todos los campos
- Se modificó `saveContact()` para llamar validación antes de persistir
- Se agregó Snackbar de error si la validación falla
- Se agregaron strings de error en strings.xml

### 2. Eliminación con Snackbar obligatorio + DESHACER (DetailFragment)
- Se modificó `confirmDelete()` para capturar valores antes de borrar
- Se agregó Snackbar "Contacto eliminado" con acción "DESHACER"
- Se captura Context seguro desde Activity para evitar crash al presionar DESHACER
- Se agregó `lastLoadedContactValues` para almacenar datos del contacto eliminado
- Se agregaron strings para Snackbar en strings.xml

### 3. Botón de guardar siempre habilitado (AddEditFragment)
- Se modificó `updateSaveButtonFAB()` para mantener FAB siempre visible y habilitado
- Se eliminó lógica de hide/show basada en el campo nombre
- La validación se sigue ejecutando al presionar guardar

---

## Invariantes Importantes

**NO cambiar sin comprender impacto:**

1. **AUTHORITY y URIs:** Deben mantenerse iguales en `DatabaseDescription`, `AndroidManifest.xml` y cualquier consulta a ContentProvider
2. **CONTACT_URI key:** `MainActivity.CONTACT_URI` es la key del Bundle para pasar URIs entre Fragments
3. **notifyChange/setNotificationUri:** Son esenciales para que los Loaders se refresquen automáticamente
4. **isTabletLayout():** Depende de que exista `R.id.rightPaneContainer` en layouts de tablet
5. **Interfaces listener:** MainActivity implementa las 3 interfaces de los Fragments para comunicación

---

## Posibles Mejoras Futuras (sin cambiar arquitectura)

- Agregar campo de búsqueda en ContactsFragment
- Permitir ordenar la lista por otros campos (teléfono, ciudad, etc.)
- Agregar foto de contacto
- Implementar migraciones de base de datos en `onUpgrade()`
- Agregar tests unitarios para validaciones
- Agregar tests de instrumentación para flujo CRUD
- Soportar múltiples idiomas (actualmente mixto inglés/español)

---

## Conclusión

AddressBlockApp sigue una arquitectura clásica y bien definida de Android con Fragments, ContentProvider y SQLite. La separación de responsabilidades es clara:

- **MainActivity:** Coordinador de navegación
- **Fragments:** UI y lógica de presentación
- **ContentProvider:** Acceso a datos y notificación de cambios
- **SQLiteOpenHelper:** Gestión de base de datos
- **CursorLoader:** Carga asíncrona de datos con actualización automática

Los cambios de mantenimiento (validaciones y Snackbar con DESHACER) se integraron sin alterar la arquitectura base, manteniendo la funcionalidad existente y agregando las nuevas capacidades requeridas.
