package poliv.jr.com.syncapplication.server;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import library.sharedpackage.communication.DC;
import library.sharedpackage.communication.DataCarrier;
import library.sharedpackage.manager.ItemManager;
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

    private void caseStatements(DataCarrier carrier) { //todo: add wait() to DC.ADD_ITEMS and  GET_ITEMS
        switch (carrier.getInfo()){
            case GET_ITEM_LIST:
                break;

            case GET_ITEMS:
                break;

            case ADD_ITEMS:
                break;

            case REMOVE_ITEMS:
                break;
        }
    }

    @Override
    public void restartServer() {
        server.restartServer();
    }


    private class RequestAsyncTask extends AsyncTask<Object, Void, DataCarrier> {


        @Override
        protected DataCarrier doInBackground(Object... requests) {
            return sendRequest((DataCarrier) requests[0], (boolean) requests[1]);
        }
    }
}
