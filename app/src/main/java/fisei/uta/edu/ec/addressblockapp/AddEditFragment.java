package fisei.uta.edu.ec.addressblockapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import fisei.uta.edu.ec.addressblockapp.data.DatabaseDescription.Contact;

/**
 * Fragment para crear o editar contactos.
 * 
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Formulario con campos para nombre, teléfono, email, calle, ciudad, estado y ZIP</li>
 *   <li>Validación de campos antes de guardar (nombre obligatorio, teléfono 10 dígitos, ZIP 5 dígitos, email opcional con formato)</li>
 *   <li>Operar en modo "nuevo" (insert) o "edición" (update)</li>
 *   <li>Persistencia vía ContentResolver (insert/update)</li>
 *   <li>Feedback al usuario vía Snackbar</li>
 * </ul>
 * 
 * <p>Modos de operación:</p>
 * <ul>
 *   <li>Modo nuevo: {@code addingNewContact = true}, no hay {@code contactUri}</li>
 *   <li>Modo edición: {@code addingNewContact = false}, {@code contactUri} recibida por Bundle</li>
 * </ul>
 * 
 * <p>Validaciones implementadas:</p>
 * <ul>
 *   <li>Nombre: Obligatorio, no puede estar vacío ni contener solo espacios</li>
 *   <li>Email: Opcional, si se ingresa debe tener formato válido (Patterns.EMAIL_ADDRESS)</li>
 *   <li>Teléfono: Obligatorio, puede contener espacios y guiones, normalizado a solo números, exactamente 10 dígitos</li>
 *   <li>ZIP: Obligatorio, solo números, exactamente 5 dígitos (\d{5})</li>
 *   <li>Calle, Ciudad, Estado: Opcionales, cualquier texto</li>
 * </ul>
 * 
 * <p>Detección de cambios no guardados:</p>
 * <ul>
 *   <li>Guarda los valores originales al cargar un contacto en modo edición</li>
 *   <li>Compara valores actuales con originales al intentar salir</li>
 *   <li>Muestra diálogo de advertencia si hay cambios sin guardar</li>
 *   <li>Permite elegir entre salir sin guardar o continuar editando</li>
 * </ul>
 */
public class AddEditFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Interface que la Activity (MainActivity) debe implementar para recibir eventos
     * de este Fragment.
     */
    public interface AddEditFragmentListener {
        /**
         * Called when the add/edit operation is completed.
         * @param contactUri URI of the added or updated contact
         */
        void onAddEditCompleted(Uri contactUri);
    }

    /** ID del Loader para cargar datos del contacto a editar */
    private static final int CONTACT_LOADER = 0;

    /** Listener para comunicarse con MainActivity */
    private AddEditFragmentListener listener;
    
    /** URI del contacto (null en modo nuevo, con valor en modo edición) */
    private Uri contactUri;
    
    /** Indica si estamos creando un contacto nuevo (true) o editando (false) */
    private boolean addingNewContact = true;

    /** Campos del formulario */
    private TextInputLayout nameTextInputLayout;
    private TextInputLayout phoneTextInputLayout;
    private TextInputLayout emailTextInputLayout;
    private TextInputLayout streetTextInputLayout;
    private TextInputLayout cityTextInputLayout;
    private TextInputLayout stateTextInputLayout;
    private TextInputLayout zipTextInputLayout;
    
    /** Botón flotante para guardar */
    private FloatingActionButton saveContactFAB;
    
    /** CoordinatorLayout para mostrar Snackbars */
    private CoordinatorLayout coordinatorLayout;

    /** Valores originales del contacto (para detectar cambios no guardados) */
    private String originalName = "";
    private String originalPhone = "";
    private String originalEmail = "";
    private String originalStreet = "";
    private String originalCity = "";
    private String originalState = "";
    private String originalZip = "";

    /**
     * Called when the fragment is attached to the Activity.
     * Obtiene una referencia al listener (MainActivity).
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (AddEditFragmentListener) context;
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
     * <p>Configura todos los TextInputLayout, el FAB de guardar, y determina
     * el modo de operación (nuevo o edición) basándose en si se recibió una URI
     * en los argumentos.</p>
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        // En modo nuevo, los valores originales están vacíos
        if (addingNewContact) {
            originalName = "";
            originalPhone = "";
            originalEmail = "";
            originalStreet = "";
            originalCity = "";
            originalState = "";
            originalZip = "";
        }
        View view = inflater.inflate(R.layout.fragment_add_edit, container, false);

        nameTextInputLayout = view.findViewById(R.id.nameTextInputLayout);
        phoneTextInputLayout = view.findViewById(R.id.phoneTextInputLayout);
        emailTextInputLayout = view.findViewById(R.id.emailTextInputLayout);
        streetTextInputLayout = view.findViewById(R.id.streetTextInputLayout);
        cityTextInputLayout = view.findViewById(R.id.cityTextInputLayout);
        stateTextInputLayout = view.findViewById(R.id.stateTextInputLayout);
        zipTextInputLayout = view.findViewById(R.id.zipTextInputLayout);

        saveContactFAB = view.findViewById(R.id.saveFloatingActionButton);
        saveContactFAB.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getView() != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
            saveContact();
        });

        coordinatorLayout = requireActivity().findViewById(R.id.coordinatorLayout);

        Bundle args = getArguments();
        if (args != null) {
            addingNewContact = false;
            contactUri = args.getParcelable(MainActivity.CONTACT_URI);
        }

        if (contactUri != null) {
            LoaderManager.getInstance(this).initLoader(CONTACT_LOADER, null, this);
        }

        updateSaveButtonFAB();
        nameTextInputLayout.getEditText().addTextChangedListener(new SimpleTextWatcher(() -> updateSaveButtonFAB()));
        return view;
    }

    /**
     * Normaliza un número de teléfono eliminando espacios y guiones.
     * 
     * @param phone El teléfono original (puede contener espacios y guiones)
     * @return El teléfono normalizado (solo dígitos)
     */
    private String normalizePhone(String phone) {
        return phone.replaceAll("[\\s-]", "");
    }

    /**
     * Verifica si hay cambios no guardados comparando los valores actuales
     * con los valores originales.
     * 
     * <p>Si el usuario modifica un campo y luego vuelve exactamente al valor original,
     * no se considera cambio.</p>
     * 
     * @return true si hay cambios sin guardar, false en caso contrario
     */
    private boolean hasUnsavedChanges() {
        String currentName = nameTextInputLayout.getEditText() != null
                ? nameTextInputLayout.getEditText().getText().toString() : "";
        String currentPhone = phoneTextInputLayout.getEditText() != null
                ? phoneTextInputLayout.getEditText().getText().toString() : "";
        String currentEmail = emailTextInputLayout.getEditText() != null
                ? emailTextInputLayout.getEditText().getText().toString() : "";
        String currentStreet = streetTextInputLayout.getEditText() != null
                ? streetTextInputLayout.getEditText().getText().toString() : "";
        String currentCity = cityTextInputLayout.getEditText() != null
                ? cityTextInputLayout.getEditText().getText().toString() : "";
        String currentState = stateTextInputLayout.getEditText() != null
                ? stateTextInputLayout.getEditText().getText().toString() : "";
        String currentZip = zipTextInputLayout.getEditText() != null
                ? zipTextInputLayout.getEditText().getText().toString() : "";

        return !currentName.equals(originalName) ||
               !currentPhone.equals(originalPhone) ||
               !currentEmail.equals(originalEmail) ||
               !currentStreet.equals(originalStreet) ||
               !currentCity.equals(originalCity) ||
               !currentState.equals(originalState) ||
               !currentZip.equals(originalZip);
    }

    /**
     * Mantiene el botón de guardar siempre visible y habilitado.
     * 
     * <p>La validación se ejecuta al presionar guardar, no al escribir.
     * Esto permite que el usuario siempre pueda intentar guardar y ver
     * los mensajes de error si hay campos inválidos.</p>
     */
    private void updateSaveButtonFAB() {
        saveContactFAB.show();
        saveContactFAB.setEnabled(true);
        saveContactFAB.setAlpha(1f);
    }

    /**
     * Valida todos los campos del formulario antes de guardar.
     * 
     * <p>Reglas de validación:</p>
     * <ul>
     *   <li>Nombre: Obligatorio, no vacío, no solo espacios</li>
     *   <li>Email: Opcional, si se ingresa debe tener formato válido</li>
     *   <li>Teléfono: Obligatorio, puede contener espacios y guiones, normalizado a solo números, exactamente 10 dígitos</li>
     *   <li>ZIP: Obligatorio, solo números, exactamente 5 dígitos</li>
     * </ul>
     * 
     * @return true si todos los campos son válidos, false en caso contrario
     */
    private boolean validateForm() {
        boolean valid = true;

        String name = nameTextInputLayout.getEditText() != null
                ? nameTextInputLayout.getEditText().getText().toString()
                : "";
        if (name.trim().isEmpty()) {
            nameTextInputLayout.setError(getString(R.string.error_name_required));
            valid = false;
        } else {
            nameTextInputLayout.setError(null);
        }

        String email = emailTextInputLayout.getEditText() != null
                ? emailTextInputLayout.getEditText().getText().toString()
                : "";
        if (!email.trim().isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailTextInputLayout.setError(getString(R.string.error_email_invalid));
            valid = false;
        } else {
            emailTextInputLayout.setError(null);
        }

        String phone = phoneTextInputLayout.getEditText() != null
                ? phoneTextInputLayout.getEditText().getText().toString()
                : "";
        String phoneNormalized = normalizePhone(phone);
        if (phoneNormalized.isEmpty()) {
            phoneTextInputLayout.setError(getString(R.string.error_phone_required));
            valid = false;
        } else if (!phoneNormalized.matches("\\d{10}")) {
            phoneTextInputLayout.setError(getString(R.string.error_phone_10_digits));
            valid = false;
        } else {
            phoneTextInputLayout.setError(null);
        }

        String zip = zipTextInputLayout.getEditText() != null
                ? zipTextInputLayout.getEditText().getText().toString()
                : "";
        String zipTrim = zip.trim();
        if (zipTrim.isEmpty()) {
            zipTextInputLayout.setError(getString(R.string.error_zip_required));
            valid = false;
        } else if (!zipTrim.matches("\\d{5}")) {
            zipTextInputLayout.setError(getString(R.string.error_zip_5_digits));
            valid = false;
        } else {
            zipTextInputLayout.setError(null);
        }

        return valid;
    }

    /**
     * Guarda el contacto después de validar el formulario.
     * 
     * <p>Flujo:</p>
     * <ol>
     *   <li>Ejecuta validación del formulario</li>
     *   <li>Si hay errores: muestra Snackbar y aborta</li>
     *   <li>Si válido: construye ContentValues con los datos</li>
     *   <li>Si es nuevo: llama ContentResolver.insert()</li>
     *   <li>Si es edición: llama ContentResolver.update()</li>
     *   <li>Muestra Snackbar de éxito/error</li>
     *   <li>Notifica al listener para cerrar el formulario</li>
     * </ol>
     */
    private void saveContact() {
        if (!validateForm()) {
            Snackbar.make(coordinatorLayout, R.string.form_has_errors, Snackbar.LENGTH_LONG).show();
            return;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(Contact.COLUMN_NAME, nameTextInputLayout.getEditText().getText().toString());
        // Guardar teléfono normalizado (solo dígitos)
        String phoneValue = phoneTextInputLayout.getEditText() != null
                ? phoneTextInputLayout.getEditText().getText().toString()
                : "";
        contentValues.put(Contact.COLUMN_PHONE, normalizePhone(phoneValue));
        contentValues.put(Contact.COLUMN_EMAIL, emailTextInputLayout.getEditText().getText().toString());
        contentValues.put(Contact.COLUMN_STREET, streetTextInputLayout.getEditText().getText().toString());
        contentValues.put(Contact.COLUMN_CITY, cityTextInputLayout.getEditText().getText().toString());
        contentValues.put(Contact.COLUMN_STATE, stateTextInputLayout.getEditText().getText().toString());
        contentValues.put(Contact.COLUMN_ZIP, zipTextInputLayout.getEditText().getText().toString());

        if (addingNewContact) {
            Uri newContactUri = requireActivity().getContentResolver().insert(Contact.CONTENT_URI, contentValues);
            if (newContactUri != null) {
                Snackbar.make(coordinatorLayout, R.string.contact_added, Snackbar.LENGTH_LONG).show();
                if (listener != null) listener.onAddEditCompleted(newContactUri);
            } else {
                Snackbar.make(coordinatorLayout, R.string.contact_not_added, Snackbar.LENGTH_LONG).show();
            }
        } else {
            // Actualizar valores originales después de guardar exitosamente
            originalName = nameTextInputLayout.getEditText().getText().toString();
            originalPhone = phoneTextInputLayout.getEditText().getText().toString();
            originalEmail = emailTextInputLayout.getEditText().getText().toString();
            originalStreet = streetTextInputLayout.getEditText().getText().toString();
            originalCity = cityTextInputLayout.getEditText().getText().toString();
            originalState = stateTextInputLayout.getEditText().getText().toString();
            originalZip = zipTextInputLayout.getEditText().getText().toString();
            int updated = requireActivity().getContentResolver().update(contactUri, contentValues, null, null);
            if (updated > 0) {
                if (listener != null) listener.onAddEditCompleted(contactUri);
                Snackbar.make(coordinatorLayout, R.string.contact_updated, Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(coordinatorLayout, R.string.contact_not_updated, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Infla el menú de opciones del Fragment.
     * El menú contiene el botón Back para salir del formulario.
     */
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_add_edit_menu, menu);
    }

    /**
     * Maneja las selecciones del menú de opciones.
     * 
     * @param item El item seleccionado
     * @return true si se manejó la acción, false en caso contrario
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_back) {
            handleBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Maneja la acción de presionar atrás.
     * Verifica si hay cambios no guardados y muestra diálogo de confirmación.
     */
    private void handleBackPressed() {
        if (hasUnsavedChanges()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.unsaved_changes_title)
                    .setMessage(R.string.unsaved_changes_message)
                    .setPositiveButton(R.string.button_discard, (dialog, which) -> {
                        // Salir sin guardar
                        if (listener != null) listener.onAddEditCompleted(null);
                    })
                    .setNegativeButton(R.string.button_continue_editing, null)
                    .show();
        } else {
            // No hay cambios, salir directamente
            if (listener != null) listener.onAddEditCompleted(null);
        }
    }

    /**
     * Crea un CursorLoader para cargar los datos del contacto a editar.
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
     * Llena todos los campos del formulario con los datos del contacto a editar.
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

            // Guardar valores originales
            originalName = data.getString(nameIndex);
            originalPhone = data.getString(phoneIndex);
            originalEmail = data.getString(emailIndex);
            originalStreet = data.getString(streetIndex);
            originalCity = data.getString(cityIndex);
            originalState = data.getString(stateIndex);
            originalZip = data.getString(zipIndex);

            // Llenar campos del formulario
            nameTextInputLayout.getEditText().setText(originalName);
            phoneTextInputLayout.getEditText().setText(originalPhone);
            emailTextInputLayout.getEditText().setText(originalEmail);
            streetTextInputLayout.getEditText().setText(originalStreet);
            cityTextInputLayout.getEditText().setText(originalCity);
            stateTextInputLayout.getEditText().setText(originalState);
            zipTextInputLayout.getEditText().setText(originalZip);
            updateSaveButtonFAB();
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

    /**
     * TextWatcher simplificado que ejecuta un Runnable cuando el texto cambia.
     * Evita el boilerplate de implementar los tres métodos de TextWatcher.
     */
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable onChanged;
        SimpleTextWatcher(Runnable r) { this.onChanged = r; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChanged.run(); }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }
}
