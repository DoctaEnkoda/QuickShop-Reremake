/*
 * This file is a part of project QuickShop, the name is DisplayProtectionListener.java
 *  Copyright (C) PotatoCraft Studio and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.maxgamer.quickshop.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.Cache;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.shop.DisplayItem;
import org.maxgamer.quickshop.shop.DisplayType;
import org.maxgamer.quickshop.util.MsgUtil;
import org.maxgamer.quickshop.util.Util;

@SuppressWarnings("DuplicatedCode")
public class DisplayProtectionListener extends ProtectionListenerBase {


    public DisplayProtectionListener(QuickShop plugin, Cache cache) {
        super(plugin, cache);
        boolean useEnhanceProtection = plugin.getConfig().getBoolean("shop.enchance-display-protect");
        if (useEnhanceProtection) {
            Bukkit.getPluginManager().registerEvents(new EnhanceDisplayProtectionListener(plugin, cache), plugin);
        }
    }


    private void sendAlert(@NotNull String msg) {
        if (!plugin.getConfig().getBoolean("send-display-item-protection-alert")) {
            return;
        }
        MsgUtil.sendGlobalAlert(msg);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void portal(EntityPortalEvent event) {
        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
            return;
        }
        if (!(event.getEntity() instanceof Item)) {
            return;
        }
        if (DisplayItem.checkIsGuardItemStack(((Item) event.getEntity()).getItemStack())) {
            event.setCancelled(true);
            event.getEntity().remove();
            sendAlert(
                    "[DisplayGuard] Somebody want dupe the display by Portal at "
                            + event.getFrom()
                            + " , QuickShop already cancel it.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void entity(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) {
            return;
        }
        if (DisplayItem.getNowUsing() != DisplayType.ARMORSTAND) {
            return;
        }
        if (!DisplayItem.checkIsGuardItemStack(((ArmorStand) event.getEntity()).getItemInHand())) { //FIXME: Update this when drop 1.13 supports
            return;
        }
        event.setCancelled(true);
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void inventory(InventoryOpenEvent event) {
        Util.inventoryCheck(event.getInventory());
    }

    //    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    //    public void inventory(InventoryMoveItemEvent event) {
    //        if (ListenerHelper.isDisabled(event.getClass())) {
    //            return;
    //        }
    //        try {
    //            ItemStack is = event.getItem();
    //            if (DisplayItem.checkIsGuardItemStack(is)) {
    //                event.setCancelled(true); ;
    //                sendAlert("[DisplayGuard] Inventory " + event.getInitiator()
    //                        .getLocation().toString() + " trying moving displayItem, QuickShop
    // already removed it.");
    //                event.setItem(new ItemStack(Material.AIR));
    //                Util.inventoryCheck(event.getDestination());
    //                Util.inventoryCheck(event.getInitiator());
    //                Util.inventoryCheck(event.getSource());
    //            }
    //        } catch (Exception e) {}
    //    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void inventory(InventoryPickupItemEvent event) {
        ItemStack itemStack = event.getItem().getItemStack();
        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
            return;
        }
        if (!DisplayItem.checkIsGuardItemStack(itemStack)) {
            return; // We didn't care that
        }
        @Nullable Location loc = event.getInventory().getLocation();
        @Nullable InventoryHolder holder = event.getInventory().getHolder();
        event.setCancelled(true);
        sendAlert(
                "[DisplayGuard] Something  "
                        + holder
                        + " at "
                        + loc
                        + " trying pickup the DisplayItem,  you should teleport to that location and to check detail..");
        Util.inventoryCheck(event.getInventory());
    }



    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void item(ItemDespawnEvent event) {
        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
            return;
        }
        final ItemStack itemStack = event.getEntity().getItemStack();
        if (DisplayItem.checkIsGuardItemStack(itemStack)) {
            event.setCancelled(true);
        }

        // Util.debugLog("We canceled an Item from despawning because they are our display item.");
    }
    // Player can't interact the item entity... of course
    //    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    //    public void player(PlayerInteractEvent e) {
    //        if (ListenerHelper.isDisabled(e.getClass())) {
    //            return;
    //        }
    //        ItemStack stack = e.getItem();
    //        if (!DisplayItem.checkIsGuardItemStack(stack)) {
    //            return;
    //        }
    //        stack.setType(Material.AIR);
    //        stack.setAmount(0);
    //        // You shouldn't be able to pick up that...
    //        e.setCancelled(true);
    //        sendAlert("[DisplayGuard] Player " + ((Player) e)
    //                .getName() + " using the displayItem, QuickShop already removed it.");
    //        Util.inventoryCheck(e.getPlayer().getInventory());
    //    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void player(PlayerFishEvent event) {
        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
            return;
        }
        if (event.getState() != State.CAUGHT_ENTITY) {
            return;
        }
        if (event.getCaught() == null) {
            return;
        }
        if (event.getCaught().getType() != EntityType.DROPPED_ITEM) {
            return;
        }
        final Item item = (Item) event.getCaught();
        final ItemStack is = item.getItemStack();
        if (!DisplayItem.checkIsGuardItemStack(is)) {
            return;
        }
        // item.remove();
        event.getHook().remove();
        // event.getCaught().remove();
        event.setCancelled(true);
        sendAlert(
                "[DisplayGuard] Player "
                        + event.getPlayer().getName()
                        + " trying hook item use Fishing Rod, QuickShop already removed it.");
        Util.inventoryCheck(event.getPlayer().getInventory());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void player(PlayerArmorStandManipulateEvent event) {
        if (!DisplayItem.checkIsGuardItemStack(event.getArmorStandItem())) {
            return;
        }
        if (DisplayItem.getNowUsing() != DisplayType.REALITEM) {
            return;
        }
        event.setCancelled(true);
        Util.inventoryCheck(event.getPlayer().getInventory());
        sendAlert(
                "[DisplayGuard] Player  "
                        + event.getPlayer().getName()
                        + " trying mainipulate armorstand contains displayItem.");
    }

}
