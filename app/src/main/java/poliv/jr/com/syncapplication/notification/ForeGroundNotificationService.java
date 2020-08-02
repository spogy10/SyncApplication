package poliv.jr.com.syncapplication.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;

import poliv.jr.com.syncapplication.MainActivity;
import poliv.jr.com.syncapplication.R;
import poliv.jr.com.syncapplication.SyncApplication;

public class ForeGroundNotificationService {
    private final Service foreGroundService;
    private final int NOTIFICATION_ID = 1;
    private final int DEFAULT_MAX_PROGRESS = 100;

    private int previousPercentage = -1;


    public ForeGroundNotificationService(Service service){
        this.foreGroundService = service;
    }

    //region Setup Notification

    private Notification.Builder createNotificationBuilder(){
        return new Notification.Builder(foreGroundService, SyncApplication.NOTIFICATION_CHANNEL_ID)
                .setContentText(foreGroundService.getString(R.string.sync_server_default_notification_text));
    }

    private PendingIntent sentUpPendingIntent(){
        Intent notificationIntent = new Intent(foreGroundService, MainActivity.class);
        return PendingIntent.getActivity(foreGroundService, 0, notificationIntent, 0);
    }

    private Notification buildNotification(Notification.Builder notificationBuilder){
        notificationBuilder.setContentIntent(sentUpPendingIntent())
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        return notificationBuilder.build();
    }

    private void updateNotification(Notification notification){
        NotificationManager manager = foreGroundService.getSystemService(NotificationManager.class);
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

    //region Miscellaneous Methods

    private void resetProgressState(){
        previousPercentage = -1;
    }

    //endregion

    //region Public Methods

    public void startForegroundServiceWithInitialNotification(){
        Notification.Builder builder = createNotificationBuilder();
        Notification notification = buildNotification(builder);
        foreGroundService.startForeground(NOTIFICATION_ID, notification);
    }

    public void defaultNotification(){
        resetProgressState();
        buildAndUpdateNotification(new NotificationBuilder() {
            @Override
            public Notification.Builder setupNotificationConfiguration(Notification.Builder builder) {
                return builder;
            }
        });
    }

    public void updateProgress(final String message, final double progress){
        final int progressPercentage = (int)(progress * DEFAULT_MAX_PROGRESS);

        if(progressPercentage == previousPercentage) return;

        previousPercentage = progressPercentage;

        buildAndUpdateNotification(new NotificationBuilder() {
            @Override
            public Notification.Builder setupNotificationConfiguration(Notification.Builder builder) {
                builder.setContentTitle(message)
                        .setProgress(DEFAULT_MAX_PROGRESS, progressPercentage, false);
                return builder;
            }
        });
    }

    //endregion


    //region Helper Interface

    private interface NotificationBuilder {
        Notification.Builder setupNotificationConfiguration(Notification.Builder builder);
    }

    //endregion
}
