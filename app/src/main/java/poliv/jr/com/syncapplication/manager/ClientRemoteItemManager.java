package poliv.jr.com.syncapplication.manager;

import android.content.Context;

import library.sharedpackage.manager.ItemManager;

public interface ClientRemoteItemManager extends ItemManager {
    void restartServer(Context context);

    void stopServer(Context context);
}
