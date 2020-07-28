package library.sharedpackage.communication;

import java.io.Serializable;

public class DataCarrier <T extends Serializable> implements Serializable {

    private DC info;
    private T data;
    private boolean request;
    public static final boolean REQUEST = true;
    public static final boolean RESPONSE = false;

    private DataCarrier(){

    }

    public DataCarrier(boolean request){
        this(request, DC.NO_INFO);
    }

    public DataCarrier(boolean request, DC info){
        this(request, info, null);
    }

    public DataCarrier(boolean request, T data){
        this(request, DC.NO_INFO, data);
    }

    public DataCarrier(boolean request, DC info, T data){
        this.info = info;
        this.data = data;
        this.request = request;
    }

    public DC getInfo() {
        return info;
    }

    public void setInfo(DC info) {
        this.info = info;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isRequest() {
        return request;
    }

    public void setRequest(boolean request) {
        this.request = request;
    }
}
