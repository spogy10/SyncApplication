package poliv.jr.com.syncapplication.notification;

public interface ForegroundNotificationService {
    void startForegroundServiceWithInitialNotification();

    void defaultNotification();

    void updateProgress(String message, double progress);
}
