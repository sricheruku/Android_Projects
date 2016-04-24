package techemanation.com.thoughtshare.gcm;

/**
 * Created by Srinivas_Cheruku on 13/01/2016.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import com.google.android.gms.gcm.GcmListenerService;
import java.util.ArrayList;
import java.util.List;
import techemanation.com.thoughtshare.ChatUsers;
import techemanation.com.thoughtshare.R;
import techemanation.com.thoughtshare.models.ChatParticipantType;
import techemanation.com.thoughtshare.utils.DatabaseActions;
import techemanation.com.thoughtshare.utils.ThoughtShareApplication;

public class MyGcmListenerService extends GcmListenerService {

    public static String ACTION="gcm.alert.received";

    @Override
    public void onMessageReceived(String from, Bundle data) {

        String chatMessage = data.getString("chat_message");
        String chatParticipantName = data.getString("chat_participant_name");// Indicates from whom we have received message
        String chatTimestamp = data.getString("chat_timestamp");

        if(DatabaseActions.getInstance(getApplicationContext())
                .insertChatData(chatMessage, chatTimestamp, ChatParticipantType.OTHER_CHAT_PARTNER, chatParticipantName))
        {
            // Upon successful insertion, If Activity is Visible, do not send Push Notification. Update the Chat on the UI.
            if(ThoughtShareApplication.isChatWindowActivityVisible())
            {
                Intent intent = new Intent(ACTION);
                intent.putExtra("chat_participant_name", chatParticipantName);
                sendBroadcast(intent);
            }
            else
            {
                ChatDataHolder.chatMessageList.add(chatMessage);
                this.generateChatMessagePushNotification(getApplicationContext(), ChatDataHolder.chatMessageList, chatParticipantName);
            }
        }
    }

    private void generateChatMessagePushNotification(Context context, List<String> chatMessageList, String sender)
    {
        String oneLineSummary = chatMessageList.size()+" new messages.";

        Intent intent = new Intent(context, ChatUsers.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        /* Add Big View Specific Configuration */
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(sender);
        inboxStyle.setSummaryText(oneLineSummary);

        for(String chat : chatMessageList)
        {
            inboxStyle.addLine(chat);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(sender)
                .setContentText(oneLineSummary) // This is for Small View
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setStyle(inboxStyle)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(10 /* ID of notification */, notificationBuilder.build());
    }

    static class ChatDataHolder
    {
        static List<String> chatMessageList=new ArrayList<>();
    }
}