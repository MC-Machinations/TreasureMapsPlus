/*
 * GNU General Public License v3
 *
 * TreasureMapsPlus, a plugin to alter treasure maps
 *
 * Copyright (C) 2024 Machine-Maker
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

import me.machinemaker.treasuremapsplus.DatapackOverride;
import me.machinemaker.treasuremapsplus.TreasureMapsPlus;
import me.machinemaker.treasuremapsplus.loot.ExplorationMapItemFunctionOverride;
import me.machinemaker.treasuremapsplus.villager.VillagerTradeOverride;
import net.minecraft.server.MinecraftServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class ServerLoad implements Listener {

    final TreasureMapsPlus plugin;

    public ServerLoad(final TreasureMapsPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEvent(final ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.STARTUP) {
            try {
                final ExplorationMapItemFunctionOverride mapFunctionOverride = new ExplorationMapItemFunctionOverride(MinecraftServer.getServer().registryAccess(), this.plugin);
                mapFunctionOverride.override();
                this.plugin.getSLF4JLogger().info("Reloading the server resources to apply treasure map changes...");
                DatapackOverride.deleteLeftoversAndReload();
                final VillagerTradeOverride villagerTradeOverride = new VillagerTradeOverride(this.plugin);
                final int replacedTrades = villagerTradeOverride.override();
                this.plugin.getSLF4JLogger().info("Found and replaced {} loot tables with a treasure map", mapFunctionOverride.replaceCount());
                this.plugin.getSLF4JLogger().info("Found and replaced {} villager trades with a treasure map", replacedTrades);
            } catch (final Exception ex) {
                throw new RuntimeException("Could not load this plugin", ex);
            }
        }
    }
}
