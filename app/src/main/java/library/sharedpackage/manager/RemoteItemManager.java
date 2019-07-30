package library.sharedpackage.manager;

import library.sharedpackage.models.FileContent;

import java.util.List;

public interface RemoteItemManager extends ItemManager {
    boolean addItems(List<FileContent> files);
}
