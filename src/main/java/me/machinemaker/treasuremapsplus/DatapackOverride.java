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

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetLoreFunction;
import org.apache.commons.io.file.PathUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.intellij.lang.annotations.Subst;

public final class DatapackOverride {

    public static final String DATAPACK_NAME = "TreasureMapsPlus";
    private static final Gson GSON = Deserializers.createLootTableSerializer()
        .registerTypeAdapterFactory(new OverrideAdapterFactory())
        .setPrettyPrinting()
        .create();
    private static final int SUBPATH_SPLIT = 3;

    private static final LootItemFunction.Builder EXPLORATION_FUNCTION_REPLACEMENT;
    private static final LootItemFunction SET_LORE_FUNCTION;

    static {
        final CompoundTag pdcNbt = new CompoundTag();
        final CompoundTag pdc = new CompoundTag();
        pdc.put(TreasureMapsPlus.IS_MAP.asString(), ByteTag.valueOf(true));
        pdc.put(TreasureMapsPlus.MAP_STRUCTURE_TAG_KEY.asString(), StringTag.valueOf("minecraft:on_treasure_maps"));
        pdcNbt.put("PublicBukkitValues", pdc);
        EXPLORATION_FUNCTION_REPLACEMENT = Utils.createSetNbtFunction(pdcNbt);
        SET_LORE_FUNCTION = SetLoreFunction.setLore().addLine(PaperAdventure.asVanilla(TreasureMapsPlus.LORE)).build();
    }

    private DatapackOverride() {
    }

    public static Set<Key> setup(final boolean replaceLootTables) throws Exception {
        final Set<Key> keys = createDatapack(replaceLootTables);

        Utils.getServer().getPackRepository().reload();
        final Collection<String> selected = Utils.getServer().getPackRepository().getSelectedIds();
        if (!selected.contains("file/" + DATAPACK_NAME)) {
            selected.add("file/" + DATAPACK_NAME);
        }
        Utils.getServer().reloadResources(selected, ServerResourcesReloadedEvent.Cause.PLUGIN);
        return keys;
    }

    private static Set<Key> createDatapack(final boolean replaceLootTables) throws URISyntaxException, IOException {
        final Path packDir = Utils.getPath(Objects.requireNonNull(DatapackOverride.class.getResource("/pack/pack.mcmeta")).toURI()).getParent();
        final Path datapackDir = Utils.getServer().storageSource.getLevelPath(LevelResource.DATAPACK_DIR).resolve(DATAPACK_NAME);
        PathUtils.copyDirectory(packDir, datapackDir, StandardCopyOption.REPLACE_EXISTING);

        final Path chestLootTables = Utils.getPath(Objects.requireNonNull(DatapackOverride.class.getResource("/data/.mcassetsroot")).toURI()).getParent().resolve("minecraft/loot_tables/chests");
        if (Files.notExists(chestLootTables)) {
            throw new RuntimeException("Cannot find vanilla chest loot tables");
        }

        final Set<Key> keys = Sets.newConcurrentHashSet();
        try (final Stream<Path> walk = Files.walk(chestLootTables)) {
            walk
                .filter(Files::isRegularFile)
                .parallel()
                .filter(path -> {
                    try {
                        return Files.readString(path, StandardCharsets.UTF_8).contains("\"function\": \"minecraft:exploration_map\"");
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }).forEach(path -> {
                    try {
                        final Path outputPath = datapackDir.resolve(path.toString().substring(1));
                        if (replaceLootTables) {
                            final LootTable table = GSON.fromJson(new BufferedReader(new InputStreamReader(Files.newInputStream(path))), LootTable.class);
                            Files.createDirectories(outputPath.getParent());
                            try (final Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                                GSON.toJson(table, writer);
                            }
                            @Subst("chests/shipwreck_map") final String value = path.subpath(SUBPATH_SPLIT, path.getNameCount()).toString().split("\\.")[0];
                            keys.add(Key.key("minecraft", value));
                        } else {
                            Files.copy(path, outputPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
        return keys;
    }

    private static class OverrideAdapterFactory implements TypeAdapterFactory {

        private final Set<LootItemFunction> cache = Sets.newIdentityHashSet();

        @SuppressWarnings("unchecked")
        @Override
        public <T> @Nullable TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
            if (!LootItemFunction.class.isAssignableFrom(type.getRawType())) {
                return null;
            }
            final TypeAdapter<LootItemFunction> delegate = gson.getDelegateAdapter(this, (TypeToken<LootItemFunction>) type);

            return (TypeAdapter<T>) new TypeAdapter<LootItemFunction>() {
                @Override
                public void write(final JsonWriter out, final LootItemFunction value) throws IOException {
                    delegate.write(out, value);
                    if (OverrideAdapterFactory.this.cache.remove(value)) {
                        delegate.write(out, SET_LORE_FUNCTION);
                    }
                }

                @Override
                public LootItemFunction read(final JsonReader in) throws IOException {
                    final JsonObject obj = Streams.parse(in).getAsJsonObject();
                    if (obj.getAsJsonPrimitive("function").getAsString().equals("minecraft:exploration_map")) {
                        final LootItemFunction function = EXPLORATION_FUNCTION_REPLACEMENT.build();
                        OverrideAdapterFactory.this.cache.add(function);
                        return function;
                    }
                    return delegate.read(new JsonTreeReader(obj));
                }
            };
        }
    }
}
