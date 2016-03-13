package martisep.timesup;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.ResourceCursorAdapter;

public class ClientCursorAdapter extends ResourceCursorAdapter {

    public ClientCursorAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        CheckedTextView name = (CheckedTextView) view.findViewById(R.id.checkedTextView1);
        name.setText(cursor.getString(cursor.getColumnIndex("topic")));
    }
}
