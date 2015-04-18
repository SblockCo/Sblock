package co.sblock.commands.chat;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.common.collect.ImmutableList;

import co.sblock.chat.ChannelManager;
import co.sblock.chat.ChatMsgs;
import co.sblock.chat.channel.Channel;
import co.sblock.chat.channel.ChannelType;
import co.sblock.commands.SblockCommand;
import co.sblock.users.OfflineUser;
import co.sblock.users.Users;

/**
 * Command for joining a chat channel without focusing on it.
 * 
 * @author Jikoo
 */
public class ChatListenCommand extends SblockCommand {

	public ChatListenCommand() {
		super("listen");
		setDescription("Join a chat channel without focusing on it.");
		setUsage(ChatColor.AQUA + "/listen <channel>"
				+ ChatColor.YELLOW + ": Listen to <channel>.");
	}

	@Override
	protected boolean onCommand(CommandSender sender, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Console support not offered at this time.");
			return true;
		}
		if (args.length == 0) {
			return false;
		}
		OfflineUser user = Users.getGuaranteedUser(((Player) sender).getUniqueId());
		Channel c = ChannelManager.getChannelManager().getChannel(args[0]);
		if (c == null) {
			user.sendMessage(ChatMsgs.errorInvalidChannel(args[0]));
			return true;
		}
		if (c.getType() == ChannelType.REGION) {
			user.sendMessage(ChatMsgs.errorRegionChannelJoin());
			return true;
		}
		user.addListening(c);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args)
			throws IllegalArgumentException {
		if (!(sender instanceof Player) || args.length > 1) {
			return ImmutableList.of();
		}
		if (args.length ==  1) {
			ArrayList<String> matches = new ArrayList<>();
			for (String channel : ChannelManager.getChannelManager().getChannelList().keySet()) {
				if (StringUtil.startsWithIgnoreCase(channel, args[0])) {
					matches.add(channel);
				}
			}
			return matches;
		}
		return super.tabComplete(sender, alias, args);
	}
}