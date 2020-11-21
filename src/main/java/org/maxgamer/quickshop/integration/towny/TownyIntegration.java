/*
 * This file is a part of project QuickShop, the name is TownyIntegration.java
 *  Copyright (C) Ghost_chu <https://github.com/Ghost-chu>
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

package org.maxgamer.quickshop.integration.towny;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.ShopPlotUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.integration.IntegrateStage;
import org.maxgamer.quickshop.integration.IntegratedPlugin;
import org.maxgamer.quickshop.integration.IntegrationStage;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.ShopChunk;
import org.maxgamer.quickshop.util.Util;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("DuplicatedCode")
@IntegrationStage(loadStage = IntegrateStage.onEnableAfter)
public class TownyIntegration extends IntegratedPlugin implements Listener {
    private final List<TownyFlags> createFlags;

    private final List<TownyFlags> tradeFlags;

    private final boolean ignoreDisabledWorlds;
    private final boolean deleteShopOnLeave;

    public TownyIntegration(QuickShop plugin) {
        super(plugin);
        createFlags =
                TownyFlags.deserialize(plugin.getConfig().getStringList("integration.towny.create"));
        tradeFlags =
                TownyFlags.deserialize(plugin.getConfig().getStringList("integration.towny.trade"));
        ignoreDisabledWorlds = plugin.getConfig().getBoolean("integration.towny.ignore-disabled-worlds");
        deleteShopOnLeave = plugin.getConfig().getBoolean("integration.towny.delete-shop-on-resident-leave");
    }

    @Override
    public @NotNull String getName() {
        return "Towny";
    }

    @EventHandler
    public void onPlayerLeave(TownRemoveResidentEvent event) {
        if (!deleteShopOnLeave) {
            return;
        }
        UUID owner = TownyAPI.getInstance().getPlayerUUID(event.getResident());
        if (owner == null) {
            return;
        }
        String worldName = event.getTown().getWorld().getName();
        Town town = event.getTown();
        //Getting all shop with world-chunk-shop mapping
        for (Map.Entry<String, Map<ShopChunk, Map<Location, Shop>>> entry : plugin.getShopManager().getShops().entrySet()) {
            //Matching world
            if (worldName.equals(entry.getKey())) {
                World world = Bukkit.getWorld(entry.getKey());
                if (world != null) {
                    //Matching Location
                    for (Map.Entry<ShopChunk, Map<Location, Shop>> chunkedShopEntry : entry.getValue().entrySet()) {
                        Map<Location, Shop> shopMap = chunkedShopEntry.getValue();
                        for (Shop shop : shopMap.values()) {
                            //Matching Owner
                            if (shop.getOwner().equals(owner)) {
                                try {
                                    //It should be equal in address
                                    if (WorldCoord.parseWorldCoord(shop.getLocation()).getTownBlock().getTown() == town) {
                                        //delete it
                                        shop.delete();
                                    }
                                } catch (NotRegisteredException ignored) {
                                    //Is not in town, continue
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean canCreateShopHere(@NotNull Player player, @NotNull Location location) {
        if (ignoreDisabledWorlds && !TownyAPI.getInstance().isTownyWorld(location.getWorld())) {
            Util.debugLog("This world disabled Towny.");
            return true;
        }
        for (TownyFlags flag : createFlags) {
            switch (flag) {
                case OWN:
                    if (!ShopPlotUtil.doesPlayerOwnShopPlot(player, location)) {
                        return false;
                    }
                    break;
                case MODIFY:
                    if (!ShopPlotUtil.doesPlayerHaveAbilityToEditShopPlot(player, location)) {
                        return false;
                    }
                    break;
                case SHOPTYPE:
                    if (!ShopPlotUtil.isShopPlot(location)) {
                        return false;
                    }
                default:
                    // Ignore
            }
        }
        return true;
    }

    @Override
    public boolean canTradeShopHere(@NotNull Player player, @NotNull Location location) {
        if (ignoreDisabledWorlds && !TownyAPI.getInstance().isTownyWorld(location.getWorld())) {
            Util.debugLog("This world disabled Towny.");
            return true;
        }
        for (TownyFlags flag : tradeFlags) {
            switch (flag) {
                case OWN:
                    if (!ShopPlotUtil.doesPlayerOwnShopPlot(player, location)) {
                        return false;
                    }
                    break;
                case MODIFY:
                    if (!ShopPlotUtil.doesPlayerHaveAbilityToEditShopPlot(player, location)) {
                        return false;
                    }
                    break;
                case SHOPTYPE:
                    if (!ShopPlotUtil.isShopPlot(location)) {
                        return false;
                    }
                default:
                    // Ignore
            }
        }
        return true;
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unload() {
        HandlerList.unregisterAll(this);
    }

}
