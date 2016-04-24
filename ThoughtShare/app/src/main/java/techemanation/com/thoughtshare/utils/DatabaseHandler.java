package techemanation.com.thoughtshare.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "ChatDatabase";

    // Tables names
    private static final String TABLE_CHATS = "Chats";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_CHATS_TABLE="CREATE TABLE "+TABLE_CHATS+"(chat_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "chat_message TEXT NOT NULL, "+
                "chat_timestamp TEXT NOT NULL, "+
                "chat_participant_type TEXT NOT NULL, " +
                "other_chat_participant_name TEXT NOT NULL)";

        db.execSQL(CREATE_CHATS_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHATS);
        // Create tables again
        onCreate(db);

    }

}