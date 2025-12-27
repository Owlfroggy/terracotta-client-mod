package owlfroggy.terracottaclient.api.message.impl;

import owlfroggy.terracottaclient.api.RequestMethod;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.codespacemanager.TemplateIdentifier;

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
}
