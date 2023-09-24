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

import java.util.Collections;
import java.util.List;
import me.machinemaker.treasuremapsplus.listener.PlayerInteract;
import me.machinemaker.treasuremapsplus.loot.ExplorationMapItemFunctionOverride;
import me.machinemaker.treasuremapsplus.villager.VillagerTradeOverride;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class TreasureMapsPlus extends JavaPlugin {

    public static final String NAMESPACE = "treasuremapsplus";
    public static final NamespacedKey IS_MAP = new NamespacedKey(NAMESPACE, "is_map");
    public static final NamespacedKey MAP_STRUCTURE_TAG_KEY = new NamespacedKey(NAMESPACE, "map_tag_key");

    private final List<Component> mapUseLore;
    private final boolean replaceChests;
    private final boolean replaceMonuments;
    private final boolean replaceMansions;

    private final ReplacementResult result;

    public TreasureMapsPlus() throws Exception {
        this.saveDefaultConfig();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
        this.mapUseLore = this.readLore();
        this.replaceChests = this.getConfig().getBoolean("replace.chests", false);
        this.replaceMonuments = this.getConfig().getBoolean("replace.villagers.monument", false);
        this.replaceMansions = this.getConfig().getBoolean("replace.villagers.mansion", false);
        final ExplorationMapItemFunctionOverride mapFunctionOverride = new ExplorationMapItemFunctionOverride(MinecraftServer.getServer().registryAccess(), this);
        mapFunctionOverride.override();
        DatapackOverride.deleteLeftoversAndReload(); // must reload resources override to count how many were changed
        final VillagerTradeOverride villagerTradeOverride = new VillagerTradeOverride(this);
        final int replacedTrades = villagerTradeOverride.override();
        this.result = new ReplacementResult(mapFunctionOverride.replaceCount(), replacedTrades);
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new PlayerInteract(), this);
        this.getSLF4JLogger().info("Found and replaced {} loot tables with a treasure map", this.result.lootTableCount);
        this.getSLF4JLogger().info("Found and replaced {} villager trades with a treasure map", this.result.tradeCount);

    }

    private List<Component> readLore() {
        final List<String> list = this.getConfig().getStringList("messages.map.use");
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(MiniMessage.miniMessage()::deserialize).toList();
    }

    public List<Component> getMapUseLore() {
        return this.mapUseLore;
    }

    public boolean shouldReplaceChests() {
        return this.replaceChests;
    }

    public boolean shouldReplaceMonuments() {
        return this.replaceMonuments;
    }

    public boolean shouldReplaceMansions() {
        return this.replaceMansions;
    }

    private record ReplacementResult(int lootTableCount, int tradeCount) {
    }
}
