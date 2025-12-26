package owlfroggy.terracottaclient.api.message.impl;

import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

import java.util.HashSet;

public class RequestTokenA2CRequest extends Request {
    private String appName;
    private HashSet<Permission> permissions;

    public String getAppName() { return appName; }
    public HashSet<Permission> getPermissions() { return permissions; }

    public RequestTokenA2CRequest(String appName, HashSet<Permission> permissions) {
        super(RequestMethod.REQUEST_TOKEN);
        this.appName = appName;
        this.permissions = permissions;
    }
}
