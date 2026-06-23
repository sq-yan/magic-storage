package com.sq_yan.magic_storage.book;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the Liber Reigallus written books. Pages are {@link Component#translatable} keys, so a single
 * book localises to both RU and EN on the client — the actual prose lives in the lang files.
 * Each book carries its own recipe as a text grid (the pack has no JEI on this MC version).
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
        List<Filterable<Component>> pages = new ArrayList<>(pageKeys.length);
        for (String key : pageKeys) {
            pages.add(Filterable.passThrough(Component.translatable(key)));
        }
        WrittenBookContent content = new WrittenBookContent(
            Filterable.passThrough(title), AUTHOR, 0, pages, true);
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return stack;
    }
}
