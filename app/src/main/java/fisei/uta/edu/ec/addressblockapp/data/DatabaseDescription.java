package fisei.uta.edu.ec.addressblockapp.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Define el contrato de la base de datos de la aplicación.
 * 
 * <p>Esta clase contiene constantes para:
 * <ul>
 *   <li>La authority del ContentProvider</li>
 *   <li>La URI base de los datos</li>
 *   <li>Nombre de tabla y columnas de la tabla contacts</li>
 *   <li>Métodos auxiliares para construir URIs</li>
 * </ul>
 * 
 * <p>El constructor es privado para evitar instanciación, ya que esta clase
 * solo contiene constantes y métodos estáticos.</p>
 */
public final class DatabaseDescription {
    /** Authority del ContentProvider. Debe coincidir con AndroidManifest.xml */
    public static final String AUTHORITY = "fisei.uta.edu.ec.addressblockapp.data";
    
    /** URI base de los datos de la aplicación */
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /** Constructor privado para evitar instanciación */
    private DatabaseDescription() {}

    /**
     * Contrato para la tabla de contactos.
     * 
     * <p>Define el nombre de la tabla, las columnas, la URI para acceder
     * a los contactos, y métodos auxiliares para construir URIs específicas.</p>
     */
    public static final class Contact implements BaseColumns {
        /** Nombre de la tabla de contactos */
        public static final String TABLE_NAME = "contacts";
        
        /** URI para acceder a todos los contactos (content://AUTHORITY/contacts) */
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(TABLE_NAME)
                .build();

        /** Columna: nombre del contacto */
        public static final String COLUMN_NAME = "name";
        
        /** Columna: número de teléfono */
        public static final String COLUMN_PHONE = "phone";
        
        /** Columna: dirección de email */
        public static final String COLUMN_EMAIL = "email";
        
        /** Columna: dirección (calle) */
        public static final String COLUMN_STREET = "street";
        
        /** Columna: ciudad */
        public static final String COLUMN_CITY = "city";
        
        /** Columna: estado/provincia */
        public static final String COLUMN_STATE = "state";
        
        /** Columna: código postal ZIP */
        public static final String COLUMN_ZIP = "zip";

        /**
         * Construye una URI para un contacto específico dado su ID.
         * 
         * @param id El ID del contacto (debe ser el _ID de la tabla)
         * @return URI del contacto específico (content://AUTHORITY/contacts/id)
         */
        public static Uri buildContactUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
}
