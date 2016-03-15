package martisep.thymesup;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {
    Button btn_import;
    Button btn_start;
    ListView list_view_topics;
    DBController dbController;
    SimpleCursorAdapter adapter;

    public static final int requestCodeImport = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //this.deleteDatabase("TimesUpDB.db");

        // bind list of topics to db
        list_view_topics = (ListView) findViewById(R.id.listViewTopics);
        dbController = new DBController(this);
        SQLiteDatabase db = dbController.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT rowid _id,* FROM " + DBController.TOPICS_TABLE, null);
        String[] from = new String[]{DBController.TOPIC_COLUMN, DBController.COUNT_COLUMN};
        int[] to = new int[]{R.id.checkedTextView1};
        adapter = new SimpleCursorAdapter(this, R.layout.list_item, cursor, from , to ,0);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.checkedTextView1) {
                    ((CheckedTextView) view).setText(
                            cursor.getString(cursor.getColumnIndex(DBController.TOPIC_COLUMN))
                                    + " ("
                                    + Integer.toString(cursor.getInt(cursor.getColumnIndex(DBController.COUNT_COLUMN)))
                                    + ")");
                    return true;
                }
                return false;
            }
        });
        list_view_topics.setAdapter(adapter);
        list_view_topics.setItemsCanFocus(false);
        list_view_topics.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // button listeners
        btn_import = (Button) findViewById(R.id.button_import);
        btn_import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent file_intent = new Intent(Intent.ACTION_GET_CONTENT);
                file_intent.setType("gagt/sdf"); //nonsensical
                try {
                    startActivityForResult(file_intent, requestCodeImport);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getBaseContext(), "No activity can handle picking a file.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        btn_start = (Button) findViewById(R.id.button_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent game_intent = new Intent(getApplicationContext(), GameActivity.class);

                // get team count
                Spinner spinner_teams = (Spinner) findViewById(R.id.team_spinner);
                int team_count = Integer.parseInt((String) spinner_teams.getSelectedItem());
                game_intent.putExtra(GameActivity.TEAM_COUNT, team_count);

                // get selected topics
                ArrayList<String> topics = new ArrayList<>();
                SparseBooleanArray checked = list_view_topics.getCheckedItemPositions();
                for (int i = 0; i < list_view_topics.getAdapter().getCount(); i++) {
                    if (checked.get(i)) {
                        Cursor c = adapter.getCursor();
                        c.moveToPosition(i);
                        topics.add(c.getString(c.getColumnIndex(DBController.TOPIC_COLUMN)));
                    }
                }

                if (topics.isEmpty()) {
                    Toast toast = Toast.makeText(getBaseContext(), "Select at least 1 topic!",
                            Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                } else {
                    game_intent.putExtra(GameActivity.TOPICS, topics);
                    startActivity(game_intent);
                }
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(data == null){
            return;
        }
        switch(requestCode){
            case requestCodeImport:
                String filepath = data.getData().getPath();
                SQLiteDatabase db = dbController.getWritableDatabase();
                String topic = getFileName(data.getData());

                try {
                    if (resultCode == RESULT_OK) {
                        try {
                            // broken special characters with UTF8 (why???)
                            BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), "ISO-8859-2"));
                            ContentValues contentValues = new ContentValues();
                            String line;
                            db.beginTransaction();
                            int count = 0;

                            // insert entries to database
                            while ((line = buffer.readLine()) != null) {
                                String[] str = line.split(";", 2);
                                String name = str[0];
                                String keywords = "";
                                if(str.length > 1){
                                    keywords = str[1];
                                }
                                count++;

                                contentValues.put(DBController.NAME_COLUMN, name);
                                contentValues.put(DBController.KEYWORDS_COLUMN, keywords);
                                contentValues.put(DBController.TOPIC_COLUMN, topic);
                                db.insert(DBController.ENTRIES_TABLE, null, contentValues);
                            }

                            // insert topic into database
                            ContentValues cv = new ContentValues();
                            cv.put(DBController.TOPIC_COLUMN, topic);
                            cv.put(DBController.COUNT_COLUMN, count);
                            db.insert(DBController.TOPICS_TABLE, null, cv);

                            // cursor has to be recreated to reflect the change in db in listView
                            Cursor myCursor = db.rawQuery("SELECT rowid _id,* FROM " + DBController.TOPICS_TABLE, null);
                            adapter.changeCursor(myCursor);

                            db.setTransactionSuccessful();
                            db.endTransaction();
                        } catch (IOException e) {
                            Log.d(null,"ioexception" + e.getMessage());
                            if (db.inTransaction())
                                db.endTransaction();
                        }
                    } else {
                        Log.d(null,"result not ok");
                        if (db.inTransaction())
                            db.endTransaction();
                    }
                } catch (Exception ex) {
                    Log.d(null,"ioexception2" + ex.getMessage());
                    if (db.inTransaction())
                        db.endTransaction();
                }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // get filename from path (also remove extension)
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    result = result.substring(0, result.indexOf("."));

                }
            } finally {
                if(cursor != null){
                    cursor.close();
                }

            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            int cut2 = result.indexOf('.',cut);
            if (cut != -1) {
                result = result.substring(cut + 1, cut2);
            }
        }
        return result;
    }
}
