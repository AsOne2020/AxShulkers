package com.artillexstudios.axshulkers.cache;

import com.artillexstudios.axshulkers.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.artillexstudios.axshulkers.AxShulkers.MESSAGES;

public class Shulkerbox {
    private final UUID uuid;
    private Inventory shulkerInventory;

    public Shulkerbox(UUID uuid, Inventory shulkerInventory) {

        this.uuid = uuid;
        this.shulkerInventory = shulkerInventory;
    }

    @NotNull
    public Inventory getShulkerInventory() {
        return shulkerInventory;
    }

    @NotNull
    public UUID getUUID() {
        return uuid;
    }

    public void updateGuiTitle() {
        final Inventory shulkerInv = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, ColorUtils.format(MESSAGES.getString("shulker-title")));
        shulkerInv.setContents(shulkerInventory.getContents());

        final List<HumanEntity> viewers = new ArrayList<>(shulkerInventory.getViewers());
        this.shulkerInventory = shulkerInv;

        final Iterator<HumanEntity> viewerIterator = viewers.iterator();

        while (viewerIterator.hasNext()) {
            viewerIterator.next().openInventory(shulkerInventory);
            viewerIterator.remove();
        }
    }
}