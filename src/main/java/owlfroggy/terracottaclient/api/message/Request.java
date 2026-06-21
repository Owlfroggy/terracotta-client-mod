package owlfroggy.terracottaclient.api.message;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.APIConnectionHandler;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;

public class Request extends Message {
    private RequestMethod method;
    private APIConnectionHandler handler;
    private boolean respondedTo = false;

    public RequestMethod getMethod() { return method; }
    public APIConnectionHandler getHandler() { return handler; }

    public void setHandler(APIConnectionHandler handler) { this.handler = handler; }
    public void markAsRespondedTo() { respondedTo = true; }
    public boolean hasBeenRespondedTo() { return respondedTo; }

    public Request(RequestMethod method, Permission requiredPermission) {
        super(MessageType.REQUEST, requiredPermission);
        this.method = method;
    }
    public Request(RequestMethod method) { this(method, null); }

    public static Request parse(JsonObject message, JsonObject data) {
        return new Request(RequestMethod.UNKNOWN, null);
    }
}
