package owlfroggy.terracottaclient.itemlibrary;

import owlfroggy.terracottaclient.api.APIErrorCode;
import owlfroggy.terracottaclient.api.APIException;

public class InvalidNBTException extends APIException {
    public InvalidNBTException(String message) {
        super(message, APIErrorCode.INVALID_ITEM_DATA);
    }
}
