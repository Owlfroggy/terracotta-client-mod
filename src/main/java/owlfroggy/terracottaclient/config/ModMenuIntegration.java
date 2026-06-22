package owlfroggy.terracottaclient.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import owlfroggy.terracottaclient.api.TokenManager;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new TokenManagementScreen(TokenManager.getAllTokens(), parent);
    }
}