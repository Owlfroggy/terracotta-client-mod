package owlfroggy.terracottaclient;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import owlfroggy.terracottaclient.api.APIErrorCode;
import owlfroggy.terracottaclient.api.APIServer;
import owlfroggy.terracottaclient.api.message.ErrorResponse;
import owlfroggy.terracottaclient.api.message.Request;
import owlfroggy.terracottaclient.api.message.Response;
import owlfroggy.terracottaclient.api.message.impl.*;
import owlfroggy.terracottaclient.codespace.*;
import owlfroggy.terracottaclient.gameinterface.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//    public Vec3d PlotOrigin = new Vec3d(5100,0,4675);
public class DFState extends Manager
implements
    ChatMessageReceiver,
    InvChangeReceiver,
    TeleportReceiver,
    ClientCommandReceiver,
    TickEndReceiver,
    PlotChangeReceiver,
    ModeChangeReceiver,
    ChunkReceiver
{
    public enum Mode {
        SPAWN,
        DEV,
        PLAY,
        BUILD,
    }
    public enum Rank {
        NON,
        NOBLE,
        EMPEROR,
        MYTHIC,
        OVERLORD
    }
    public enum PlotType {
        UNKNOWN,
        BASIC,
        LARGE,
        MASSIVE,
        MEGA,
        WORLD,
    }
    public enum CodespaceCorner {
        FRONT_LEFT,
        FRONT_RIGHT,
        BACK_LEFT,
        BACK_RIGHT
    }
    public enum ScanState {
        NOT_SCANNED,
        SCANNING_BOUNDS,
        SCANNING_CODE,
        SCANNED
    }
    static final Component PREFERENCES_ITEM_NAME = (
        Component.empty().setStyle(Style.EMPTY.withItalic(false))
            .append(Component.literal("◇ ").withColor(0x7F7F2A))
            .append(Component.literal("Preferences").withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" ◇").withColor(0x7F7F2A))
    );
    public static final Component PREFERENCES_ITEM_TOOLTIP = (
        Component.empty().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)
            .append(
                Component.literal("Edit your preferences here.").setStyle(
                    Style.EMPTY
                        .withColor(ChatFormatting.GRAY)
                        .withItalic(false)
                        .withBold(false)
                        .withUnderlined(false)
                        .withStrikethrough(false)
                        .withObfuscated(false)
                )
            )
    );

    public static final HashMap<PlotType, Integer> CODESPACE_Z_SIZES = new HashMap<>(Map.of(
        PlotType.BASIC, 51,
        PlotType.LARGE, 101,
        PlotType.MASSIVE, 301,
        PlotType.MEGA, 300,
        PlotType.WORLD, 300
    ));public static final HashMap<PlotType, Integer> CODESPACE_X_SIZES = new HashMap<>(Map.of(
        PlotType.BASIC, 20,
        PlotType.LARGE, 20,
        PlotType.MASSIVE, 20,
        PlotType.MEGA, 300,
        PlotType.WORLD, 300
    ));
    private final HashMap<String, TemplateType> NAMES_TO_TEMPLATE_TYPES = new HashMap<>(Map.of(
        "PLAYER EVENT", TemplateType.PLAYER_EVENT,
        "ENTITY EVENT", TemplateType.ENTITY_EVENT,
        "GAME EVENT", TemplateType.GAME_EVENT,
        "FUNCTION", TemplateType.FUNCTION,
        "PROCESS", TemplateType.PROCESS
    ));
    private static final Pattern MODE_REGEX = Pattern.compile("You are currently (?:at )?(\\w+)");
    private static final Pattern PLOT_REGEX = Pattern.compile("^→ (.+) \\[(\\d+)");
    private static final double TP_MAGIC_Y_VALUE = 52.15763;
    private static final double TP_MAGIC_Y_VALUE_UNDERGROUND = TP_MAGIC_Y_VALUE - 12;

    public boolean modeRefreshQueued = false;

    public final HashMap<Vec3i, CachedTemplate> templatesByLocation = new HashMap<>();
    public final HashMap<TemplateType, HashMap<String, ArrayList<CachedTemplate>>> templatesByName = new HashMap<>(Map.of(
        TemplateType.PLAYER_EVENT, new HashMap<>(),
        TemplateType.ENTITY_EVENT, new HashMap<>(),
        TemplateType.GAME_EVENT, new HashMap<>(),
        TemplateType.FUNCTION, new HashMap<>(),
        TemplateType.PROCESS, new HashMap<>()
    ));
    public final HashMap<java.lang.Integer, CodespaceFloor> floors = new HashMap<>();

    private final LinkedList<BlockPos> queuedBlockRescans = new LinkedList<>();
    private final LinkedList<ChunkPos> queuedChunkRescans = new LinkedList<>();
    private ChunkPos nextChunkToScan = null;

    private ScanState scanState = ScanState.NOT_SCANNED;
    public void setScanState(ScanState newState) {
        if (scanState != newState) APIServer.broadcastNotification(new ScanStateChangedC2ANotification(newState));
        scanState = newState;
    }
    public ScanState getScanState() { return scanState; }
    public boolean isScanning() { return scanState == ScanState.SCANNING_BOUNDS || scanState == ScanState.SCANNING_CODE; }
    public boolean isScanned() { return scanState == ScanState.SCANNED; }

    private Rank rank = null;
    public Rank getRank() { return rank != null ? rank : Rank.NON; }

    private Vec3 plotOrigin;
    public Vec3 getPlotOrigin() {return plotOrigin;}
    public Vec3i getIntPlotOrigin() {return new Vec3i((int)plotOrigin.x, (int)plotOrigin.y, (int)plotOrigin.z);}

    private Mode mode = Mode.SPAWN;
    public Mode getMode() {return mode;}

    private String plotName = "Spawn";
    public String getPlotName() {return plotName;}

    private int plotId = -1;
    public int getPlotId() {return plotId;}

    private PlotType plotType = PlotType.UNKNOWN;
    public PlotType getPlotType() {return plotType;};

    private boolean doesHaveUndergroundCodespace = false;
    public boolean hasUndergroundCodespace() { return doesHaveUndergroundCodespace; }

    private int totalCodespaceChunks = -1;
    public int getTotalCodespaceChunks() {return totalCodespaceChunks;}

    private CompletableFuture<Optional<Vec3>> ptpFuture;
    private AtomicReference<Vec3> plotScanTargetPos = new AtomicReference<Vec3>(null);

    private boolean hideNextWhois = false;
    public boolean shouldHideNextWhois() { return hideNextWhois; }

    private int t = 0;
    private boolean useWorldPlotScanRoutine = false;

    public boolean hasRank(Rank r) {
        return getRank().ordinal() >= r.ordinal();
    }

    public CodespaceFloor getFloor(int yLevel) {
        if (floors.containsKey(yLevel)) {
            return floors.get(yLevel);
        } else {
            CodespaceFloor floor = new CodespaceFloor(yLevel);
            floors.put(yLevel, floor);
            return floor;
        }
    }

    public ArrayList<CachedTemplate> getTemplatesByName(TemplateType type, String name) {
        return templatesByName.get(type).get(name);
    }

    /**
     * returns null if the template is not in the cache
     * if there are multiple templates with the same identifier, returns the one at `index`
     */
    public CachedTemplate getTemplateByIdentifier(TemplateIdentifier id, int index) {
        ArrayList<CachedTemplate> templates = templatesByName.get(id.type()).get(id.name());
        if (templates == null) return null;

        return templates.get(index);
    }
    /**
     * returns null if the template is not in the cache
     * if there are multiple templates with the same identifier, returns the first one in the ArrayList
     */
    public CachedTemplate getTemplateByIdentifier(TemplateIdentifier id) {
        return getTemplateByIdentifier(id, 0);
    }

    private void addTemplate(TemplateType type, String name, Vec3i plotSpacePos) {
        CachedTemplate template = new CachedTemplate(type,name,plotSpacePos);

        if (templatesByLocation.containsKey(plotSpacePos))
            throw new RuntimeException("Failed to add template "+template.toString()+" because its location is already occupied by "+templatesByLocation.get(plotSpacePos));
        templatesByLocation.put(plotSpacePos,template);

        if (!templatesByName.get(type).containsKey(name))
            templatesByName.get(type).put(name,new ArrayList<>());
        templatesByName.get(type).get(name).add(template);

        getFloor(plotSpacePos.getY()).getRow(plotSpacePos.getX()).addTemplate(template);
    }

    private void removeTemplate(CachedTemplate template) {
        templatesByLocation.remove(template.plotSpacePos);

        templatesByName.get(template.id.type()).get(template.id.name()).remove(template);
        if (templatesByName.get(template.id.type()).get(template.id.name()).isEmpty())
            templatesByName.get(template.id.type()).remove(template.id.name());

        CodespaceFloor floor = getFloor(template.plotSpacePos.getY());
        CodespaceRow row = floor.getRow(template.plotSpacePos.getX());
        row.removeTemplate(template);
    }

    private void clearTemplates() {
        templatesByLocation.clear();
        for (HashMap<String,ArrayList<CachedTemplate>> templateMap : templatesByName.values()) {
            templateMap.clear();
        }
        floors.clear();
    }

    /**
     * @param blockPos in WORLD SPACE!!
     */
    public void processBlockTemplate(BlockPos blockPos) {
        if (TCClient.MCI.level == null) return;

        //remove old template at this block
        Vec3i templatePos = TCClient.DF_STATE.toPlotSpace(blockPos).offset(1,0,0);
        if (templatesByLocation.containsKey(templatePos))
            removeTemplate(templatesByLocation.get(templatePos));


        // check if this block is a sign
        BlockState signBlockState = TCClient.MCI.level.getBlockState(blockPos);
        Identifier signBlockId = BuiltInRegistries.BLOCK.getKey(signBlockState.getBlock());
        if (!signBlockId.equals(Identifier.withDefaultNamespace("oak_wall_sign"))) return;
        BlockEntity blockEntity = TCClient.MCI.level.getBlockEntity(blockPos);
        if (blockEntity instanceof SignBlockEntity sign) {
            // check if this is a header
            Component topLine = sign.getFrontText().getMessage(0,false);
            if (!NAMES_TO_TEMPLATE_TYPES.containsKey(topLine.getString())) return;
            if (!TCClient.DF_STATE.isWorldPosInCodespace(blockPos)) return;
            TemplateType templateType = NAMES_TO_TEMPLATE_TYPES.get(topLine.getString());

            // add template
            String templateName = sign.getFrontText().getMessage(1,false).getString();
            addTemplate(templateType, templateName, templatePos);
        };
    }

    public void processChunkTemplates(ChunkPos chunkPos) {
        int chunkX = chunkPos.x();
        int chunkZ = chunkPos.z();

        Vec3 plotOrigin = TCClient.DF_STATE.getPlotOrigin();
        if (plotOrigin == null) return;
        if (!TCClient.isChunkLoaded(chunkPos)) return;;

        if (queuedChunkRescans.contains(chunkPos)) {
            queuedChunkRescans.remove(chunkPos);
            // scanning progres report
            TCClient.MCI.player.sendOverlayMessage(
                Component.nullToEmpty(
                   " Scanning codespace: " + (int)(100-(double)queuedChunkRescans.size()/TCClient.DF_STATE.getTotalCodespaceChunks()*100) + "%"
                )
            );
        }

        //dont scan chunk if it doesnt touch the codespace
        if (!(
            TCClient.DF_STATE.isWorldPosInCodespace(new Vec3(chunkX*16,0,chunkZ*16)) ||
            TCClient.DF_STATE.isWorldPosInCodespace(new Vec3(chunkX*16+15,0,chunkZ*16)) ||
            TCClient.DF_STATE.isWorldPosInCodespace(new Vec3(chunkX*16,0,chunkZ*16+15)) ||
            TCClient.DF_STATE.isWorldPosInCodespace(new Vec3(chunkX*16+15,0,chunkZ*16+15))
        )) {
            return;
        }

        for (int floor = 1; floor <= 50; floor++) {
            int y = floor*5;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos blockPos = new BlockPos(chunkX*16+x, y, chunkZ*16+z);
                    processBlockTemplate(blockPos);
                }
            }
        }
    }

    public void queueChunkForScan(ChunkPos chunkPos) {
        queuedChunkRescans.add(chunkPos);
    }

    public Vec3 getPlotCorner(CodespaceCorner corner){
        if (plotOrigin == null)
            throw new RuntimeException("Cannot get plot corner because plot origin is unknown");

        Vec3 coords = plotOrigin;
        if (corner == CodespaceCorner.FRONT_RIGHT || corner == CodespaceCorner.BACK_RIGHT) {
            coords = coords.add(-1,0,CODESPACE_Z_SIZES.get(plotType));
        }
        if (corner == CodespaceCorner.BACK_LEFT || corner == CodespaceCorner.BACK_RIGHT) {
            coords = coords.add(-CODESPACE_X_SIZES.get(plotType),0,0);
        }
        return coords;
    }

    public boolean isWorldPosInCodespace(Vec3 worldPos) {
        if (plotOrigin == null)
            throw new RuntimeException("Cannot get plot corner because plot origin is unknown");

        Vec3 minusCorner = getPlotCorner(CodespaceCorner.BACK_LEFT);
        Vec3 plusCorner = getPlotCorner(CodespaceCorner.FRONT_RIGHT);

        if (worldPos.x < minusCorner.x || worldPos.z < minusCorner.z) return false;
        if (worldPos.x > plusCorner.x || worldPos.z > plusCorner.z) return false;

        return true;
    }
    public boolean isWorldPosInCodespace(BlockPos worldPos) {
        return isWorldPosInCodespace(new Vec3((double)worldPos.getX(), (double)worldPos.getY(), (double)worldPos.getZ()));
    }

    public BlockPos clampWorldPosToCodespace(BlockPos worldPos) {
        Vec3 minusCorner = getPlotCorner(CodespaceCorner.BACK_LEFT);
        Vec3 plusCorner = getPlotCorner(CodespaceCorner.FRONT_RIGHT);

        return new BlockPos(
            (int)Math.clamp(worldPos.getX(),minusCorner.x,plusCorner.x),
            worldPos.getY(),
            (int)Math.clamp(worldPos.getZ(),minusCorner.z,plusCorner.z)
        );
    }
    public Vec3 clampWorldPosToCodespace(Vec3 worldPos) {
        Vec3 minusCorner = getPlotCorner(CodespaceCorner.BACK_LEFT);
        Vec3 plusCorner = getPlotCorner(CodespaceCorner.FRONT_RIGHT);

        return new Vec3(
            Math.clamp(worldPos.x(),minusCorner.x,plusCorner.x),
            worldPos.y(),
            Math.clamp(worldPos.z(),minusCorner.z,plusCorner.z)
        );
    }

    public void queueModeRefresh() {
        if (modeRefreshQueued) return;
        modeRefreshQueued = true;
        TCClient.COMMAND_MANAGER.queueCommand("locate");
    }

    public void forceChangeMode(Mode newMode) {
        Mode oldMode = mode;
        mode = newMode;
        APIServer.resolvePendingRequests((Request request) -> {
            if (request instanceof ChangeModeA2CRequest r) {
                if (r.getMode() != mode)
                    return new ErrorResponse(APIErrorCode.GENERIC_ERROR,"Something else changed the mode to %s instead of %s".formatted(mode,r.getMode()));

                return new ChangeModeC2AResponse();
            }
            return null;
        });
        if (oldMode != mode) TCClient.fireModeChangeReceivers(mode);
    }

    /**
     * Updates plot bounds, size, and code contents
     *
     * NOTE: always call this via MCI.execute() or things will break
     */
    public void scanPlot() {
        if (isScanning()) {return;}

        CompletableFuture.runAsync(() -> {
            try {
                if (mode != Mode.DEV)
                    throw new RuntimeException("Player must be in dev mode to scan plot.");

                setScanState(ScanState.SCANNING_BOUNDS);

                PlotType currentSizeGuess = PlotType.BASIC;
                Optional<Vec3> teleportResult = Optional.empty();
                Vec3 plotOriginGuess = null;
                useWorldPlotScanRoutine = false;

                plotScanTargetPos.set(null);
                doesHaveUndergroundCodespace = false;
                clearTemplates();

                // get plot origin
                // tries teleporting to x -1 so that plots without a buildspace (worldplots) still work
                ptpFuture = new CompletableFuture<>();
                TCClient.COMMAND_MANAGER.queueCommand(String.format("ptp -1.0 %s 0.0", TP_MAGIC_Y_VALUE));
                try {
                    Optional<Vec3> result = ptpFuture.get(5, TimeUnit.SECONDS);
                    if (result.isEmpty())
                        throw new RuntimeException("Failed to get plot origin");

                    // if the player was teleported to a hypercube dimension, that means this is a world plot
                    String dimensionNamespace = TCClient.MCI.level.dimension().identifier().getNamespace();
                    if (dimensionNamespace.equals("hypercube")) {
                        currentSizeGuess = PlotType.WORLD;
                        useWorldPlotScanRoutine = true;
                    }
                    // if the player stayed in the codespace, that means the plot origin is now known
                    else {
                        plotOriginGuess = result.get().multiply(1, 0, 1).add(1.0, 0.0, 0.0);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Plot scan failed during origin fetch due to not receiving a teleport response");
                }

                // world plot routine
                if (useWorldPlotScanRoutine) {
                    // teleport to codespace spawn and work out plot origin/underground status from there
                    // it's fine to skip the size loop here since world plots can only ever have one size of codespace
                    ptpFuture = new CompletableFuture<>();
                    TCClient.COMMAND_MANAGER.queueCommand("p s -d");
                    try {
                        Optional<Vec3> result = ptpFuture.get(5, TimeUnit.SECONDS);
                        if (result.isEmpty()) throw new RuntimeException("Failed to get world plot data");
                        Vec3 newPos = result.get();

                        doesHaveUndergroundCodespace = newPos.y == 5;
                        plotOriginGuess = newPos.multiply(1.0,0.0,1.0).add(11.5, 0.0, -10.5);

                        // wait until chunk loads or else things break spectacularly
                        int ticksWaited = 0;
                        while (!TCClient.isChunkLoaded(plotOriginGuess)) {
                            if (ticksWaited > 20*5) throw new RuntimeException("Failed to get world plot data");
                            Thread.sleep(50);
                            ticksWaited++;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Plot scan failed during world plot data gathering due to not receiving a teleport response");
                    }
                }
                // normal plot routine
                else {
                    // test for underground codespace
                    ptpFuture = new CompletableFuture<>();
                    TCClient.COMMAND_MANAGER.queueCommand(String.format("ptp -4 %s 4", TP_MAGIC_Y_VALUE_UNDERGROUND));
                    try {
                        Optional<Vec3> result = ptpFuture.get(5, TimeUnit.SECONDS);
                        if (result.isPresent()) doesHaveUndergroundCodespace = true;
                    } catch (Exception e) {
                        throw new RuntimeException("Plot scan failed during underground codespace check due to not receiving a teleport response");
                    }

                    // get plot size
                    sizeGuessLoop: while (getMode() == Mode.DEV) {
                        Vec3 plotSpacePos;
                        switch (currentSizeGuess) {
                            case PlotType.BASIC -> plotSpacePos = new Vec3(-1, TP_MAGIC_Y_VALUE, 51);
                            case PlotType.LARGE -> plotSpacePos = new Vec3(-1, TP_MAGIC_Y_VALUE, 101);
                            case PlotType.MASSIVE -> plotSpacePos = new Vec3(-300, TP_MAGIC_Y_VALUE, 1);
                            default -> {
                                break sizeGuessLoop;
                            }
                        }

                        plotScanTargetPos.set(plotSpacePos.add(plotOriginGuess));
                        String command = String.format("ptp %s %s %s", plotSpacePos.x, plotSpacePos.y, plotSpacePos.z);

                        TCClient.COMMAND_MANAGER.queueCommand(command);
                        ptpFuture = new CompletableFuture<>();
                        try {
                            teleportResult = ptpFuture.get(5, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new RuntimeException("Plot scan failed during size fetch due to not receiving a teleport response");
                        }

                        // if teleport target was out of bounds, the plot size has been found
                        if (teleportResult.isEmpty()) {
                            break;
                        } else {
                            currentSizeGuess = PlotType.values()[currentSizeGuess.ordinal() + 1];
                        }
                    }
                }


                plotOrigin = plotOriginGuess;
                plotType = currentSizeGuess;

                Vec3 minusCorner = getPlotCorner(CodespaceCorner.BACK_LEFT);
                Vec3 plusCorner = getPlotCorner(CodespaceCorner.FRONT_RIGHT);

                int minusCornerChunkX = (int) Math.floor(minusCorner.x / 16);
                int minusCornerChunkZ = (int) Math.floor(minusCorner.z / 16);
                int plusCornerChunkX = (int) Math.floor(plusCorner.x / 16);
                int plusCornerChunkZ = (int) Math.floor(plusCorner.z / 16);

                // queue every chunk in the codespace for a rescan
                totalCodespaceChunks = 0;
                queuedChunkRescans.clear();
                boolean reverseZ = false;
                for (int cx = minusCornerChunkX; cx <= plusCornerChunkX; cx++) {
                    for (
                        int cz = (reverseZ ? plusCornerChunkZ : minusCornerChunkZ);
                        reverseZ ? (cz >= minusCornerChunkZ) : (cz <= plusCornerChunkZ);
                        cz += (reverseZ ? -1 : 1)
                    ) {
                        queueChunkForScan(new ChunkPos(cx, cz));
                        totalCodespaceChunks++;
                    }
                    reverseZ = !reverseZ;
                }

//                MsgHelper.safeMessage(Component.literal("Detected plot size:" + currentSizeGuess));
//                MsgHelper.safeMessage(Component.literal("Detected plot origin:" + plotOrigin));
                setScanState(ScanState.SCANNING_CODE);
            } catch (Exception e) {
                failScan(e);
            }
        });
    }

    public void failScan(String errorMessage) {
        setScanState(ScanState.NOT_SCANNED);
        queuedChunkRescans.clear();
        if (TCClient.MOVEMENT_MANAGER.getCurrentMovementId().equals("SCAN_QUEUED_CHUNK"))
            TCClient.MOVEMENT_MANAGER.stopMovement();
        if (ptpFuture != null) {
            ptpFuture.cancel(true);
        }
        APIServer.resolvePendingRequests(request -> {
            if (request instanceof RescanPlotA2CRequest r) {
                return new ErrorResponse(APIErrorCode.SCAN_FAILED, errorMessage);
            }
            return null;
        });
    }
    private void failScan(Exception error) {
        TCClient.LOGGER.error("Error while scanning plot", error);
        failScan(error.getMessage());
    }

    public Vec3 toPlotSpace(Vec3 worldSpacePos) {
        return worldSpacePos.subtract(getPlotOrigin());
    }
    public Vec3i toPlotSpace(Vec3i worldSpacePos) {
        return worldSpacePos.subtract(getIntPlotOrigin());
    }
    public Vec3i toPlotSpace(BlockPos worldSpacePos) {
        return worldSpacePos.subtract(getIntPlotOrigin());
    }

    public Vec3 toWorldSpace(Vec3 plotSpacePos) {
        return plotSpacePos.add(getPlotOrigin());
    }
    public Vec3i toWorldSpace(Vec3i plotSpacePos) {
        return plotSpacePos.offset(getIntPlotOrigin());
    }
    public Vec3i toWorldSpace(BlockPos plotSpacePos) {
        return plotSpacePos.offset(getIntPlotOrigin());
    }

    public void onTeleported(Vec3 newPos, Vec3 oldPos) {
        String dimensionNamespace = TCClient.MCI.level.dimension().identifier().getNamespace();
        if (
            ptpFuture != null && !ptpFuture.isDone()
            && (
                newPos.y == TP_MAGIC_Y_VALUE
                || newPos.y == TP_MAGIC_Y_VALUE_UNDERGROUND
                || (useWorldPlotScanRoutine && dimensionNamespace.equals("minecraft") && (newPos.y == 5 || newPos.y == 50))
            )
            && (plotScanTargetPos.get() == null || newPos.closerThan(plotScanTargetPos.get(),0.01))
        ) {
            ptpFuture.complete(Optional.of(newPos));
        }
//        TCClient.MCI.player.sendMessage(Text.literal(newPos.toString()),false);
    }

    public void onSlotChanged(int slot, ItemStack newItem) {
        //check for preferences item entering inventory as spawn indicator
        if (
            mode != Mode.SPAWN &&
            slot == 37 &&
            newItem.getItem() == Items.COMPARATOR &&
            newItem.getComponents().has(DataComponents.CUSTOM_DATA) &&
            Objects.equals(PREFERENCES_ITEM_NAME, newItem.getCustomName()) &&
            Objects.equals(PREFERENCES_ITEM_TOOLTIP, newItem.getTooltipLines(Item.TooltipContext.EMPTY, TCClient.MCI.player, TooltipFlag.NORMAL).get(1))
        ) {
            CompoundTag customData = newItem.get(DataComponents.CUSTOM_DATA).copyTag();
            Optional<CompoundTag> publicBukkitValues = customData.getCompound("PublicBukkitValues");
            if (
                publicBukkitValues.isPresent() &&
                publicBukkitValues.get().getString("hypercube:item_instance").isPresent()
            ) {
                queueModeRefresh();
            }
        }
    }

    public void onChatMessage(Component message) {
        String messageStr = message.getString();
        String[] messageStrLines = messageStr.split("\n");

        if (
            messageStr.equals("» You are now in dev mode.") ||
            messageStr.equals("» You are now in build mode.") ||
            messageStr.startsWith("» Joined game: ")
        ) {
            queueModeRefresh();
        }

        if (ptpFuture != null && !ptpFuture.isDone() && MsgHelper.isMessageOutOfBoundsError(message)) {
            ptpFuture.complete(Optional.empty());
        }

        if (MsgHelper.isMessageModeChangeFailure(message)) {
            APIServer.resolvePendingRequests(r -> {
                if (!(r instanceof ChangeModeA2CRequest)) return null;
                if (message.equals(MsgHelper.MUST_BE_ON_PLOT_TEXT)) {
                    return new ErrorResponse(APIErrorCode.AT_SPAWN, "Player is at spawn");
                } else {
                    return new ErrorResponse(APIErrorCode.NO_PERMISSION, "Player does not have permissions for this plot");
                }
            });
        }

        locateParser: if (MsgHelper.isMessageLocateResult(message)) {
            Matcher modeMatcher = MODE_REGEX.matcher(messageStrLines[1]);
            if (!modeMatcher.find()) break locateParser;

            Mode newMode = Mode.SPAWN;
            switch (modeMatcher.group(1)) {
                case "coding" -> newMode = Mode.DEV;
                case "building" -> newMode = Mode.BUILD;
                case "playing" -> newMode = Mode.PLAY;
                case "spawn" -> newMode = Mode.SPAWN;
            }

            forceChangeMode(newMode);

            modeRefreshQueued = false;

            int oldPlotId = plotId;
            if (mode == Mode.SPAWN) {
                plotId = -1;
                plotName = "Spawn";
                plotOrigin = null;
                plotType = PlotType.UNKNOWN;
                doesHaveUndergroundCodespace = false;
            } else {
                Matcher plotMatcher = PLOT_REGEX.matcher(messageStrLines[3]);
                if (!plotMatcher.find()) break locateParser;

                plotName = plotMatcher.group(1);
                plotId = Integer.parseInt(plotMatcher.group(2));
            }

            if (oldPlotId != plotId) {
                doesHaveUndergroundCodespace = false;
                APIServer.broadcastNotification(new PlotChangedC2ANotification(plotId,plotName));
                TCClient.firePlotChangeReceivers(plotId,mode);
            }
        }

        whoisParser: if (MsgHelper.isMessageWhoisResult(message)) {
            String playerName = TCClient.MCI.player.getName().getString();
            if (!messageStrLines[1].startsWith("Profile of "+playerName+ " "))
                break whoisParser;

            hideNextWhois = false;
            String rankLine = messageStrLines[3];
            rank = Rank.NON;
            if (rankLine.contains("Overlord")) rank = Rank.OVERLORD;
            else if (rankLine.contains("Mythic")) rank = Rank.MYTHIC;
            else if (rankLine.contains("Emperor")) rank = Rank.EMPEROR;
            else if (rankLine.contains("Noble")) rank = Rank.NOBLE;

            TCClient.LOGGER.info("Detected rank: {}",rank);
        }
    }

    public void onClientSendCommand(String command) {
        // update client state when adding or removing underground codespace
        if (command.startsWith("plot")) {
            if (command.equalsIgnoreCase("plot codespace underground create")) {
                doesHaveUndergroundCodespace = true;
                return;
            }
            if (command.equalsIgnoreCase("plot codespace underground remove")) {
                doesHaveUndergroundCodespace = false;
                return;
            }
        }
    }

    public void onTickEnd(Minecraft client) {
        t++;
        //=- figure out rank -=\\
        if (rank == null && t % 20 == 0 && !hideNextWhois && mode != Mode.PLAY && !TCClient.loadedChunks.isEmpty()) {
            hideNextWhois = true;
            TCClient.COMMAND_MANAGER.queueCommand("whois");
        }

        //=- codespace scanning -=\\
        // scan queued blocks
        while (!queuedBlockRescans.isEmpty()) {
            BlockPos next = queuedBlockRescans.poll();
            if (next == null) {continue;}
            processBlockTemplate(next);
        }

        // rescan queued chunks if they are loaded
        for (ChunkPos chunkPos : new ArrayList<>(queuedChunkRescans)) {
            if (TCClient.isChunkLoaded(chunkPos)) processChunkTemplates(chunkPos);
        }

        // move to chunks if they need to be scanned
        if (
            !TCClient.MOVEMENT_MANAGER.isMoving() &&
            !queuedChunkRescans.isEmpty() &&
            (nextChunkToScan == null || !TCClient.MOVEMENT_MANAGER.getCurrentMovementId().equals("SCAN_QUEUED_CHUNK"))
        ) {
            nextChunkToScan = queuedChunkRescans.peek();

            TCClient.MOVEMENT_MANAGER.setMovementDestination(
                TCClient.DF_STATE.toPlotSpace(
                    TCClient.DF_STATE.clampWorldPosToCodespace(new Vec3(nextChunkToScan.x()*16+16,52,nextChunkToScan.z()*16+16))
                ),
                "SCAN_QUEUED_CHUNK"
            );
        }

        if (nextChunkToScan != null && !queuedChunkRescans.contains(nextChunkToScan)) {
            nextChunkToScan = null;
            TCClient.MOVEMENT_MANAGER.stopMovement("SCAN_QUEUED_CHUNK");
        }

        if (nextChunkToScan == null && queuedChunkRescans.isEmpty() && scanState == ScanState.SCANNING_CODE) {
            MsgHelper.safeTCMessage(Component.literal("Plot scan complete!"));
            setScanState(ScanState.SCANNED);
            APIServer.resolvePendingRequests(request -> {
                if (request instanceof RescanPlotA2CRequest r) {
                    return new RescanPlotC2AResponse();
                }
                return null;
            });
        }
    }

    @Override
    public void onPlotChanged(int plotId, DFState.Mode mode) {
        clearTemplates();
        queuedChunkRescans.clear();
        queuedBlockRescans.clear();
        if (isScanning()) {
            failScan("Plot was left mid-scan.");
        } else {
            setScanState(ScanState.NOT_SCANNED);
        }
    }

    @Override
    public void onModeChanged(Mode newMode) {
        if (newMode != Mode.DEV && isScanning())
            failScan("Player left dev mode mid-scan.");
    }

    @Override
    public void onChunkLoad(ChunkPos chunkPos) {
        if (TCClient.DF_STATE.getMode() != DFState.Mode.DEV) return;
        processChunkTemplates(chunkPos);
    }

    @Override
    public void onChunkDelta(ClientboundSectionBlocksUpdatePacket packet) {
        packet.runUpdates((blockPos, blockState) -> {
            Vec3 plotOrigin = TCClient.DF_STATE.getPlotOrigin();
            if (plotOrigin == null) return;

            // rescanning
            BlockPos immutablePos = blockPos.immutable();
            queuedBlockRescans.add(immutablePos);
        });
    }

    @Override
    public void onBlockEntityUpdate(ClientboundBlockEntityDataPacket packet) {
        Vec3 plotOrigin = TCClient.DF_STATE.getPlotOrigin();
        if (plotOrigin == null) return;
        queuedBlockRescans.add(packet.getPos().immutable());
    }
}
