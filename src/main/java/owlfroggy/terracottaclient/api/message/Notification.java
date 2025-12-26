package owlfroggy.terracottaclient.api.message;

import owlfroggy.terracottaclient.api.NotificationMethod;

public class Notification extends Message {
    NotificationMethod method;

    public Notification(NotificationMethod method) {
        super(MessageType.NOTIFICATION);
        this.method = method;
    }
}
