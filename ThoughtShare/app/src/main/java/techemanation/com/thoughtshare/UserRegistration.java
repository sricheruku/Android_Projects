package techemanation.com.thoughtshare;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

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
import java.util.Timer;
import java.util.TimerTask;

import techemanation.com.thoughtshare.gcm.RegistrationIntentService;
import techemanation.com.thoughtshare.utils.NetworkUtil;

public class UserRegistration extends AppCompatActivity {

    private AutoCompleteTextView actCity, actName;
    private SharedPreferences spChatUser;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "GCM";
    private String gChatParticipantName;
    private String gCity;
    private ProgressDialog gProgressDialog;
    private TaskRegisterUser gTtaskRegisterUser;
    private Timer gTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button btnRegister = (Button)findViewById(R.id.btnRegister);
        actCity = (AutoCompleteTextView) findViewById(R.id.actCity);
        actName = (AutoCompleteTextView) findViewById(R.id.actName);

        spChatUser=getSharedPreferences(getString(R.string.user_pref), MODE_PRIVATE);

        String spUserName = spChatUser.getString("userName", "");
        if(!spUserName.equals(""))
        {
            showChatUsersActivity();
            finish();
        }

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gChatParticipantName = actName.getText().toString().trim();
                gCity = actCity.getText().toString().trim();
                // There should be a strict validation for Name. Name should not match existing ones.
                if(NetworkUtil.getConnectivityStatus(getApplicationContext())!=0) {
                    initRegistration();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Please check your Internet Connection.", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    private void showChatUsersActivity()
    {
        Intent intent = new Intent(UserRegistration.this, ChatUsers.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private  void initRegistration()
    {
        if (checkPlayServices()) {
            /* Start IntentService to register this application with GCM
             * Once this Service is able to get a GCM Token, it will Broadcast the event which the mBroadcastReceiver will handle
            */
            Log.w("GCM", "Starting Intent service for GCM");
            gTimer=new Timer();
            final Intent gcmIntent = new Intent(UserRegistration.this, RegistrationIntentService.class);

            gProgressDialog=new ProgressDialog(UserRegistration.this);
            gProgressDialog.setTitle("Registering...");
            gProgressDialog.setMessage("Please wait...");
            gProgressDialog.setIndeterminate(true);
            gProgressDialog.setCancelable(false);
            gProgressDialog.show();
            long delayInMillis = 15000;
            gTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            // This will forcefully stop the GCM Intent Service OR gTtaskRegisterUser when not Responded within time Specified
                            Log.w("GCM", "Reached timed out");
                            gTimer.cancel();
                            stopService(gcmIntent);
                            gTtaskRegisterUser.cancel(true);
                            gProgressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Taking longer than usual time. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }, delayInMillis);

            startService(gcmIntent);
        }
    }

    class TaskRegisterUser extends AsyncTask<String,Void,String> {
        private Context ctx;
        String chatParticipantName, city, gcmToken, jsonStream, msg;
        Boolean error;
        JSONObject jsonObject;

        public TaskRegisterUser(Context ctx)
        {
            this.ctx=ctx;
        }

        @Override
        protected String doInBackground(String... params) {
            String reg_url="http://spottechnician.com/thought_share/Add_Chat_User.php";
            chatParticipantName=params[0];
            city=params[1];
            gcmToken=params[2];

            HttpURLConnection httpURLConnection=null;

            try {
                URL url=new URL(reg_url);
                httpURLConnection=(HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream OS=httpURLConnection.getOutputStream();

                BufferedWriter bufferedWriter=new BufferedWriter(new OutputStreamWriter(OS,"UTF-8"));
                String data= URLEncoder.encode("chat_participant_name", "UTF-8")+"="+URLEncoder.encode(chatParticipantName,"UTF-8")+ "&"
                        +URLEncoder.encode("city", "UTF-8")+"="+URLEncoder.encode(city,"UTF-8")+ "&"
                        +URLEncoder.encode("gcm_token", "UTF-8")+"="+URLEncoder.encode(gcmToken,"UTF-8");

                bufferedWriter.write(data);
                bufferedWriter.flush();
                if(bufferedWriter!=null)
                {
                    bufferedWriter.close();
                }

                InputStream inputStream=httpURLConnection.getInputStream();
                BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder=new StringBuilder();

                while((jsonStream=bufferedReader.readLine())!=null)
                {
                    stringBuilder.append(jsonStream+"\n");
                }
                if(bufferedReader!=null)
                {
                    bufferedReader.close();
                }
                if(inputStream!=null)
                {
                    inputStream.close();
                }
                if(OS!=null)
                {
                    OS.close();
                }
                jsonStream= stringBuilder.toString().trim();

                if(jsonStream!=null) {

                    Log.w("GCM", "Received JSON - "+jsonStream);
                    jsonObject=new JSONObject(jsonStream);
                    error=jsonObject.getBoolean("error");
                    msg=jsonObject.getString("message");
                    return msg;
                }
                else
                {
                    return "Network error!";
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.e("error: ", e.getMessage());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("error: ",e.getMessage());
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if(httpURLConnection!=null)
                {
                    httpURLConnection.disconnect();
                }
                gProgressDialog.dismiss();
            }
            return "There was some problem connecting to Internet. Try again.";
        }

        @Override
        protected void onPostExecute(String result) {

            gProgressDialog.dismiss();
            gTimer.cancel();

            if(error!=null) {
                if (error) {
                    Toast.makeText(ctx, "" + result, Toast.LENGTH_LONG).show();
                } else {

                    Toast.makeText(ctx, "You have been successfully Registered.", Toast.LENGTH_LONG).show();
                    spChatUser.edit().putString("userName", gChatParticipantName).apply();
                    showChatUsersActivity();
                    finish();
                }
            }
            else
            {
                Toast.makeText(ctx, "" + result, Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter iFilter= new IntentFilter(RegistrationIntentService.ACTION);
        registerReceiver(mBroadcastReceiver, iFilter);
        Log.w("GCM", "Receiver Registered");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
        Log.w("GCM", "Receiver Unregistered");
    }

    private BroadcastReceiver mBroadcastReceiver=new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {

            gTtaskRegisterUser = new TaskRegisterUser(getApplicationContext());
            String gcmToken = intent.getExtras().getString("gcm_token", "");
            Log.w("GCM", "Broadcast received with GCM - "+gcmToken);

            if (!gcmToken.equals("")) {
                Log.w("GCM", "Inserting data into Server after receiving GCM...");
                gTtaskRegisterUser.execute(gChatParticipantName, gCity, gcmToken);
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Failed to Register with Google Cloud Messaging Service. Please try again.", Toast.LENGTH_LONG).show();
            }
        }
    };
}
