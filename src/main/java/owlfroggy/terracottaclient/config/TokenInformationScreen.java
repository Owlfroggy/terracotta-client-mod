package owlfroggy.terracottaclient.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Abilities;
import owlfroggy.terracottaclient.MsgHelper;
import owlfroggy.terracottaclient.TCClient;
import owlfroggy.terracottaclient.api.APIToken;
import owlfroggy.terracottaclient.api.Permission;
import owlfroggy.terracottaclient.api.TokenManager;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Set;

public class TokenInformationScreen extends Screen {
    private APIToken token;
    public final Screen parent;

    public TokenInformationScreen(APIToken token, Screen parent) {
        super(Component.literal("Application Information Screen"));
        this.token = token;
        this.parent = parent;
    }

    @Override
    public void onClose() {
        TCClient.MCI.gui.setScreen(parent);
    }

    @Override
    protected void init() {
        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.back"), button -> onClose())
                .size(200, 20)
                .pos(this.width/2 - 100, this.height - 30)
                .build()
        );
    }

    public static String formatTimestamp(long timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy hh:mm:ss a");

        return Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(
            this.width/2-150,0,
            this.width/2+150,this.height,
            0x70000000
        );

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        Component title = Component.translatable(
            "terracotta-client.permissions.info.title",
            Component.literal(token.getAppName()).withColor(MsgHelper.COLOR.TC_ORANGE)
        );

        int y = 20;

        graphics.centeredText(this.font, title, this.width/2, y, 0xFFFFFFFF);
        y += 20;

        Component firstConnected = Component.translatable(
            "terracotta-client.permissions.info.firstCreated",
            Component.literal(formatTimestamp(token.getCreatedOnTimestamp()))
                .withColor(MsgHelper.COLOR.LIGHT_GRAY)
        ).withColor(TextColor.WHITE);
        graphics.centeredText(this.font, firstConnected, this.width/2, y, -1);
        y += 10;

        Component lastConnected = Component.translatable(
            "terracotta-client.permissions.info.lastConnected",
            Component.literal(formatTimestamp(token.getLastUsedTimestamp()))
                .withColor(MsgHelper.COLOR.LIGHT_GRAY)
        ).withColor(TextColor.WHITE);
        graphics.centeredText(this.font, lastConnected, this.width/2, y, -1);
        y += 10;

        Component expires = Component.translatable(
            "terracotta-client.permissions.info.expires",
            Component.literal(formatTimestamp(token.getExpiresOnTimestamp()))
                .withColor(MsgHelper.COLOR.LIGHT_GRAY)
        ).withColor(TextColor.WHITE);
        graphics.centeredText(this.font, expires, this.width/2, y, -1);
        y += 20;

        Component abilities = Component.translatable(
            "terracotta-client.permissions.info.permissions"
        ).withColor(TextColor.WHITE);
        graphics.centeredText(this.font, abilities, this.width/2, y, -1);
        y += 10;

        Set<Permission> perms = token.getPermissions();
        for (Component c : MsgHelper.textifyPermissionsSeparateComps(perms)) {
            graphics.centeredText(this.font, c, this.width/2, y, -1);
            y += 10;
        }




        graphics.text(TCClient.MCI.font,"hiii!!!!! :D",3,this.height-10,0x20_808080, false);
    }
}
