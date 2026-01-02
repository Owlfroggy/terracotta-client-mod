package owlfroggy.terracottaclient.api;

import owlfroggy.terracottaclient.api.message.Request;

public class MessageParsingException extends RuntimeException {
    private final Request reducedRequest;
    public Request getReducedRequest() { return reducedRequest; }
    public MessageParsingException(String message, Request reducedRequest) {
        super(message);
        this.reducedRequest = reducedRequest;
    }
    public MessageParsingException(Exception e, Request reducedRequest) {
        super(e);
        this.reducedRequest = reducedRequest;
    }
}
