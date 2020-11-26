/*
 * This file is a part of project QuickShop, the name is ShopTypeChangeEvent.java
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

package org.maxgamer.quickshop.event;

import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.shop.Shop;
import org.maxgamer.quickshop.shop.ShopType;

/**
 * Calling when shop item was changed
 */
public class ShopTypeChangeEvent extends QSEvent implements Cancellable {
    @Getter
    private final ShopType oldType;

    @Getter
    private final ShopType newType;

    @Getter
    @NotNull
    private final Shop shop;

    private boolean cancelled;

    /**
     * Will call when shop type was changed.
     *
     * @param shop    Target shop
     * @param oldType The old shop type
     * @param newType The new shop type
     */
    public ShopTypeChangeEvent(@NotNull Shop shop, ShopType oldType, ShopType newType) {
        this.shop = shop;
        this.oldType = oldType;
        this.newType = newType;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

}
