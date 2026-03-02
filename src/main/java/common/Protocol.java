package common;

public class Protocol {
    // Types de paquets échangés via socket
    public static final String LOGIN       = "LOGIN";
    public static final String LOGOUT      = "LOGOUT";
    public static final String REGISTER    = "REGISTER";
    public static final String SEND_MSG    = "SEND_MSG";
    public static final String RECEIVE_MSG = "RECEIVE_MSG";
    public static final String USER_LIST   = "USER_LIST";
    public static final String STATUS_UPDATE = "STATUS_UPDATE";
    public static final String MSG_STATUS  = "MSG_STATUS";
    public static final String ERROR       = "ERROR";
    public static final String SUCCESS     = "SUCCESS";
    public static final String GET_HISTORY    = "GET_HISTORY";
    public static final String HISTORY        = "HISTORY";

    // Port du serveur
    public static final int PORT = 8080;
    public static final String HOST = "localhost";
}