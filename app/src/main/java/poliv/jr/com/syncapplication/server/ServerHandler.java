package poliv.jr.com.syncapplication.server;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import library.sharedpackage.communication.DC;
import library.sharedpackage.communication.DataCarrier;
import library.sharedpackage.manager.ItemManager;
import library.sharedpackage.models.FileContent;
import poliv.jr.com.syncapplication.utility.Utility;

public class ServerHandler implements Runnable, RequestHandlerInterface {

    private static final String CONNECTION_RESET_EXCEPTION_STRING = "java.net.SocketException: Connection reset";
    private static final String END_OF_FILE_EXCEPTION_STRING = "java.io.EOFException";

    private Server server;
    private ItemManager remoteManager;

    private AtomicBoolean unreadResponse = new AtomicBoolean(false);
    private DataCarrier tempResponseHolder;

    private static ServerHandler ourInstance = null;


    public static ServerHandler getInstance(ItemManager remoteManager){
        ourInstance = new ServerHandler(remoteManager);

        return ourInstance;
    }

    public static ServerHandler getInstance(){
        if(ourInstance == null || ourInstance.remoteManager == null)
            return null;

        return ourInstance;
    }

    private ServerHandler(ItemManager remoteManager){
        this.remoteManager = remoteManager;
        server = Server.getInstance(this);
    }

    @Override
    public void run() {
        Utility.outputVerbose("ServerHandler thread started");

        DC action = DC.NO_INFO;
        try{
            while (!action.equals(DC.DISCONNECT)){
                DataCarrier carrier = server.receiveObject();
                if(carrier.isRequest()){
                    action = carrier.getInfo();
                    caseStatements(carrier);
                }else {//it is a response
                    tempResponseHolder = carrier;
                    unreadResponse.compareAndSet(false, true);
                }
            }
            Utility.outputVerbose("server disconnected normally");
        } catch (IOException e) {
            String message = "Error occurred in ServerHandler run method";
            if(e.toString().equals(CONNECTION_RESET_EXCEPTION_STRING) || e.toString().equals(END_OF_FILE_EXCEPTION_STRING))
                message = "disconnected from server";
            else
                e.printStackTrace();
            Utility.outputError(message, e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Utility.outputError("Error occurred in ServerHandler run method", e);
        } finally {
            server.endServer();
            server.restartServer();
        }
    }

    private DataCarrier sendRequest(DataCarrier request, boolean responseRequired){
        if(server.isServerOff() || !server.areStreamsInitialized()){
            String header = request.isRequest()? "Request:" : "Response:";
            Utility.outputVerbose(header + " " + request.getInfo() + " failed to send because connection not setup");
            return new DataCarrier(DC.CONNECTION_NOT_SETUP, false);
        }

        DataCarrier response = new DataCarrier(DC.SERVER_CONNECTION_ERROR, false);
        try{
            server.sendObject(request);

            if(responseRequired) {
                response = waitForResponse();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Utility.outputError("Error sending request", e);
        }

        return response;
    }

    private DataCarrier sendRequestUsingAsyncTask(DataCarrier request, boolean responseRequired){
        DataCarrier response = new DataCarrier(DC.GENERAL_ERROR, false);
        try {
            response = new RequestAsyncTask().execute(request, responseRequired).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return response;
    }

    private DataCarrier waitForResponse() {
        while(!unreadResponse.get()){
            /*wait until response comes in*/
        }

        unreadResponse.compareAndSet(true, false);
        return tempResponseHolder;
    }

    @Override
    public void restartServer() {
        server.restartServer();
    }

    private void caseStatements(DataCarrier carrier) {
        switch (carrier.getInfo()){
            case GET_ITEM_LIST:
                getItemList(carrier);
                break;

            case GET_ITEMS:
                getItems(carrier);
                break;

            case ADD_ITEMS:
                addItems(carrier);
                break;

            case REMOVE_ITEMS:
                removeItems(carrier);
                break;

            case CANCEL_OPERATION:
                break;
        }
    }

    private void getItems(DataCarrier carrier) {
        LinkedList<String> fileNames = (LinkedList<String>) carrier.getData();

        sendFiles(fileNames);
    }

    private void addItems(DataCarrier carrier) {
        LinkedList<FileContent> files = (LinkedList<FileContent>) carrier.getData();

        receiveFiles(files);
    }

    private void removeItems(DataCarrier carrier) {
        LinkedList<String> fileNames = (LinkedList<String>) carrier.getData();

        Boolean data = remoteManager.removeItems(fileNames);

        DataCarrier<Boolean> response = new DataCarrier<>(DC.REMOVE_ITEMS, data, false);

        sendRequestUsingAsyncTask(response, false);
    }

    private void getItemList(DataCarrier carrier) {
        LinkedList<String> data = (LinkedList<String>) remoteManager.getItemsList();

        DataCarrier response = new DataCarrier<>(DC.GET_ITEM_LIST, data, false);

        sendRequestUsingAsyncTask(response, false);
    }

    private void sendFiles(LinkedList<String> fileNames) {
        boolean success = true;

        LinkedList<FileContent> files = (LinkedList<FileContent>) remoteManager.getItems(fileNames);
        DataCarrier<LinkedList<FileContent>> initialResponse = new DataCarrier<>(DC.GET_ITEMS, files, false);
        sendRequestUsingAsyncTask(initialResponse, false);

        Utility.outputVerbose("FileContents list sent, ready to send files");

        for(FileContent fileContent : files){
            Utility.outputVerbose("Attempting to send "+fileContent.getFileName());
            DataCarrier<FileContent> sendFile = new DataCarrier<>(DC.GET_ITEMS, fileContent, false);
            boolean currentSuccess = server.sendFile(sendFile);
            Utility.outputVerbose("Sending "+fileContent.getFileName()+": "+currentSuccess);

            success = currentSuccess && success;
        }

        Utility.outputVerbose("Finished sending files: "+success);

    }

    private void receiveFiles(LinkedList<FileContent> files) {
        boolean success = true;

        DataCarrier initialResponse = new DataCarrier(DC.OK_TO_SEND_FILES, false);
        sendRequestUsingAsyncTask(initialResponse, false);

        Utility.outputVerbose("Ok to send files sent, prepared to receive files");

        for(FileContent fileContent : files){
            Utility.outputVerbose("Attempting to receive "+fileContent.getFileName());
            DataCarrier<FileContent> receiveFile = new DataCarrier<>(DC.ADD_ITEMS, fileContent, true);
            boolean currentSuccess = server.receiveFile(receiveFile);

            Utility.outputVerbose("Receiving "+fileContent.getFileName()+": "+currentSuccess);
            success = currentSuccess && success;
        }

        Utility.outputVerbose("Finished receiving files: "+success);
    }


    private class RequestAsyncTask extends AsyncTask<Object, Void, DataCarrier> {


        @Override
        protected DataCarrier doInBackground(Object... requests) {
            return sendRequest((DataCarrier) requests[0], (boolean) requests[1]);
        }
    }
}
