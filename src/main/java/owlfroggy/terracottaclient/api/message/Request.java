package owlfroggy.terracottaclient.api.message;

import owlfroggy.terracottaclient.api.RequestMethod;

public class Request extends Message {
    private RequestMethod method;

    public RequestMethod getMethod() { return method; }

    public Request(RequestMethod method) {
        super(MessageType.REQUEST);
        this.method = method;
    }
}
