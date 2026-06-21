package owlfroggy.terracottaclient.itemlibrary;

import owlfroggy.terracottaclient.api.APIErrorCode;
import owlfroggy.terracottaclient.api.APIException;

public class NoSpaceException extends APIException {
    public NoSpaceException(String message) {
        super(message, APIErrorCode.NO_SPACE);
    }
}
