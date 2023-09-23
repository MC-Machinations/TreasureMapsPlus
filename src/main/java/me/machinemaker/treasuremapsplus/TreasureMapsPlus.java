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
package me.machinemaker.treasuremapsplus;

import me.machinemaker.treasuremapsplus.listener.PlayerInteract;
import me.machinemaker.treasuremapsplus.loot.ExplorationMapItemFunctionOverride;
import me.machinemaker.treasuremapsplus.villager.VillagerTradeOverride;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.Style.style;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

public final class TreasureMapsPlus extends JavaPlugin {

    public static final String NAMESPACE = "treasuremapsplus";
    public static final NamespacedKey IS_MAP = new NamespacedKey(NAMESPACE, "is_map");
    public static final NamespacedKey MAP_STRUCTURE_TAG_KEY = new NamespacedKey(NAMESPACE, "map_tag_key");
    public static final Component LORE = text("Use to receive treasure!", style(GREEN).decoration(ITALIC, false));

    private final ExplorationMapItemFunctionOverride explorationMapItemFunctionOverride;
    private final int changedVillagerTrades;

    public TreasureMapsPlus() throws Exception {
        this.saveDefaultConfig();
        this.explorationMapItemFunctionOverride = new ExplorationMapItemFunctionOverride(MinecraftServer.getServer().registryAccess(), this.getConfig().getBoolean("replace.chests", false));
        this.explorationMapItemFunctionOverride.override();
        DatapackOverride.deleteLeftoversAndReload();
        this.changedVillagerTrades = VillagerTradeOverride.setup(
            this.getConfig().getBoolean("replace.villagers.monument", false),
            this.getConfig().getBoolean("replace.villagers.mansion", false)
        );
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new PlayerInteract(), this);
        this.getSLF4JLogger().info("Found and replaced {} loot tables with a treasure map", this.explorationMapItemFunctionOverride.replaceCount());
        this.getSLF4JLogger().info("Found and replaced {} villager trades with a treasure map", this.changedVillagerTrades);

    }
}
