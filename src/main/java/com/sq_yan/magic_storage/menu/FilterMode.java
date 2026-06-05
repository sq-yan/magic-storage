package com.sq_yan.magic_storage.menu;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.equipment.Equippable;

public enum FilterMode {
    NONE,
    POTIONS,
    ARMOR,
    TOOLS,
    FOOD,
    BLOCKS;

    public FilterMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return switch (this) {
            case NONE -> true;
            case POTIONS -> stack.has(DataComponents.POTION_CONTENTS);
            case ARMOR -> {
                Equippable eq = stack.get(DataComponents.EQUIPPABLE);
                yield eq != null && isArmorSlot(eq.slot());
            }
            case TOOLS -> {
                var item = stack.getItem();
                yield stack.has(DataComponents.TOOL)
                    || stack.has(DataComponents.WEAPON)
                    || item instanceof BowItem
                    || item instanceof CrossbowItem
                    || item instanceof TridentItem
                    || item instanceof FishingRodItem
                    || item instanceof ShieldItem;
            }
            case FOOD -> stack.has(DataComponents.FOOD);
            case BLOCKS -> stack.getItem() instanceof BlockItem;
        };
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD
            || slot == EquipmentSlot.CHEST
            || slot == EquipmentSlot.LEGS
            || slot == EquipmentSlot.FEET
            || slot == EquipmentSlot.BODY;
    }
}
