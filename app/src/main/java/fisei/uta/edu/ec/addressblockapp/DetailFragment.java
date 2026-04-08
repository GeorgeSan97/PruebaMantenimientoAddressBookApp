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

public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface DetailFragmentListener {
        void onContactDeleted();
        void onEditContact(Uri contactUri);
        void onBackFromDetails();
    }

    private static final int CONTACT_LOADER = 0;

    private DetailFragmentListener listener;
    private Uri contactUri;

    private CoordinatorLayout coordinatorLayout;
    private ContentValues lastLoadedContactValues;

    private TextView nameTextView;
    private TextView phoneTextView;
    private TextView emailTextView;
    private TextView streetTextView;
    private TextView cityTextView;
    private TextView stateTextView;
    private TextView zipTextView;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (DetailFragmentListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_details_menu, menu);
    }

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

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader(requireContext(), contactUri, null, null, null, null);
    }

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

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) { }
}
