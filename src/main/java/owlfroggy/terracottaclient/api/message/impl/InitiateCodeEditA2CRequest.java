package owlfroggy.terracottaclient.api.message.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.codespacemanager.TemplateIdentifier;
import owlfroggy.terracottaclient.codespacemanager.TemplateType;

public class InitiateCodeEditA2CRequest extends Request {
    private String[] placeTemplates;
    private TemplateIdentifier[] breakTemplates;

    public String[] getPlaceTemplates() { return placeTemplates; }
    public TemplateIdentifier[] getBreakTemplates() { return breakTemplates; }

    public InitiateCodeEditA2CRequest(String[] placeTemplates, TemplateIdentifier[] breakTemplates) {
        super(RequestMethod.INITIATE_CODE_EDIT);
        this.placeTemplates = placeTemplates;
        this.breakTemplates = breakTemplates;
    }

    public static Request parse(JsonObject message, JsonObject data) {
        // todo: better error handling
        String[] updateTemplates = data.getAsJsonArray("place_templates")
            .asList().stream().map(JsonElement::getAsString).toArray(String[]::new);

        TemplateIdentifier[] breakTemplates = data.getAsJsonArray("break_templates")
            .asList().stream().map(
                (JsonElement elm) -> new TemplateIdentifier(
                    TemplateType.valueOf(elm.getAsJsonObject().get("type").getAsString()),
                    elm.getAsJsonObject().get("name").getAsString()
                )
            ).toArray(TemplateIdentifier[]::new);

        return new InitiateCodeEditA2CRequest(updateTemplates, breakTemplates);
    }
}