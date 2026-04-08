package fisei.uta.edu.ec.addressblockapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.snackbar.Snackbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import fisei.uta.edu.ec.addressblockapp.data.DatabaseDescription.Contact;

/**
 * Fragment que muestra los detalles de un contacto seleccionado.
 * 
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Mostrar todos los datos del contacto en TextViews</li>
 *   <li>Permitir editar el contacto vía menú</li>
 *   <li>Permitir eliminar el contacto con confirmación y Snackbar con DESHACER</li>
 *   <li>Cargar datos automáticamente usando CursorLoader</li>
 *   <li>Notificar a MainActivity cuando se edita, elimina o vuelve atrás</li>
 * </ul>
 * 
 * <p>Funcionalidad de eliminación con DESHACER:</p>
 * <ul>
 *   <li>Al eliminar, se capturan los datos del contacto en {@code lastLoadedContactValues}</li>
 *   <li>Se muestra Snackbar "Contacto eliminado" con acción "DESHACER"</li>
 *   <li>Si el usuario presiona DESHACER, se reinserta el contacto con los datos capturados</li>
 *   <li>El Context usado es de la Activity para evitar crash si el Fragment es detachado</li>
 * </ul>
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Interface que la Activity (MainActivity) debe implementar para recibir eventos
     * de este Fragment.
     */
    public interface DetailFragmentListener {
        /** Called when a contact is deleted */
        void onContactDeleted();
        
        /** Called when the edit action is triggered
         * @param contactUri URI of the contact to edit
         */
        void onEditContact(Uri contactUri);
        
        /** Called when the back action is triggered */
        void onBackFromDetails();
    }

    /** ID del Loader para cargar datos del contacto */
    private static final int CONTACT_LOADER = 0;

    /** Listener para comunicarse con MainActivity */
    private DetailFragmentListener listener;
    
    /** URI del contacto a mostrar */
    private Uri contactUri;

    /** CoordinatorLayout para mostrar Snackbars */
    private CoordinatorLayout coordinatorLayout;
    
    /** Copia de los valores del contacto cargado, usada para DESHACER eliminación */
    private ContentValues lastLoadedContactValues;

    /** TextViews para mostrar cada campo del contacto */
    private TextView nameTextView;
    private TextView phoneTextView;
    private TextView emailTextView;
    private TextView streetTextView;
    private TextView cityTextView;
    private TextView stateTextView;
    private TextView zipTextView;

    /**
     * Called when the fragment is attached to the Activity.
     * Obtiene una referencia al listener (MainActivity).
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (DetailFragmentListener) context;
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
     * <p>Obtiene la URI del contacto del Bundle, configura todos los TextViews,
     * y carga los datos del contacto usando CursorLoader.</p>
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            contactUri = getArguments().getParcelable(MainActivity.CONTACT_URI);
        }
        coordinatorLayout = requireActivity().findViewById(R.id.coordinatorLayout);
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        nameTextView = view.findViewById(R.id.nameTextView);
        phoneTextView = view.findViewById(R.id.phoneTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        streetTextView = view.findViewById(R.id.streetTextView);
        cityTextView = view.findViewById(R.id.cityTextView);
        stateTextView = view.findViewById(R.id.stateTextView);
        zipTextView = view.findViewById(R.id.zipTextView);
        LoaderManager.getInstance(this).initLoader(CONTACT_LOADER, null, this);
        return view;
    }

    /**
     * Infla el menú de opciones del Fragment.
     * El menú contiene acciones: Back, Edit, Delete.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_details_menu, menu);
    }

    /**
     * Maneja las selecciones del menú de opciones.
     * 
     * @param item El item seleccionado
     * @return true si se manejó la acción, false en caso contrario
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            if (listener != null) listener.onEditContact(contactUri);
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        } else if (id == R.id.action_back) {
            if (listener != null) listener.onBackFromDetails();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Muestra un diálogo de confirmación y elimina el contacto si el usuario confirma.
     * 
     * <p>Flujo de eliminación:</p>
     * <ol>
     *   <li>Captura los valores del contacto en {@code deletedValues}</li>
     *   <li>Captura un Context seguro desde la Activity (evita crash si Fragment es detachado)</li>
     *   <li>Elimina el contacto vía ContentResolver</li>
     *   <li>Muestra Snackbar "Contacto eliminado" con acción "DESHACER"</li>
     *   <li>Si el usuario presiona DESHACER, reinserta el contacto con {@code deletedValues}</li>
     *   <li>Notifica al listener para hacer popBackStack en teléfono</li>
     * </ol>
     * 
     * <p>Nota: El Context se captura desde {@code coordinatorLayout.getContext()} que es de la Activity,
     * no del Fragment. Esto evita el crash {@code IllegalStateException: Fragment not attached to a context}
     * cuando el Snackbar se muestra después de que el Fragment fue removido del back stack.</p>
     */
    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_title)
                .setMessage(R.string.confirm_message)
                .setPositiveButton(R.string.button_delete, (d, w) -> {
                    final ContentValues deletedValues = lastLoadedContactValues;
                    final Context snackbarContext = coordinatorLayout != null
                            ? coordinatorLayout.getContext()
                            : getContext();
                    if (snackbarContext != null) {
                        snackbarContext.getContentResolver().delete(contactUri, null, null);
                    }

                    Snackbar.make(coordinatorLayout, R.string.snackbar_contact_deleted, Snackbar.LENGTH_LONG)
                            .setAction(R.string.snackbar_undo, v -> {
                                if (deletedValues != null && snackbarContext != null) {
                                    snackbarContext.getContentResolver().insert(Contact.CONTENT_URI, deletedValues);
                                }
                            })
                            .show();

                    if (listener != null) listener.onContactDeleted();
                })
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    /**
     * Crea un CursorLoader para cargar los datos del contacto específico.
     * 
     * @param id ID del loader (debe ser CONTACT_LOADER)
     * @param args Argumentos opcionales (no usados)
     * @return CursorLoader configurado para cargar el contacto específico
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(requireContext(), contactUri, null, null, null, null);
    }

    /**
     * Called when the loader has finished loading the contact data.
     * Muestra los datos en los TextViews y guarda una copia en {@code lastLoadedContactValues}
     * para poder restaurar el contacto si se hace DESHACER.
     * 
     * @param loader El loader que terminó
     * @param data Cursor con los datos del contacto
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            int nameIndex = data.getColumnIndex(Contact.COLUMN_NAME);
            int phoneIndex = data.getColumnIndex(Contact.COLUMN_PHONE);
            int emailIndex = data.getColumnIndex(Contact.COLUMN_EMAIL);
            int streetIndex = data.getColumnIndex(Contact.COLUMN_STREET);
            int cityIndex = data.getColumnIndex(Contact.COLUMN_CITY);
            int stateIndex = data.getColumnIndex(Contact.COLUMN_STATE);
            int zipIndex = data.getColumnIndex(Contact.COLUMN_ZIP);

            nameTextView.setText(data.getString(nameIndex));
            phoneTextView.setText(data.getString(phoneIndex));
            emailTextView.setText(data.getString(emailIndex));
            streetTextView.setText(data.getString(streetIndex));
            cityTextView.setText(data.getString(cityIndex));
            stateTextView.setText(data.getString(stateIndex));
            zipTextView.setText(data.getString(zipIndex));

            ContentValues values = new ContentValues();
            values.put(Contact.COLUMN_NAME, data.getString(nameIndex));
            values.put(Contact.COLUMN_PHONE, data.getString(phoneIndex));
            values.put(Contact.COLUMN_EMAIL, data.getString(emailIndex));
            values.put(Contact.COLUMN_STREET, data.getString(streetIndex));
            values.put(Contact.COLUMN_CITY, data.getString(cityIndex));
            values.put(Contact.COLUMN_STATE, data.getString(stateIndex));
            values.put(Contact.COLUMN_ZIP, data.getString(zipIndex));
            lastLoadedContactValues = values;
        }
    }

    /**
     * Called when a previously created loader is being reset.
     * No se requiere acción específica en este caso.
     * 
     * @param loader El loader que está siendo reset
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) { }
}
