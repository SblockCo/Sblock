package com.easterlyn.kitchensink.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Private;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import co.aikar.locales.MessageKey;
import com.easterlyn.EasterlynCore;
import com.easterlyn.command.CoreContexts;
import com.easterlyn.user.User;
import com.easterlyn.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

@CommandAlias("colour|color")
@Description("{@@sink.module.colour.description}")
@CommandPermission("easterlyn.command.colour")
public class ColourCommand extends BaseCommand {

	@Dependency
	EasterlynCore core;

	@Default
	@Private
	public void colour(BukkitCommandIssuer issuer) {
		StringBuilder builder = new StringBuilder();
		for (ChatColor colour : ChatColor.values()) {
			builder.append(colour).append('&').append(colour.getChar()).append(' ')
					.append(colour.name().toLowerCase()).append(ChatColor.RESET).append(' ');
		}
		// ACF forces color code translations for & codes, have to manually circumvent.
		issuer.getIssuer().sendMessage(builder.toString());
	}

	@Subcommand("select")
	@Description("{@@sink.module.colour.select.description}")
	@CommandPermission("easterlyn.command.colour.select.self")
	@Syntax("<colour>")
	@CommandCompletion("@colour @playerOnlineWithPerm")
	public void select(@Flags("colour") ChatColor colour, @Flags(CoreContexts.ONLINE_WITH_PERM) User user) {
		user.setColor(colour);
		String colourName = colour + StringUtil.getFriendlyName(colour);
		core.getLocaleManager().sendMessage(user.getPlayer(), "sink.module.colour.set.self",
				"{value}", colourName);
		user.sendMessage("Set colour to " + colourName);
		if (!getCurrentCommandIssuer().getUniqueId().equals(user.getUniqueId())) {
			getCurrentCommandIssuer().sendInfo(MessageKey.of("sink.module.colour.set.other"),
					"{target}", user.getDisplayName(), "{value}", colourName);
		}
	}

	@Subcommand("set")
	@Description("{@@sink.module.colour.set.description}")
	@CommandPermission("easterlyn.command.colour.select.other")
	public void set(ChatColor colour, User target) {
		select(colour, target);
	}

	// Intentionally not configurable, heck you.
	@Private
	@CommandAlias("lel")
	@Description("【ＴＡＳＴＥ  ＴＨＥ  ＰＡＩＮＢＯＷ】")
	@Syntax("[painful to view sentence]")
	@CommandPermission("easterlyn.command.lel")
	public void requestLordEnglishEyeFuck(String message) {
		ChatColor[] rainbow = { ChatColor.DARK_RED, ChatColor.RED, ChatColor.GOLD,
				ChatColor.YELLOW, ChatColor.GREEN, ChatColor.DARK_GREEN, ChatColor.AQUA,
				ChatColor.DARK_AQUA, ChatColor.BLUE, ChatColor.DARK_BLUE, ChatColor.LIGHT_PURPLE,
				ChatColor.DARK_PURPLE };
		StringBuilder lelOut = new StringBuilder();
		for (int i = 0; i < message.length();) {
			for (int j = 0; i < message.length() && j < rainbow.length; ++i, ++j) {
				char charAt = message.charAt(i);
				if (Character.isWhitespace(charAt)) {
					--j;
					lelOut.append(charAt);
					continue;
				}
				lelOut.append(rainbow[j]).append(ChatColor.MAGIC).append(charAt);
			}
		}
		Bukkit.broadcastMessage(lelOut.substring(0, Math.max(lelOut.length() - 1, 0)));
	}

}