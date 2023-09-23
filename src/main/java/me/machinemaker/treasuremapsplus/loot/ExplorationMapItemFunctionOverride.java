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

public class ExplorationMapItemFunctionOverride {

    private static final LootItemFunction.Builder SET_PDC_FUNCTION;
    private static final LootItemFunction.Builder SET_LORE_FUNCTION = SetLoreFunction.setLore().addLine(PaperAdventure.asVanilla(TreasureMapsPlus.LORE));

    static {
        final CompoundTag pdcNbt = new CompoundTag();
        final CompoundTag pdc = new CompoundTag();
        pdc.put(TreasureMapsPlus.IS_MAP.asString(), ByteTag.valueOf(true));
        pdc.put(TreasureMapsPlus.MAP_STRUCTURE_TAG_KEY.asString(), StringTag.valueOf("minecraft:on_treasure_maps"));
        pdcNbt.put("PublicBukkitValues", pdc);
        SET_PDC_FUNCTION = Utils.createSetNbtFunction(pdcNbt);
    }


    private final boolean replaceChests;
    private final BiMap<LootItemFunction, SequenceFunction> functionMap = HashBiMap.create(3);
    private final RegistryOverride<LootItemFunctionType> registryOverride;

    @SuppressWarnings("unchecked")
    public ExplorationMapItemFunctionOverride(final boolean replaceChests) {
        this.replaceChests = replaceChests;
        final MapCodec.MapCodecCodec<ExplorationMapFunction> mapCodecCodec = (MapCodec.MapCodecCodec<ExplorationMapFunction>) LootItemFunctions.EXPLORATION_MAP.codec();
        final Codec<? extends LootItemFunction> replacementCodec = mapCodecCodec.codec().xmap(this::createSequenceFunction, this::retrieveExplorationFunction).codec();
        this.registryOverride = new RegistryOverride<>(
            Registries.LOOT_FUNCTION_TYPE,
            ResourceKey.create(Registries.LOOT_FUNCTION_TYPE, new ResourceLocation(ResourceLocation.DEFAULT_NAMESPACE, "exploration_map")),
            new LootItemFunctionType(replacementCodec));
    }

    public void override() {
        if (this.replaceChests) {
            this.registryOverride.override();
        }
    }

    public int replaceCount() {
        return this.functionMap.size();
    }

    private SequenceFunction createSequenceFunction(final LootItemFunction explorationFunction) {
        return this.functionMap.computeIfAbsent(explorationFunction, ignored -> {
            return SequenceFunction.of(List.of(SET_PDC_FUNCTION.build(), SET_LORE_FUNCTION.build()));
        });
    }

    @SuppressWarnings("unchecked")
    private <L extends LootItemFunction> L retrieveExplorationFunction(final SequenceFunction sequenceFunction) {
        return (L) this.functionMap.inverse().get(sequenceFunction);
    }

}
