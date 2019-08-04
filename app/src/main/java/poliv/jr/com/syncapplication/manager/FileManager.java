package poliv.jr.com.syncapplication.manager;

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
import poliv.jr.com.syncapplication.server.RequestHandlerInterface;
import poliv.jr.com.syncapplication.server.ServerHandler;
import poliv.jr.com.syncapplication.utility.Utility;

public class FileManager implements ClientRemoteItemManager, FileFilter {

    private static File folder;

    private RequestHandlerInterface requestHandlerInterface;

    private static final String[] FILE_EXTENSIONS = new String[]{"mp4", "mkv", "flv"};

    private final List<String> FILE_EXTENSIONS_LIST;

    private static FileManager instance;

    public static FileManager getInstance(String folderPath){
        instance = new FileManager(folderPath);

        return instance;
    }

    public static FileManager getInstance() throws FileManagerNotInitializedException {
        if(instance == null)
            throw new FileManagerNotInitializedException();

        return instance;
    }

    private FileManager(String folderPath){
        folder = new File(folderPath);
        FILE_EXTENSIONS_LIST = new LinkedList<>(Arrays.asList(FILE_EXTENSIONS));
        requestHandlerInterface = ServerHandler.getInstance(this);
    }

//    @Override
//    public boolean addItems(List<FileContent> files) {
//        boolean success = true;
//
//        if(!Utility.isExternalStorageWritable()){
//            Utility.outputVerbose("Unable to add items because external storage is not writable");
//            return false;
//        }
//
//        for(FileContent file : files){
//            if(!createFile(file))
//                success = false;
//        }
//
//        return success;
//    }

    public void setRequestSenderInterface(RequestHandlerInterface requestHandlerInterface) {
        this.requestHandlerInterface = requestHandlerInterface;
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

//    private boolean createFile(FileContent fileContent){
//        boolean success = false;
//
//        if(!Utility.isExternalStorageWritable()){
//            Utility.outputVerbose("Unable to create file "+ fileContent.getFileName() +" because external storage is not writable");
//            return false;
//        }
//
//        try {
//            Files.write(new File(folder, fileContent.getFileName()).toPath(), fileContent.getData());
//            success = true;
//        } catch (IOException e) {
//            e.printStackTrace();
//            Utility.outputError("Error unable to create file :"+fileContent.getFileName(), e);
//            success = false;
//        }
//
//        return success;
//    }

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

    @Override
    public void restartServer() {
        requestHandlerInterface.restartServer();
    }
}
