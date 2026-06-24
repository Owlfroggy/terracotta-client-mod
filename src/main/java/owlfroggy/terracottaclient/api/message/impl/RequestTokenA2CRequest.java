package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;

import java.util.HashSet;

public class RequestTokenA2CRequest extends Request {
    public enum Lifetime {
        MONTH,
        WEEK,
        DAY,
        SESSION,
    }

    private String appName;
    private HashSet<Permission> permissions;
    private Lifetime lifetime;

    public String getAppName() { return appName; }
    public HashSet<Permission> getPermissions() { return permissions; }
    public Lifetime getLifetime() { return lifetime; }

    public RequestTokenA2CRequest(String appName, HashSet<Permission> permissions, Lifetime lifetime) {
        super(RequestMethod.REQUEST_TOKEN);
        this.appName = appName;
        this.permissions = permissions;
        this.lifetime = lifetime;
    }

    public static RequestTokenA2CRequest parse(JsonObject message, JsonObject data) {
        HashSet<Permission> permissions = new HashSet<>();
        for (JsonElement permission : data.get("permissions").getAsJsonArray()) {
            Permission p;
            try { p = Permission.valueOf(permission.getAsString()); }
            catch (Exception e) { throw new RuntimeException("invalid_permission"); }
            permissions.add(p);
        }

        Lifetime lifetime;
        try {
            lifetime = Enum.valueOf(Lifetime.class, data.get("lifetime").getAsString());
        } catch (Exception e) {
            lifetime = Lifetime.SESSION;
        }

        return new RequestTokenA2CRequest(data.get("app_name").getAsString(), permissions, lifetime);
    }

}
