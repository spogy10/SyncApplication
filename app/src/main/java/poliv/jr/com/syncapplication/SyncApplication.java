package poliv.jr.com.syncapplication;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class SyncApplication extends Application {
    public static final String NOTIFICATION_CHANNEL_ID = "syncappServiceChannel";
    private static final String NOTIFICATION_CHANNEL_NAME = "Sync App Notification";
    private static final int NOTIFICATION_IMPORTANCE_LEVEL = NotificationManager.IMPORTANCE_LOW;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NOTIFICATION_IMPORTANCE_LEVEL);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }
}
