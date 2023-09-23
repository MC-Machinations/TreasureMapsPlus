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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import me.machinemaker.mirror.paper.PaperMirror;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static java.lang.invoke.MethodType.methodType;

public final class Utils {

    private static final class Reflection {

        private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
        private static final MethodHandle CRAFT_PLAYER_GET_HANDLE;
        private static final MethodHandle CRAFT_ITEM_STACK_AS_CRAFTBUKKIT_MIRROR;

        static {
            final Class<?> craftPlayerClass = PaperMirror.getCraftBukkitClass("entity.CraftPlayer");
            CRAFT_PLAYER_GET_HANDLE = sneaky(() -> LOOKUP.findVirtual(craftPlayerClass, "getHandle", methodType(ServerPlayer.class)));
            final Class<?> craftItemStackClass = PaperMirror.getCraftBukkitClass("inventory.CraftItemStack");
            CRAFT_ITEM_STACK_AS_CRAFTBUKKIT_MIRROR = sneaky(() -> LOOKUP.findStatic(craftItemStackClass, "asCraftMirror", methodType(craftItemStackClass, net.minecraft.world.item.ItemStack.class)));
        }

        private Reflection() {
        }
    }

    private Utils() {
    }

    public static MinecraftServer getServer() {
        return Objects.requireNonNull(MinecraftServer.getServer());
    }

    public static ServerPlayer getNmsPlayer(final Player player) {
        return (ServerPlayer) sneaky(() -> Reflection.CRAFT_PLAYER_GET_HANDLE.invoke(player));
    }

    public static ItemStack getBukkitStackMirror(final net.minecraft.world.item.ItemStack stack) {
        return (ItemStack) sneaky(() -> Reflection.CRAFT_ITEM_STACK_AS_CRAFTBUKKIT_MIRROR.invoke(stack));
    }

    @SuppressWarnings("deprecation")
    public static LootItemFunction.Builder createSetNbtFunction(final CompoundTag tag) {
        return SetNbtFunction.setTag(tag);
    }

    public static <T, E extends Throwable> T sneaky(final CheckedSupplier<T, E> supplier) {
        try {
            return supplier.get();
        } catch (final Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public interface CheckedSupplier<T, E extends Throwable> {

        T get() throws E;
    }
}
