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

import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.file.PathUtils;

public final class DatapackOverride {

    public static final String DATAPACK_NAME = "TreasureMapsPlus";

    private DatapackOverride() {
    }

    public static void deleteLeftoversAndReload() throws Exception {
        // datapack was only useful pre 1.20.1
        final Path datapackDir = Utils.getServer().storageSource.getLevelPath(LevelResource.DATAPACK_DIR).resolve(DATAPACK_NAME);
        if (Files.exists(datapackDir)) {
            PathUtils.deleteDirectory(datapackDir);
        }

        Utils.getServer().getPackRepository().reload();
        final Collection<String> selected = Utils.getServer().getPackRepository().getSelectedPacks().stream().map(Pack::getId).collect(Collectors.toCollection(ArrayList::new));
        selected.remove("file/" + DATAPACK_NAME);
        Utils.getServer().reloadResources(selected, ServerResourcesReloadedEvent.Cause.PLUGIN);
    }
}
