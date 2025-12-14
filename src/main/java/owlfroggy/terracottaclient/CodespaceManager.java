package owlfroggy.terracottaclient;

import com.google.gson.JsonObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import owlfroggy.terracottaclient.codespacemanager.*;
import owlfroggy.terracottaclient.gameinterface.ChunkReceiver;
import owlfroggy.terracottaclient.gameinterface.PlotChangeReceiver;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;
import owlfroggy.terracottaclient.mixin.SequencedPacketAccessor;

import java.util.*;

public class CodespaceManager extends Manager implements ChunkReceiver, PlotChangeReceiver, TickEndReceiver {
    private class CodeEdit {
        public enum Action {
            PLACE,
            REPLACE,
            BREAK
        }

        public Vec3i plotSpacePos;
        public String templateData = null;
        public Action action;

        CodeEdit(Vec3i plotSpacePos, String templateData, Action action) {
            this.plotSpacePos = plotSpacePos;
            this.templateData = templateData;
            this.action = action;
        }
    }

    public enum CodeEditState {
        MOVING,
        PLACING,
        BREAKING,
        WAITING_FOR_PLACE_RESPONSE,
        WAITING_FOR_BREAK_RESPONSE,
    }

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
    private boolean isEditingCode = false;
    private final Queue<CodeEdit> queuedCodeEdits = new LinkedList<>();
    private CodeEdit currentCodeEdit = null;
    private CodeEditState currentCodeEditState;
    private ItemStack oldOffhandItem;
    private ItemStack oldFirstSlotItem;

    public CodespaceFloor getFloor(int yLevel) {
        if (floors.containsKey(yLevel)) {
            return floors.get(yLevel);
        } else {
            CodespaceFloor floor = new CodespaceFloor(yLevel);
            floors.put(yLevel, floor);
            return floor;
        }
    }

    public void editCode(String[] placeTemplates, TemplateIdentifier[] breakTemplates) throws Exception {
        if (TCClient.DF_STATE.getPlotOrigin() == null) {
            throw new IllegalStateException("Plot has not been scanned");
        }

        queuedCodeEdits.clear();
        currentCodeEdit = null;
        oldOffhandItem = TCClient.MCI.player.getInventory().getStack(PlayerInventory.OFF_HAND_SLOT);
        oldFirstSlotItem = TCClient.MCI.player.getInventory().getStack(9);

        //TODO: make it be able to take positions from templates that are being deleted
        Queue<Vec3i> openPositions = new LinkedList<>();
        openFinderLoop: for (int floorY = 50; floorY <= 250; floorY += 5) {
            for (int rowX = -2; rowX >= -17; rowX -= 3) {
                if (getFloor(floorY).getRow(rowX).templates.isEmpty()) {
                    openPositions.add(new Vec3i(rowX,floorY,0));
                    if (openPositions.size() >= placeTemplates.length) break openFinderLoop;
                }
            }
        }

        if (openPositions.size() < placeTemplates.length) {
            throw new Exception("Not enough space is present in the codespace to place all templates");
        }

        for (TemplateIdentifier identifier : breakTemplates) {
            CachedTemplate cachedTemplate = getTemplateByIdentifier(identifier);
            if (cachedTemplate == null) {
                TCClient.LOGGER.warn("Template will not be broken because it is absent from the cache: "+identifier);
                continue;
            }

            queuedCodeEdits.add(new CodeEdit(
                cachedTemplate.plotSpacePos,
                null,
                CodeEdit.Action.BREAK
            ));
        }

        placeLoop: for (String rawTemplateData : placeTemplates) {
            JsonObject templateData;
            TemplateIdentifier identifier;

            try {
                templateData = TemplateDataUtils.parseTemplateData(rawTemplateData);
                identifier = TemplateDataUtils.getIdentifier(templateData);
            } catch (Exception e) {
                TCClient.LOGGER.error("A template was skipped because it was invalid ("+rawTemplateData+") ",e);
                continue placeLoop;
            }

            CachedTemplate cachedTemplate = getTemplateByIdentifier(identifier);

            TCClient.LOGGER.info(identifier.toString());
            if (cachedTemplate == null) {

                Vec3i pos = openPositions.poll();

                queuedCodeEdits.add(new CodeEdit(
                    pos,
                    rawTemplateData,
                    CodeEdit.Action.PLACE
                ));
            } else {
                queuedCodeEdits.add(new CodeEdit(
                    cachedTemplate.plotSpacePos,
                    rawTemplateData,
                    CodeEdit.Action.REPLACE
                ));
            }
        }

        isEditingCode = true;
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
//        if (row.templates.isEmpty()) {
//            floor.rows.remove(row.xPos, row);
//            if (floor.rows.isEmpty()) {
//                floors.remove(floor.yLevel, floor);
//            }
//        }
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
            codeEditLogic: if (isEditingCode) {
                //code editing

                if (currentCodeEdit == null) {
                    TCClient.LOGGER.info("{}",queuedCodeEdits);
                    if (queuedCodeEdits.isEmpty()) {
                        client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(45, oldOffhandItem));
                        client.player.getInventory().setStack(PlayerInventory.OFF_HAND_SLOT,oldOffhandItem);

                        client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(9, oldFirstSlotItem));
                        client.player.getInventory().setStack(9,oldFirstSlotItem);

                        client.player.playerScreenHandler.sendContentUpdates();
                        isEditingCode = false;
                        break codeEditLogic;
                    }
                    currentCodeEdit = queuedCodeEdits.poll();
                    currentCodeEditState = CodeEditState.MOVING;
                }

                switch (currentCodeEditState) {
                    case CodeEditState.MOVING -> {
                        Vec3d goalPos = Utils.toVec3d(currentCodeEdit.plotSpacePos).add(new Vec3d(-1,2.2,0));
                        if (!TCClient.MOVEMENT_MANAGER.isMoving()) {
                            // if movement is complete, place
                            if (TCClient.DF_STATE.toWorldSpace(goalPos).distanceTo(TCClient.MCI.player.getPos()) < 1) {
                                if (currentCodeEdit.action == CodeEdit.Action.PLACE) {
                                    currentCodeEditState = CodeEditState.PLACING;
                                } else {
                                    currentCodeEditState = CodeEditState.BREAKING;
                                }
                            } else {
                                TCClient.MOVEMENT_MANAGER.setMovementDestination(
                                    goalPos,
                                "CODE_EDIT"
                                );
                            }
                        }
                    }

                    case CodeEditState.BREAKING -> {
                        GameOptions settings = TCClient.MCI.options;

                        // clear a slot for the new template to go into
                        // (so a billion dropped items dont spawn)
                        client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(9, new ItemStack(Items.AIR)));

                        // sneak
                        PlayerInput sneakInput = new PlayerInput(
                            settings.forwardKey.isPressed(),
                            settings.backKey.isPressed(),
                            settings.leftKey.isPressed(),
                            settings.rightKey.isPressed(),
                            settings.jumpKey.isPressed(),
                            true,
                            settings.sprintKey.isPressed()
                        );
                        client.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(sneakInput));

                        // break
                        BlockPos pos = new BlockPos(TCClient.DF_STATE.toWorldSpace(currentCodeEdit.plotSpacePos));
                        ((SequencedPacketAccessor)client.interactionManager).invokeSendSequencedPacket(TCClient.MCI.world, sequence -> {
                            TCClient.MCI.interactionManager.breakBlock(pos);
                            return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP, sequence);
                        });

                        // unsneak
                        PlayerInput unsneakInput = new PlayerInput(
                            settings.forwardKey.isPressed(),
                            settings.backKey.isPressed(),
                            settings.leftKey.isPressed(),
                            settings.rightKey.isPressed(),
                            settings.jumpKey.isPressed(),
                            settings.sneakKey.isPressed(),
                            settings.sprintKey.isPressed()
                        );
                        client.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(unsneakInput));

                        if (currentCodeEdit.action == CodeEdit.Action.BREAK) {
                            currentCodeEdit = null;
                            currentCodeEditState = CodeEditState.MOVING;
                        } else {
                            currentCodeEditState = CodeEditState.PLACING;
                        }
                    }

                    case CodeEditState.PLACING -> {
                        // create code template item
                        ItemStack item = new ItemStack(Items.LIGHT_BLUE_TERRACOTTA);
                        NbtCompound root = new NbtCompound();

                        NbtCompound publicBukkitValues = new NbtCompound();
                        String templateData = String.format("""
                        {"author":"Terracotta Client","name":"Compiled Template","version":1,"code":"%s" }
                        """, currentCodeEdit.templateData);
                        publicBukkitValues.putString("hypercube:codetemplatedata", templateData);
                        root.put("PublicBukkitValues", publicBukkitValues);

                        NbtCompound terracottaData = new NbtCompound();
                        terracottaData.put("hidden", NbtByte.of(true));
                        terracottaData.put("instance", NbtInt.of(TCClient.INSTANCE_ID));
                        root.put("terracotta_metadata",terracottaData);

                        item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));

                        client.getNetworkHandler().sendPacket(new CreativeInventoryActionC2SPacket(45, item));

                        // place item
                        BlockPos blockPos = new BlockPos(TCClient.DF_STATE.toWorldSpace(currentCodeEdit.plotSpacePos));
                        BlockHitResult hit = new BlockHitResult(
                            blockPos.toBottomCenterPos(),
                            Direction.DOWN,
                            blockPos,
                            false
                        );
                        TCClient.MCI.interactionManager.interactBlock(client.player,Hand.OFF_HAND, hit);

                        currentCodeEdit = null;
                        currentCodeEditState = CodeEditState.MOVING;
                    }

                    case CodeEditState.WAITING_FOR_PLACE_RESPONSE -> {
                        currentCodeEdit = null;
                    }
                }

            }
            else {
                // chunk scanning
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
    }

    @Override
    public void onPlotChanged(int plotId, DFState.Mode mode) {
        clearTemplates();
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
