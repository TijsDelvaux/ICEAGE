
/**
 * A server sends messages to the client in this format:
 *      code + ":" + message
 * where the code is one of the following:
 */
public enum MsgClient {
    DEFAULT,                //:msg
    TOAST,                  //:msg
    CONFIRM_REGISTRATION,   //:msg
    DECLINE_REGISTRATION,   //:msg
    CONFIRM_PICKUP,         //:msg
    DECLINE_PICKUP,         //:msg
    TEAMMATE_PICKUP,        //:msg
    DECLINE_ACORN,          //:msg
    CONFIRM_ACORN,          //:msg
    YOU_OWN_THIS_ACORN,     //:msg
    TRAP_LOSS,              //:cost:msg
    TRAP_REWARD,            //:reward:msg
    TEAMMATE_TRAP_LOSS,     //:cost:msg
    TEAMMATE_TRAP_REWARD,   //:reward:msg
    CONFIRM_PLACEMENT_TRAP, //:cost:msg
    DECLINE_PLACEMENT_TRAP, //:msg
    UPDATE_EXCLUDE_LIST,     //TODO
}
