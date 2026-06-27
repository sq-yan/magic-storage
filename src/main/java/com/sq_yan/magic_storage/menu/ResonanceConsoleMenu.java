package com.sq_yan.magic_storage.menu;

import com.sq_yan.magic_storage.blockentity.HeartStorageBlockEntity;
import com.sq_yan.magic_storage.blockentity.StorageCellBlockEntity;
import com.sq_yan.magic_storage.net.AggregatedItemHandler;
import com.sq_yan.magic_storage.registry.MSBlocks;
import com.sq_yan.magic_storage.registry.MSMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Reigall's Resonance Console — opened by right-clicking a Heart Storage with the Tuning Fork.
 * Shows the whole cell network as a spatial map; selecting a cell exposes its 27 slots as live
 * slots, and a Defragment button compacts the network. Slot model mirrors {@link MagicStorageMenu}:
 * one real slot per cell-slot (cell {@code k} owns aggregated slots {@code [k*27, k*27+27)}),
 * positioned on-screen by the client; the rest stay off-screen.
 */
public class ResonanceConsoleMenu extends AbstractContainerMenu {

    public static final int PLAYER_INV_SIZE = 36;
    public static final int PLAYER_INV_X = 47;
    public static final int PLAYER_INV_Y = 150;
    public static final int HOTBAR_Y = 210;

    public static final int BUTTON_DEFRAG = 1;
    /** Cell→cell move buttons are encoded as {@code TRANSFER_BASE + from*100 + to} (cell count is well under 100). */
    public static final int TRANSFER_BASE = 1000;

    private static final int OFFSCREEN = -2000;
    private static final int REBUILD_INTERVAL_TICKS = 10;

    private final @Nullable HeartStorageBlockEntity heart;
    private final BlockPos heartPos;
    private final AggregatedItemHandler aggregated;
    private final ContainerLevelAccess access;

    private int aggregatedSlotCount;
    private List<BlockPos> cellPositions = List.of();
    private int rebuildCounter = 0;

    /** Client constructor — reads heart pos + cell positions from the network buffer. */
    public ResonanceConsoleMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        super(MSMenus.RESONANCE_CONSOLE.get(), id);
        Level level = inv.player.level();
        this.heartPos = buf.readBlockPos();
        List<BlockPos> positions = readPositions(buf);
        this.heart = level.getBlockEntity(heartPos) instanceof HeartStorageBlockEntity h ? h : null;
        this.aggregated = AggregatedItemHandler.fromPositions(level, positions);
        this.access = ContainerLevelAccess.create(level, heartPos);
        addSlots(inv);
    }

    /** Server constructor — builds the live aggregated handler from the heart's connected cells. */
    public ResonanceConsoleMenu(int id, Inventory inv, HeartStorageBlockEntity heart) {
        super(MSMenus.RESONANCE_CONSOLE.get(), id);
        this.heart = heart;
        this.heartPos = heart.getBlockPos();
        Level level = heart.getLevel();
        this.aggregated = heart.buildAggregated();
        this.access = level != null
            ? ContainerLevelAccess.create(level, heartPos)
            : ContainerLevelAccess.NULL;
        addSlots(inv);
    }

    private void addSlots(Inventory inv) {
        this.aggregatedSlotCount = aggregated.getSlots();
        this.cellPositions = aggregated.cellPositions();

        for (int i = 0; i < aggregatedSlotCount; i++) {
            addSlot(new SlotItemHandler(aggregated, i, OFFSCREEN, OFFSCREEN));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, 9 + row * 9 + col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    public int getAggregatedSlotCount() {
        return aggregatedSlotCount;
    }

    public BlockPos getHeartPos() {
        return heartPos;
    }

    /** Cell positions in connection order — index {@code k} owns slots {@code [k*27, k*27+27)}. */
    public List<BlockPos> getCellPositions() {
        return cellPositions;
    }

    public int getCellCount() {
        return cellPositions.size();
    }

    /** Non-empty slot count for cell {@code cellIdx}, computed from the synced slots (works client-side). */
    public int usedInCell(int cellIdx) {
        int base = cellIdx * StorageCellBlockEntity.SIZE;
        int used = 0;
        for (int j = 0; j < StorageCellBlockEntity.SIZE && base + j < aggregatedSlotCount; j++) {
            if (!this.slots.get(base + j).getItem().isEmpty()) used++;
        }
        return used;
    }

    // openMenu = single source of truth for slot count. Here we only refresh the BE references in
    // the aggregated handler in case a chunk reloaded; if the connected count changed the player
    // must reopen the console.
    private void refreshCellReferencesIfStable() {
        if (heart == null || heart.getLevel() == null || heart.getLevel().isClientSide()) return;
        if (heart.isRemoved()) return;
        List<StorageCellBlockEntity> fresh = heart.getConnectedCells();
        if (fresh.size() * StorageCellBlockEntity.SIZE != aggregatedSlotCount) return;
        aggregated.rebuild(fresh);
    }

    @Override
    public void broadcastChanges() {
        if (++rebuildCounter >= REBUILD_INTERVAL_TICKS) {
            rebuildCounter = 0;
            refreshCellReferencesIfStable();
        }
        super.broadcastChanges();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int idx) {
        Slot slot = this.slots.get(idx);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        int playerStart = aggregatedSlotCount;
        int playerEnd = aggregatedSlotCount + PLAYER_INV_SIZE;

        if (idx < playerStart) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!aggregated.distribute(stack)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        Level level = player.level();
        if (id == BUTTON_DEFRAG) {
            if (heart != null && !level.isClientSide()) {
                boolean moved = heart.defragment();
                if (moved) {
                    level.playSound(null, heartPos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, 1.2F);
                }
                if (player instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable(
                        moved ? "message.magic_storage.defrag_done" : "message.magic_storage.defrag_empty"), true);
                }
            }
            return true;
        }
        if (id >= TRANSFER_BASE) {
            if (heart != null && !level.isClientSide()) {
                int t = id - TRANSFER_BASE;
                boolean moved = transferCell(t / 100, t % 100);
                if (moved) {
                    level.playSound(null, heartPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 1.4F);
                }
            }
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    /** Move every stack from cell {@code fromIdx} into cell {@code toIdx} (whatever fits). Returns true if anything moved. */
    private boolean transferCell(int fromIdx, int toIdx) {
        if (heart == null) return false;
        List<StorageCellBlockEntity> cells = heart.getConnectedCells();
        if (fromIdx == toIdx || fromIdx < 0 || toIdx < 0
            || fromIdx >= cells.size() || toIdx >= cells.size()) {
            return false;
        }
        net.minecraftforge.items.ItemStackHandler src = cells.get(fromIdx).getItems();
        net.minecraftforge.items.ItemStackHandler dst = cells.get(toIdx).getItems();
        boolean moved = false;
        for (int i = 0; i < src.getSlots(); i++) {
            ItemStack s = src.getStackInSlot(i);
            if (s.isEmpty()) continue;
            ItemStack remaining = s.copy();
            for (int j = 0; j < dst.getSlots() && !remaining.isEmpty(); j++) {
                remaining = dst.insertItem(j, remaining, false);
            }
            if (remaining.getCount() != s.getCount()) {
                moved = true;
                src.setStackInSlot(i, remaining);
            }
        }
        return moved;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, MSBlocks.HEART_STORAGE.get());
    }

    private static List<BlockPos> readPositions(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<BlockPos> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) result.add(buf.readBlockPos());
        return result;
    }
}
