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
    
    private static final boolean REQUEST = DataCarrier.REQUEST;
    private static final boolean RESPONSE = DataCarrier.RESPONSE;

    private Server server;
    private ItemManager remoteManager;

    private AtomicBoolean unreadResponse = new AtomicBoolean(false);
    private AtomicBoolean stopServer = new AtomicBoolean(false);
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
            while (!action.equals(DC.DISCONNECT) && !stopServer.get()){
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
        } catch (Exception e){
            e.printStackTrace();
            Utility.outputError("Error occurred in ServerHandler run method", e);
        } finally {
            server.endServer();
            if(!stopServer.get())
                restartServer();
        }
    }



    //region SERVER MANAGEMENT

    @Override
    public void restartServer() {
        server.restartServer();
    }

    @Override
    public void stopServer() {
        DataCarrier dc = new DataCarrier(REQUEST, DC.DISCONNECT);

        sendRequestUsingAsyncTask(dc, false);

        stopServer.compareAndSet(false, true);
    }
    //endregion



    //region REQUEST AND RESPONSE METHODS
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
                cancelOperation(carrier);
                break;

            case FINISHED_SENDING_FILES:
                finishedSendingFiles(carrier);
                break;

            case CONNECTION_SETUP:
                connectionSetup(carrier);
        }
    }

    private void finishedSendingFiles(DataCarrier carrier) {

    }

    private void cancelOperation(DataCarrier carrier) {
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

        DataCarrier<Boolean> response = new DataCarrier<>(RESPONSE, DC.REMOVE_ITEMS, data);

        sendRequestUsingAsyncTask(response, false);
    }

    private void getItemList(DataCarrier carrier) {
        LinkedList<String> data = (LinkedList<String>) remoteManager.getItemsList();

        DataCarrier response = new DataCarrier<>(RESPONSE, DC.GET_ITEM_LIST, data);

        sendRequestUsingAsyncTask(response, false);
    }

    private void sendFiles(LinkedList<String> fileNames) {
        LinkedList<FileContent> files = (LinkedList<FileContent>) remoteManager.getItems(fileNames);
        DataCarrier<LinkedList<FileContent>> initialResponse = new DataCarrier<>(RESPONSE, DC.GET_ITEMS, files);
        sendRequestUsingAsyncTask(initialResponse, false);

        Utility.outputVerbose("FileContents list sent, ready to send files");

        boolean success = executeFileTransfer(files, true);

        DataCarrier finishedResponse = new DataCarrier(REQUEST, DC.FINISHED_SENDING_FILES);
        sendRequest(finishedResponse, false);

        Utility.outputVerbose("Finished sending files: "+success);
    }

    private void receiveFiles(LinkedList<FileContent> files) {
        DataCarrier initialResponse = new DataCarrier(RESPONSE, DC.OK_TO_SEND_FILES);
        sendRequestUsingAsyncTask(initialResponse, false);

        Utility.outputVerbose("Ok to send files sent, prepared to receive files");

        boolean success = executeFileTransfer(files, false);

        Utility.outputVerbose("Finished receiving files: "+success);
    }


    private boolean executeFileTransfer(LinkedList<FileContent> files, boolean send){
        if(send) return executeSendFileTransfer(files);

        return executeReceiveFileTransfer(files);
    }

    private boolean executeSendFileTransfer(LinkedList<FileContent> files) {
        boolean success = true;
        for(FileContent fileContent : files){
            Utility.outputVerbose("Attempting to send "+fileContent.getFileName());
            DataCarrier<FileContent> sendFile = new DataCarrier<>(RESPONSE, DC.GET_ITEMS, fileContent);
            boolean currentSuccess = server.sendFile(sendFile, null);
            Utility.outputVerbose("Sending "+fileContent.getFileName()+": "+currentSuccess);

            success = currentSuccess && success;
        }
        return success;
    }

    private boolean executeReceiveFileTransfer(LinkedList<FileContent> files) {
        boolean success = true;
        for(FileContent fileContent : files){
            Utility.outputVerbose("Attempting to receive "+fileContent.getFileName());
            DataCarrier<FileContent> receiveFile = new DataCarrier<>(REQUEST, DC.ADD_ITEMS, fileContent);
            boolean currentSuccess = server.receiveFile(receiveFile, null);

            Utility.outputVerbose("Receiving "+fileContent.getFileName()+": "+currentSuccess);
            success = currentSuccess && success;
        }
        return success;
    }

    private void connectionSetup(DataCarrier carrier) {
        DataCarrier<Boolean> response = new DataCarrier<>(RESPONSE, DC.CONNECTION_SETUP,true);

        sendRequest(response, false);
    }

    public DataCarrier testConnection() {
        DataCarrier request = new DataCarrier(REQUEST, DC.CONNECTION_SETUP);

        return sendRequest(request, true);
    }
    //endregion




    //region REQUEST AND RESPONSE UTILITY METHODS

    private DataCarrier sendRequest(DataCarrier request, boolean responseRequired){
        if(server.isServerOff() || !server.areStreamsInitialized()){
            String header = request.isRequest()? "Request:" : "Response:";
            Utility.outputVerbose(header + " " + request.getInfo() + " failed to send because connection not setup");
            return new DataCarrier(RESPONSE, DC.CONNECTION_NOT_SETUP);
        }

        DataCarrier response = new DataCarrier(RESPONSE, DC.SERVER_CONNECTION_ERROR);
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
        DataCarrier response = new DataCarrier(RESPONSE, DC.GENERAL_ERROR);
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

    private class RequestAsyncTask extends AsyncTask<Object, Void, DataCarrier> {


        @Override
        protected DataCarrier doInBackground(Object... requests) {
            return sendRequest((DataCarrier) requests[0], (boolean) requests[1]);
        }
    }
    //endregion
}
