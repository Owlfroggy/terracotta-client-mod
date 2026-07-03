package owlfroggy.terracottaclient;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import owlfroggy.terracottaclient.api.APIErrorCode;
import owlfroggy.terracottaclient.api.APIServer;
import owlfroggy.terracottaclient.api.message.ErrorResponse;
import owlfroggy.terracottaclient.api.message.Response;
import owlfroggy.terracottaclient.api.message.impl.InitiateCodeEditA2CRequest;
import owlfroggy.terracottaclient.api.message.impl.InitiateCodeEditC2AResponse;
import owlfroggy.terracottaclient.codespace.*;
import owlfroggy.terracottaclient.gameinterface.ClientBlockUpdateReceiver;
import owlfroggy.terracottaclient.gameinterface.ChunkReceiver;
import owlfroggy.terracottaclient.gameinterface.ModeChangeReceiver;
import owlfroggy.terracottaclient.gameinterface.TickEndReceiver;
import owlfroggy.terracottaclient.mixin.SequencedPacketAccessor;

import java.util.*;

public class CodeEditManager extends Manager
implements
    ChunkReceiver,
    ClientBlockUpdateReceiver,
    TickEndReceiver,
    ModeChangeReceiver
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

    public enum EndCause {
        FINISHED_SUCCESSFULLY,
        ABORTED,
        LEFT_DEV_MODE,
    }

    // TODO(UPDATE): fix reach extender
    private ItemStack REACH_EXTENDER = null; //Utils.applyReachToItem(new ItemStack(Items.ARROW), "editor_reach_thingy");
    private static final int TEMPLATE_VACUUM_SLOT = 0;

    private GlobalEditState editState = GlobalEditState.IDLE;
    private final ArrayList<CodeEdit> queuedCodeEdits = new ArrayList<>();
    private final ArrayList<CodeEdit> stagedCodeEdits = new ArrayList<>();
    private final HashMap<Vec3i, CodeEdit> codeEditsByPlotPos = new HashMap<>();
    private CodeEdit currentBatchCoreEdit = null;
    private int stagedEditActiveIndex = 0;
    private ItemStack oldOffhandItem;
    private ItemStack oldFirstSlotItem;
    private int oldHeldSlot = 0;

    private ItemStack getReachExtender() {
        if (REACH_EXTENDER == null) {
            this.REACH_EXTENDER = Utils.applyReachToItem(new ItemStack(Items.ARROW), "editor_reach_thingy");
        }
        return this.REACH_EXTENDER;
    }

    private void clearState() {
        queuedCodeEdits.clear();
        stagedCodeEdits.clear();
        codeEditsByPlotPos.clear();
        currentBatchCoreEdit = null;
        stagedEditActiveIndex = 0;
    }

    public void stopEditing(EndCause cause) {
        editState = GlobalEditState.IDLE;
        if (oldOffhandItem != null) Utils.setItemInSlot(Inventory.SLOT_OFFHAND, oldOffhandItem);
        if (oldFirstSlotItem != null) Utils.setItemInSlot(TEMPLATE_VACUUM_SLOT, oldFirstSlotItem);

        TCClient.MCI.player.getInventory().setSelectedSlot(oldHeldSlot);

        TCClient.MOVEMENT_MANAGER.setShouldHoldFastSpeed(false);
        TCClient.MOVEMENT_MANAGER.stopMovement("CODE_EDIT");
        TCClient.MCI.player.inventoryMenu.broadcastChanges();

        clearState();

        Response response;
        if (cause == EndCause.FINISHED_SUCCESSFULLY) {
            response = new InitiateCodeEditC2AResponse();
        } else if (cause == EndCause.LEFT_DEV_MODE) {
            response = new ErrorResponse(
                APIErrorCode.EDIT_FAILED_LEFT_DEV,
                "Player left dev mode before editing could complete."
            );
        } else if (cause == EndCause.ABORTED) {
            response = new ErrorResponse(
                APIErrorCode.EDIT_FAILED_ABORTED,
                "Code edit operation was aborted before it could complete"
            );
        } else {
            response = new ErrorResponse(APIErrorCode.EDIT_FAILED, "Code edit could not be completed");
        }
        APIServer.resolvePendingRequests(r -> {
            if (r instanceof InitiateCodeEditA2CRequest) {
                return response;
            }
            return null;
        });

    }

    public void editCode(String[] placeTemplates, TemplateIdentifier[] breakTemplates) throws Exception {
        if (!TCClient.DF_STATE.isScanned()) {
            throw new IllegalStateException("Plot has not been scanned");
        }

        clearState();

        oldOffhandItem = TCClient.MCI.player.getInventory().getItem(Inventory.SLOT_OFFHAND);
        oldFirstSlotItem = TCClient.MCI.player.getInventory().getItem(TEMPLATE_VACUUM_SLOT);
        oldHeldSlot = TCClient.MCI.player.getInventory().getSelectedSlot();

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
                        blockState.getBlock() != Blocks.DYED_TERRACOTTA.lightBlue() &&
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
    public void onTickEnd(Minecraft client) {
        if (!TCClient.DF_STATE.isScanned()) return;

        if (TCClient.DF_STATE.getMode() == DFState.Mode.DEV) {
            codeEditLogic: if (editState != GlobalEditState.IDLE) {
                // break out of editor mode if all edits have been completed
                if (queuedCodeEdits.isEmpty() && stagedCodeEdits.isEmpty()) {
                    stopEditing(EndCause.FINISHED_SUCCESSFULLY);

                    break codeEditLogic;
                }

                // make sure the player is always trying to be in the right position
                Vec3 goalPos = TCClient.MCI.player.position();
                if (!TCClient.MOVEMENT_MANAGER.isMoving() && currentBatchCoreEdit != null) {
                    goalPos = Utils.toVec3d(currentBatchCoreEdit.plotSpacePos).add(new Vec3(0.5,2.2,0.5));

                    TCClient.MOVEMENT_MANAGER.setMovementDestination(
                        goalPos,
                        "CODE_EDIT"
                    );
                }

                // make sure the player is hovering over the first slot
                TCClient.MCI.player.getInventory().setSelectedSlot(0);

                //code editing
                switch (editState) {
                    case MOVING -> {
                        // stage new edits
                        if (stagedCodeEdits.isEmpty()) {
                            // find the closest edit to the player and make that the core edit
                            Vec3 playerPos = TCClient.MCI.player.position();
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
                                if (edit.plotSpacePos.closerThan(coreEdit.plotSpacePos,60.0)) {
                                    queuedCodeEdits.remove(i);
                                    stagedCodeEdits.add(edit);
                                }
                            }

                            stagedEditActiveIndex = stagedCodeEdits.size()-1;

                        }
                        // the actual movement code is handled above the switch (editState) statement so that
                        // getting moved out of alignment during editing doesn't make the placer get stuck
                        // if movement is complete, switch to editing mode
                        if (TCClient.DF_STATE.toWorldSpace(goalPos).distanceTo(TCClient.MCI.player.position()) < 1) {
                            editState = GlobalEditState.EDITING;
                        }
                    }

                    case EDITING -> {
                        // if the player got moved away from the core edit, move them back
                        Vec3 playerPos = client.player.position();
                        Vec3 coreEditPos = Utils.toVec3d(TCClient.DF_STATE.toWorldSpace(currentBatchCoreEdit.plotSpacePos));
                        if (!playerPos.closerThan(coreEditPos,4,4) || stagedCodeEdits.isEmpty()) {
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
                                int maxInactivityCycles = 10 + client.getConnection().getPlayerInfo(client.player.getUUID()).getLatency()*50/1000;
                                CodeEdit.State oldState = activeEdit.state;
                                if (activeEdit.inactivityCycles > maxInactivityCycles) {
                                    if (client.level.getBlockState(new BlockPos(TCClient.DF_STATE.toWorldSpace(activeEdit.plotSpacePos))).getBlock() == Blocks.AIR) {
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
                                Options settings = TCClient.MCI.options;

                                // clear a slot for the new template to go into
                                // (so a billion dropped items dont spawn)
                                Utils.setItemInSlot(TEMPLATE_VACUUM_SLOT,new ItemStack(Items.AIR),true);
                                // make sure reach is extended
                                Utils.setItemInSlot(Inventory.SLOT_OFFHAND,getReachExtender(),true);

                                // sneak
                                Input sneakInput = new Input(
                                    settings.keyUp.isDown(),
                                    settings.keyDown.isDown(),
                                    settings.keyLeft.isDown(),
                                    settings.keyRight.isDown(),
                                    settings.keyJump.isDown(),
                                    true,
                                    settings.keySprint.isDown()
                                );
                                client.getConnection().send(new ServerboundPlayerInputPacket(sneakInput));

                                // break
                                BlockPos pos = new BlockPos(TCClient.DF_STATE.toWorldSpace(activeEdit.plotSpacePos));
                                ((SequencedPacketAccessor)client.gameMode).invokeStartPrediction(TCClient.MCI.level, sequence -> {
                                    TCClient.MCI.gameMode.destroyBlock(pos);
                                    return new ServerboundPlayerActionPacket(net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP, sequence);
                                });

                                // unsneak
                                Input unsneakInput = new Input(
                                    settings.keyUp.isDown(),
                                    settings.keyDown.isDown(),
                                    settings.keyLeft.isDown(),
                                    settings.keyRight.isDown(),
                                    settings.keyJump.isDown(),
                                    settings.keyShift.isDown(),
                                    settings.keySprint.isDown()
                                );
                                client.getConnection().send(new ServerboundPlayerInputPacket(unsneakInput));

                                activeEdit.state = CodeEdit.State.WAITING_FOR_BREAK_VERIFICATION;
                            }
                            case PLACING -> {
                                // create code template item
                                ItemStack item = new ItemStack(Items.DYED_TERRACOTTA.lightBlue());
                                CompoundTag root = new CompoundTag();

                                CompoundTag publicBukkitValues = new CompoundTag();
                                String templateData = String.format("""
                                {"author":"Terracotta Client","name":"Compiled Template","version":1,"code":"%s" }
                                """, activeEdit.templateData);
                                publicBukkitValues.putString("hypercube:codetemplatedata", templateData);
                                root.put("PublicBukkitValues", publicBukkitValues);

                                CompoundTag terracottaData = new CompoundTag();
                                terracottaData.put("hidden", ByteTag.valueOf(true));
                                terracottaData.put("instance", IntTag.valueOf(TCClient.INSTANCE_ID));
                                root.put("terracotta_metadata",terracottaData);

                                item.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                                Utils.applyReachToItem(item,"editor_reach_thingy");

                                Utils.setItemInSlot(Inventory.SLOT_OFFHAND, item);

                                // place item
                                BlockPos blockPos = new BlockPos(TCClient.DF_STATE.toWorldSpace(activeEdit.plotSpacePos));
                                BlockHitResult hit = new BlockHitResult(
                                    new Vec3(blockPos),
                                    Direction.DOWN,
                                    blockPos,
                                    false
                                );
                                TCClient.MCI.gameMode.useItemOn(client.player, InteractionHand.OFF_HAND, hit);

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
    public void onChunkDelta(ClientboundSectionBlocksUpdatePacket packet) {
        packet.runUpdates((blockPos, blockState) -> {
            Vec3 plotOrigin = TCClient.DF_STATE.getPlotOrigin();
            if (plotOrigin == null) return;

            BlockPos immutablePos = blockPos.immutable();

            Vec3i plotSpacePos = TCClient.DF_STATE.toPlotSpace(immutablePos);
            processCodeEditResponse(plotSpacePos,blockState,false);
        });
    }

    @Override
    public void onBlockEntityUpdate(ClientboundBlockEntityDataPacket packet) {}

    @Override
    public void onClientBlockUpdate(BlockPos pos, BlockState state) {
        Vec3 plotOrigin = TCClient.DF_STATE.getPlotOrigin();
        if (plotOrigin == null) return;

        processCodeEditResponse(TCClient.DF_STATE.toPlotSpace(pos), state,true);
    }

    @Override
    public void onModeChanged(DFState.Mode newMode) {
        if (newMode != DFState.Mode.DEV) {
            stopEditing(EndCause.LEFT_DEV_MODE);
        }
    }
}
