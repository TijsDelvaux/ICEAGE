package server;

/**
 * A client sends messages to the server in this format:
 *      userName + ":" + teamName + ":" + code + ":" + message
 * where the code is one of the following:
 */
public enum MsgServer {
    DEFAULT,
    ACORN_REQUEST,
    ACORN_PICKUP,
    ENTER_ZONE,
    PLACE_TRAP
}
