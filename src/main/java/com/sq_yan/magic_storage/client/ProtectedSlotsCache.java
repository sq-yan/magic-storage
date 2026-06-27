package com.sq_yan.magic_storage.client;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ProtectedSlotsCache {
    private static volatile Set<Integer> slots = Collections.emptySet();
    private static volatile boolean markMode = false;
    private static volatile boolean hotbarProtected = true;

    private ProtectedSlotsCache() {}

    public static boolean isMarkMode() { return markMode; }
    public static void setMarkMode(boolean v) { markMode = v; }
    public static void toggleMarkMode() { markMode = !markMode; }

    public static boolean isHotbarProtected() { return hotbarProtected; }
    public static void setHotbarProtected(boolean v) { hotbarProtected = v; }

    public static Set<Integer> get() {
        return slots;
    }

    public static boolean contains(int slot) {
        return slots.contains(slot);
    }

    public static void replace(int[] arr, boolean hotbarProtectedValue) {
        Set<Integer> next = new LinkedHashSet<>();
        for (int s : arr) next.add(s);
        slots = Collections.unmodifiableSet(next);
        hotbarProtected = hotbarProtectedValue;
    }

    public static void toggle(int slot) {
        Set<Integer> next = new LinkedHashSet<>(slots);
        if (!next.remove(slot)) next.add(slot);
        slots = Collections.unmodifiableSet(next);
    }

    public static int[] toArray() {
        int[] arr = slots.stream().mapToInt(Integer::intValue).toArray();
        java.util.Arrays.sort(arr);
        return arr;
    }
}
