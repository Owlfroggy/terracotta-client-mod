package owlfroggy.terracottaclient.codespacemanager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import owlfroggy.terracottaclient.TCClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public class TemplateDataUtils {
    // shoutout to chatgpt for this banger of a function
    // i sure hope it doesn't have a horrifying security flaw that i misesed
    public static JsonObject parseTemplateData(String rawTemplateData) throws InvalidTemplateException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(rawTemplateData));
            GZIPInputStream gzip = new GZIPInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            gzip.transferTo(baos);
            String jsonString = baos.toString(StandardCharsets.UTF_8);
            return JsonParser.parseString(jsonString).getAsJsonObject();
        } catch (Exception e) {
            throw new InvalidTemplateException("Raw template data is malformed ("+e.getMessage()+")");
        }
    }

    public static TemplateIdentifier getIdentifier(JsonObject templateData) throws InvalidTemplateException {
        JsonObject firstBlock = templateData.getAsJsonArray("blocks").get(0).getAsJsonObject();
        String block = firstBlock.getAsJsonPrimitive("block").getAsString();
        String name = firstBlock.getAsJsonPrimitive((Objects.equals(block, "func") || Objects.equals(block, "process")) ? "data" : "action").getAsString();
        TemplateType type = null;
        switch (block) {
            case "func" -> type = TemplateType.FUNCTION;
            case "process" -> type = TemplateType.PROCESS;
            case "event" -> type = TemplateType.PLAYER_EVENT;
            case "entity_event" -> type = TemplateType.ENTITY_EVENT;
        }

        if (type == null)
            throw new InvalidTemplateException("Invalid header block '"+block+"'");

        return new TemplateIdentifier(type,name);
    }
}
