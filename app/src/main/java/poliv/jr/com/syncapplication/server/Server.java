package poliv.jr.com.syncapplication.server;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import library.sharedpackage.communication.DataCarrier;
import library.sharedpackage.models.FileContent;
import poliv.jr.com.syncapplication.utility.Utility;

public class Server implements Runnable {
    private Socket connection = null;
    private ObjectOutputStream os = null;
    private ObjectInputStream is = null;
    private Thread t;


    private Runnable runnable;


    private static Server ourInstance;
    private boolean serverOff = true;

    static Server getInstance(Runnable runnable){
        if(ourInstance != null)
            ourInstance.endServer();

        ourInstance = new Server(runnable);

        return ourInstance;
    }

    static Server getInstance(){
        if(ourInstance == null || ourInstance.runnable == null)
            return null;

        return ourInstance;
    }

    private Server(Runnable runnable) {
        this.runnable = runnable;
        setUpConnection();
    }


    boolean isServerOff(){
        return serverOff;
    }

    private void setUpConnection() {
        t = new Thread(this);
        t.start();
    }




    private void waitForRequests() {
        Utility.outputVerbose("Waiting for connection");

        try{
            serverOff = false;
            connection = new Socket(Utility.getHOST(), Utility.getPORT());
            if(initStreams()){
                Utility.outputVerbose("connection received");
                if(runnable != null){
                    runnable.run();
                    return;
                }
                Utility.outputVerbose("Could not start ServerHandler Thread");
            }

            endServer();

        } catch (IOException e) {
            e.printStackTrace();
            Utility.outputError("Error starting server", e);
        }
    }

    void restartServer(){
        Utility.outputVerbose("Restarting Server");
        endServer();
        setUpConnection();
    }

    private boolean initStreams() {
        try{
            if(connection == null)
                return false;

            os = new ObjectOutputStream(connection.getOutputStream());
            is = new ObjectInputStream(connection.getInputStream());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Utility.outputError("Error initializing streams", e);
        }

        return false;
    }

    private void notifyRequestSent(String request) {
        Utility.outputVerbose("Request: "+request+" sent");
    }

    private void notifyResponseSent(String response){
        Utility.outputVerbose("Response: "+response+" sent");
    }

    private void notifyRequestReceived(String request) {
        Utility.outputVerbose("Request "+request+" received");
    }

    private void notifyResponseReceived(String response){
        Utility.outputVerbose("Response "+response+" received");
    }

    void sendObject(DataCarrier dc) throws IOException {
        os.writeObject(dc);
        if(dc.isRequest())
            notifyRequestSent(dc.getInfo().toString());
        else
            notifyResponseSent(dc.getInfo().toString());
    }

    public boolean sendFile(DataCarrier<FileContent> dc){ //todo: create these methods todo https://stackoverflow.com/questions/10367698/java-multiple-file-transfer-over-socket?answertab=votes#tab-top
        boolean success = false;

        if(dc.isRequest())
            Utility.outputVerbose("Request "+ dc.getInfo() +" to send file commence");
        else
            Utility.outputVerbose("Response "+ dc.getInfo() +" to send file commence");

        FileInputStream fis = null;
        try{
            FileContent fileContent = dc.getData();
            String folderPath = Utility.getFolderPath();
            File file = new File(folderPath, fileContent.getFileName());
            fis = new FileInputStream(file);
            if(FileUtils.sizeOf(file) < (FileUtils.ONE_GB * 2))
                IOUtils.copy(fis, os);
            else
                IOUtils.copyLarge(fis, os);
            success = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Utility.outputError("Error sending file", e);
        } catch (IOException e) {
            e.printStackTrace();
            Utility.outputError("Error sending file", e);
        } finally {
            if(fis == null){
                success = false;
            }else{
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Utility.outputError("Error closing input stream of send file method", e);
                    success = false;
                }
            }
        }
        if(success){
            if(dc.isRequest())
                notifyRequestSent(dc.getInfo().toString());
            else
                notifyResponseSent(dc.getInfo().toString());
            return true;
        }else{
            if(dc.isRequest())
                Utility.outputVerbose("Could not send file for request: "+ dc.getInfo());
            else
                Utility.outputVerbose("Could not send file for response: "+ dc.getInfo());
        }

        return false;
    }

    public boolean receiveFile(DataCarrier<FileContent> dc) {
        boolean success = false;

        if(dc.isRequest())
            Utility.outputVerbose("Request "+ dc.getInfo() +" to receive file commence");
        else
            Utility.outputVerbose("Response "+ dc.getInfo() +" to receive file commence");

        FileOutputStream fos = null;
        try{
            FileContent fileContent = dc.getData();
            String folderPath = Utility.getFolderPath();
            File file = new File(folderPath, fileContent.getFileName());
            fos = new FileOutputStream(file);

            int n;
            long fileSize = fileContent.getFileSize();
            byte[] buffer = new byte[1024 * 4];
            while ( (fileSize > 0) && (IOUtils.EOF != (n = is.read(buffer, 0, (int)Math.min(buffer.length, fileSize)))) ) { //checks if fileSize is 0 or if EOF sent
                fos.write(buffer, 0, n);
                fileSize -= n;
                Utility.outputVerbose("fileSize: "+fileSize+"\nn: "+ n);
            }

            success = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Utility.outputError("Error receiving file", e);
        } catch (IOException e) {
            e.printStackTrace();
            Utility.outputError("Error receiving file", e);
        } finally {
            if(fos == null){
                success = false;
            }else{
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Utility.outputError("Error closing out stream of receive file method", e); //todo: change to ui element
                    success = false;
                }
            }
        }
        if(success){
            if(dc.isRequest())
                notifyRequestSent(dc.getInfo().toString());
            else
                notifyResponseSent(dc.getInfo().toString());
            return true;
        }else{
            if(dc.isRequest())
                Utility.outputVerbose("Could not receive file for request: "+ dc.getInfo());
            else
                Utility.outputVerbose("Could not receive file for response: "+ dc.getInfo());
        }

        return false;
    }

    DataCarrier receiveObject() throws IOException, ClassNotFoundException {
        DataCarrier dc = (DataCarrier) is.readObject();
        if(dc.isRequest())
            notifyRequestReceived(dc.getInfo().toString());
        else
            notifyResponseReceived(dc.getInfo().toString());
        return dc;
    }

    private void closeConnection(){
        try {
            if(os != null)
                os.close();
            if(is != null)
                is.close();
            if(connection != null)
                connection.close();
            Utility.outputVerbose("Server connections closed");
        } catch (IOException e) {
            e.printStackTrace();
            Utility.outputError("Error closing connections", e);
        }

        os = null;
        is = null;
        connection = null;
    }

    public boolean areStreamsInitialized(){
        return connection != null && os != null && is != null;
    }

    void endServer(){
        closeConnection();
        Utility.outputVerbose("Server ended");

        serverOff = true;
    }


    @Override
    public void run() {
        waitForRequests();
    }
}
