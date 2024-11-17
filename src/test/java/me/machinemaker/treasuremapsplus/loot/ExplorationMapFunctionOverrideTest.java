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

import java.util.Collections;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExplorationMapFunctionOverrideTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testFunctionOverride() {
        final ExplorationMapItemFunctionOverride override = new ExplorationMapItemFunctionOverride(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), Collections.emptyList(), true);
        final LootItemFunctionType<?> replacementValue = override.registryOverride.value();
        final LootItemFunctionType<?> oldValue = LootItemFunctions.EXPLORATION_MAP;
        assertEquals(oldValue, BuiltInRegistries.LOOT_FUNCTION_TYPE.getValueOrThrow(ExplorationMapItemFunctionOverride.EXPLORATION_FUNCTION_KEY), "the value in the registry didn't match the value in the field");
        override.override();
        assertEquals(replacementValue, BuiltInRegistries.LOOT_FUNCTION_TYPE.getValueOrThrow(ExplorationMapItemFunctionOverride.EXPLORATION_FUNCTION_KEY), "the replacement value wasn't found in the registry");
    }
}
