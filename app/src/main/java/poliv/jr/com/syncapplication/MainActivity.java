package poliv.jr.com.syncapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import poliv.jr.com.syncapplication.utility.Utility;

public class MainActivity extends AppCompatActivity { //todo: insert request permissions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Utility.outputVerbose(new File(Utility.getFolderPath()).listFiles()[0].getName());
    }
}
