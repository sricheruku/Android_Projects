package techemanation.com.thoughtshare;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import techemanation.com.thoughtshare.adapters.ChatListItemAdapter;
import techemanation.com.thoughtshare.gcm.MyGcmListenerService;
import techemanation.com.thoughtshare.models.ChatMessage;
import techemanation.com.thoughtshare.models.ChatParticipantType;
import techemanation.com.thoughtshare.utils.DatabaseActions;
import techemanation.com.thoughtshare.utils.DatabaseHandler;
import techemanation.com.thoughtshare.utils.NetworkUtil;
import techemanation.com.thoughtshare.utils.ThoughtShareApplication;

public class ChatWindow extends AppCompatActivity {

    private EditText txtMessage;
    private List<ChatMessage> listChatMessages;
    private ChatListItemAdapter chatListItemAdapter;
    private DatabaseHandler dbHandler;
    private String gCurrentUserName, gOtherChatParticipantName, gcmToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_window);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        dbHandler=new DatabaseHandler(getApplicationContext());

        Button btnSend=(Button) findViewById(R.id.btnSend);
        txtMessage= (EditText) findViewById(R.id.txtMessage);
        ListView listViewChats= (ListView) findViewById(R.id.listViewChats);

        listChatMessages= new ArrayList<>();
        chatListItemAdapter=new ChatListItemAdapter(this.getApplicationContext(), R.layout.template_chat_message, listChatMessages);
        listViewChats.setAdapter(chatListItemAdapter);

        gCurrentUserName=getSharedPreferences(getString(R.string.user_pref), MODE_PRIVATE).getString("userName", "");
        gOtherChatParticipantName = getIntent().getExtras().getString("other_chat_participant_name", "");
        gcmToken = getIntent().getExtras().getString("gcm_token", "");
        this.setTitle(gOtherChatParticipantName);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String chatMessage = txtMessage.getText().toString().trim();
                String chatTimestamp = getCurrentTime();
                if(NetworkUtil.getConnectivityStatus(getApplicationContext())!=0) {
                    new TaskSendChatMessage(getApplicationContext()).execute(gCurrentUserName, chatMessage, chatTimestamp, gcmToken);
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Please check your Internet Connection.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void fetchAndPopulateChatHistory(String otherChatParticipantName)
    {
        SQLiteDatabase readableDB=null;
        Cursor dataPointer=null;
        try
        {
            readableDB = dbHandler.getReadableDatabase();
            String query="SELECT chat_message, chat_timestamp, chat_participant_type " +
                    "FROM Chats WHERE other_chat_participant_name = ?";
            dataPointer= readableDB.rawQuery(query, new String[]{otherChatParticipantName});
            while (dataPointer.moveToNext())
            {
                listChatMessages.add(new ChatMessage(dataPointer.getString(0), dataPointer.getString(1), dataPointer.getString(2)));
            }
            chatListItemAdapter.notifyDataSetChanged();// Refresh the UI with latest Data
        }
        catch(Exception ex)
        {
            Log.w("GCM", ex.getMessage());
        }
        finally
        {
            dataPointer.close();
            readableDB.close();
        }
    }

    private void refreshUI(String chatMessage, String chatTimestamp, String chatParticipantType)
    {
        this.listChatMessages.add(new ChatMessage(chatMessage, chatTimestamp, chatParticipantType));
        this.chatListItemAdapter.notifyDataSetChanged();
        Log.w("GCM", "UI refreshed successfully");
    }

    private void insertAndRefreshChat(String chatMessage, String chatTimestamp, String chatParticipantType, String otherChatParticipantName)
    {
        boolean isSuccessfullyInserted = DatabaseActions.getInstance(getApplicationContext())
                .insertChatData(chatMessage, chatTimestamp, chatParticipantType, otherChatParticipantName);
        if(isSuccessfullyInserted)
        {
            refreshUI(chatMessage, chatTimestamp, chatParticipantType);
        }
        else
        {
            Log.w("GCM", "Data insertion failed");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    private String getCurrentTime()
    {
        String customDateFormat="hh:mm aaa";
        return  (String) DateFormat.format(customDateFormat, Calendar.getInstance().getTime());
    }

    class TaskSendChatMessage extends AsyncTask<String,Void,String> {
        private ProgressDialog pDialog;
        String receivedDataStream, chatParticipantName, chatMessage, chatTimestamp, gcmToken;
        Timer timer;
        Context ctx;

        public TaskSendChatMessage(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog=new ProgressDialog(ChatWindow.this);
            pDialog.setTitle("Sending...");
            pDialog.setMessage("Please wait...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

            timer=new Timer();
            long delayInMillis = 10000;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            timer.cancel();
                            TaskSendChatMessage.this.cancel(true);
                            pDialog.dismiss();
                            Toast.makeText(getApplicationContext(), getString(R.string.TimeOutMessage), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }, delayInMillis);
        }

        @Override
        protected String doInBackground(String... params) {
            String reg_url = "https://android.googleapis.com/gcm/send";
            chatParticipantName = params[0];
            chatMessage = params[1];
            chatTimestamp = params[2];
            gcmToken = params[3];

            HttpURLConnection httpURLConnection = null;
            try {
                URL url = new URL(reg_url);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestProperty("Authorization", "key=AIzaSyAV_lvc1lU5Fj3h9YbYmAYVfTOhCNhKpes");// API key
                httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                OutputStream OS = httpURLConnection.getOutputStream();

                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(OS, "UTF-8"));
                String data = URLEncoder.encode("registration_id", "UTF-8") + "=" + URLEncoder.encode(gcmToken, "UTF-8") + "&" +
                        URLEncoder.encode("data.chat_message", "UTF-8") + "=" + URLEncoder.encode( chatMessage, "UTF-8") + "&" +
                        URLEncoder.encode("data.chat_timestamp", "UTF-8") + "=" + URLEncoder.encode(chatTimestamp, "UTF-8")+ "&" +
                        URLEncoder.encode("data.chat_participant_name", "UTF-8") + "=" + URLEncoder.encode(chatParticipantName, "UTF-8");
                bufferedWriter.write(data);
                bufferedWriter.flush();
                bufferedWriter.close();

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                while ((receivedDataStream = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receivedDataStream + "\n");
                }
                bufferedReader.close();

                if (inputStream != null) {
                    inputStream.close();
                }
                if (OS != null) {
                    OS.close();
                }
                receivedDataStream = stringBuilder.toString().trim();

                if (receivedDataStream != null) {

                    Log.w("GCM", "Response - "+receivedDataStream);
                    return receivedDataStream;

                } else {
                    return "empty";
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.e("error: ", e.getMessage());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("error: ", e.getMessage());
            }
             finally {
                if (httpURLConnection != null) {

                    httpURLConnection.disconnect();

                }
            }
            pDialog.dismiss();

            return "There was some problem connecting to Internet. Try again.";
        }

        @Override
        protected void onPostExecute(String result) {

            pDialog.dismiss();
            timer.cancel();
            // When GCM Server accepts the message for delivery, it returns an id. Format: id=<some_id>
            String[] substrings = receivedDataStream.split("=");
            if (substrings[0].equals("id"))
            {
                insertAndRefreshChat(chatMessage, chatTimestamp, ChatParticipantType.CURRENT_USER, gOtherChatParticipantName);
                txtMessage.setText("");
            }
            else
            {
                Toast.makeText(ctx, "Unable to send your Message. Please try again.", Toast.LENGTH_LONG).show();
                Log.w("GCM", "Unfavorable GCM response - "+result);
            }
        }
    }

    /* The following events are handled so as to Update the status of the Current Activity.
     * ThoughtShareApplication is a Custom class used to track the status of the Activity */
    @Override
    protected void onResume() {
        super.onResume();
        this.fetchAndPopulateChatHistory(gOtherChatParticipantName);
        ThoughtShareApplication.chatWindowActivityResumed();
        IntentFilter iFilter= new IntentFilter(MyGcmListenerService.ACTION);
        registerReceiver(myBroadcastReceiver, iFilter);
        Log.w("GCM", "Broadcast Registered");
    }

    @Override
    protected void onPause() {
        super.onPause();
        ThoughtShareApplication.chatWindowActivityPaused();
        unregisterReceiver(myBroadcastReceiver);
        Log.w("GCM", "Broadcast UnRegistered");
    }

    private void playDefaultSound()
    {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        MediaPlayer.create(ChatWindow.this, defaultSoundUri);
    }

    private BroadcastReceiver myBroadcastReceiver=new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            // If message is received from User with whom current Chat is going on, Refresh the UI, else do not.
            String lOtherChatParticipantName = intent.getExtras().getString("chat_participant_name");
            if(gOtherChatParticipantName.equals(lOtherChatParticipantName))
            {
                fetchAndPopulateChatHistory(gOtherChatParticipantName);
                playDefaultSound();
            }
        }
    };

}