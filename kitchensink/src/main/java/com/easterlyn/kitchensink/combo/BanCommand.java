package com.easterlyn.kitchensink.combo;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BukkitCommandIssuer;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import com.easterlyn.EasterlynCore;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class BanCommand extends BaseCommand implements Listener {

	@Dependency
	EasterlynCore core;

	@CommandAlias("ban")
	@Description("{@@sink.module.ban.description}")
	@CommandPermission("easterlyn.command.ban")
	@CommandCompletion("@player")
	public void ban(BukkitCommandIssuer issuer, OfflinePlayer target, @Default("Big brother is watching.") String reason) {
		tempban(issuer, target, new Date(Long.MAX_VALUE), reason);
	}

	@CommandAlias("tempban")
	@CommandPermission("easterlyn.command.tempban")
	@Description("{@@sink.module.ban.tempban.description}")
	@CommandCompletion("@player @date")
	public void tempban(BukkitCommandIssuer issuer, OfflinePlayer target, Date date, @Default("Big brother is watching.") String reason) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm 'on' dd MMM yyyy");
		String locale = core.getLocaleManager().getLocale(target.getPlayer());
		String listReason = core.getLocaleManager().getValue("sink.module.ban.banned", locale);
		if (listReason == null) {
			listReason = "Banned: ";
		}
		listReason += reason;
		if (date.getTime() < Long.MAX_VALUE) {
			String value = core.getLocaleManager().getValue("sink.module.ban.expiration", locale,
					"{value}", dateFormat.format(date));
			if (value != null) {
				listReason += '\n' + value;
			}
		}
		target.banPlayer(listReason, date, issuer.getIssuer().getName() + " on " + dateFormat.format(new Date()), true);
		core.getLocaleManager().broadcast("sink.module.ban.announcement",
				"{target}", target.getName() == null ? target.getUniqueId().toString() : target.getName(),
				"{reason}", reason);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		// Silence banned quits, we broadcast it ourselves.
		if (event.getPlayer().isBanned()) {
			event.setQuitMessage(null);
		}
	}

}
