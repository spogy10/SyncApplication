package poliv.jr.com.syncapplication.server;

public interface StoppableService extends Runnable {
    void stopService();
}
