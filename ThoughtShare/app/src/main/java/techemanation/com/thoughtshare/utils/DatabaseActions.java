package techemanation.com.thoughtshare.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by Srinivas_Cheruku on 25/03/2016.
 */
public class DatabaseActions
{
    private Context context;
    public static DatabaseActions getInstance(Context context)
    {
        return new DatabaseActions(context);
    }

    private DatabaseActions(Context context)
    {
        this.context=context;
    }

    public boolean insertChatData(String chatMessage, String chatTimestamp, String chatParticipantType, String otherChatParticipantName)
    {
        DatabaseHandler dbHandler=new DatabaseHandler(this.context);
        SQLiteDatabase writableChatDB=null;
        try
        {
            writableChatDB = dbHandler.getWritableDatabase();
            ContentValues insertValues = new ContentValues();
            insertValues.put("chat_message", chatMessage);
            insertValues.put("chat_timestamp", chatTimestamp);
            insertValues.put("chat_participant_type", chatParticipantType);
            insertValues.put("other_chat_participant_name", otherChatParticipantName);
            writableChatDB.insert("Chats", null, insertValues);
            Log.w("GCM", "Data inserted successfully");
            return true;
        }
        catch (Exception ex)
        {
            Log.w("GCM: DB exception: ", ex.getMessage());
            return false;
        }
        finally
        {
            writableChatDB.close();
        }
    }
}
