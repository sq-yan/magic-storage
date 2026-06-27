package com.sq_yan.magic_storage.book;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Builds the Liber Reigallus written books. Pages are {@link Component#translatable} keys, so a single
 * book localises to both RU and EN on the client — the actual prose lives in the lang files.
 * Each book carries its own recipe as a text grid (the pack has no JEI on this MC version).
 *
 * <p>1.20.1 has no data components — written books are stored as item NBT: {@code title}, {@code author},
 * and a {@code pages} list of JSON-serialised text components. {@code resolved=true} keeps the
 * translatable pages from being re-parsed as legacy strings.
 */
public final class LoreBooks {
    private static final String AUTHOR = "Reigall";

    private LoreBooks() {}

    public static ItemStack book1() {
        return make("Liber Reigallus I",
            "book.magic_storage.reigallus1.p1",
            "book.magic_storage.reigallus1.p2",
            "book.magic_storage.reigallus1.p3",
            "book.magic_storage.reigallus1.p4");
    }

    public static ItemStack book2() {
        return make("Liber Reigallus II",
            "book.magic_storage.reigallus2.p1",
            "book.magic_storage.reigallus2.p2",
            "book.magic_storage.reigallus2.p3");
    }

    public static ItemStack book3() {
        return make("Liber Reigallus III",
            "book.magic_storage.reigallus3.p1",
            "book.magic_storage.reigallus3.p2",
            "book.magic_storage.reigallus3.p3");
    }

    private static ItemStack make(String title, String... pageKeys) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", title);
        tag.putString("author", AUTHOR);
        ListTag pages = new ListTag();
        for (String key : pageKeys) {
            String json = Component.Serializer.toJson(Component.translatable(key));
            pages.add(StringTag.valueOf(json));
        }
        tag.put("pages", pages);
        tag.putBoolean("resolved", true);
        return stack;
    }
}
