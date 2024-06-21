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

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import xyz.jpenilla.reflectionremapper.ReflectionRemapper;

public record RegistryOverride<T>(ResourceKey<? extends Registry<T>> registryKey, ResourceKey<T> resourceKey, T value) {

    private static final MethodHandle TO_ID_MAP;
    private static final MethodHandle BY_VALUE_MAP;

    private static final MethodHandle SET_HOLDER_VALUE;

    static {
        try {
            final ReflectionRemapper remapper = ReflectionRemapper.noop();
            final MethodHandles.Lookup mappedRegistryLookup = MethodHandles.privateLookupIn(MappedRegistry.class, MethodHandles.lookup());
            TO_ID_MAP = mappedRegistryLookup.findGetter(MappedRegistry.class, remapper.remapFieldName(MappedRegistry.class, "toId"), Reference2IntMap.class);
            BY_VALUE_MAP = mappedRegistryLookup.findGetter(MappedRegistry.class, remapper.remapFieldName(MappedRegistry.class, "byValue"), Map.class);

            final MethodHandles.Lookup referenceHolderLookup = MethodHandles.privateLookupIn(Holder.Reference.class, MethodHandles.lookup());
            SET_HOLDER_VALUE = referenceHolderLookup.findSetter(Holder.Reference.class, remapper.remapFieldName(Holder.Reference.class, "value"), Object.class);

        } catch (final Throwable ex) {
            throw new RuntimeException(ex);
        }
    }


    public void override(final RegistryAccess access) {
        final Registry<T> registry = access.registryOrThrow(this.registryKey);
        final Holder.Reference<T> holder = registry.getHolderOrThrow(this.resourceKey);
        final T oldValue = holder.value();
        final int id = registry.getId(oldValue);
        swapToIdMap(registry, id, oldValue, this.value());
        swapByValueMap(registry, holder, oldValue, this.value());
        swapInHolder(holder, this.value());
    }

    @SuppressWarnings("unchecked")
    private static <T> void swapToIdMap(final Registry<T> registry, final int id, final T oldValue, final T newValue) {
        try {
            final Reference2IntMap<T> map = (Reference2IntMap<T>) TO_ID_MAP.invoke(registry);
            map.put(newValue, id);
            map.remove(oldValue, id);
        } catch (final Throwable e) {
            throw new RuntimeException("Could not get toId map from " + registry, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void swapByValueMap(final Registry<T> registry, final Holder.Reference<T> holder, final T oldValue, final T newValue) {
        try {
            final Map<T, Holder.Reference<T>> map = (Map<T, Holder.Reference<T>>) BY_VALUE_MAP.invoke(registry);
            map.remove(oldValue);
            map.put(newValue, holder);
        } catch (final Throwable e) {
            throw new RuntimeException("Could not get byValue map from " + registry, e);
        }
    }

    private static <T> void swapInHolder(final Holder.Reference<T> holder, final T newValue) {
        try {
            SET_HOLDER_VALUE.invoke(holder, newValue);
        } catch (final Throwable e) {
            throw new RuntimeException("Could not swap value in holder " + holder, e);
        }
    }
}
