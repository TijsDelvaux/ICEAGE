package server;

/**
 * A server sends messages to the client in this format:
 *      code + ":" + message
 * where the code is one of the following:
 */
public enum MsgClient {
    DEFAULT,
    TOAST,
    CONFIRM_REGISTRATION,
    DECLINE_REGISTRATION,
    CONFIRM_PICKUP,
    DECLINE_PICKUP,
    TEAMMATE_PICKUP,
    DECLINE_ACORN,
    CONFIRM_ACORN,
    UPDATE_EXCLUDE_LIST
}
