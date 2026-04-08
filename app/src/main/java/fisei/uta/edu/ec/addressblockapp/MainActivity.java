package fisei.uta.edu.ec.addressblockapp;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity implements
        ContactsFragment.ContactsFragmentListener,
        DetailFragment.DetailFragmentListener,
        AddEditFragment.AddEditFragmentListener {

    public static final String CONTACT_URI = "CONTACT_URI";

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

    private boolean isTabletLayout() {
        return findViewById(R.id.rightPaneContainer) != null;
    }

    // Muestra un fragmento, reemplazando el contenido actual.
    private void displayFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                .replace(isTabletLayout() ? R.id.rightPaneContainer : R.id.fragmentContainer, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    // Muestra el fragmento de detalles.
    private void displayDetailFragment(Uri contactUri) {
        DetailFragment detailFragment = new DetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(CONTACT_URI, contactUri);
        detailFragment.setArguments(args);

        // En tablets, el detalle no se añade al back stack porque siempre está visible.
        displayFragment(detailFragment, !isTabletLayout());
    }

    // Muestra el fragmento para añadir/editar un contacto.
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

    @Override
    public void onContactSelected(Uri contactUri) {
        // Cuando un contacto es seleccionado, muestra sus detalles.
        displayDetailFragment(contactUri);
    }

    @Override
    public void onAddContact() {
        // Cuando se pulsa el botón de añadir, muestra el fragmento de edición en modo "nuevo".
        displayAddEditFragment(null);
    }

    @Override
    public void onContactDeleted() {
        // En un teléfono, al borrar, vuelve a la lista de contactos.
        // En una tablet, la lista de contactos ya es visible, no se necesita acción extra.
        if (!isTabletLayout()) {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void onEditContact(Uri contactUri) {
        // Muestra el fragmento de edición con los datos del contacto a editar.
        displayAddEditFragment(contactUri);
    }

    @Override
    public void onBackFromDetails() {
        // Al pulsar retroceder en detalles, vuelve a la lista (en teléfonos).
        if (!isTabletLayout()) {
            getSupportFragmentManager().popBackStack();
        }
    }

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
