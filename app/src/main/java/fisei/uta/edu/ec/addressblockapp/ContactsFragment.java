package fisei.uta.edu.ec.addressblockapp;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fisei.uta.edu.ec.addressblockapp.data.DatabaseDescription.Contact;

/**
 * Fragment que muestra la lista de contactos de la aplicación.
 * 
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Mostrar la lista de contactos en un RecyclerView</li>
 *   <li>Permitir seleccionar un contacto para ver sus detalles</li>
 *   <li>Permitir agregar un nuevo contacto vía FAB</li>
 *   <li>Cargar datos automáticamente usando CursorLoader</li>
 *   <li>Notificar a MainActivity cuando se selecciona un contacto o se agrega uno nuevo</li>
 * </ul>
 * 
 * <p>Este Fragment implementa LoaderCallbacks para cargar datos de forma asíncrona
 * y recibir actualizaciones automáticas cuando los datos cambian (gracias a notifyChange
 * del ContentProvider).</p>
 */
public class ContactsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Interface que la Activity (MainActivity) debe implementar para recibir eventos
     * de este Fragment.
     */
    public interface ContactsFragmentListener {
        /**
         * Called when a contact is selected in the list.
         * @param contactUri URI of the selected contact
         */
        void onContactSelected(Uri contactUri);
        
        /**
         * Called when the add contact FAB is pressed.
         */
        void onAddContact();
    }

    /** Listener para comunicarse con MainActivity */
    private ContactsFragmentListener listener;
    
    /** Adaptador para el RecyclerView que muestra la lista de contactos */
    private ContactsAdapter contactsAdapter;
    
    /** ID del Loader para cargar la lista de contactos */
    private static final int CONTACTS_LOADER = 0;

    /**
     * Called when the fragment is attached to the Activity.
     * Obtiene una referencia al listener (MainActivity).
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (ContactsFragmentListener) context;
    }

    /**
     * Called when the fragment is detached from the Activity.
     * Libera la referencia al listener para evitar memory leaks.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * Infla y configura la vista del Fragment.
     * 
     * <p>Configura el RecyclerView con LinearLayoutManager, el adaptador,
     * una decoración para separar items, y el FAB para agregar contactos.</p>
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        contactsAdapter = new ContactsAdapter(contactUri -> {
            if (listener != null) listener.onContactSelected(contactUri);
        });
        recyclerView.setAdapter(contactsAdapter);
        recyclerView.addItemDecoration(new ItemDivider(requireContext()));
        recyclerView.setHasFixedSize(true);

        FloatingActionButton addButton = view.findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> { if (listener != null) listener.onAddContact(); });
        return view;
    }

    /**
     * Called when the fragment's activity has been created.
     * Inicializa el LoaderManager para cargar los datos de contactos.
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LoaderManager.getInstance(this).initLoader(CONTACTS_LOADER, null, this);
    }

    /**
     * Fuerza una actualización visual de la lista de contactos.
     * Este método no se usa actualmente porque el Loader maneja las actualizaciones automáticamente.
     */
    public void updateContactList() {
        contactsAdapter.notifyDataSetChanged();
    }

    /**
     * Crea un CursorLoader para cargar los datos.
     * 
     * <p>Carga todos los contactos ordenados por nombre en orden ascendente,
     * sin distinción de mayúsculas/minúsculas (COLLATE NOCASE).</p>
     * 
     * @param id ID del loader (debe ser CONTACTS_LOADER)
     * @param args Argumentos opcionales (no usados)
     * @return CursorLoader configurado para cargar la lista de contactos
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (id == CONTACTS_LOADER) {
            return new CursorLoader(requireContext(),
                    Contact.CONTENT_URI,
                    null,
                    null,
                    null,
                    Contact.COLUMN_NAME + " COLLATE NOCASE ASC");
        }
        throw new IllegalArgumentException("Unknown loader id: " + id);
    }

    /**
     * Called when the loader has finished loading its data.
     * Actualiza el adaptador con el nuevo cursor.
     * 
     * @param loader El loader que terminó
     * @param data Cursor con los datos cargados
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        contactsAdapter.swapCursor(data);
    }

    /**
     * Called when a previously created loader is being reset.
     * Limpia el cursor del adaptador para evitar referencias a datos obsoletos.
     * 
     * @param loader El loader que está siendo reset
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        contactsAdapter.swapCursor(null);
    }
}
