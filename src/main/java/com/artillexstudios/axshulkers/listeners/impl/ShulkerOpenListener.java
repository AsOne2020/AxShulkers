package com.artillexstudios.axshulkers.listeners.impl;

import com.artillexstudios.axshulkers.AxShulkers;
import com.artillexstudios.axshulkers.cache.Shulkerbox;
import com.artillexstudios.axshulkers.cache.Shulkerboxes;
import com.artillexstudios.axshulkers.utils.MessageUtils;
import com.artillexstudios.axshulkers.utils.ShulkerUtils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.artillexstudios.axshulkers.AxShulkers.CONFIG;
import static com.artillexstudios.axshulkers.AxShulkers.MESSAGES;

public class ShulkerOpenListener implements Listener {
    private final ConcurrentHashMap<UUID, Long> cds = new ConcurrentHashMap<>();

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && ShulkerUtils.hasShulkerOpen(event.getPlayer()) != null) {
            event.getPlayer().closeInventory();
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR || event.getHand() != EquipmentSlot.HAND) return;

        final Player player = event.getPlayer();
        if (openShulker(player, player.getInventory().getItemInMainHand())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onShulkerClick(@NotNull InventoryClickEvent event) {
        if (!event.getClick().equals(ClickType.RIGHT)) return;
        if (!CONFIG.getBoolean("opening-from-inventory.enabled")) return;
        if (!ShulkerUtils.isAllowedInventoryType(event.getView().getTopInventory())) return;

        final Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() != null && !event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
            if (!event.getClickedInventory().equals(event.getWhoClicked().getEnderChest())) return;
            if (!CONFIG.getBoolean("opening-from-inventory.open-from-enderchest")) return;
        }

        if (event.getView().getTopInventory().getType().equals(InventoryType.SHULKER_BOX)) {
            for (Shulkerbox shulkerbox : Shulkerboxes.getShulkerMap().values()) {
                if (!shulkerbox.getShulkerInventory().equals(event.getView().getTopInventory())) continue;
                if (!shulkerbox.getUUID().equals(ShulkerUtils.getShulkerUUID(event.getCurrentItem()))) continue;

                event.setCancelled(true);
                AxShulkers.getScheduler().runNextTick(t -> {
                    event.getWhoClicked().closeInventory();
                });
                return;
            }
        }

        if (event.getCurrentItem() == null) return;
        if (openShulker(player, event.getCurrentItem())) event.setCancelled(true);
    }

    private boolean openShulker(@NotNull Player player, @NotNull ItemStack it) {
        if (!player.hasPermission("axshulkers.use")) return false;
        if (!ShulkerUtils.isShulker(it)) return false;
        if (it.getAmount() > 1) {
            it.setAmount(1);
            return false;
        }

        if (CONFIG.getBoolean("disable-shulker-opening")) return false;

        if (CONFIG.getStringList("blacklisted-worlds").contains(player.getWorld().getName())) {
            MessageUtils.sendMsgP(player, "errors.blacklisted-world");
            return false;
        }

        long openCooldown = CONFIG.getLong("open-cooldown-milliseconds");
        if (cds.containsKey(player.getUniqueId()) && System.currentTimeMillis() - cds.get(player.getUniqueId()) < openCooldown) {
            MessageUtils.sendMsgP(player, "cooldown", Map.of(
                    "%seconds%", "" + ((openCooldown - System.currentTimeMillis() + cds.get(player.getUniqueId())) / 1000L + 1)
            ));
            return false;
        }

        cds.put(player.getUniqueId(), System.currentTimeMillis());

        final String name = ShulkerUtils.getShulkerName(it);

        if (Shulkerboxes.getShulker(it, name, player) == null) return false;
        AxShulkers.getScheduler().runAtLocation(player.getLocation(), t -> { // folia support
            Shulkerbox shulkerbox = Shulkerboxes.getShulker(it, name, player);
            if (shulkerbox == null) return;
            if (player.getOpenInventory().getTopInventory().getType().equals(InventoryType.SHULKER_BOX)) {
                shulkerbox.close();
            }

            ShulkerUtils.clearShulkerContents(it);

            shulkerbox.setItem(it);
            shulkerbox.updateReference();
            shulkerbox.openShulkerFor(player);

            MessageUtils.sendMsgP(player, "open.message", Collections.singletonMap("%name%", shulkerbox.getTitle()));

            if (!MESSAGES.getString("open.sound").isEmpty()) {
                player.playSound(player.getLocation(), Sound.valueOf(MESSAGES.getString("open.sound")), 1f, 1f);
            }
        });
        return true;
    }
}
