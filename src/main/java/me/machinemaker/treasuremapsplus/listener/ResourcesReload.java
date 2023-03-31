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

import io.papermc.paper.datapack.Datapack;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import me.machinemaker.treasuremapsplus.DatapackOverride;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ResourcesReload implements Listener {

    private final Plugin plugin;

    public ResourcesReload(final Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEvent(final ServerResourcesReloadedEvent event) throws Exception {
        for (final Datapack pack : Bukkit.getDatapackManager().getPacks()) {
            if (pack.getName().equals("file/" + DatapackOverride.DATAPACK_NAME)) {
                if (!pack.isEnabled()) {
                    this.plugin.getSLF4JLogger().warn("You must leave the datapack {} enabled for this plugin to work", pack.getName());
                    Bukkit.getScheduler().runTaskLater(this.plugin, () -> pack.setEnabled(true), 1L);
                }
                return;
            }
        }
        this.plugin.getSLF4JLogger().warn("Could not find the datapack for this plugin, rebuilding it...");
        DatapackOverride.setup(this.plugin.getConfig().getBoolean("replace.chests"));
    }
}
