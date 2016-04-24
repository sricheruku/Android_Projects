package techemanation.com.thoughtshare.utils;

import android.app.Application;

/**
 * Created by Srinivas_Cheruku on 19/03/2016.
 *
 * This class can be used to track the status of the Activity
 */

public class ThoughtShareApplication extends Application
{
    public static boolean isChatWindowActivityVisible() {
        return chatWindowActivityVisible;
    }

    public static void chatWindowActivityResumed() {
        chatWindowActivityVisible = true;
    }

    public static void chatWindowActivityPaused() {
        chatWindowActivityVisible = false;
    }

    private static boolean chatWindowActivityVisible;
}
