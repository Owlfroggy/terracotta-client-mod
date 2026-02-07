package owlfroggy.terracottaclient;

import com.google.gson.JsonObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import owlfroggy.terracottaclient.api.APIServer;
import owlfroggy.terracottaclient.api.message.impl.InitiateCodeEditA2CRequest;
import owlfroggy.terracottaclient.api.message.impl.InitiateCodeEditC2AResponse;
import owlfroggy.terracottaclient.codespace.*;
import owlfroggy.terracottaclient.gameinterface.ClientBlockUpdateReceiver;
import owlfroggy.terracottaclient.gameinterface.ChunkReceiver;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;
import owlfroggy.terracottaclient.mixin.SequencedPacketAccessor;

import java.util.*;

public class CodeEditManager extends Manager
implements
    ChunkReceiver,
    ClientBlockUpdateReceiver,
    TickEndReceiver
{
    private class CodeEdit {
        public enum Action {
            PLACE,
            REPLACE,
            BREAK
        }

        public enum State {
            PLACING,
            WAITING_FOR_PLACE_VERIFICATION,
            BREAKING,
            WAITING_FOR_BREAK_VERIFICATION,
            DONE,
        }

        public Vec3i plotSpacePos;
        public String templateData = null;
        public Action action;
        public State state;
        public int inactivityCycles = 0;
        public boolean breakWasDefinitelySuccessful = false;

        CodeEdit(Vec3i plotSpacePos, String templateData, Action action, State state) {
            this.plotSpacePos = plotSpacePos;
            this.templateData = templateData;
            this.action = action;
            this.state = state;
        }
    }
    private enum GlobalEditState {
        MOVING,
        EDITING,
        IDLE,
    }

    private final ItemStack REACH_EXTENDER = Utils.applyReachToItem(new ItemStack(Items.ARROW), "editor_reach_thingy");
    private static final int TEMPLATE_VACUUM_SLOT = 0;

    private GlobalEditState editState = GlobalEditState.IDLE;
    private final ArrayList<CodeEdit> queuedCodeEdits = new ArrayList<>();
    private final ArrayList<CodeEdit> stagedCodeEdits = new ArrayList<>();
    private final HashMap<Vec3i, CodeEdit> codeEditsByPlotPos = new HashMap<>();
    private CodeEdit currentBatchCoreEdit = null;
    private int stagedEditActiveIndex = 0;
    private ItemStack oldOffhandItem;
    private ItemStack oldFirstSlotItem;

    public void editCode(String[] placeTemplates, TemplateIdentifier[] breakTemplates) throws Exception {
        if (TCClient.DF_STATE.getPlotOrigin() == null) {
            throw new IllegalStateException("Plot has not been scanned");
        }

        queuedCodeEdits.clear();
        stagedCodeEdits.clear();
        codeEditsByPlotPos.clear();
        currentBatchCoreEdit = null;
        oldOffhandItem = TCClient.MCI.player.getInventory().getStack(PlayerInventory.OFF_HAND_SLOT);
        oldFirstSlotItem = TCClient.MCI.player.getInventory().getStack(TEMPLATE_VACUUM_SLOT);
        TCClient.MOVEMENT_MANAGER.setShouldHoldFastSpeed(true);

        //TODO: make it be able to take positions from templates that are being deleted
        Queue<Vec3i> openPositions = new LinkedList<>(); //plot space
        int bottomFloor = TCClient.DF_STATE.hasUndergroundCodespace() ? 5 : 50;
        openFinderLoop: for (int floorY = bottomFloor; floorY <= 250; floorY += 5) {
            for (int rowX = -2; rowX >= -17; rowX -= 3) {
                if (TCClient.DF_STATE.getFloor(floorY).getRow(rowX).templates.isEmpty()) {
                    openPositions.add(new Vec3i(rowX,floorY,0));
                    if (openPositions.size() >= placeTemplates.length) break openFinderLoop;
                }
            }
        }

        if (openPositions.size() < placeTemplates.length) {
            throw new Exception("Not enough space is present in the codespace to place all templates");
        }

        for (TemplateIdentifier identifier : breakTemplates) {
            CachedTemplate cachedTemplate = TCClient.DF_STATE.getTemplateByIdentifier(identifier);
            if (cachedTemplate == null) {
                TCClient.LOGGER.warn("Template will not be broken because it is absent from the cache: "+identifier);
                continue;
            }

            CodeEdit edit = new CodeEdit(
                cachedTemplate.plotSpacePos,
                null,
                CodeEdit.Action.BREAK,
                CodeEdit.State.BREAKING
            );
            queuedCodeEdits.add(edit);
            codeEditsByPlotPos.put(cachedTemplate.plotSpacePos, edit);
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

            CachedTemplate cachedTemplate = TCClient.DF_STATE.getTemplateByIdentifier(identifier);

            if (cachedTemplate == null) {
                Vec3i pos = openPositions.poll();
                CodeEdit edit = new CodeEdit(
                    pos,
                    rawTemplateData,
                    CodeEdit.Action.PLACE,
                    CodeEdit.State.PLACING
                );
                queuedCodeEdits.add(edit);
                codeEditsByPlotPos.put(pos, edit);
            } else {
                CodeEdit edit = new CodeEdit(
                    cachedTemplate.plotSpacePos,
                    rawTemplateData,
                    CodeEdit.Action.REPLACE,
                    CodeEdit.State.BREAKING
                );
                queuedCodeEdits.add(edit);
                codeEditsByPlotPos.put(cachedTemplate.plotSpacePos, edit);
            }
        }

        editState = GlobalEditState.MOVING;
    }

    private void processCodeEditResponse(Vec3i plotSpacePos, BlockState blockState, boolean cameFromClient) {
        CodeEdit edit = codeEditsByPlotPos.getOrDefault(plotSpacePos, null);
        if (!TCClient.isChunkLoaded(TCClient.DF_STATE.toWorldSpace(plotSpacePos))) return;

        if (edit != null) {
            switch (edit.state) {
                case WAITING_FOR_BREAK_VERIFICATION -> {
                    // block was successfully broken
                    if (blockState.getBlock() == Blocks.AIR && !cameFromClient) {
                        edit.breakWasDefinitelySuccessful = true;
                        if (edit.action == CodeEdit.Action.REPLACE) {
                            edit.state = CodeEdit.State.PLACING;
                        } else {
                            edit.state = CodeEdit.State.DONE;
                        }
                    }
                    else if (cameFromClient) {
                        edit.state = CodeEdit.State.BREAKING;
                    }
                }
                case WAITING_FOR_PLACE_VERIFICATION -> {
                    // block was successfully placed
                    if (
                        blockState.getBlock() != Blocks.AIR &&
                        blockState.getBlock() != Blocks.LIGHT_BLUE_TERRACOTTA &&
                        !cameFromClient
                    ) {
                        edit.state = CodeEdit.State.DONE;
                    } else {
                        edit.state = CodeEdit.State.PLACING;
                    }
                }
            }
        }
    }

    public boolean isEditingCode() {
        return editState != GlobalEditState.IDLE;
    }

    @Override
    public void onTickEnd(MinecraftClient client) {
        if (TCClient.DF_STATE.getPlotOrigin() == null) return;

        if (TCClient.DF_STATE.getMode() == DFState.Mode.DEV) {
            codeEditLogic: if (editState != GlobalEditState.IDLE) {
                // break out of editor mode if all edits have been completed
                if (queuedCodeEdits.isEmpty() && stagedCodeEdits.isEmpty()) {
                    Utils.setItemInSlot(PlayerInventory.OFF_HAND_SLOT, oldOffhandItem);
                    Utils.setItemInSlot(TEMPLATE_VACUUM_SLOT, oldFirstSlotItem);

                    TCClient.MOVEMENT_MANAGER.setShouldHoldFastSpeed(false);

                    client.player.playerScreenHandler.sendContentUpdates();
                    editState = GlobalEditState.IDLE;

                    APIServer.resolvePendingRequests(r -> {
                       if (r instanceof InitiateCodeEditA2CRequest) {
                           return new InitiateCodeEditC2AResponse();
                       }
                       return null;
                    });

                    break codeEditLogic;
                }

                //code editing
                switch (editState) {
                    case MOVING -> {
                        // stage new edits
                        if (stagedCodeEdits.isEmpty()) {
                            // find the closest edit to the player and make that the core edit
                            Vec3d playerPos = TCClient.MCI.player.getEntityPos();
                            CodeEdit coreEdit = null;
                            double minDistance = 999999999.0; //refactor this when world plots come out fr fr
                            for (CodeEdit edit : queuedCodeEdits) {
                                double distance = TCClient.DF_STATE.toWorldSpace(Utils.toVec3d(edit.plotSpacePos)).distanceTo(playerPos);
                                if (distance < minDistance) {
                                    minDistance = distance;
                                    coreEdit = edit;
                                }
                            }

                            assert coreEdit != null;
                            currentBatchCoreEdit = coreEdit;
                            stagedCodeEdits.add(coreEdit);

                            // stage all edits which are within placing range of this edit
                            for (int i = queuedCodeEdits.size()-1; i >= 0; i--) {
                                CodeEdit edit = queuedCodeEdits.get(i);
                                if (edit.plotSpacePos.isWithinDistance(coreEdit.plotSpacePos,60.0)) {
                                    queuedCodeEdits.remove(i);
                                    stagedCodeEdits.add(edit);
                                }
                            }

                            stagedEditActiveIndex = stagedCodeEdits.size()-1;
                        }

                        // move to the right place
                        if (!TCClient.MOVEMENT_MANAGER.isMoving()) {
                            Vec3d goalPos = Utils.toVec3d(currentBatchCoreEdit.plotSpacePos).add(new Vec3d(0.5,2.2,0.5));

                            // if movement is complete, switch to editing mode
                            if (TCClient.DF_STATE.toWorldSpace(goalPos).distanceTo(TCClient.MCI.player.getEntityPos()) < 1) {
                                editState = GlobalEditState.EDITING;
                            } else {
                                TCClient.MOVEMENT_MANAGER.setMovementDestination(
                                    goalPos,
                                "CODE_EDIT"
                                );
                            }
                        }
                    }

                    case EDITING -> {
                        // if the player got moved away from the core edit, move them back
                        Vec3d playerPos = client.player.getEntityPos();
                        Vec3d coreEditPos = Utils.toVec3d(TCClient.DF_STATE.toWorldSpace(currentBatchCoreEdit.plotSpacePos));
                        if (!playerPos.isWithinRangeOf(coreEditPos,4,4)) {
                            editState = GlobalEditState.MOVING;
                            break codeEditLogic;
                        }

                        CodeEdit activeEdit = stagedCodeEdits.get(stagedEditActiveIndex);
                        int checkedEdits = 1;
                        int maxChecks = stagedCodeEdits.size();
                        // loop through edits until an actionable one has been found
                        while (
                            !(
                                activeEdit.state == CodeEdit.State.PLACING || activeEdit.state == CodeEdit.State.BREAKING
                                && TCClient.isChunkLoaded(TCClient.DF_STATE.toWorldSpace(activeEdit.plotSpacePos))
                            )
                        ) {
                            // don't loop endlessly if there are no actionable edits
                            if (checkedEdits > maxChecks) {
                                break codeEditLogic; // basically just waits this tick out
                            }

                            // if the previous edit is done, remove it from the list
                            if (activeEdit.state == CodeEdit.State.DONE) {
                                stagedCodeEdits.remove(stagedEditActiveIndex);
                                if (stagedCodeEdits.isEmpty()) {
                                    editState = GlobalEditState.MOVING;
                                    break codeEditLogic;
                                }
                            } else {
                                // retry edits that have gotten stuck in the waiting stage
                                if (activeEdit.state == CodeEdit.State.WAITING_FOR_BREAK_VERIFICATION || activeEdit.state == CodeEdit.State.WAITING_FOR_PLACE_VERIFICATION) {
                                    activeEdit.inactivityCycles += 1;
                                }
                                int maxInactivityCycles = 10 + client.getNetworkHandler().getPlayerListEntry(client.player.getUuid()).getLatency()*50/1000;
                                CodeEdit.State oldState = activeEdit.state;
                                if (activeEdit.inactivityCycles > maxInactivityCycles) {
                                    if (client.world.getBlockState(new BlockPos(TCClient.DF_STATE.toWorldSpace(activeEdit.plotSpacePos))).getBlock() == Blocks.AIR) {
                                        activeEdit.state = switch (activeEdit.action) {
                                            case REPLACE, PLACE -> CodeEdit.State.PLACING;
                                            case BREAK -> CodeEdit.State.DONE;
                                        };
                                    } else {
                                        activeEdit.state = switch (activeEdit.action) {
                                            case REPLACE -> activeEdit.breakWasDefinitelySuccessful ? CodeEdit.State.DONE : CodeEdit.State.BREAKING;
                                            case BREAK -> CodeEdit.State.BREAKING;
                                            case PLACE -> CodeEdit.State.DONE;
                                        };
                                    }

                                    TCClient.LOGGER.warn("Code edit (action {}) at {} got stuck in {}! Retrying with state: {}", activeEdit.action.name(), activeEdit.plotSpacePos, oldState.name(), activeEdit.state.name());
                                }
                            }

                            checkedEdits++;
                            stagedEditActiveIndex--;
                            if (stagedEditActiveIndex < 0) stagedEditActiveIndex = stagedCodeEdits.size()-1;
                            activeEdit = stagedCodeEdits.get(stagedEditActiveIndex);
                        }

                        activeEdit.inactivityCycles = 0;

                        switch (activeEdit.state) {
                            case BREAKING -> {
                                GameOptions settings = TCClient.MCI.options;

                                // clear a slot for the new template to go into
                                // (so a billion dropped items dont spawn)
                                Utils.setItemInSlot(TEMPLATE_VACUUM_SLOT,new ItemStack(Items.AIR),true);
                                // make sure reach is extended
                                Utils.setItemInSlot(PlayerInventory.OFF_HAND_SLOT,REACH_EXTENDER,true);

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
                                BlockPos pos = new BlockPos(TCClient.DF_STATE.toWorldSpace(activeEdit.plotSpacePos));
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

                                activeEdit.state = CodeEdit.State.WAITING_FOR_BREAK_VERIFICATION;
                            }
                            case PLACING -> {
                                // create code template item
                                ItemStack item = new ItemStack(Items.LIGHT_BLUE_TERRACOTTA);
                                NbtCompound root = new NbtCompound();

                                NbtCompound publicBukkitValues = new NbtCompound();
                                String templateData = String.format("""
                                {"author":"Terracotta Client","name":"Compiled Template","version":1,"code":"%s" }
                                """, activeEdit.templateData);
                                publicBukkitValues.putString("hypercube:codetemplatedata", templateData);
                                root.put("PublicBukkitValues", publicBukkitValues);

                                NbtCompound terracottaData = new NbtCompound();
                                terracottaData.put("hidden", NbtByte.of(true));
                                terracottaData.put("instance", NbtInt.of(TCClient.INSTANCE_ID));
                                root.put("terracotta_metadata",terracottaData);

                                item.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(root));
                                Utils.applyReachToItem(item,"editor_reach_thingy");

                                Utils.setItemInSlot(PlayerInventory.OFF_HAND_SLOT, item);

                                // place item
                                BlockPos blockPos = new BlockPos(TCClient.DF_STATE.toWorldSpace(activeEdit.plotSpacePos));
                                BlockHitResult hit = new BlockHitResult(
                                    blockPos.toBottomCenterPos(),
                                    Direction.DOWN,
                                    blockPos,
                                    false
                                );
                                TCClient.MCI.interactionManager.interactBlock(client.player,Hand.OFF_HAND, hit);

                                activeEdit.state = CodeEdit.State.WAITING_FOR_PLACE_VERIFICATION;
                            }
                        }
                    }
                }

            }
        }
    }

    @Override
    public void onChunkLoad(ChunkPos chunkPos) {}

    @Override
    public void onChunkDelta(ChunkDeltaUpdateS2CPacket packet) {
        packet.visitUpdates((blockPos, blockState) -> {
            Vec3d plotOrigin = TCClient.DF_STATE.getPlotOrigin();
            if (plotOrigin == null) return;

            BlockPos immutablePos = blockPos.toImmutable();

            Vec3i plotSpacePos = TCClient.DF_STATE.toPlotSpace(immutablePos);
            processCodeEditResponse(plotSpacePos,blockState,false);
        });
    }

    @Override
    public void onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet) {}

    @Override
    public void onClientBlockUpdate(BlockPos pos, BlockState state) {
        Vec3d plotOrigin = TCClient.DF_STATE.getPlotOrigin();
        if (plotOrigin == null) return;

        processCodeEditResponse(TCClient.DF_STATE.toPlotSpace(pos), state,true);
    }
}
