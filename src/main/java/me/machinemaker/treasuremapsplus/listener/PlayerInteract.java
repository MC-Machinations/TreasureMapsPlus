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
package me.machinemaker.treasuremapsplus.listener;

import java.util.ArrayList;
import java.util.List;
import me.machinemaker.treasuremapsplus.TreasureMapsPlus;
import me.machinemaker.treasuremapsplus.Utils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PlayerInteract implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEvent(final PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY || event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        if (isSpecialMap(event.getItem())) {
            event.setUseItemInHand(Event.Result.DENY);
            final ServerPlayer serverPlayer = Utils.getNmsPlayer(event.getPlayer());
            final List<net.minecraft.world.item.ItemStack> randomItems = rollLootTable(event.getItem(), serverPlayer);
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.getItem().setAmount(event.getItem().getAmount() - 1);
            }
            for (final net.minecraft.world.item.ItemStack randomItem : randomItems) {
                event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), Utils.getBukkitStackMirror(randomItem.copy()), item1 -> {
                    item1.setPickupDelay(0);
                });
            }
        }
    }

    private static List<net.minecraft.world.item.ItemStack> rollLootTable(final ItemStack item, final ServerPlayer player) {
        boolean isChest = true;
        @Nullable ResourceLocation lootTable = null;
        final PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        final @Nullable String tagKey = pdc.get(TreasureMapsPlus.MAP_STRUCTURE_TAG_KEY, PersistentDataType.STRING);
        if (tagKey != null) {
            final TagKey<Structure> structureTagKey = TagKey.create(Registries.STRUCTURE, new ResourceLocation(tagKey));
            if (structureTagKey == StructureTags.ON_TREASURE_MAPS) {
                lootTable = BuiltInLootTables.BURIED_TREASURE;
            } else if (structureTagKey == StructureTags.ON_OCEAN_EXPLORER_MAPS) {
                isChest = false;
                lootTable = EntityType.ELDER_GUARDIAN.getDefaultLootTable();
            } else if (structureTagKey == StructureTags.ON_WOODLAND_EXPLORER_MAPS) {
                lootTable = BuiltInLootTables.WOODLAND_MANSION;
            }
        }
        if (lootTable == null) {
            lootTable = BuiltInLootTables.BURIED_TREASURE;
        }

        final LootParams params = isChest ? createChestParams(player) : createEntityParams(player);
        final List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
        Utils.getServer().getLootData().getLootTable(lootTable).getRandomItems(params, items::add);
        return items;

    }

    private static LootParams createChestParams(final ServerPlayer player) {
        return new LootParams.Builder(player.serverLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(player.blockPosition()))
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .withLuck(player.getLuck())
            .create(LootContextParamSets.CHEST);
    }

    private static LootParams createEntityParams(final ServerPlayer player) {
        final ElderGuardian guardian = new ElderGuardian(EntityType.ELDER_GUARDIAN, player.level());
        return new LootParams.Builder(player.serverLevel())
            .withParameter(LootContextParams.THIS_ENTITY, guardian)
            .withParameter(LootContextParams.ORIGIN, player.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, player.serverLevel().damageSources().playerAttack(player))
            .withOptionalParameter(LootContextParams.KILLER_ENTITY, player)
            .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, player)
            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
            .withLuck(player.getLuck())
            .create(LootContextParamSets.ENTITY);
    }

    private static boolean isSpecialMap(final @Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.MAP && stack.getItemMeta().getPersistentDataContainer().has(TreasureMapsPlus.IS_MAP, PersistentDataType.BYTE);
    }
}
