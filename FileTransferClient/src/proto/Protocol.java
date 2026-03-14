package proto;

public final class Protocol {
    private Protocol(){}

    // client -> server
    public static final int LOGIN = 1;
    public static final int LIST_USERS = 2;
    public static final int SEND_FILE = 3;
    public static final int PULL_PENDING = 4;
    public static final int HISTORY = 6;
    public static final int LOGOUT = 9;

    // server -> client
    public static final int LOGIN_RESULT = 11;
    public static final int USERS_RESULT = 12;
    public static final int SEND_RESULT = 13;
    public static final int PENDING_RESULT = 14;
    public static final int HISTORY_RESULT = 16;

    // server push to client
    public static final int FILE_INCOMING = 100;

    public static final int ERROR = 500;
}