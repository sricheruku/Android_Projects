package techemanation.com.thoughtshare.gcm;

/**
 * Created by Srinivas_Cheruku on 13/01/2016.
 */

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import techemanation.com.thoughtshare.R;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "GCM";
    public static String ACTION="gcm.token.received";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Intent mIntent = new Intent(ACTION);
        try {
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            mIntent.putExtra("gcm_token", token);
            Log.w(TAG, "GCM Registration Token: " + token);

        } catch (Exception e) {
            mIntent.putExtra("gcm_token", "");
            Log.w(TAG, "Failed to complete token refresh", e);
        }

        sendBroadcast(mIntent);// This will Broadcast the event to UserRegistration Activity
        Log.w("GCM", "GCM broadcast sent");
    }
}
