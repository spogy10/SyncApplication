package poliv.jr.com.syncapplication.exceptions;

public class FileManagerNotInitializedException extends Exception {
    public FileManagerNotInitializedException() {
        this("Error MyFileManager not initialized");
    }

    public FileManagerNotInitializedException(String message) {
        super(message);
    }
}
