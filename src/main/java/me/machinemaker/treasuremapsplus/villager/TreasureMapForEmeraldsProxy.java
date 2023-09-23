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

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter;
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies;

@Proxies(className = "net.minecraft.world.entity.npc.VillagerTrades$TreasureMapForEmeralds")
interface TreasureMapForEmeraldsProxy {

    @FieldGetter("emeraldCost")
    int emeraldCost(Object instance);

    @FieldGetter("destination")
    TagKey<Structure> destination(Object instance);

    @FieldGetter("displayName")
    String displayName(Object instance);

    @FieldGetter("maxUses")
    int maxUses(Object instance);

    @FieldGetter("villagerXp")
    int villagerXp(Object instance);
}
