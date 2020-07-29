package poliv.jr.com.syncapplication.manager;

import android.content.Context;
import android.content.Intent;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import library.sharedpackage.models.FileContent;
import poliv.jr.com.syncapplication.exceptions.FileManagerNotInitializedException;
import poliv.jr.com.syncapplication.server.ServerHandler;
import poliv.jr.com.syncapplication.utility.Utility;

public class FileManager implements ClientRemoteItemManager, FileFilter {

    private static File folder;

    private static final String[] FILE_EXTENSIONS = new String[]{"mp4", "mkv", "flv"};

    private final List<String> FILE_EXTENSIONS_LIST;

    private static FileManager instance;

    public static FileManager getInstance(String folderPath){
        if(instance == null)
            instance = new FileManager(folderPath);
        else
            instance.setupFileManager(folderPath);

        return instance;
    }

    public static FileManager getInstance() throws FileManagerNotInitializedException {
        if(instance == null)
            throw new FileManagerNotInitializedException();

        return instance;
    }

    private FileManager(String folderPath){
        FILE_EXTENSIONS_LIST = new LinkedList<>(Arrays.asList(FILE_EXTENSIONS));
        setupFileManager(folderPath);
    }

    private void setupFileManager(String folderPath){
        folder = new File(folderPath);
    }

    @Override
    public boolean removeItems(List<String> fileNames) {
        boolean success = true;

        if(!Utility.isExternalStorageWritable()){
            Utility.outputVerbose("Unable to remove items because external storage is not writable");
            return false;
        }

        for(String fileName : fileNames){
            try{

                Files.deleteIfExists(new File(folder, fileName).toPath());

            } catch (IOException e) {
                e.printStackTrace();
                String message = "Error deleting file :"+fileName;
                Utility.outputError(message, e);
                success = false;
            }
        }

        return success;
    }

    @Override
    public List<String> getItemsList() {
        LinkedList<String> list = new LinkedList<>();

        if(!Utility.isExternalStorageReadable()){
            Utility.outputVerbose("Unable to get items list because external storage is not readable");
            return list;
        }

        for(File file : folder.listFiles(this)){
            list.add(file.getName());
        }

        return list;
    }

    @Override
    public List<FileContent> getItems(List<String> fileNames) {
        LinkedList<FileContent> list = new LinkedList<>();

        if(!Utility.isExternalStorageReadable()){
            Utility.outputVerbose("Unable to get items because external storage is not readable");
            return list;
        }

        for(String fileName : fileNames){
            list.add(retrieveFile(fileName));
        }

        Utility.outputVerbose("created list of B files");
        return list;
    }

    @Override
    public boolean accept(File pathname) {
        boolean isFile, isRightFileType;

        String fileExtension = getFileExtension(pathname.getName());

        isFile = pathname.isFile();

        isRightFileType = FILE_EXTENSIONS_LIST.contains(fileExtension);

        return isFile && isRightFileType;
    }

    private String getFileExtension(String fileName){
        int lastIndex = fileName.lastIndexOf('.');

        return (lastIndex > 0) ? fileName.substring(++lastIndex) : "";
    }

    private FileContent retrieveFile(String fileName){
        FileContent fileContent = null;

        if(!Utility.isExternalStorageReadable()){
            Utility.outputVerbose("Unable to retrieve file "+ fileName +" because external storage is not writable");
            return fileContent;
        }

        File file = new File(folder, fileName);
        if(file.isFile()){
            fileContent = new FileContent(fileName, FileUtils.sizeOf(file));
        }else{
            Utility.outputVerbose("error retrieving file:" + fileName +  "does not exist");
        }

        return fileContent;
    }

    private Intent generateIntent(Context context){
        return new Intent(context, ServerHandler.class);
    }

    @Override
    public void restartServer(Context context) {
        context.startService(generateIntent(context));
    }

    @Override
    public void stopServer(Context context) {
        context.stopService(generateIntent(context));
    }


}
