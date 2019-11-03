package poliv.jr.com.syncapplication.utility;

import android.os.Environment;
import android.util.Log;

import java.io.File;

import library.sharedpackage.communication.DataCarrier;

public class Utility {

    private static final String DEFAULT_ITEM_FOLDER_NAME = "Videos";
    //private static final String DEFAULT_ITEM_FOLDER_NAME = "Videos";

    private static final String LOG_TAG = "SyncAppLogger";

    private static String HOST = "192.168.100.67";
    private static final int PORT = 4000;

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }


    public static String getFolderPath(){
        return Environment.getExternalStoragePublicDirectory("") + File.separator + DEFAULT_ITEM_FOLDER_NAME;
    }

    public static String getHOST() {
        return HOST;
    }

    public static void setHost(String host){
        HOST = host;
    }

    public static int getPORT() {
        return PORT;
    }

    public static boolean responseCheck(DataCarrier response) {
        if(!response.isRequest()) {
            if (!response.getInfo().IsErrorCode) {
                outputVerbose("No Error Code");
                return true;
            }

            outputVerbose(response.getInfo().toReadableString());
        }else{
            outputVerbose("Response set as request");
        }

        return false;
    }

    public static void outputVerbose(String message){
        Log.d(LOG_TAG, message);
    }

    public static void outputError(Exception e){
        outputError("", e);
    }

    public static void outputError(String message, Exception e){
        Log.e(LOG_TAG, message, e);
    }
}
