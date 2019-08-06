package library.sharedpackage.communication;

public enum DC {

    ADD_ITEMS,
    CANCEL_OPERATION,
    CONNECTION_NOT_SETUP(true),
    DISCONNECT,
    GENERAL_ERROR(true),
    GET_ITEMS,
    GET_ITEM_LIST,
    NO_ERROR,
    NO_INFO,
    OK_TO_SEND_FILES,
    REMOTE_SERVER_ERROR(true),
    REMOVE_ITEMS,
    SERVER_CONNECTION_ERROR(true),
    SYNC_LISTS,
    FINISHED_SENDING_FILES;

    public final boolean IsErrorCode;

    DC() {
        IsErrorCode = false;
    }

    DC(boolean IsErrorCode) {
        this.IsErrorCode = IsErrorCode;
    }


    public String toReadableString() {
        return this.toString().replace('_', ' ');
    }
}