package com.sq_yan.magic_storage.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * Reflective access to a couple of vanilla fields the GUI needs to read/write:
 * {@code Slot.x} / {@code Slot.y} (we reposition storage slots into a scrolling grid) and
 * {@code AbstractContainerScreen.hoveredSlot} (cross-screen mark overlay).
 *
 * <p>The bundled {@code META-INF/accesstransformer.cfg} ({@code public-f}) makes these fields
 * public and non-final at runtime, so the writes below succeed. We go through reflection with the
 * SRG field names — resolved by {@link ObfuscationReflectionHelper} in both dev (official mappings)
 * and production (SRG) — because ForgeGradle 6.0.54 in this workspace does not apply the project
 * access transformer at compile time (so {@code slot.x = ...} won't javac against final fields).
 * The transformer still applies at runtime, which is what actually matters.
 *
 * <p>SRG names verified against the 1.20.1 Mojang client mappings:
 * {@code Slot.x -> f_40220_}, {@code Slot.y -> f_40221_},
 * {@code AbstractContainerScreen.hoveredSlot -> f_97734_}.
 */
public final class SlotAccess {
    private static final Field SLOT_X = ObfuscationReflectionHelper.findField(Slot.class, "f_40220_");
    private static final Field SLOT_Y = ObfuscationReflectionHelper.findField(Slot.class, "f_40221_");
    private static final Field HOVERED_SLOT =
        ObfuscationReflectionHelper.findField(AbstractContainerScreen.class, "f_97734_");

    private SlotAccess() {}

    /** Move a slot to the given screen-local position. */
    public static void setPos(Slot slot, int x, int y) {
        try {
            SLOT_X.setInt(slot, x);
            SLOT_Y.setInt(slot, y);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("magic_storage: cannot set slot position (AT not applied?)", e);
        }
    }

    /** The slot currently under the cursor in a container screen, or null. */
    public static Slot hoveredSlot(AbstractContainerScreen<?> screen) {
        try {
            return (Slot) HOVERED_SLOT.get(screen);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("magic_storage: cannot read hoveredSlot", e);
        }
    }
}
