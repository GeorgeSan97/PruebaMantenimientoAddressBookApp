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

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    public interface ContactClickListener {
        void onClick(Uri contactUri);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;
        private long rowID;
        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onClick(DatabaseDescription.Contact.buildContactUri(rowID));
                }
            });
        }
        public void setRowID(long id) { this.rowID = id; }
    }

    private Cursor cursor = null;
    private final ContactClickListener clickListener;

    public ContactsAdapter(ContactClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (cursor == null) return;
        cursor.moveToPosition(position);
        int idIndex = cursor.getColumnIndex(DatabaseDescription.Contact._ID);
        int nameIndex = cursor.getColumnIndex(DatabaseDescription.Contact.COLUMN_NAME);
        holder.setRowID(cursor.getLong(idIndex));
        holder.textView.setText(cursor.getString(nameIndex));
    }

    @Override
    public int getItemCount() {
        return cursor != null ? cursor.getCount() : 0;
    }

    public void swapCursor(Cursor newCursor) {
        this.cursor = newCursor;
        notifyDataSetChanged();
    }
}
