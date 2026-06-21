package owlfroggy.terracottaclient.api;

import owlfroggy.terracottaclient.api.message.Request;

public class MessageParsingException extends APIException {
    private final Request reducedRequest;
    public Request getReducedRequest() { return reducedRequest; }
    public MessageParsingException(String message, Request reducedRequest) {
        super(message, APIErrorCode.MALFORMED_MESSAGE);
        this.reducedRequest = reducedRequest;
    }
    public MessageParsingException(Exception e, Request reducedRequest) {
        super(e, APIErrorCode.MALFORMED_MESSAGE);
        this.reducedRequest = reducedRequest;
    }
}
