package com.sq_yan.magic_storage.book;

import com.sq_yan.magic_storage.block.CrystalExpanderBlock;
import com.sq_yan.magic_storage.block.HeartStorageT1Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;

/**
 * Hands out the Liber Reigallus books at deterministic milestones (no RNG-gated recipes):
 *  - Book I  on first login                 → Storage Cell + Heart Storage recipes
 *  - Book II on first Heart Storage placed   → Crystal Expander recipe
 *  - Book III on first Crystal Expander placed → Reigall's Tuning Fork recipe
 * Each is given once; the flag lives in PlayerPersisted so it survives death / reconnect.
 */
public final class LoreBookEvents {
    private static final String BOOK1 = "magic_storage_book1";
    private static final String BOOK2 = "magic_storage_book2";
    private static final String BOOK3 = "magic_storage_book3";

    private LoreBookEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(LoreBookEvents::onLogin);
        MinecraftForge.EVENT_BUS.addListener(LoreBookEvents::onPlace);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!hasFlag(player, BOOK1)) {
            give(player, LoreBooks.book1());
            setFlag(player, BOOK1);
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Block block = event.getPlacedBlock().getBlock();
        if (block instanceof HeartStorageT1Block && !hasFlag(player, BOOK2)) {
            give(player, LoreBooks.book2());
            setFlag(player, BOOK2);
        } else if (block instanceof CrystalExpanderBlock && !hasFlag(player, BOOK3)) {
            give(player, LoreBooks.book3());
            setFlag(player, BOOK3);
        }
    }

    private static void give(ServerPlayer player, ItemStack book) {
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }

    private static boolean hasFlag(Player player, String key) {
        return player.getPersistentData()
            .getCompound(ServerPlayer.PERSISTED_NBT_TAG)
            .getBoolean(key);
    }

    private static void setFlag(Player player, String key) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        persisted.putBoolean(key, true);
        root.put(ServerPlayer.PERSISTED_NBT_TAG, persisted);
    }
}
