package owlfroggy.terracottaclient;

import com.google.common.cache.Cache;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.loot.condition.TimeCheckLootCondition;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import owlfroggy.terracottaclient.codespacemanager.CachedTemplate;
import owlfroggy.terracottaclient.codespacemanager.CodespaceFloor;
import owlfroggy.terracottaclient.codespacemanager.CodespaceRow;
import owlfroggy.terracottaclient.codespacemanager.TemplateType;
import owlfroggy.terracottaclient.gameinterface.ChunkReceiver;
import owlfroggy.terracottaclient.gameinterface.ModeChangeReceiver;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;

import java.util.*;

public class CodespaceManager extends Manager implements ChunkReceiver, ModeChangeReceiver, TickEndReceiver {
    private final HashMap<String, TemplateType> NAMES_TO_TEMPLATE_TYPES = new HashMap<>(Map.of(
        "PLAYER EVENT", TemplateType.PLAYER_EVENT,
        "ENTITY EVENT", TemplateType.ENTITY_EVENT,
        "FUNCTION", TemplateType.FUNCTION,
        "PROCESS", TemplateType.PROCESS
    ));

    public final HashMap<Vec3i, CachedTemplate> templatesByLocation = new HashMap<>();
    public final HashMap<TemplateType, HashMap<String, ArrayList<CachedTemplate>>> templatesByName = new HashMap<>(Map.of(
        TemplateType.PLAYER_EVENT, new HashMap<>(),
        TemplateType.ENTITY_EVENT, new HashMap<>(),
        TemplateType.FUNCTION, new HashMap<>(),
        TemplateType.PROCESS, new HashMap<>()
    ));
    public final HashMap<java.lang.Integer, CodespaceFloor> floors = new HashMap<>();

    private final LinkedList<BlockPos> queuedBlockRescans = new LinkedList<>();
    private final LinkedList<ChunkPos> queuedChunkRescans = new LinkedList<>();

    private ChunkPos nextChunkToScan = null;

    public CodespaceFloor getFloor(int yLevel) {
        if (floors.containsKey(yLevel)) {
            return floors.get(yLevel);
        } else {
            CodespaceFloor floor = new CodespaceFloor(yLevel);
            floors.put(yLevel, floor);
            return floor;
        }
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

        templatesByName.get(template.type).get(template.name).remove(template);
        if (templatesByName.get(template.type).get(template.name).isEmpty())
            templatesByName.get(template.type).remove(template.name);

        CodespaceFloor floor = getFloor(template.plotSpacePos.getY());
        CodespaceRow row = floor.getRow(template.plotSpacePos.getX());
        row.removeTemplate(template);
        if (row.templates.isEmpty()) {
            floor.rows.remove(row.xPos, row);
            if (floor.rows.isEmpty()) {
                floors.remove(floor.yLevel, floor);
            }
        }
    }

    private void clearTemplates() {
        templatesByLocation.clear();
        for (HashMap<String,ArrayList<CachedTemplate>> templateMap : templatesByName.values()) {
            templateMap.clear();
        }
        floors.clear();
        queuedChunkRescans.clear();
    }

    /**
     * @param blockPos in WORLD SPACE!!
     */
    public void processBlockTemplate(BlockPos blockPos) {
        //remove old template at this block
        Vec3i templatePos = TCClient.DF_STATE.toPlotSpace(blockPos).add(1,0,0);
        if (templatesByLocation.containsKey(templatePos))
            removeTemplate(templatesByLocation.get(templatePos));


        // check if this block is a sign
        BlockState signBlockState = TCClient.MCI.world.getBlockState(blockPos);
        Identifier signBlockId = Registries.BLOCK.getId(signBlockState.getBlock());
        if (!signBlockId.equals(Identifier.ofVanilla("oak_wall_sign"))) return;
        BlockEntity blockEntity = TCClient.MCI.world.getBlockEntity(blockPos);
        if (blockEntity instanceof SignBlockEntity sign) {
            // check if this is a header
            Text topLine = sign.getFrontText().getMessage(0,false);
            if (!NAMES_TO_TEMPLATE_TYPES.containsKey(topLine.getString())) return;
            if (!TCClient.DF_STATE.isWorldPosInCodespace(blockPos)) return;
            TemplateType templateType = NAMES_TO_TEMPLATE_TYPES.get(topLine.getString());

            // add template
            String templateName = sign.getFrontText().getMessage(1,false).getString();
            addTemplate(templateType, templateName, templatePos);
        };
    }

    public void scanChunk(ChunkPos chunkPos) {
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

        Vec3d plotOrigin = TCClient.DF_STATE.getPlotOrigin();
        if (plotOrigin == null) return;

        queuedChunkRescans.remove(chunkPos);

        //dont scan chunk if it doesnt touch the codespace
        if (!(
            TCClient.DF_STATE.isWorldPosInCodespace(new Vec3d(chunkX*16,0,chunkZ*16)) ||
            TCClient.DF_STATE.isWorldPosInCodespace(new Vec3d(chunkX*16+15,0,chunkZ*16)) ||
            TCClient.DF_STATE.isWorldPosInCodespace(new Vec3d(chunkX*16,0,chunkZ*16+15)) ||
            TCClient.DF_STATE.isWorldPosInCodespace(new Vec3d(chunkX*16+15,0,chunkZ*16+15))
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

    @Override
    public void onTickEnd(MinecraftClient client) {
        while (!queuedBlockRescans.isEmpty()) {
            BlockPos next = queuedBlockRescans.poll();
            if (next == null) {continue;}
            processBlockTemplate(next);
        }

        if (TCClient.DF_STATE.getMode() == DFState.Mode.DEV) {
            if (
                !TCClient.MOVEMENT_MANAGER.isMoving() &&
                !queuedChunkRescans.isEmpty() &&
                (nextChunkToScan == null || !Objects.equals(TCClient.MOVEMENT_MANAGER.getCurrentMovementId(), "SCAN_QUEUED_CHUNK"))
            ) {
                nextChunkToScan = queuedChunkRescans.peek();
                TCClient.MOVEMENT_MANAGER.setMovementDestination(
                    TCClient.DF_STATE.toPlotSpace(
                        TCClient.DF_STATE.clampWorldPosToCodespace(new Vec3d(nextChunkToScan.x*16+16,52,nextChunkToScan.z*16+16))
                    ),
                    "SCAN_QUEUED_CHUNK"
                );
            }

            if (nextChunkToScan != null && !queuedChunkRescans.contains(nextChunkToScan)) {
                nextChunkToScan = null;
                TCClient.MOVEMENT_MANAGER.cancelMovement("SCAN_QUEUED_CHUNK");
            }
        }
    }

    @Override
    public void onModeChanged(DFState.Mode newMode) {
        if (newMode == DFState.Mode.SPAWN) {
            clearTemplates();
        }
    }

    @Override
    public void onChunkLoad(ChunkPos chunkPos) {
        if (TCClient.DF_STATE.getMode() != DFState.Mode.DEV) return;
        scanChunk(chunkPos);
    }

    @Override
    public void onChunkDelta(ChunkDeltaUpdateS2CPacket packet) {
        packet.visitUpdates((blockPos, blockState) -> {
            Vec3d plotOrigin = TCClient.DF_STATE.getPlotOrigin();
            if (plotOrigin == null) return;
            queuedBlockRescans.add(blockPos.toImmutable());
        });
    }

    @Override
    public void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet) {
        Vec3d plotOrigin = TCClient.DF_STATE.getPlotOrigin();
        if (plotOrigin == null) return;
        queuedBlockRescans.add(packet.getPos().toImmutable());
    }
}
