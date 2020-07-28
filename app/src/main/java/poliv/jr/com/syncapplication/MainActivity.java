package poliv.jr.com.syncapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import poliv.jr.com.syncapplication.manager.ClientRemoteItemManager;
import poliv.jr.com.syncapplication.manager.FileManager;
import poliv.jr.com.syncapplication.utility.Utility;

//todo: create notifications
//todo: for foreground services that probably won't close connection on sleep https://developer.android.com/guide/components/services
//todo: run server connection in foreground service


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
