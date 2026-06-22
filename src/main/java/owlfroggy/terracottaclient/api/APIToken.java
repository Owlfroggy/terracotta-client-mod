package owlfroggy.terracottaclient.api;

import java.time.Instant;
import java.util.Set;

public class APIToken {
    private final String token;
    private final String appName;
    private final Set<Permission> permissions;
    private final long createdOnTimestamp;
    private final long expiresOnTimestamp;
    private long lastUsedTimestamp;

    public String getToken() { return token; }
    public String getAppName() { return appName; }
    public Set<Permission> getPermissions() { return permissions; }
    public long getCreatedOnTimestamp() { return createdOnTimestamp; }
    public long getExpiresOnTimestamp() { return expiresOnTimestamp; }
    public long getLastUsedTimestamp() { return lastUsedTimestamp; }

    /** Sets `lastUsedTimestamp` to the current time. Does NOT automatically re-write token file. */
    public void bumpLastUsedTimestamp() { this.lastUsedTimestamp = Instant.now().getEpochSecond(); }

    public APIToken(String token, String appName, Set<Permission> permissions, long createdOnTimestamp, long expiresOnTimestamp, long lastUsedTimestamp) {
        this.token = token;
        this.appName = appName;
        this.permissions = permissions;
        this.createdOnTimestamp = createdOnTimestamp;
        this.expiresOnTimestamp = expiresOnTimestamp;
        this.lastUsedTimestamp = lastUsedTimestamp;
    }
}
