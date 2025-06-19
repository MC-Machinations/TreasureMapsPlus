/*
 * GNU General Public License v3
 *
 * TreasureMapsPlus, a plugin to alter treasure maps
 *
 * Copyright (C) 2023 Machine-Maker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package me.machinemaker.treasuremapsplus.villager;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import me.machinemaker.treasuremapsplus.TreasureMapsPlus;
import me.machinemaker.treasuremapsplus.Utils;
import net.kyori.adventure.text.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;
import xyz.jpenilla.reflectionremapper.proxy.ReflectionProxyFactory;

import static me.machinemaker.treasuremapsplus.Utils.sneaky;
import static net.kyori.adventure.text.Component.translatable;

public class VillagerTradeOverride {

    private static final Logger LOGGER = LogUtils.getClassLogger();
    private static final Class<?> TREASURE_MAP_TRADE_LISTING_CLASS;
    private static final Class<?> TYPE_SPECIFIC_TRADE_CLASS;

    private static final TreasureMapForEmeraldsProxy TREASURE_MAP_PROXY;
    private static final TypeSpecificTradeProxy TYPE_SPECIFIC_TRADE_PROXY;


    private static final List<Collection<VillagerTrades.ItemListing[]>> TRADE_SETS = List.of(
        VillagerTrades.TRADES.get(VillagerProfession.CARTOGRAPHER).values()
    );


    static {
        final ReflectionRemapper remapper = ReflectionRemapper.noop();
        TREASURE_MAP_TRADE_LISTING_CLASS = sneaky(() -> Class.forName(remapper.remapClassName("net.minecraft.world.entity.npc.VillagerTrades$TreasureMapForEmeralds")));
        TYPE_SPECIFIC_TRADE_CLASS = sneaky(() -> Class.forName(remapper.remapClassName("net.minecraft.world.entity.npc.VillagerTrades$TypeSpecificTrade")));


        final ReflectionProxyFactory factory = ReflectionProxyFactory.create(remapper, VillagerTradeOverride.class.getClassLoader());
        TREASURE_MAP_PROXY = factory.reflectionProxy(TreasureMapForEmeraldsProxy.class);
        TYPE_SPECIFIC_TRADE_PROXY = factory.reflectionProxy(TypeSpecificTradeProxy.class);
    }

    private final List<Component> lore;
    private final boolean replaceMonuments;
    private final boolean replaceMansions;

    public VillagerTradeOverride(final TreasureMapsPlus plugin) {
        this(plugin.getMapUseLore(), plugin.shouldReplaceMonuments(), plugin.shouldReplaceMansions());
    }

    @VisibleForTesting
    VillagerTradeOverride(final List<Component> lore, final boolean replaceMonuments, final boolean replaceMansions) {
        this.lore = lore;
        this.replaceMonuments = replaceMonuments;
        this.replaceMansions = replaceMansions;
    }

    public int override() {
        int changed = 0;
        for (final Collection<VillagerTrades.ItemListing[]> tradeSet : TRADE_SETS) {
            for (final VillagerTrades.ItemListing[] listings : tradeSet) {
                for (int i = 0; i < listings.length; i++) {
                    final VillagerTrades.ItemListing listing = listings[i];
                    if (TREASURE_MAP_TRADE_LISTING_CLASS.isInstance(listing)) {
                        final VillagerTrades.ItemListing override = this.createOverride(listing);
                        if (listing != override) {
                            changed++;
                        }
                        listings[i] = override;
                    } else if (TYPE_SPECIFIC_TRADE_CLASS.isInstance(listing)) {
                        final Map<ResourceKey<VillagerType>, VillagerTrades.ItemListing> trades = TYPE_SPECIFIC_TRADE_PROXY.trades(listing);
                        final ImmutableMap.Builder<ResourceKey<VillagerType>, VillagerTrades.ItemListing> newBuilder = ImmutableMap.builder();
                        for (final Map.Entry<ResourceKey<VillagerType>, VillagerTrades.ItemListing> entry : trades.entrySet()) {
                            if (!(TREASURE_MAP_TRADE_LISTING_CLASS.isInstance(entry.getValue()))) {
                                newBuilder.put(entry);
                            } else {
                                final VillagerTrades.ItemListing override = this.createOverride(entry.getValue());
                                if (entry.getValue() != override) {
                                    changed++;
                                }
                                newBuilder.put(entry.getKey(), override);
                            }
                        }
                        listings[i] = TYPE_SPECIFIC_TRADE_PROXY.create(newBuilder.build());
                    }
                }
            }
        }

        return changed;
    }

    private VillagerTrades.ItemListing createOverride(final VillagerTrades.ItemListing original) {
        if (original instanceof OverrideListing) {
            return original;
        }
        final String name = TREASURE_MAP_PROXY.displayName(original);
        if (name.endsWith("monument")) {
            return this.replaceMonuments ? new OverrideListing(original, this::createStack) : original;
        } else if (name.endsWith("mansion")) {
            return this.replaceMansions ? new OverrideListing(original, this::createStack) : original;
        } else if (name.startsWith("filled_map.village_") || name.startsWith("filled_map.explorer_") || name.endsWith(".trial_chambers")) {
            // skip these maps, no point in replacing them, it defeats their whole purpose
            return original;
        } else {
            LOGGER.warn("Unhandled villager trade type \"{}\"", name);
            return original;
        }
    }

    private ItemStack createStack(final VillagerTrades.ItemListing listing) {
        final ItemStack map = new ItemStack(Items.MAP);
        final org.bukkit.inventory.ItemStack bukkitStack = Utils.getBukkitStackMirror(map);
        final boolean edited = bukkitStack.editMeta(meta -> {
            meta.displayName(translatable(TREASURE_MAP_PROXY.displayName(listing)));
            meta.lore(this.lore);
            meta.getPersistentDataContainer().set(TreasureMapsPlus.IS_MAP, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(TreasureMapsPlus.MAP_STRUCTURE_TAG_KEY, PersistentDataType.STRING, TREASURE_MAP_PROXY.destination(listing).location().toString());
        });
        Preconditions.checkState(edited, "Could not edit itemstack meta");
        return map;
    }

    private record OverrideListing(VillagerTrades.ItemListing original, Function<VillagerTrades.ItemListing, ItemStack> mapStackCreator) implements VillagerTrades.ItemListing {

        private static final float PRICE_MULTIPLIER = 0.2f;

        @Override
        public @Nullable MerchantOffer getOffer(final Entity entity, final RandomSource random) {
            if (!entity.level().paperConfig().environment.treasureMaps.enabled) {
                return null;
            }
            return new MerchantOffer(
                new ItemCost(Items.EMERALD, TREASURE_MAP_PROXY.emeraldCost(this.original)),
                Optional.of(new ItemCost(Items.COMPASS)),
                this.mapStackCreator.apply(this.original),
                TREASURE_MAP_PROXY.maxUses(this.original),
                TREASURE_MAP_PROXY.villagerXp(this.original),
                PRICE_MULTIPLIER
            );
        }
    }
}
