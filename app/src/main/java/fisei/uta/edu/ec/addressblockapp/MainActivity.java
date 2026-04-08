package fisei.uta.edu.ec.addressblockapp;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Activity principal de la aplicación AddressBlockApp.
 * 
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Punto de entrada de la aplicación (Activity launcher)</li>
 *   <li>Coordinador de navegación entre Fragments</li>
 *   <li>Implementa 3 interfaces listener para comunicarse con los Fragments</li>
 *   <li>Detecta layout de tablet vs teléfono para comportamiento dual-pane</li>
 * </ul>
 * 
 * <p>La aplicación sigue una arquitectura basada en Fragments donde esta Activity
 * actúa como contenedor y coordinador, mientras que cada Fragment maneja su propia UI.</p>
 */
public class MainActivity extends AppCompatActivity implements
        ContactsFragment.ContactsFragmentListener,
        DetailFragment.DetailFragmentListener,
        AddEditFragment.AddEditFragmentListener {

    /**
     * Key usada en Bundle para pasar URIs de contactos entre Fragments.
     * Esta constante es usada para identificar la URI del contacto cuando se
     * navega entre ContactsFragment, DetailFragment y AddEditFragment.
     */
    public static final String CONTACT_URI = "CONTACT_URI";

    /**
     * Inicializa la Activity.
     * 
     * <p>Configura la Toolbar como ActionBar y carga ContactsFragment al inicio
     * si no hay estado guardado y existe el contenedor de fragmentos.
     * </p>
     * 
     * @param savedInstanceState Estado guardado de la Activity (null si es primer inicio)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar la Toolbar como ActionBar para mostrar los menús de los Fragments
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null && findViewById(R.id.fragmentContainer) != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ContactsFragment())
                    .commit();
        }
    }

    /**
     * Detecta si el dispositivo tiene un layout de tablet.
     * 
     * <p>Un layout de tablet tiene dos paneles: {@code fragmentContainer} a la izquierda
     * y {@code rightPaneContainer} a la derecha. En tablets, los detalles se muestran
     * en el panel derecho sin usar el back stack, mientras que en teléfonos se usan
     * transiciones con back stack.</p>
     * 
     * @return true si el layout es de tablet (existe rightPaneContainer), false si es teléfono
     */
    private boolean isTabletLayout() {
        return findViewById(R.id.rightPaneContainer) != null;
    }

    /**
     * Muestra un fragmento reemplazando el contenido actual.
     * 
     * <p>En tablets, el fragmento se muestra en {@code rightPaneContainer}. En teléfonos,
     * se muestra en {@code fragmentContainer}. Opcionalmente puede añadirse al back stack
     * para permitir navegación hacia atrás.</p>
     * 
     * @param fragment El fragmento a mostrar
     * @param addToBackStack true para añadir al back stack (permitir back), false en caso contrario
     */
    private void displayFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .replace(isTabletLayout() ? R.id.rightPaneContainer : R.id.fragmentContainer, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    /**
     * Muestra el fragmento de detalles de un contacto.
     * 
     * <p>Crea una instancia de DetailFragment, le pasa la URI del contacto vía Bundle,
     * y lo muestra. En tablets no se añade al back stack porque el detalle siempre
     * está visible en el panel derecho. En teléfonos sí se añade para permitir volver
     * a la lista con el botón back.</p>
     * 
     * @param contactUri URI del contacto a mostrar en detalle
     */
    private void displayDetailFragment(Uri contactUri) {
        DetailFragment detailFragment = new DetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(CONTACT_URI, contactUri);
        detailFragment.setArguments(args);

        // En tablets, el detalle no se añade al back stack porque siempre está visible.
        displayFragment(detailFragment, !isTabletLayout());
    }

    /**
     * Muestra el fragmento para crear o editar un contacto.
     * 
     * <p>Si {@code contactUri} es null, el fragmento opera en modo "nuevo contacto".
     * Si {@code contactUri} tiene valor, el fragmento opera en modo "edición" y carga
     * los datos del contacto existente. Siempre se añade al back stack para permitir volver.</p>
     * 
     * @param contactUri URI del contacto a editar (null para crear nuevo)
     */
    private void displayAddEditFragment(Uri contactUri) {
        AddEditFragment addEditFragment = new AddEditFragment();
        if (contactUri != null) {
            Bundle args = new Bundle();
            args.putParcelable(CONTACT_URI, contactUri);
            addEditFragment.setArguments(args);
        }
        displayFragment(addEditFragment, true);
    }

    // --- IMPLEMENTACIÓN DE LOS MÉTODOS DE LAS INTERFACES ---

    /**
     * Callback cuando un contacto es seleccionado en ContactsFragment.
     * 
     * @param contactUri URI del contacto seleccionado
     */
    @Override
    public void onContactSelected(Uri contactUri) {
        // Cuando un contacto es seleccionado, muestra sus detalles.
        displayDetailFragment(contactUri);
    }

    /**
     * Callback cuando se presiona el botón de agregar en ContactsFragment.
     */
    @Override
    public void onAddContact() {
        // Cuando se pulsa el botón de añadir, muestra el fragmento de edición en modo "nuevo".
        displayAddEditFragment(null);
    }

    /**
     * Callback cuando un contacto es eliminado en DetailFragment.
     * 
     * <p>En teléfonos, hace popBackStack para volver a la lista de contactos.
     * En tablets, no hace nada porque la lista ya está visible en el panel izquierdo.</p>
     */
    @Override
    public void onContactDeleted() {
        // En un teléfono, al borrar, vuelve a la lista de contactos.
        // En una tablet, la lista de contactos ya es visible, no se necesita acción extra.
        if (!isTabletLayout()) {
            getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * Callback cuando se presiona el botón de editar en DetailFragment.
     * 
     * @param contactUri URI del contacto a editar
     */
    @Override
    public void onEditContact(Uri contactUri) {
        // Muestra el fragmento de edición con los datos del contacto a editar.
        displayAddEditFragment(contactUri);
    }

    /**
     * Callback cuando se presiona el botón back en DetailFragment.
     * 
     * <p>En teléfonos, hace popBackStack para volver a la lista.
     * En tablets, no hace nada porque el detalle está en un panel separado.</p>
     */
    @Override
    public void onBackFromDetails() {
        // Al pulsar retroceder en detalles, vuelve a la lista (en teléfonos).
        if (!isTabletLayout()) {
            getSupportFragmentManager().popBackStack();
        }
    }

    /**
     * Callback cuando se completa la operación de agregar o editar contacto.
     * 
     * <p>Hace popBackStack para cerrar el formulario. En tablets, adicionalmente
     * muestra el detalle del contacto guardado/actualizado en el panel derecho.</p>
     * 
     * @param contactUri URI del contacto que fue agregado o actualizado
     */
    @Override
    public void onAddEditCompleted(Uri contactUri) {
        // Quita el fragmento de Add/Edit de la pila.
        getSupportFragmentManager().popBackStack();

        // En tablets, después de guardar, actualiza el panel de detalles.
        if (isTabletLayout()) {
            displayDetailFragment(contactUri);
        }
    }
}
