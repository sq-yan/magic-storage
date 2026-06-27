package com.sq_yan.magic_storage.menu;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

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
        Item item = stack.getItem();
        return switch (this) {
            case NONE -> true;
            case POTIONS -> item instanceof PotionItem;
            case ARMOR -> item instanceof ArmorItem;
            case TOOLS -> item instanceof TieredItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || item instanceof FishingRodItem
                || item instanceof ShieldItem;
            case FOOD -> stack.isEdible();
            case BLOCKS -> item instanceof BlockItem;
        };
    }
}
