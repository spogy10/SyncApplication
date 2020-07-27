package poliv.jr.com.syncapplication.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.LinkedList;

import library.sharedpackage.communication.DC;
import library.sharedpackage.communication.DataCarrier;
import library.sharedpackage.models.FileContent;
import poliv.jr.com.syncapplication.server.Server;
import poliv.jr.com.syncapplication.utility.Utility;

public class FileTransferWorker extends ListenableWorker {
    private Server server;

    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public FileTransferWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(new CallbackToFutureAdapter.Resolver<Result>() {
            @Nullable
            @Override
            public Object attachCompleter(@NonNull CallbackToFutureAdapter.Completer<Result> completer) throws Exception {
                return null;
            }
        });
    }

    private boolean executeFileTransfer(LinkedList<FileContent> files, boolean send){
        if(send) return executeSendFileTransfer(files);

        return executeReceiveFileTransfer(files);
    }

    private boolean executeSendFileTransfer(LinkedList<FileContent> files) {
        boolean success = true;
        for(FileContent fileContent : files){
            Utility.outputVerbose("Attempting to send "+fileContent.getFileName());
            DataCarrier<FileContent> sendFile = new DataCarrier<>(false, DC.GET_ITEMS, fileContent);
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
            DataCarrier<FileContent> receiveFile = new DataCarrier<>(true, DC.ADD_ITEMS, fileContent);
            boolean currentSuccess = server.receiveFile(receiveFile, null);

            Utility.outputVerbose("Receiving "+fileContent.getFileName()+": "+currentSuccess);
            success = currentSuccess && success;
        }
        return success;
    }
    
    
}
