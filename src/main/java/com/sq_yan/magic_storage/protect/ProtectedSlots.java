package com.sq_yan.magic_storage.protect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ProtectedSlots {
    public static final String NBT_KEY = "magic_storage_protected_slots";
    public static final String HOTBAR_KEY = "magic_storage_protect_hotbar";
    public static final int PLAYER_INV_SLOTS = 36;
    public static final int HOTBAR_SLOTS = 9;

    private ProtectedSlots() {}

    /** Whether the hotbar (slots 0..8) is protected from Quick Dump. Default true. */
    public static boolean isHotbarProtected(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        return !persisted.contains(HOTBAR_KEY) || persisted.getBoolean(HOTBAR_KEY);
    }

    public static void setHotbarProtected(Player player, boolean value) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        persisted.putBoolean(HOTBAR_KEY, value);
        root.put(ServerPlayer.PERSISTED_NBT_TAG, persisted);
    }

    public static Set<Integer> read(Player player) {
        CompoundTag persisted = player.getPersistentData().getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        int[] arr = persisted.getIntArray(NBT_KEY);
        Set<Integer> out = new LinkedHashSet<>();
        for (int s : arr) if (s >= 0 && s < PLAYER_INV_SLOTS) out.add(s);
        return out;
    }

    public static void write(Player player, Set<Integer> slots) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
        int[] arr = slots.stream().mapToInt(Integer::intValue).filter(s -> s >= 0 && s < PLAYER_INV_SLOTS).toArray();
        persisted.putIntArray(NBT_KEY, arr);
        root.put(ServerPlayer.PERSISTED_NBT_TAG, persisted);
    }

    /**
     * Drop slot indices that are currently empty. Returns the cleaned set and writes it back if it changed.
     */
    public static Set<Integer> cleanEmpty(Player player) {
        Set<Integer> current = read(player);
        if (current.isEmpty()) return current;
        var inv = player.getInventory();
        Set<Integer> cleaned = new LinkedHashSet<>();
        for (int s : current) {
            if (s >= 0 && s < PLAYER_INV_SLOTS && !inv.getItem(s).isEmpty()) {
                cleaned.add(s);
            }
        }
        if (cleaned.size() != current.size()) write(player, cleaned);
        return cleaned;
    }

    public static int[] toArray(Set<Integer> slots) {
        int[] arr = slots.stream().mapToInt(Integer::intValue).toArray();
        Arrays.sort(arr);
        return arr;
    }
}
