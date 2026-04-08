package fisei.uta.edu.ec.addressblockapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static fisei.uta.edu.ec.addressblockapp.data.DatabaseDescription.Contact;

/**
 * Helper para gestionar la base de datos SQLite de la aplicación.
 * 
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Crear la base de datos si no existe</li>
 *   <li>Crear la tabla de contactos en onCreate()</li>
 *   <li>Manejar migraciones de base de datos en onUpgrade() (actualmente no-op para versión 1)</li>
 * </ul>
 * 
 * <p>Configuración:</p>
 * <ul>
 *   <li>Nombre de base de datos: AddressBook.db</li>
 *   <li>Versión actual: 1</li>
 *   <li>Tabla: contacts con columnas _ID, name, phone, email, street, city, state, zip</li>
 * </ul>
 */
class AddressBookDatabaseHelper extends SQLiteOpenHelper {
    /** Nombre del archivo de base de datos */
    private static final String DATABASE_NAME = "AddressBook.db";
    
    /** Versión actual de la base de datos */
    private static final int DATABASE_VERSION = 1;

    /**
     * Constructor del helper.
     * 
     * @param context Contexto de la aplicación
     */
    AddressBookDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Crea la tabla de contactos cuando la base de datos se crea por primera vez.
     * 
     * <p>La tabla contacts tiene la siguiente estructura:</p>
     * <pre>
     * CREATE TABLE contacts (
     *   _ID INTEGER PRIMARY KEY,
     *   name TEXT,
     *   phone TEXT,
     *   email TEXT,
     *   street TEXT,
     *   city TEXT,
     *   state TEXT,
     *   zip TEXT
     * );
     * </pre>
     * 
     * @param db La base de datos donde crear la tabla
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        final String CREATE_CONTACTS_TABLE =
                "CREATE TABLE " + Contact.TABLE_NAME + " (" +
                        Contact._ID + " INTEGER PRIMARY KEY, " +
                        Contact.COLUMN_NAME + " TEXT, " +
                        Contact.COLUMN_PHONE + " TEXT, " +
                        Contact.COLUMN_EMAIL + " TEXT, " +
                        Contact.COLUMN_STREET + " TEXT, " +
                        Contact.COLUMN_CITY + " TEXT, " +
                        Contact.COLUMN_STATE + " TEXT, " +
                        Contact.COLUMN_ZIP + " TEXT);";
        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    /**
     * Maneja migraciones de base de datos cuando la versión aumenta.
     * 
     * <p>Actualmente es no-op (sin operación) porque estamos en versión 1.
     * En versiones futuras, aquí se implementaría la lógica para migrar datos
     * cuando se cambie la estructura de la tabla.</p>
     * 
     * @param db La base de datos
     * @param oldVersion Versión anterior de la base de datos
     * @param newVersion Nueva versión de la base de datos
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // no-op for version 1
    }
}
