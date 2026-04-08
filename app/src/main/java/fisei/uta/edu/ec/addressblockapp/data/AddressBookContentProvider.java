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

public class AddressBookContentProvider extends ContentProvider {

    private static final int CONTACTS = 1;
    private static final int CONTACT_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, Contact.TABLE_NAME, CONTACTS);
        uriMatcher.addURI(AUTHORITY, Contact.TABLE_NAME + "/#", CONTACT_ID);
    }

    private AddressBookDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new AddressBookDatabaseHelper(getContext());
        return true;
    }

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

    private void notifyChange(Uri uri) {
        if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
    }
}
