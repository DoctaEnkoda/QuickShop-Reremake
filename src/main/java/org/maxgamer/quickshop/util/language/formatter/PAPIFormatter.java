/*
 * This file is a part of project QuickShop, the name is PAPIFormatter.java
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

package org.maxgamer.quickshop.util.language.formatter;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.util.language.Formatter;

public class PAPIFormatter implements Formatter {

    @Override
    public @NotNull String format(@NotNull String raw, @Nullable CommandSender sender, @Nullable String... args) {
        if (sender instanceof OfflinePlayer) {
            return PlaceholderAPI.setPlaceholders((OfflinePlayer) sender, raw);
        }
        return raw;
    }
}
