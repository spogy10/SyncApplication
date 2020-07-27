package poliv.jr.com.syncapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.List;
import java.util.concurrent.ExecutionException;

import library.sharedpackage.models.FileContent;
import poliv.jr.com.syncapplication.manager.ClientRemoteItemManager;
import poliv.jr.com.syncapplication.manager.FileManager;
import poliv.jr.com.syncapplication.server.Server;
import poliv.jr.com.syncapplication.utility.Utility;
import poliv.jr.com.syncapplication.worker.FileTransferWorker;

//todo: create notifications
//todo: for background services that probably won't close connection on sleep https://developer.android.com/guide/components/services


public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 5;
    private static ClientRemoteItemManager fileManager;

    private EditText etIpAddress;

    private Button btStartServer, btStopServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        onStartUp();

        etIpAddress = findViewById(R.id.ipAddress);

        etIpAddress.setText(Utility.getHOST());

        btStartServer = findViewById(R.id.startServer);

        btStopServer = findViewById(R.id.stopServer);

        btStartServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startServer(etIpAddress.getText().toString());
            }
        });

        btStopServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopServer();
            }
        });
    }

    private void onStartUp(){
        checkForWritePermission();
    }

    private void startServer(String host){
        Utility.setHost(host);
        fileManager = FileManager.getInstance(Utility.getFolderPath());
    }

    private void stopServer(){
        if(fileManager != null)
            fileManager.stopServer();
    }

    public boolean transferFilesInWorker(List<FileContent> files, boolean send, Server sever){
        WorkRequest transferFileRequest =
                new OneTimeWorkRequest.Builder(FileTransferWorker.class)
                        .build();

        Operation op = WorkManager.getInstance(this).enqueue(transferFileRequest);
        try {
            return op.getResult().get() == Operation.SUCCESS;
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void checkForWritePermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finishAndRemoveTask();
            }
        }
    }
}
