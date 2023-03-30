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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.apache.commons.io.file.PathUtils;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.intellij.lang.annotations.Subst;

public final class DatapackOverride {

    private static final String DATAPACK_NAME = "TreasureMapsPlus";
    private static final Gson GSON = Deserializers.createLootTableSerializer()
        .registerTypeAdapterFactory(new OverrideAdapterFactory())
        .setPrettyPrinting()
        .create();
    private static final int SUBPATH_SPLIT = 3;

    private static final JsonObject OVERRIDE = new JsonObject();

    static {
        final CompoundTag pdcNbt = new CompoundTag();
        final CompoundTag pdc = new CompoundTag();
        pdc.put(TreasureMapsPlus.IS_MAP.toString(), ByteTag.valueOf(true));
        pdcNbt.put("PublicBukkitValues", pdc);
        OVERRIDE.addProperty("function", "minecraft:set_nbt");
        OVERRIDE.addProperty("tag", pdcNbt.toString());
    }

    private DatapackOverride() {
    }

    static Set<Key> setup() throws Exception {
        final Set<Key> keys = createDatapack();

        MinecraftServer.getServer().getPackRepository().reload();
        Bukkit.getDatapackManager().getPacks().stream().filter(datapack -> {
            return datapack.getName().equals("file/" + DATAPACK_NAME);
        }).findAny().ifPresent(datapack -> {
            datapack.setEnabled(true);
        });
        return keys;
    }

    private static Set<Key> createDatapack() throws URISyntaxException, IOException {
        final Path packDir = getPath(Objects.requireNonNull(DatapackOverride.class.getResource("/pack/pack.mcmeta")).toURI()).getParent();
        final Path datapackDir = MinecraftServer.getServer().storageSource.getLevelPath(LevelResource.DATAPACK_DIR).resolve(DATAPACK_NAME);
        PathUtils.copyDirectory(packDir, datapackDir, StandardCopyOption.REPLACE_EXISTING);

        final Path chestLootTables = getPath(Objects.requireNonNull(DatapackOverride.class.getResource("/data/.mcassetsroot")).toURI()).getParent().resolve("minecraft/loot_tables/chests");
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
                        final LootTable table = GSON.fromJson(new BufferedReader(new InputStreamReader(Files.newInputStream(path))), LootTable.class);
                        final Path outputPath = datapackDir.resolve(path.toString().substring(1));
                        Files.createDirectories(outputPath.getParent());
                        try (final Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                            GSON.toJson(table, writer);
                        }
                        @Subst("chests/shipwreck_map") final String value = path.subpath(SUBPATH_SPLIT, path.getNameCount()).toString().split("\\.")[0];
                        keys.add(Key.key("minecraft", value));
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
        return keys;
    }

    private static class OverrideAdapterFactory implements TypeAdapterFactory {

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
                }

                @Override
                public LootItemFunction read(final JsonReader in) throws IOException {
                    final JsonObject obj = Streams.parse(in).getAsJsonObject();
                    if (obj.getAsJsonPrimitive("function").getAsString().equals("minecraft:exploration_map")) {
                        return delegate.read(new JsonTreeReader(OVERRIDE));
                    }
                    return delegate.read(new JsonTreeReader(obj));
                }
            };
        }
    }

    @SuppressWarnings("DuplicateExpressions")
    private static Path getPath(final URI param0) throws IOException {
        try {
            return Path.of(param0);
        } catch (final FileSystemNotFoundException ignored) {
        }

        try {
            FileSystems.newFileSystem(param0, Collections.emptyMap());
        } catch (final FileSystemAlreadyExistsException ignored) {
        }

        return Path.of(param0);
    }
}
