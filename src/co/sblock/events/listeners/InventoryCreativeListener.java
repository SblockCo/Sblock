package co.sblock.events.listeners;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/**
 * Listener for InventoryCreativeEvents. Used to clean input items from creative clients, preventing
 * server/client crashes.
 * 
 * @author Jikoo
 */
public class InventoryCreativeListener implements Listener {

	/**
	 * EventHandler for InventoryCreativeEvents. Triggered when a creative client spawns an item.
	 * 
	 * @param event the InventoryCreativeEvent
	 */
	@EventHandler
	public void onInventoryCreative(InventoryCreativeEvent event) {
		if (event.getWhoClicked().hasPermission("group.denizen")) {
			return;
		}

		if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) {
			return;
		}

		if (event.getCursor().getType().name() == "BANNER") {
			// Banners actually come with NBT tags when using pick-block. We'll just avoid them for now.
			return;
		}

		// TODO blacklist certain server-heavy items such as end portal
		// TODO block pick-block on similar items

		// By not using the original ItemStack, we remove all lore and attributes spawned.
		ItemStack cleanedItem = new ItemStack(event.getCursor().getType());
		// Why Bukkit doesn't have a constructor ItemStack(MaterialData) I don't know.
		cleanedItem.setData(event.getCursor().getData());

		// No invalid durabilities.
		if (event.getCursor().getDurability() < 1) {
			cleanedItem.setDurability((short) 1);
		} else if (event.getCursor().getDurability() < event.getCursor().getType()
				.getMaxDurability()) {
			cleanedItem.setDurability(event.getCursor().getDurability());
		}

		// No overstacking, no negative amounts (negative dispensed by dropper/dispenser = infinite)
		if (event.getCursor().getAmount() > event.getCursor().getMaxStackSize()) {
			event.getCursor().setAmount(event.getCursor().getMaxStackSize());
		} else if (event.getCursor().getAmount() < 1) {
			event.getCursor().setAmount(1);
		} else {
			cleanedItem.setAmount(event.getCursor().getAmount());
		}

		// Creative enchanted books are allowed a single enchant
		if (event.getCursor().getType() == Material.ENCHANTED_BOOK && event.getCursor().hasItemMeta()) {
			EnchantmentStorageMeta meta = (EnchantmentStorageMeta) Bukkit.getItemFactory().getItemMeta(Material.ENCHANTED_BOOK);
			for (Map.Entry<Enchantment, Integer> entry : ((EnchantmentStorageMeta) event.getCursor().getItemMeta()).getStoredEnchants().entrySet()) {
				meta.addStoredEnchant(entry.getKey(), entry.getValue(), false);
				break;
			}
			cleanedItem.setItemMeta(meta);
		}

		event.setCursor(cleanedItem);
	}
}
