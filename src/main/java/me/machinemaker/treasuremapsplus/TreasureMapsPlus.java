package me.machinemaker.treasuremapsplus;

import io.papermc.paper.util.MCUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.Style.style;
import static net.kyori.adventure.text.format.TextDecoration.*;

public final class TreasureMapsPlus extends JavaPlugin implements Listener {

    static final NamespacedKey IS_MAP = new NamespacedKey("treasuremapsplus", "is_map");
    static final Component LORE = text("Use to generate buried treasure loot!", style(GREEN, ITALIC.withState(false)));

    private final Set<Key> lootTables;
    public TreasureMapsPlus() throws Exception {
        this.lootTables = DatapackOverride.setup();
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getSLF4JLogger().info("Found {} loot tables with a buried treasure map, {}", this.lootTables.size(), this.lootTables);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEvent(final PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        if (isSpecialMap(event.getItem())) {
            event.setUseItemInHand(Event.Result.DENY);
            final LootContext context = new LootContext.Builder(((CraftPlayer) event.getPlayer()).getHandle().getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(MCUtil.toBlockPosition(event.getPlayer().getLocation())))
                .withLuck(Optional.ofNullable(event.getPlayer().getAttribute(Attribute.GENERIC_LUCK)).map(AttributeInstance::getValue).orElse(0D).floatValue())
                .withParameter(LootContextParams.THIS_ENTITY, ((CraftPlayer) event.getPlayer()).getHandle())
                .create(LootContextParamSets.CHEST);
            final ObjectArrayList<net.minecraft.world.item.ItemStack> randomItems = MinecraftServer.getServer().getLootTables().get(BuiltInLootTables.BURIED_TREASURE).getRandomItems(context);
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.getItem().setAmount(0);
            }
            randomItems.forEach(i -> {
                event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), CraftItemStack.asBukkitCopy(i), item1 -> {
                    item1.setPickupDelay(0);
                });
            });
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEvent(final LootGenerateEvent event) {
        if (this.lootTables.contains(event.getLootTable().getKey())) {
            event.getLoot().forEach(item -> {
                if (isSpecialMap(item)) {
                    item.lore(List.of(LORE));
                }
            });
        }
    }

    private static boolean isSpecialMap(final @Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.MAP && stack.getItemMeta().getPersistentDataContainer().has(IS_MAP);
    }

}
