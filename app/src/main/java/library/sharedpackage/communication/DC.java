package library.sharedpackage.communication;

public enum DC {

    NO_INFO,
    GET_ITEM_LIST,
    ADD_ITEMS,
    REMOVE_ITEMS,
    GET_ITEMS,
    SYNC_LISTS,
    SERVER_CONNECTION_ERROR,
    REMOTE_SERVER_ERROR,
    NO_ERROR,
    GENERAL_ERROR,
    CONNECTION_NOT_SETUP,
    DISCONNECT,
    OK_TO_SEND_FILES,
    CANCEL_OPERATION;


    public static String toReadableString(DC dc){
        return dc.toString().replace('_', ' ');
    }
}