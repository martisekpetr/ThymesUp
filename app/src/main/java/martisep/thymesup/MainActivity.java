package martisep.thymesup;

import android.app.Dialog;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
    Button btnimport;
    Button btnstart;
    ListView listView;
    DBController dbController;
    public static final int requestcode = 42;
    SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //this.deleteDatabase("TimesUpDB.db");

        // bind list of topics to db
        listView = (ListView) findViewById(R.id.listView1);
        dbController = new DBController(this);
        SQLiteDatabase db = dbController.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT rowid _id,* FROM " + DBController.TOPICS_TABLE, null);
        String[] from = new String[]{DBController.TOPIC_COLUMN};
        int[] to = new int[]{R.id.checkedTextView1};
        adapter = new SimpleCursorAdapter(this, R.layout.list_item, cursor, from , to ,0);
        listView.setAdapter(adapter);
        listView.setItemsCanFocus(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        // button listeners
        btnimport = (Button) findViewById(R.id.button_import);
        btnimport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent fileintent = new Intent(Intent.ACTION_GET_CONTENT);
                fileintent.setType("gagt/sdf"); //nonsensical
                try {
                    startActivityForResult(fileintent, requestcode);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getBaseContext(), "No activity can handle picking a file.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        btnstart = (Button) findViewById(R.id.button_start);
        btnstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gameintent = new Intent(getApplicationContext(), GameActivity.class);
                Spinner teamspinner = (Spinner)findViewById(R.id.team_spinner);
                int team_count = Integer.parseInt((String)teamspinner.getSelectedItem());
                gameintent.putExtra(GameActivity.TEAM_COUNT_MESSAGE, team_count);

                ArrayList<String> topics = new ArrayList<>();
                ListView lv = (ListView) findViewById(R.id.listView1);
                SparseBooleanArray checked = lv.getCheckedItemPositions();
                for(int i = 0; i < lv.getAdapter().getCount(); i++){
                    if(checked.get(i)){
                        Cursor c = adapter.getCursor();
                        c.moveToPosition(i);
                        topics.add(c.getString(c.getColumnIndex(DBController.TOPIC_COLUMN)));
                    }
                }
                gameintent.putExtra(GameActivity.TOPICS_MESSAGE, topics);
                startActivity(gameintent);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(data == null){
            return;
        }
        switch(requestCode){
            case requestcode:
                String filepath = data.getData().getPath();
                dbController = new DBController(getApplicationContext());
                SQLiteDatabase db = dbController.getWritableDatabase();
                String new_topic = getFileName(data.getData());
                Log.d(null, new_topic);
                //db.execSQL("delete from " + tableName); // wut??
                try {
                    if (resultCode == RESULT_OK) {
                        try {
                            // broken special characters with UTF8 (why???)
                            BufferedReader buffer = new BufferedReader(new InputStreamReader(new FileInputStream(filepath), "ISO-8859-2"));
                            ContentValues contentValues = new ContentValues();
                            String line;
                            db.beginTransaction();
                            Log.d(null,"begin reading file");
                            while ((line = buffer.readLine()) != null) {
                                String[] str = line.split(";", 2);  // defining 2 columns with null or blank field //values acceptance
                                //Name, Keywords
                                String name = str[0];
                                String keywords = "";
                                if(str.length > 1){
                                    keywords = str[1];
                                }


                                contentValues.put(DBController.NAME_COLUMN, name);
                                contentValues.put(DBController.KEYWORDS_COLUMN, keywords);
                                contentValues.put(DBController.TOPIC_COLUMN, new_topic);
                                db.insert(DBController.ENTRIES_TABLE, null, contentValues);

                            }
                            ContentValues cv = new ContentValues();
                            cv.put(DBController.TOPIC_COLUMN, new_topic);
                            db.insert(DBController.TOPICS_TABLE, null, cv);

                            Log.d(null,"finished reading file");
                            Toast.makeText(getBaseContext(), "Successfully Updated Database.",
                                    Toast.LENGTH_SHORT).show();
                            Cursor myCursor = db.rawQuery("SELECT rowid _id,* FROM " + DBController.TOPICS_TABLE, null);
                            adapter.changeCursor(myCursor);

                            db.setTransactionSuccessful();
                            db.endTransaction();
                        } catch (IOException e) {
                            Log.d(null,"ioexception" + e.getMessage());
                            if (db.inTransaction())
                                db.endTransaction();
                            Dialog d = new Dialog(this);
                            d.setTitle(e.getMessage() + "first");
                            d.show();
                            // db.endTransaction();
                        }
                    } else {
                        if (db.inTransaction())
                            db.endTransaction();
                        Log.d(null,"result not ok");
                        Dialog d = new Dialog(this);
                        d.setTitle("Only CSV files allowed");
                        d.show();
                    }
                } catch (Exception ex) {
                    if (db.inTransaction())
                        db.endTransaction();
                    Log.d(null,"ioexception2" + ex.getMessage());
                    Dialog d = new Dialog(this);
                    d.setTitle(ex.getMessage() + "second");
                    d.show();
                    // db.endTransaction();
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
