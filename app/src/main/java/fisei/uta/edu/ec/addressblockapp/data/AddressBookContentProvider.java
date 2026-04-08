package fisei.uta.edu.ec.addressblockapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static fisei.uta.edu.ec.addressblockapp.data.DatabaseDescription.AUTHORITY;
import static fisei.uta.edu.ec.addressblockapp.data.DatabaseDescription.Contact;

/**
 * ContentProvider que provee acceso a los datos de contactos de la aplicación.
 * 
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Implementar operaciones CRUD (query, insert, update, delete) sobre contactos</li>
 *   <li>Intermediar entre ContentResolver y SQLite Database</li>
 *   <li>Notificar cambios para que los Loaders se refresquen automáticamente</li>
 *   <li>Validar URIs usando UriMatcher</li>
 * </ul>
 * 
 * <p>URIs soportadas:</p>
 * <ul>
 *   <li>content://fisei.uta.edu.ec.addressblockapp.data/contacts → CONTACTS (todos los contactos)</li>
 *   <li>content://fisei.uta.edu.ec.addressblockapp.data/contacts/# → CONTACT_ID (contacto específico)</li>
 * </ul>
 * 
 * <p>La notificación de cambios (notifyChange) es crucial: cuando se inserta/update/delete,
 * los CursorLoaders que observan esas URIs reciben automáticamente los nuevos datos.</p>
 */
public class AddressBookContentProvider extends ContentProvider {

    /** Código para URI de lista de contactos (todos) */
    private static final int CONTACTS = 1;
    
    /** Código para URI de contacto específico (con ID) */
    private static final int CONTACT_ID = 2;

    /** UriMatcher para validar y distinguir las URIs recibidas */
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, Contact.TABLE_NAME, CONTACTS);
        uriMatcher.addURI(AUTHORITY, Contact.TABLE_NAME + "/#", CONTACT_ID);
    }

    /** Helper para acceder a la base de datos SQLite */
    private AddressBookDatabaseHelper dbHelper;

    /**
     * Inicializa el ContentProvider.
     * 
     * <p>Crea una instancia de AddressBookDatabaseHelper para acceder a la base de datos.</p>
     * 
     * @return true si la inicialización fue exitosa
     */
    @Override
    public boolean onCreate() {
        dbHelper = new AddressBookDatabaseHelper(getContext());
        return true;
    }

    /**
     * Retorna el tipo MIME de los datos para una URI dada.
     * 
     * @param uri La URI a consultar
     * @return El tipo MIME (dir para lista, item para elemento individual)
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + Contact.TABLE_NAME;
            case CONTACT_ID:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + Contact.TABLE_NAME;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    /**
     * Consulta datos de contactos.
     * 
     * <p>Si la URI es CONTACTS, retorna todos los contactos.
     * Si la URI es CONTACT_ID, retorna solo el contacto específico.</p>
     * 
     * <p>Establece setNotificationUri para que el Loader observe cambios en esta URI.</p>
     * 
     * @param uri La URI a consultar (CONTACTS o CONTACT_ID)
     * @param projection Columnas a retornar (null = todas)
     * @param selection Cláusula WHERE (opcional)
     * @param selectionArgs Argumentos para WHERE (opcional)
     * @param sortOrder Orden de los resultados (opcional)
     * @return Cursor con los resultados
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        switch (uriMatcher.match(uri)) {
            case CONTACTS:
                cursor = db.query(Contact.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case CONTACT_ID:
                String idSelection = Contact._ID + "=?";
                String[] idArgs = new String[]{ String.valueOf(ContentUris.parseId(uri)) };
                cursor = db.query(Contact.TABLE_NAME, projection, idSelection, idArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Invalid query Uri: " + uri);
        }
        if (getContext() != null) cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * Inserta un nuevo contacto.
     * 
     * <p>Solo acepta la URI CONTACTS. Inserta en la base de datos,
     * construye la nueva URI del contacto insertado, y notifica el cambio.</p>
     * 
     * @param uri Debe ser CONTACTS (lista de contactos)
     * @param values Valores del nuevo contacto
     * @return URI del nuevo contacto insertado
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != CONTACTS) throw new IllegalArgumentException("Invalid insert Uri: " + uri);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(Contact.TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri newUri = Contact.buildContactUri(rowId);
            notifyChange(newUri);
            return newUri;
        }
        throw new SQLException("Insert failed: " + uri);
    }

    /**
     * Actualiza un contacto existente.
     * 
     * <p>Solo acepta la URI CONTACT_ID (contacto específico).
     * Actualiza en la base de datos y notifica el cambio si se actualizó al menos una fila.</p>
     * 
     * @param uri Debe ser CONTACT_ID (contacto específico)
     * @param values Valores a actualizar
     * @param selection No usado (el ID viene de la URI)
     * @param selectionArgs No usado (el ID viene de la URI)
     * @return Cantidad de filas actualizadas
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
            case CONTACT_ID:
                String idWhere = Contact._ID + "=?";
                String[] args = new String[]{ String.valueOf(ContentUris.parseId(uri)) };
                count = db.update(Contact.TABLE_NAME, values, idWhere, args);
                break;
            default:
                throw new IllegalArgumentException("Invalid update Uri: " + uri);
        }
        if (count > 0) notifyChange(uri);
        return count;
    }

    /**
     * Elimina un contacto.
     * 
     * <p>Solo acepta la URI CONTACT_ID (contacto específico).
     * Elimina de la base de datos y notifica el cambio si se eliminó al menos una fila.</p>
     * 
     * @param uri Debe ser CONTACT_ID (contacto específico)
     * @param selection No usado (el ID viene de la URI)
     * @param selectionArgs No usado (el ID viene de la URI)
     * @return Cantidad de filas eliminadas
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
            case CONTACT_ID:
                String idWhere = Contact._ID + "=?";
                String[] args = new String[]{ String.valueOf(ContentUris.parseId(uri)) };
                count = db.delete(Contact.TABLE_NAME, idWhere, args);
                break;
            default:
                throw new IllegalArgumentException("Invalid delete Uri: " + uri);
        }
        if (count > 0) notifyChange(uri);
        return count;
    }

    /**
     * Notifica a los observadores que los datos en la URI han cambiado.
     * 
     * <p>Esto hace que los CursorLoaders que observan esta URI se recarguen automáticamente,
     * actualizando la UI sin necesidad de código adicional.</p>
     * 
     * @param uri La URI que cambió
     */
    private void notifyChange(Uri uri) {
        if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
    }
}
