package co.sblock.commands.cheat;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.ImmutableList;

import co.sblock.Sblock;
import co.sblock.chat.Language;
import co.sblock.commands.SblockCommand;

/**
 * Command for setting the amount of the ItemStack in hand.
 * 
 * @author Jikoo
 */
public class MoreCommand extends SblockCommand {

	public MoreCommand(Sblock plugin) {
		super(plugin, "more");
		this.setDescription("Have all the things! Increase or decrease item in main hand.");
		this.setUsage("/more [optional amount]");
		this.setPermissionLevel("felt");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Console support not offered at this time.");
			return true;
		}
		Player player = (Player) sender;
		ItemStack stack = player.getInventory().getItemInMainHand();
		if (stack == null || stack.getType() == Material.AIR) {
			return false;
		}
		int amount;
		if (args.length > 0) {
			try {
				amount = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				amount = stack.getType().getMaxStackSize();
			}

			amount += stack.getAmount();

			if (amount > 64) {
				amount = 64;
			}
		} else {
			amount = stack.getType().getMaxStackSize();
		}
		if (amount == 1) {
			// Default 64 for unstackable stuff or when people use /more 0
			amount = 64;
		}
		stack.setAmount(amount);
		player.sendMessage(Language.getColor("good") + "Stack in hand set to " + Language.getColor("emphasis.good") + amount);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws IllegalArgumentException {
		return ImmutableList.of();
	}
}
