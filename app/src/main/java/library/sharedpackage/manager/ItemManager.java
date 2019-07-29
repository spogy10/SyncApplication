package library.sharedpackage.manager;

import library.sharedpackage.models.FileContent;

import java.util.List;

public interface ItemManager { //todo change the methods using filecontent

    boolean addItems(List<FileContent> files);

    boolean removeItems(List<String> fileNames);

    List<String> getItemsList();

    List<FileContent> getItems(List<String> fileNames);
}
