package martisep.thymesup;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBController extends SQLiteOpenHelper{
    private static final String LOGCAT = null;
    public static final String ENTRIES_TABLE = "entries";
    public static final String TOPICS_TABLE = "topics";
    public static final String NAME_COLUMN = "Name";
    public static final String KEYWORDS_COLUMN = "Keywords";
    public static final String TOPIC_COLUMN = "Topic";
    public static final String COUNT_COLUMN = "Count";



    public DBController(Context context) {
        super(context, "TimesUpDB.db", null, 1); // hardwired, not pretty, whatever
        Log.d(LOGCAT, "Created"); //debug, i guess
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + ENTRIES_TABLE
                + " ( Id INTEGER PRIMARY KEY, "
                + NAME_COLUMN
                + " TEXT, "
                + KEYWORDS_COLUMN
                + " TEXT, "
                + TOPIC_COLUMN
                + " TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "
                + TOPICS_TABLE
                + " ( Id INTEGER PRIMARY KEY, "
                + TOPIC_COLUMN
                + " TEXT, "
                + COUNT_COLUMN
                + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ENTRIES_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + TOPICS_TABLE);
        onCreate(db);
    }
}
