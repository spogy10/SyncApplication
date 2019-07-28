package poliv.jr.com.syncapplication.manager;

import java.util.List;

import sharedpackage.manager.ItemManager;
import sharedpackage.models.FileContent;

public class FileManager implements ItemManager {
    @Override
    public boolean addItems(List<FileContent> files) {
        return false;
    }

    @Override
    public boolean removeItems(List<String> fileNames) {
        return false;
    }

    @Override
    public List<String> getItemsList() {
        return null;
    }

    @Override
    public List<FileContent> getItems(List<String> fileNames) {
        return null;
    }
}
