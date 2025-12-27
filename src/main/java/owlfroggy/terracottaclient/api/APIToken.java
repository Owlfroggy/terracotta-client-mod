package owlfroggy.terracottaclient.api;

import java.util.Set;

public class APIToken {
    private final String token;
    private final String appName;
    private final Set<Permission> permissions;
    private final long createdOnTimestamp;

    public String getToken() { return token; }
    public String getAppName() { return appName; }
    public Set<Permission> getPermissions() { return permissions; }
    public long getCreatedOnTimestamp() { return createdOnTimestamp; }

    public APIToken(String token, String appName, Set<Permission> permissions, long createdOnTimestamp) {
        this.token = token;
        this.appName = appName;
        this.permissions = permissions;
        this.createdOnTimestamp = createdOnTimestamp;
    }
}
