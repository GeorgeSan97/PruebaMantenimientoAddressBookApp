package fisei.uta.edu.ec.addressblockapp;

import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import fisei.uta.edu.ec.addressblockapp.data.DatabaseDescription;

/**
 * Adaptador para RecyclerView que muestra la lista de contactos.
 * 
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Mantener un Cursor con los datos de contactos</li>
 *   <li>Crear ViewHolders para cada item de la lista</li>
 *   <li>Vincular datos del cursor a las vistas en cada ViewHolder</li>
 *   <li>Notificar cuando un contacto es seleccionado</li>
 * </ul>
 * 
 * <p>Este adaptador usa un Cursor como fuente de datos, lo que permite
 * actualizaciones eficientes cuando los datos cambian (via swapCursor).</p>
 */
public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    /**
     * Interface para manejar clicks en items de contacto.
     */
    public interface ContactClickListener {
        /**
         * Called when a contact item is clicked.
         * @param contactUri URI of the clicked contact
         */
        void onClick(Uri contactUri);
    }

    /**
     * ViewHolder para un item de contacto en el RecyclerView.
     * 
     * <p>Mantiene la referencia al TextView que muestra el nombre del contacto
     * y el ID de la fila del cursor para construir la URI al hacer click.</p>
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        /** TextView que muestra el nombre del contacto */
        public final TextView textView;
        
        /** ID de la fila del cursor correspondiente a este ViewHolder */
        private long rowID;
        
        /**
         * Constructor del ViewHolder.
         * @param itemView La vista del item
         */
        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onClick(DatabaseDescription.Contact.buildContactUri(rowID));
                }
            });
        }
        
        /**
         * Establece el ID de la fila para este ViewHolder.
         * @param id ID de la fila del cursor
         */
        public void setRowID(long id) { this.rowID = id; }
    }

    /** Cursor con los datos de contactos */
    private Cursor cursor = null;
    
    /** Listener para notificar clicks en items */
    private final ContactClickListener clickListener;

    /**
     * Constructor del adaptador.
     * 
     * @param clickListener Listener para manejar clicks en items
     */
    public ContactsAdapter(ContactClickListener clickListener) {
        this.clickListener = clickListener;
    }

    /**
     * Crea un nuevo ViewHolder cuando el RecyclerView necesita uno.
     * 
     * @param parent El ViewGroup padre
     * @param viewType El tipo de vista (no usado en este caso)
     * @return Un nuevo ViewHolder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Vincula los datos del cursor a un ViewHolder específico.
     * 
     * <p>Mueve el cursor a la posición especificada, obtiene el ID y nombre
     * del contacto, y actualiza el ViewHolder.</p>
     * 
     * @param holder El ViewHolder a actualizar
     * @param position La posición en el cursor
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (cursor == null) return;
        cursor.moveToPosition(position);
        int idIndex = cursor.getColumnIndex(DatabaseDescription.Contact._ID);
        int nameIndex = cursor.getColumnIndex(DatabaseDescription.Contact.COLUMN_NAME);
        holder.setRowID(cursor.getLong(idIndex));
        holder.textView.setText(cursor.getString(nameIndex));
    }

    /**
     * Retorna la cantidad de items en el adaptador.
     * 
     * @return Cantidad de filas en el cursor, o 0 si el cursor es null
     */
    @Override
    public int getItemCount() {
        return cursor != null ? cursor.getCount() : 0;
    }

    /**
     * Reemplaza el cursor actual con uno nuevo y notifica los cambios.
     * 
     * <p>Este método es llamado por el LoaderManager cuando los datos cambian.
     * El RecyclerView se actualizará automáticamente para reflejar los nuevos datos.</p>
     * 
     * @param newCursor El nuevo cursor con los datos (puede ser null)
     */
    public void swapCursor(Cursor newCursor) {
        this.cursor = newCursor;
        notifyDataSetChanged();
    }
}
