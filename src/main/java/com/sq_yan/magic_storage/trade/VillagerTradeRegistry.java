package com.sq_yan.magic_storage.trade;

import com.sq_yan.magic_storage.registry.MSItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;

import java.util.List;

public final class VillagerTradeRegistry {
    private static final String HEART_BOUGHT_KEY = "magic_storage_heart_bought";
    private static final int HEART_PRICE = 7;
    private static final int CRYSTAL_PRICE = 10;
    private static final int CRYSTAL_LEVEL = 3;

    private VillagerTradeRegistry() {}

    public static void register() {
        VillagerTradesEvent.BUS.addListener(VillagerTradeRegistry::onVillagerTrades);
        TradeWithVillagerEvent.BUS.addListener(VillagerTradeRegistry::onTradeWithVillager);
        PlayerInteractEvent.EntityInteract.BUS.addListener(VillagerTradeRegistry::onEntityInteract);
    }

    /** Crystal still goes through the vanilla pool — random reroll loop is intentional gameplay. */
    private static void onVillagerTrades(VillagerTradesEvent event) {
        if (!event.getType().equals(VillagerProfession.LIBRARIAN)) return;
        List<VillagerTrades.ItemListing> crystalLevel = event.getTrades().get(CRYSTAL_LEVEL);
        if (crystalLevel != null) crystalLevel.add(new CrystalListing());
    }

    private static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (event.getMerchantOffer().getResult().is(MSItems.MAGIC_HEART.get())) {
            markHeartBought(event.getEntity());
        }
    }

    /**
     * Synchronise every librarian's offer list with this player's heart-bought flag on right-click:
     *  - flag NOT set & no Heart offer present → inject one (persistent on this villager).
     *  - flag IS set  & Heart offer present    → remove it (also persistent — next no-flag player re-injects).
     *
     *  Net effect: any librarian sells the Heart to any player who hasn't bought yet, regardless of level
     *  or trade-pool RNG. After one player buys, the Heart vanishes for that player on every librarian forever.
     */
    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof Villager villager)) return;
        if (!villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN)) return;

        MerchantOffers offers = villager.getOffers();
        if (offers == null) return;

        boolean hasHeartOffer = offers.stream().anyMatch(o -> o.getResult().is(MSItems.MAGIC_HEART.get()));
        boolean bought = hasBoughtHeart(event.getEntity());

        if (bought) {
            if (hasHeartOffer) offers.removeIf(o -> o.getResult().is(MSItems.MAGIC_HEART.get()));
        } else {
            if (!hasHeartOffer) offers.add(createHeartOffer());
        }
    }

    private static MerchantOffer createHeartOffer() {
        return new MerchantOffer(
            new ItemCost(Items.EMERALD, HEART_PRICE),
            new ItemStack(MSItems.MAGIC_HEART.get(), 1),
            1, 30, 0.0f
        );
    }

    public static boolean hasBoughtHeart(Player player) {
        return player.getPersistentData()
            .getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG)
            .getBooleanOr(HEART_BOUGHT_KEY, false);
    }

    public static void markHeartBought(Player player) {
        CompoundTag root = player.getPersistentData();
        CompoundTag persisted = root.getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        persisted.putBoolean(HEART_BOUGHT_KEY, true);
        root.put(ServerPlayer.PERSISTED_NBT_TAG, persisted);
    }

    private static final class CrystalListing implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(ServerLevel level, Entity trader, RandomSource random) {
            return new MerchantOffer(
                new ItemCost(Items.EMERALD, CRYSTAL_PRICE),
                new ItemStack(MSItems.MAGIC_CRYSTAL.get(), 1),
                12, 15, 0.05f
            );
        }
    }
}
