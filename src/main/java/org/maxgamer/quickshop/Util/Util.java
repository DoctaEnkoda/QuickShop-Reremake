package org.maxgamer.quickshop.Util;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.Sign;
import org.bukkit.potion.PotionEffect;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.Shop.DisplayItem;
import org.maxgamer.quickshop.Shop.Shop;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author MACHENIKE
 *
 */
@SuppressWarnings("deprecation")
public class Util {
	private static final EnumSet<Material> blacklist = EnumSet.noneOf(Material.class);
	private static final EnumSet<Material> shoppables = EnumSet.noneOf(Material.class);
	private static final EnumMap<Material, Entry<Double,Double>> restrictedPrices = new EnumMap<Material, Entry<Double,Double>>(Material.class);
	private static QuickShop plugin;
	private static Method storageContents;

	public static void initialize() {
		blacklist.clear();
		shoppables.clear();
		restrictedPrices.clear();

		plugin = QuickShop.instance;
		for (String s : plugin.getConfig().getStringList("shop-blocks")) {
			Material mat = Material.getMaterial(s.toUpperCase());
			if (mat == null) {
				try {
					mat = Material.matchMaterial(s);
				} catch (NumberFormatException e) {
				}
			}
			if (mat == null) {
				plugin.getLogger().info("Invalid shop-block: " + s);
			} else {
				shoppables.add(mat);
			}
		}
		List<String> configBlacklist = plugin.getConfig().getStringList("blacklist");
		for (String s : configBlacklist) {
			Material mat = Material.getMaterial(s.toUpperCase());
			if (mat == null) {
				mat = Material.matchMaterial(s);
				if (mat == null) {
					plugin.getLogger().info(s + " is not a valid material.  Check your spelling or ID");
					continue;
				}
			}
			blacklist.add(mat);
		}		

		for (String s : plugin.getConfig().getStringList("price-restriction")) {
			String[] sp = s.split(";");
			if (sp.length==3) {
				try {
					Material mat = Material.matchMaterial(sp[0]);
					if (mat == null) {
						throw new Exception();
					}

					restrictedPrices.put(mat, new SimpleEntry<Double,Double>(Double.valueOf(sp[1]), Double.valueOf(sp[2])));
				} catch (Exception e) {
					plugin.getLogger().info("Invalid price restricted material: " + s);
				}
			}
		}
		
		try {
			storageContents = Inventory.class.getMethod("getStorageContents");
		} catch (Exception e) {
			try {
				storageContents = Inventory.class.getMethod("getContents");
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	/** Return an entry with min and max prices, but null if there isn't a price restriction */
	public static Entry<Double,Double> getPriceRestriction(Material material) {
		return restrictedPrices.get(material);
	}

	public static boolean isTransparent(Material m) {
		boolean trans = m.isTransparent();
		return trans;
	}

	public static void parseColours(YamlConfiguration config) {
		Set<String> keys = config.getKeys(true);
		for (String key : keys) {
			String filtered = config.getString(key);
			if (filtered.startsWith("MemorySection")) {
				continue;
			}
			filtered = ChatColor.translateAlternateColorCodes('&', filtered);
			config.set(key, filtered);
		}
	}

	/**
	 * Returns true if the given block could be used to make a shop out of.
	 * 
	 * @param b
	 *            The block to check. Possibly a chest, dispenser, etc.
	 * @return True if it can be made into a shop, otherwise false.
	 */
	public static boolean canBeShop(Block b) {
		BlockState bs = b.getState();
		if (bs instanceof InventoryHolder == false)
			return false;
		return shoppables.contains(bs.getType());
	}

	/**
	 * Gets the percentage (Without trailing %) damage on a tool.
	 * 
	 * @param item
	 *            The ItemStack of tools to check
	 * @return The percentage 'health' the tool has. (Opposite of total damage)
	 */
	public static String getToolPercentage(ItemStack item) {
		double dura = item.getDurability();
		double max = item.getType().getMaxDurability();
		DecimalFormat formatter = new DecimalFormat("0");
		return formatter.format((1 - dura / max) * 100.0);
	}

	/**
	 * Returns the chest attached to the given chest. The given block must be a
	 * chest.
	 * 
	 * @param b
	 *            The chest to check.
	 * @return the block which is also a chest and connected to b.
	 */
	public static Block getSecondHalf(Block b) {
		if (b.getType() != Material.CHEST && b.getType() != Material.TRAPPED_CHEST)
			return null;
		Block[] blocks = new Block[4];
		blocks[0] = b.getRelative(1, 0, 0);
		blocks[1] = b.getRelative(-1, 0, 0);
		blocks[2] = b.getRelative(0, 0, 1);
		blocks[3] = b.getRelative(0, 0, -1);
		for (Block c : blocks) {
			if (c.getType() == b.getType()) {
				return c;
			}
		}
		return null;
	}
	
	/**
	 * Checks whether someone else's shop is within reach of a hopper being placed by a player.
	 * 
	 * @param b 
	 *            The block being placed.
	 * @param p
	 *            The player performing the action.
	 * @return true if a nearby shop was found, false otherwise.
	 */
	public static boolean isOtherShopWithinHopperReach(Block b, Player p) {
		// Check 5 relative positions that can be affected by a hopper: behind, in front of, to the right,
		// to the left and underneath.
		Block[] blocks = new Block[5];
		blocks[0] = b.getRelative(0, 0, -1);
		blocks[1] = b.getRelative(0, 0, 1);
		blocks[2] = b.getRelative(1, 0, 0);
		blocks[3] = b.getRelative(-1, 0, 0);
		blocks[4] = b.getRelative(0, 1, 0);
		for (Block c : blocks) {
			Shop firstShop = plugin.getShopManager().getShop(c.getLocation());
			// If firstShop is null but is container, it can be used to drain contents from a shop created
			// on secondHalf.
			Block secondHalf = getSecondHalf(c);
			Shop secondShop = secondHalf == null ? null : plugin.getShopManager().getShop(secondHalf.getLocation());
			if (firstShop != null && !p.getUniqueId().equals(firstShop.getOwner())
					|| secondShop != null && !p.getUniqueId().equals(secondShop.getOwner())) {
				return true;
			}
		}
		return false;
	}


	public static String serialize(ItemStack iStack) {
		YamlConfiguration cfg = new YamlConfiguration();
		cfg.set("item", iStack);
		return cfg.saveToString();
	}

	public static ItemStack deserialize(String config) throws InvalidConfigurationException {
		YamlConfiguration cfg = new YamlConfiguration();
		cfg.loadFromString(config);
		cfg.getString("item");
		ItemStack stack = cfg.getItemStack("item");
		return stack;
	}

	/**
	 * Fetches an ItemStack's name - For example, converting INK_SAC:11 to
	 * Dandellion Yellow, or WOOL:14 to Red Wool
	 * 
	 * @param itemStack
	 *            The itemstack to fetch the name of
	 * @return The human readable item name.
	 */
	public static String getName(ItemStack itemStack) {
//		if (NMS.isPotion(itemStack.getType())) {
//			return CustomPotionsName.getFullName(itemStack);
//		}		
		String vanillaName = itemStack.getType().name();
		return prettifyText(vanillaName);
	}

	/**
	 * Converts a name like IRON_INGOT into Iron Ingot to improve readability
	 * 
	 * @param ugly
	 *            The string such as IRON_INGOT
	 * @return A nicer version, such as Iron Ingot
	 * 
	 */
	public static String prettifyText(String ugly) {
		String[] nameParts = ugly.split("_");
		if (nameParts.length==1) {
			return firstUppercase(ugly);
		}

		StringBuilder sb=new StringBuilder();
		for (String part : nameParts) {
			sb.append(firstUppercase(part)+" ");
		}

		return sb.toString();
	}

	// Let's make very long names shorter for our sign
	public static String getNameForSign(ItemStack itemStack) {
//		if (NMS.isPotion(itemStack.getType())) {
//			return CustomPotionsName.getSignName(itemStack);
//		}
		
		ItemStack is = itemStack.clone();
		is.setAmount(1);
		
		if(is.hasItemMeta()) {
			if(is.getItemMeta().hasDisplayName()) {
				return is.getItemMeta().getDisplayName();
			}
		}
		
		String name = MsgUtil.getItemi18n(itemStack.getType().name()).trim();

		String[] nameParts = name.split("_");
		if (nameParts.length==1) {
			return firstUppercase(nameParts[0]);
		}

		for (int i=0; i<nameParts.length-1; i++) {
			int length = StringUtils.join(nameParts).length();
			if (length>16) {
				nameParts[i] = nameParts[i].substring(0, 1)+".";
			} else {
				nameParts[i] = firstUppercase(nameParts[i]);
			}
		}

		nameParts[nameParts.length-1] = firstUppercase(nameParts[nameParts.length-1]);

		return StringUtils.join(nameParts);
	}

	public static String firstUppercase(String string) {
		if (string.length()>1) {
			return Character.toUpperCase(string.charAt(0))+string.substring(1).toLowerCase();
		} else {
			return string.toUpperCase();
		}
	}


	public static String toRomain(Integer value) {
		return toRoman(value.intValue());
	}

	private static final String[] ROMAN = { "X", "IX", "V", "IV", "I" };
	private static final int[] DECIMAL = { 10, 9, 5, 4, 1 };

	/**
	 * Converts the given number to roman numerals. If the number is >= 40 or <=
	 * 0, it will just return the number as a string.
	 * 
	 * @param n
	 *            The number to convert
	 * @return The roman numeral representation of this number, or the number in
	 *         decimal form as a string if n <= 0 || n >= 40.
	 */
	public static String toRoman(int n) {
		if (n <= 0 || n >= 40)
			return "" + n;
		String roman = "";
		for (int i = 0; i < ROMAN.length; i++) {
			while (n >= DECIMAL[i]) {
				n -= DECIMAL[i];
				roman += ROMAN[i];
			}
		}
		return roman;
	}
	/**
	 * @param mat
	 *            The material to check
	 * @return Returns true if the item is a tool (Has durability) or false if
	 *         it doesn't.
	 */
	public static boolean isTool(Material mat) {
		if(mat.getMaxDurability()==0){
			return false;
		}else{
			return true;
		}
	}

	/**
	 * Compares two items to each other. Returns true if they match.
	 * 
	 * @param stack1
	 *            The first item stack
	 * @param stack2
	 *            The second item stack
	 * @return true if the itemstacks match. (Material, durability, enchants, name)
	 */
	public static boolean matches(ItemStack stack1, ItemStack stack2) {
		if (stack1 == stack2)
			return true; // Referring to the same thing, or both are null.
		if (stack1 == null || stack2 == null)
			return false; // One of them is null (Can't be both, see above)
		if (stack1.getType() != stack2.getType())
			return false; // Not the same material
		if (stack1.getDurability() != stack2.getDurability())
			return false; // Not the same durability
		if (!stack1.getEnchantments().equals(stack2.getEnchantments()))
			return false; // They have the same enchants
		if (stack1.getItemMeta().hasDisplayName() || stack2.getItemMeta().hasDisplayName()) {
			if (stack1.getItemMeta().hasDisplayName() && stack2.getItemMeta().hasDisplayName()) {
				if (!stack1.getItemMeta().getDisplayName().equals(stack2.getItemMeta().getDisplayName())) {
					return false; // items have different display name
				}
			} else {
				return false; // one of the item stacks have a display name
			}
		}
		try {
			Class.forName("org.bukkit.inventory.meta.EnchantmentStorageMeta");
			boolean book1 = stack1.getItemMeta() instanceof EnchantmentStorageMeta;
			boolean book2 = stack2.getItemMeta() instanceof EnchantmentStorageMeta;
			if (book1 != book2)
				return false;// One has enchantment meta, the other does not.
			if (book1 == true) { // They are the same here (both true or both
				// false). So if one is true, the other is
				// true.
				Map<Enchantment, Integer> ench1 = ((EnchantmentStorageMeta) stack1.getItemMeta()).getStoredEnchants();
				Map<Enchantment, Integer> ench2 = ((EnchantmentStorageMeta) stack2.getItemMeta()).getStoredEnchants();
				if (!ench1.equals(ench2))
					return false; // Enchants aren't the same.
			}
		} catch (ClassNotFoundException e) {
			// Nothing. They dont have a build high enough to support this.
		}
		return true;
	}

	/**
	 * Formats the given number according to how vault would like it. E.g. $50
	 * or 5 dollars.
	 * 
	 * @return The formatted string.
	 */
	public static String format(double n) {
		try {
			return plugin.getEcon().format(n);
		} catch (NumberFormatException e) {
			return "$" + n;
		}
	}

	/**
	 * @param m
	 *            The material to check if it is blacklisted
	 * @return true if the material is black listed. False if not.
	 */
	public static boolean isBlacklisted(Material m) {
		return blacklist.contains(m);
	}

	/**
	 * Fetches the block which the given sign is attached to
	 * 
	 * @param sign
	 *            The sign which is attached
	 * @return The block the sign is attached to
	 */
	public static Block getAttached(Block b) {
		try {
			Sign sign = (Sign) b.getState().getData(); // Throws a NPE
			// sometimes??
			BlockFace attached = sign.getAttachedFace();
			if (attached == null)
				return null;
			return b.getRelative(attached);
		} catch (NullPointerException e) {
			return null; // /Not sure what causes this.
		}
	}

	/**
	 * Counts the number of items in the given inventory where
	 * Util.matches(inventory item, item) is true.
	 * 
	 * @param inv
	 *            The inventory to search
	 * @param item
	 *            The ItemStack to search for
	 * @return The number of items that match in this inventory.
	 */
	public static int countItems(Inventory inv, ItemStack item) {
		int items = 0;
		for (ItemStack iStack : inv.getContents()) {
			if (iStack == null)
				continue;
			if (Util.matches(item, iStack)) {
				items += iStack.getAmount();
			}
		}
		Util.debugLog("Items: "+items);
		return items;
	}

	/**
	 * Returns the number of items that can be given to the inventory safely.
	 * 
	 * @param inv
	 *            The inventory to count
	 * @param item
	 *            The item prototype. Material, durabiltiy and enchants must
	 *            match for 'stackability' to occur.
	 * @return The number of items that can be given to the inventory safely.
	 */
	public static int countSpace(Inventory inv, ItemStack item) {
		int space = 0;
		
		try {
			ItemStack[] contents = (ItemStack[])storageContents.invoke(inv);
			for (ItemStack iStack : contents) {
				if (iStack == null || iStack.getType() == Material.AIR) {
					space += item.getMaxStackSize();
				} else if (matches(item, iStack)) {
					space += item.getMaxStackSize() - iStack.getAmount();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Util.debugLog("Space:"+space);
		return space;
	}

	/**
	 * Returns true if the given location is loaded or not.
	 * 
	 * @param loc
	 *            The location
	 * @return true if the given location is loaded or not.
	 */
	public static boolean isLoaded(Location loc) {
		// plugin.getLogger().log(Level.WARNING, "Checking isLoaded(Location loc)");
		if (loc.getWorld() == null) {
			// plugin.getLogger().log(Level.WARNING, "Is not loaded. (No world)");
			return false;
		}
		// Calculate the chunks coordinates. These are 1,2,3 for each chunk, NOT
		// location rounded to the nearest 16.
		int x = (int) Math.floor((loc.getBlockX()) / 16.0);
		int z = (int) Math.floor((loc.getBlockZ()) / 16.0);
		if (loc.getWorld().isChunkLoaded(x, z)) {
			// plugin.getLogger().log(Level.WARNING, "Chunk is loaded " + x + ", " + z);
			return true;
		} else {
			// plugin.getLogger().log(Level.WARNING, "Chunk is NOT loaded " + x + ", " + z);
			return false;
		}
	}

	public static BlockFace getYawFace(float yaw) {
		if (yaw > 315 && yaw <= 45) {
			return BlockFace.NORTH;
		} else if (yaw > 45 && yaw <= 135) {
			return BlockFace.EAST;
		} else if (yaw > 135 && yaw <= 225) {
			return BlockFace.SOUTH;
		} else {
			return BlockFace.WEST;
		}
	}
	
	
	public static boolean isClassAvailable(String qualifiedName) {
		try {
			Class.forName(qualifiedName);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	public static void sendMessageToOps(String message) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.isOp() || player.hasPermission("quickshop.alert")) {
				player.sendMessage(message);
			}
		}
	}
	//Use NMS
	public static void sendItemholochat(ItemStack itemStack, Player player, String normalText) {
	    try {
	        String json = ItemNMS.saveJsonfromNMS(itemStack);
	        if (json == null)
	            return;
	        TextComponent normalmessage = new TextComponent(normalText+"   "+MsgUtil.getMessage("menu.preview"));
	        ComponentBuilder cBuilder = new ComponentBuilder(json);
	        HoverEvent he = new HoverEvent(HoverEvent.Action.SHOW_ITEM, cBuilder.create());
	        normalmessage.setHoverEvent(he);
	        player.spigot().sendMessage(normalmessage);
	    } catch (Throwable t) {
	        sendItemholochatAsNormaly(itemStack, player, normalText);
	    }
	}

	// Without NMS
	public static void sendItemholochatAsNormaly(ItemStack itemStack, Player player, String normalText) {
		try {
		String Itemname = null;
		List<String> Itemlore = new ArrayList<>();
		String finalItemdata = null;
		Map<Enchantment, Integer> enchs = new HashMap<Enchantment, Integer>();
		Map<String, Integer> Itemenchs = new HashMap<String, Integer>();
		if (itemStack.hasItemMeta()) {
			ItemMeta iMeta = itemStack.getItemMeta();
			if (iMeta.hasDisplayName()) {
				Itemname = iMeta.getDisplayName();
			} else {
				Itemname = MsgUtil.getItemi18n(itemStack.getType().name());
			}
			if (iMeta.hasLore()) {
				Itemlore = iMeta.getLore();
				} else {
				Itemlore = new ArrayList<String>();
			}
			if (iMeta.hasEnchants()) {
				enchs = iMeta.getEnchants();
				for (Entry<Enchantment, Integer> entries : enchs.entrySet()) {
					String i18n = MsgUtil.getEnchi18n(entries.getKey());
					if (i18n != null) {
						Itemenchs.put(i18n, entries.getValue());
					} else {
						Itemenchs = null;
					}
				}
			}
		} else {
			Itemname = MsgUtil.getDisplayName(itemStack);
			Itemlore = null;
			Itemenchs = null;
		}
		if(Itemname!=MsgUtil.getItemi18n(itemStack.getType().name())) {
			finalItemdata = Itemname+" "+ChatColor.GRAY+"("+MsgUtil.getItemi18n(itemStack.getType().name())+ChatColor.GRAY+")";
		}else {
			finalItemdata = Itemname;
		}
		
		finalItemdata += "\n";
		List<String> a = new ArrayList<>();
		List<Integer> b = new ArrayList<>();
		a.addAll(Itemenchs.keySet());
		b.addAll(Itemenchs.values());
		for (int i = 0; i < a.size(); i++) {
			finalItemdata += ChatColor.GRAY + a.get(i) + " " + Util.formatEnchLevel(b.get(i)) + "\n";
		}
		
		String potionResult = getPotiondata(itemStack);
		if(potionResult!=null) {
			finalItemdata += potionResult;
		}

		if (Itemlore != null) {
			for (String string : Itemlore) {
				finalItemdata += ChatColor.DARK_PURPLE +""+ ChatColor.ITALIC + string + "\n";
			}
		}
		TextComponent normalmessage = new TextComponent(normalText+"   "+MsgUtil.getMessage("menu.preview"));
		ComponentBuilder cBuilder = new ComponentBuilder(finalItemdata);
		HoverEvent he = new HoverEvent(HoverEvent.Action.SHOW_TEXT, cBuilder.create());
		normalmessage.setHoverEvent(he);
		player.spigot().sendMessage(normalmessage);
		}catch (Exception e) {
			player.sendMessage(normalText);
			QuickShop.instance.getLogger().severe("QuickShop cannot send Advanced chat message, Are you using CraftBukkit? Please use Spigot or SpigotFork.");
		}
	}
	private static String formatEnchLevel(Integer level) {
		switch (level) {
		case 1:
			return "I";
		case 2:
			return "II";
		case 3:
			return "III";
		case 4:
			return "IV";
		case 5:
			return "V";
		default:
			return String.valueOf(level);

	}
	}
	/**
	 * @param iStack
	 * @return
	 */
	public static String getPotiondata(ItemStack iStack) {
		if((iStack.getType() != Material.POTION)==true && (iStack.getType() !=Material.LINGERING_POTION)==true && (iStack.getType() !=Material.SPLASH_POTION)==true){
			return null;
		}
		List<String> pEffects =  new ArrayList<String>();
		PotionMeta pMeta = (PotionMeta)iStack.getItemMeta();
		if(pMeta.getBasePotionData().getType()!=null) {
			if(!(pMeta.getBasePotionData().isUpgraded())){
				pEffects.add(ChatColor.BLUE+MsgUtil.getPotioni18n(pMeta.getBasePotionData().getType().getEffectType()));
			}else {
				pEffects.add(ChatColor.BLUE+MsgUtil.getPotioni18n(pMeta.getBasePotionData().getType().getEffectType())+" II");
			}
			
		}
		if(pMeta.hasCustomEffects()) {
			List<PotionEffect> cEffects = pMeta.getCustomEffects();
			for (PotionEffect potionEffect : cEffects) {
				pEffects.add(MsgUtil.getPotioni18n(potionEffect.getType())+" "+formatEnchLevel(potionEffect.getAmplifier()));
			}
		}
		if(pEffects != null && pEffects.isEmpty() == false) {
			String result = new String();
			for (String effectString : pEffects) {
				result+=effectString+"\n";
			}
			return result;
		}else {
		return null;
		} 
	}
	public static void sendDeprecatedMethodWarn() {
		QuickShop.instance.getLogger().warning("Some plugin calling Deprecated method, Please contact author to use new api!");
	}

	public static boolean isDevEdition() {
		if(QuickShop.instance.getDescription().getVersion().contains("dev")||QuickShop.instance.getDescription().getVersion().contains("alpha")||QuickShop.instance.getDescription().getVersion().contains("beta")||QuickShop.instance.getDescription().getVersion().contains("snapshot")) {
			return true;
		}else {
			return false;
		}
	}
	public static void inventoryCheck(Inventory inv){
				try{
					for (int i =0; i < inv.getSize(); i++)
						if (DisplayItem.checkShopItem(inv.getItem(i))) {
							// Found Item and remove it.
							inv.setItem(i, new ItemStack(Material.AIR, 0));
							plugin.getLogger().warning("[Exploit Alert] A QuickShop item found in "+inv.getHolder().toString()+" Deleteing...");
							Util.sendMessageToOps(ChatColor.RED+"[QuickShop][Exploit alert] A QuickShop item found in "+inv.getHolder().toString()+" Deleteing...");
						}
				}catch (Throwable t){
				}

	}
	public static void debugLog(String logs)	{
		if(plugin.getConfig().getBoolean("dev-mode")) {
			plugin.getLogger().info("[DEBUG]"+logs);
		}
		
	}
}
