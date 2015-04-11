package co.sblock.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import co.sblock.utilities.captcha.Captcha;
import co.sblock.utilities.captcha.CruxiteDowel;
import co.sblock.utilities.inventory.InventoryUtils;

/**
 * Temporary command - Currently, furnaces cannot be opened, but I still want to test the Entry process.
 * 
 * @author Jikoo
 */
public class EntryCarveCommand extends SblockCommand {

	public EntryCarveCommand() {
		super("carve");
		this.setUsage("/carve with a punchcard in hand");
		this.setPermissionLevel("denizen");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (!Captcha.isPunch(player.getItemInHand())) {
				return false;
			}
			ItemStack dowel = CruxiteDowel.carve(player.getItemInHand());
			player.setItemInHand(InventoryUtils.decrement(player.getItemInHand(), 1));
			player.getInventory().addItem(dowel);
			return true;
		}
		return false;
	}

}
