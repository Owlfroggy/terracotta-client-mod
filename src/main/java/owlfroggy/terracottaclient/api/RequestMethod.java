package owlfroggy.terracottaclient.api;

public enum RequestMethod {
    UNKNOWN,
    REQUEST_TOKEN,
    PROVIDE_TOKEN,
    DISPOSE_TOKEN,
    INITIATE_CODE_EDIT,
    CHANGE_MODE,
    START_EDITING_ITEM,
    STOP_EDITING_ITEM,
    RENDER_ITEM,
    GIVE_ITEM,
    GET_INVENTORY,
    GET_CODE_VALUES,
    RESCAN_PLOT,
}
