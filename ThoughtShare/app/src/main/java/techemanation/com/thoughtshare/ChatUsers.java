package techemanation.com.thoughtshare;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import techemanation.com.thoughtshare.adapters.ChatUsersItemAdapter;
import techemanation.com.thoughtshare.models.ChatUser;
import techemanation.com.thoughtshare.utils.NetworkUtil;

public class ChatUsers extends AppCompatActivity {

    private ChatUsersItemAdapter chatUsersItemAdapter;
    private List<ChatUser> listChatUsers;
    private HashMap<String, String> userGCMPropertySet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_users);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        userGCMPropertySet = new HashMap<>();
        listChatUsers = new ArrayList<>();

        ListView listViewChatUsers = (ListView) findViewById(R.id.listViewChatUsers);
        chatUsersItemAdapter=new ChatUsersItemAdapter(this.getApplicationContext(), R.layout.template_chat_user, listChatUsers);
        listViewChatUsers.setAdapter(chatUsersItemAdapter);
        listViewChatUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Send OtherChatParticipantName and GCMToken to ChatWindow
                String otherChatParticipantName = ((TextView) view.findViewById(R.id.tvChatUserName)).getText().toString();
                String gcmToken = userGCMPropertySet.get(otherChatParticipantName);
                Intent intent=new Intent(getApplicationContext(), ChatWindow.class);
                intent.putExtra("other_chat_participant_name", otherChatParticipantName);
                intent.putExtra("gcm_token", gcmToken);
                startActivity(intent);
            }
        });
        FetchAllChatUsers();
    }

    private void FetchAllChatUsers()
    {
        if(NetworkUtil.getConnectivityStatus(getApplicationContext())!=0) {
            new TaskFetchAllChatUsers(getApplicationContext()).execute();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Please check your Internet Connection.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_refresh_button, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId()==R.id.btnMenuRefresh)
        {
            FetchAllChatUsers();
            return true;
        }
        onBackPressed();
        return true;
    }

    public class TaskFetchAllChatUsers extends AsyncTask<String,Void,String> {
        private Context ctx;
        JSONObject jsonObject;
        private ProgressDialog pDialog;
        String msg, jsonStreamChatUsers;
        Boolean error;
        Timer timer;
        public TaskFetchAllChatUsers(Context ctx) {
            this.ctx=ctx;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog=new ProgressDialog(ChatUsers.this);
            pDialog.setTitle("Refreshing Chat list...");
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
                                TaskFetchAllChatUsers.this.cancel(true);
                                pDialog.dismiss();
                                Toast.makeText(getApplicationContext(), getString(R.string.TimeOutMessage), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }, delayInMillis);
        }

        @Override
        protected String doInBackground(String... params) {
            String reg_url = "http://spottechnician.com/thought_share/Fetch_All_Chat_Users.php";
            String currentUserName=getSharedPreferences(getString(R.string.user_pref), MODE_PRIVATE).getString("userName", "");
            HttpURLConnection httpURLConnection = null;
            try {
                URL url = new URL(reg_url);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream OS = httpURLConnection.getOutputStream();

                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(OS, "UTF-8"));
                String data = URLEncoder.encode("current_username", "UTF-8") + "=" + URLEncoder.encode(currentUserName, "UTF-8");
                bufferedWriter.write(data);
                bufferedWriter.flush();
                bufferedWriter.close();

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                while ((jsonStreamChatUsers = bufferedReader.readLine()) != null) {
                    stringBuilder.append(jsonStreamChatUsers + "\n");
                }
                bufferedReader.close();
                if (inputStream != null) {
                    inputStream.close();
                }
                if (OS != null) {
                    OS.close();
                }
                jsonStreamChatUsers = stringBuilder.toString().trim();

                if (jsonStreamChatUsers != null) {

                    //Log.w("GCM", "Received JSON - "+jsonStreamChatUsers);
                    jsonObject = new JSONObject(jsonStreamChatUsers);

                    error = jsonObject.getBoolean("error");
                    msg = jsonObject.getString("message");

                    if (error != null) {
                        if (error)
                        {
                            return msg;
                        }
                        else
                        {
                            JSONArray arrChatUsers = jsonObject.getJSONArray("all_chat_users");
                            listChatUsers.clear();
                            for (int i = 0; i < arrChatUsers.length(); i++) {

                                JSONObject eachRequest = (JSONObject) arrChatUsers.get(i);
                                String chatUsername = eachRequest.getString("CHAT_PARTICIPANT_NAME");
                                String city = eachRequest.getString("CITY");
                                String GCMToken = eachRequest.getString("GCM_TOKEN");
                                userGCMPropertySet.put(chatUsername, GCMToken);
                                listChatUsers.add(new ChatUser(chatUsername, city));
                            }

                        }

                    } else {
                        return "no data";
                    }

                } else {
                    return "no data";
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.e("error: ", e.getMessage());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("error: ", e.getMessage());
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
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
            if (error!= null) {
                if (error) {
                    Toast.makeText(ctx, result, Toast.LENGTH_LONG).show();

                } else {
                   chatUsersItemAdapter.notifyDataSetChanged();// Refresh the UI with the latest data
                }
            }
            else
            {
                Toast.makeText(ctx, "" + result, Toast.LENGTH_SHORT).show();
            }
        }
    }

}
