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
package me.machinemaker.treasuremapsplus.loot;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.papermc.paper.adventure.PaperAdventure;
import java.util.List;
import me.machinemaker.treasuremapsplus.RegistryOverride;
import me.machinemaker.treasuremapsplus.TreasureMapsPlus;
import me.machinemaker.treasuremapsplus.Utils;
import net.kyori.adventure.text.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.functions.SequenceFunction;
import net.minecraft.world.level.storage.loot.functions.SetLoreFunction;
import org.jetbrains.annotations.VisibleForTesting;

public class ExplorationMapItemFunctionOverride {

    @VisibleForTesting
    static final ResourceKey<LootItemFunctionType> EXPLORATION_FUNCTION_KEY = ResourceKey.create(Registries.LOOT_FUNCTION_TYPE, new ResourceLocation(ResourceLocation.DEFAULT_NAMESPACE, "exploration_map"));
    private static final LootItemFunction.Builder SET_PDC_FUNCTION;

    static {
        final CompoundTag pdcNbt = new CompoundTag();
        final CompoundTag pdc = new CompoundTag();
        pdc.put(TreasureMapsPlus.IS_MAP.asString(), ByteTag.valueOf(true));
        pdc.put(TreasureMapsPlus.MAP_STRUCTURE_TAG_KEY.asString(), StringTag.valueOf("minecraft:on_treasure_maps"));
        pdcNbt.put("PublicBukkitValues", pdc);
        SET_PDC_FUNCTION = Utils.createSetNbtFunction(pdcNbt);
    }


    @VisibleForTesting
    final RegistryOverride<LootItemFunctionType> registryOverride;
    private final RegistryAccess access;
    private final List<Component> lore;
    private final boolean replaceChests;
    private final BiMap<LootItemFunction, SequenceFunction> functionMap = HashBiMap.create(3);

    public ExplorationMapItemFunctionOverride(final RegistryAccess registryAccess, final TreasureMapsPlus plugin) {
        this(registryAccess, plugin.getMapUseLore(), plugin.shouldReplaceChests());
    }

    @VisibleForTesting
    @SuppressWarnings("unchecked")
    ExplorationMapItemFunctionOverride(final RegistryAccess registryAccess, final List<Component> lore, final boolean replaceChests) {
        this.access = registryAccess;
        this.lore = lore;
        this.replaceChests = replaceChests;
        final MapCodec.MapCodecCodec<ExplorationMapFunction> mapCodecCodec = (MapCodec.MapCodecCodec<ExplorationMapFunction>) LootItemFunctions.EXPLORATION_MAP.codec();
        final Codec<? extends LootItemFunction> replacementCodec = mapCodecCodec.codec().xmap(this::createSequenceFunction, this::retrieveExplorationFunction).codec();
        this.registryOverride = new RegistryOverride<>(
            Registries.LOOT_FUNCTION_TYPE,
            EXPLORATION_FUNCTION_KEY,
            new LootItemFunctionType(replacementCodec));
    }

    public void override() {
        if (this.replaceChests) {
            this.registryOverride.override(this.access);
        }
    }

    public int replaceCount() {
        return this.functionMap.size();
    }

    private SequenceFunction createSequenceFunction(final LootItemFunction explorationFunction) {
        return this.functionMap.computeIfAbsent(explorationFunction, ignored -> {
            final SetLoreFunction.Builder setLoreBuilder = SetLoreFunction.setLore();
            this.lore.stream().map(PaperAdventure::asVanilla).forEach(setLoreBuilder::addLine);
            return SequenceFunction.of(List.of(SET_PDC_FUNCTION.build(), setLoreBuilder.build()));
        });
    }

    @SuppressWarnings("unchecked")
    private <L extends LootItemFunction> L retrieveExplorationFunction(final SequenceFunction sequenceFunction) {
        return (L) this.functionMap.inverse().get(sequenceFunction);
    }

}
