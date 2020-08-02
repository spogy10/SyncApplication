package poliv.jr.com.syncapplication.server;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableDouble;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import library.sharedpackage.communication.DC;
import library.sharedpackage.communication.DataCarrier;
import static library.sharedpackage.communication.DataCarrier.REQUEST;
import static library.sharedpackage.communication.DataCarrier.RESPONSE;
import library.sharedpackage.manager.ItemManager;
import library.sharedpackage.models.FileContent;
import poliv.jr.com.syncapplication.R;
import poliv.jr.com.syncapplication.exceptions.FileManagerNotInitializedException;
import poliv.jr.com.syncapplication.manager.FileManager;
import poliv.jr.com.syncapplication.notification.ForeGroundNotificationService;
import poliv.jr.com.syncapplication.utility.Utility;

public class ServerHandler extends Service implements StoppableService {

    private static final String CONNECTION_RESET_EXCEPTION_STRING = "java.net.SocketException: Connection reset";
    private static final String END_OF_FILE_EXCEPTION_STRING = "java.io.EOFException";

    private Server server;
    private ItemManager remoteManager;
    private ForeGroundNotificationService notificationService;

    private AtomicBoolean unreadResponse = new AtomicBoolean(false);
    private AtomicBoolean stopServer = new AtomicBoolean(false);
    private DataCarrier tempResponseHolder;

    public ServerHandler() {
    }

    //region Service Methods

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationService = new ForeGroundNotificationService(this);
        notificationService.startForegroundServiceWithInitialNotification();

        try {
            this.remoteManager = FileManager.getInstance();
        } catch (FileManagerNotInitializedException e) {
            Utility.outputError("File manager not initialized", e);
            e.printStackTrace();
            stopSelf();
        }
        server = Server.getInstance(this);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utility.outputVerbose("Destroying Service");
        stopServer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    //endregion



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
            else
                stopSelf();
        }
    }



    //region SERVER MANAGEMENT

    private void restartServer() {
        server.restartServer();
    }

    private void stopServer() {
        Utility.outputVerbose("Stopping Server");
        DataCarrier dc = new DataCarrier(REQUEST, DC.DISCONNECT);

        sendRequestUsingAsyncTask(dc, false);

        stopServer.compareAndSet(false, true);
    }
    //endregion

    @Override
    public void stopService() {
        stopSelf();
    }



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
        boolean result;
        if(send)
            result = executeSendFileTransfer(files);
        else
            result =  executeReceiveFileTransfer(files);

        notificationService.defaultNotification();
        return result;
    }

    private boolean executeSendFileTransfer(LinkedList<FileContent> files) {
        boolean success = true;
        int count = 1;
        final int total = files.size();
        for(FileContent fileContent : files){
            Utility.outputVerbose("Attempting to send "+fileContent.getFileName());
            final DataCarrier<FileContent> sendFile = new DataCarrier<>(RESPONSE, DC.GET_ITEMS, fileContent);
            final String message = String.format(getString(R.string.file_transfer_send_message), fileContent.getFileName(), count, total);

            boolean currentSuccess = trackTransferProgress(message, new NotificationProgressTracker() {
                @Override
                public boolean trackProgress(ObservableDouble progress) {
                    return server.sendFile(sendFile, progress);
                }
            });

            Utility.outputVerbose("Sending "+fileContent.getFileName()+": "+currentSuccess);
            success = currentSuccess && success;
            count++;
        }
        return success;
    }

    private boolean executeReceiveFileTransfer(LinkedList<FileContent> files) {
        boolean success = true;
        int count = 1;
        final int total = files.size();
        for(FileContent fileContent : files){
            Utility.outputVerbose("Attempting to receive "+fileContent.getFileName());
            final DataCarrier<FileContent> receiveFile = new DataCarrier<>(REQUEST, DC.ADD_ITEMS, fileContent);
            final String message = String.format(getString(R.string.file_transfer_receive_message), fileContent.getFileName(), count, total);

            boolean currentSuccess = trackTransferProgress(message, new NotificationProgressTracker() {
                @Override
                public boolean trackProgress(ObservableDouble progress) {
                    return server.receiveFile(receiveFile, progress);
                }
            });

            Utility.outputVerbose("Receiving "+fileContent.getFileName()+": "+currentSuccess);
            success = currentSuccess && success;
            count++;
        }
        return success;
    }

    private boolean trackTransferProgress(String transferMessage, NotificationProgressTracker tracker){
        final ObservableDouble progress = new ObservableDouble(0);
        final Observable.OnPropertyChangedCallback observableCallBack = createProgressUpdateCallBack(transferMessage);

        progress.addOnPropertyChangedCallback(observableCallBack);
        updateNotification(transferMessage, progress.get());

        boolean success = tracker.trackProgress(progress);

        progress.removeOnPropertyChangedCallback(observableCallBack);
        updateNotification("", progress.get());

        return success;
    }

    private Observable.OnPropertyChangedCallback createProgressUpdateCallBack(final String message) {
        return new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                final ObservableDouble observableDouble = (ObservableDouble) sender;
                updateNotification(message, observableDouble.get());
            }
        };
    }

    private void updateNotification(String message, double progress){
        notificationService.updateProgress(message, progress);
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

    //region Helper Interface

    private interface NotificationProgressTracker{
        boolean trackProgress(ObservableDouble progress);
    }

    //endregion
}
