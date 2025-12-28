package owlfroggy.terracottaclient.api.message;

import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.APIConnectionHandler;
import owlfroggy.terracottaclient.api.RequestMethod;

public class Request extends Message {
    private RequestMethod method;
    private APIConnectionHandler handler;

    public RequestMethod getMethod() { return method; }
    public APIConnectionHandler getHandler() { return handler; }

    public void setHandler(APIConnectionHandler handler) { this.handler = handler; }

    public Request(RequestMethod method) {
        super(MessageType.REQUEST);
        this.method = method;
    }

    public static Request parse(JsonObject message, JsonObject data) {
        return new Request(RequestMethod.UNKNOWN);
    }
}
