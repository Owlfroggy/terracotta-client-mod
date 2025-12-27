package owlfroggy.terracottaclient.api;

import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.message.ErrorResponse;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.api.message.Response;
import owlfroggy.terracottaclient.api.message.impl.ProvideTokenA2CRequest;
import owlfroggy.terracottaclient.api.message.impl.RequestTokenA2CRequest;
import owlfroggy.terracottaclient.api.message.impl.RequestTokenC2AResponse;

import java.time.Instant;
import java.util.HashSet;

public class APIConnectionHandler {
    private final char[] TOKEN_CHARACTERS = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','0','1','2','3','4','5','6','7','8','9'};
    private int TOKEN_LENGTH = 32;

    private WebSocket connection;
    private ClientHandshake handshake;
    private Thread homeThread;

    private final int id;

    // auth-related
    private String appName = "<uninitialized app>";
    private APIToken token = null;
    private Request authenticationRequest = null;
    private HashSet<Permission> permissions;


    APIConnectionHandler(WebSocket connection, ClientHandshake handshake, int id) {
        this.id = id;
        this.connection = connection;
        this.handshake = handshake;
        homeThread = Thread.currentThread();
    }

    private String generateTokenString() {
        StringBuilder t = new StringBuilder();
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            t.append(TOKEN_CHARACTERS[(int) (Math.random() * TOKEN_CHARACTERS.length)]);
        }

        return t.toString();
    }

    private void respond(Request request, Response response) {
        response.setId(request.getId());
        String json = response.serialize();
        connection.send(json);
    }

    public int getId() {
        return id;
    }

    public void allowAuthentication() {
        if (authenticationRequest instanceof RequestTokenA2CRequest r) {
            String tokenString = generateTokenString();
            permissions = r.getPermissions();
            token = TCClient.API_SERVER.registerNewToken(tokenString, r.getAppName(), permissions);
            respond(r,new RequestTokenC2AResponse(tokenString));
            TCClient.MCI.player.sendMessage(Text.literal("authed "+appName).withColor(Colors.GREEN),false);
            // TODO: save token somewhere (include its permissions, name, and expire date)
        }
        if (authenticationRequest instanceof ProvideTokenA2CRequest) {
            // TODO: implement this :)
        }
    }

    public void denyAuthentication() {
        respond(authenticationRequest,new ErrorResponse(
            APIErrorCode.AUTHENTICATION_DENIED,
            "Authentication was denied from within Minecraft."
        ));
        // TODO: send a different error message for invalid token
        authenticationRequest = null;
    }

    public void onRequest(Request request) {
        if (request instanceof RequestTokenA2CRequest r) {
            if (appName.equals("<uninitialized app>")) appName = r.getAppName();
            if (token != null) {
                respond(r,new ErrorResponse(
                    APIErrorCode.TOKEN_ALREADY_PROVIDED,
                    "Applications cannot use more than one token."
                ));
                return;
            }
            if (authenticationRequest != null) {
                respond(r,new ErrorResponse(
                    APIErrorCode.ALREADY_AUTHENTICATING,
                    "An authentication attempt is already in progress."
                ));
                return;
            }

            authenticationRequest = r;

            TCClient.MCI.player.sendMessage(Text.literal(
                r.getAppName() + "is tryin  to connect w/ permissions: " + r.getPermissions().toString()
                + "     & appid = " + getId()
            ), false);
        }
    }

    public void sendNotification() {

    }
}
