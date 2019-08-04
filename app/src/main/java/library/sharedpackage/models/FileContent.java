package library.sharedpackage.models;

import java.io.Serializable;

public class FileContent implements Serializable {
    private String fileName = "";
    private long fileSize;


    public FileContent(String fileName, long fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    @Override
    public String toString() {
        return "FileContent{ " +
                "fileName = '" + fileName + '\'' +
                ", fileSize = " + fileSize/(1024*1024) +
                "MB }";
    }
}
