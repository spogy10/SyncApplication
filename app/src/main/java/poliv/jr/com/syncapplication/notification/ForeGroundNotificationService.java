package poliv.jr.com.syncapplication.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;

import androidx.databinding.ObservableDouble;

import poliv.jr.com.syncapplication.MainActivity;
import poliv.jr.com.syncapplication.R;
import poliv.jr.com.syncapplication.SyncApplication;

public class ForeGroundNotificationService {
    private final Service foreGroundSerive;

    private final int NOTIFICATION_ID = 1;


    public ForeGroundNotificationService(Service notify){
        this.foreGroundSerive = notify;
    }

    //region Setup Notification

    private Notification.Builder createNotificationBuilder(){
        return new Notification.Builder(foreGroundSerive, SyncApplication.NOTIFICATION_CHANNEL_ID)
                .setContentText("Sync App")
                .setSmallIcon(R.drawable.ic_launcher_foreground);
    }

    private PendingIntent sentUpPendingIntent(){
        Intent notificationIntent = new Intent(foreGroundSerive, MainActivity.class);
        return PendingIntent.getActivity(foreGroundSerive, 0, notificationIntent, 0);
    }

    private Notification.Builder buildDefaultNotification(Notification.Builder builder){
        //todo: update build stuff with default stuff I guess
        return builder;
    }

    private Notification buildNotification(Notification.Builder notificationBuilder){
        notificationBuilder.setContentIntent(sentUpPendingIntent());
        return notificationBuilder.build();
    }

    private void updateNotification(Notification notification){
        NotificationManager manager = foreGroundSerive.getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, notification);
    }
    private void buildAndUpdateNotification(Notification.Builder notificationBuilder){
        Notification notification = buildNotification(notificationBuilder);
        updateNotification(notification);
    }

    private void buildAndUpdateNotification(NotificationBuilder notificationBuilder){
        Notification.Builder builder = createNotificationBuilder();
        builder = notificationBuilder.setupNotificationConfiguration(builder);
        buildAndUpdateNotification(builder);
    }

    //endregion



    public void startForegroundServiceWithInitialNotification(){
        Notification.Builder builder = createNotificationBuilder();
        builder = buildDefaultNotification(builder);
        Notification notification = buildNotification(builder);
        foreGroundSerive.startForeground(NOTIFICATION_ID, notification);
    }

    public void defaultNotification(){
        buildAndUpdateNotification(new NotificationBuilder() {
            @Override
            public Notification.Builder setupNotificationConfiguration(Notification.Builder builder) {
                return buildDefaultNotification(builder);
            }
        });
    }

    public void updateProgress(String message){
        buildAndUpdateNotification(new NotificationBuilder() {
            @Override
            public Notification.Builder setupNotificationConfiguration(Notification.Builder builder) {
                //todo: insert update progress config here
                return builder;
            }
        });
    }

    public void updateProgress(String message, ObservableDouble progress){
        buildAndUpdateNotification(new NotificationBuilder() {
            @Override
            public Notification.Builder setupNotificationConfiguration(Notification.Builder builder) {
                //todo: insert update progress config here
                return builder;
            }
        });
    }



    private interface NotificationBuilder {
        Notification.Builder setupNotificationConfiguration(Notification.Builder builder);
    }
}
