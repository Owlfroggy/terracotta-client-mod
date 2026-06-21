package owlfroggy.terracottaclient.api;

public abstract class APIException extends RuntimeException {
    public final APIErrorCode code;
    public APIException(String message, APIErrorCode code) {
        super(message);
        this.code = code;
    }
    public APIException(Exception exception, APIErrorCode code) {
        super(exception);
        this.code = code;
    }
}
