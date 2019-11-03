package poliv.jr.com.syncapplication.manager;

import library.sharedpackage.manager.ItemManager;

public interface ClientRemoteItemManager extends ItemManager {
    void restartServer();

    void stopServer();
}
