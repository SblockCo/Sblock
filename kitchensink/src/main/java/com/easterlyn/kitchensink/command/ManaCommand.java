package com.easterlyn.kitchensink.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.easterlyn.EasterlynCaptchas;
import com.easterlyn.EasterlynCore;
import com.easterlyn.command.CoreContexts;
import com.easterlyn.util.Colors;
import com.easterlyn.util.EconomyUtil;
import com.easterlyn.util.ExperienceUtil;
import com.easterlyn.util.StringUtil;
import java.text.DecimalFormat;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("mana")
@Description("{@@sink.module.mana.description}")
@CommandPermission("easterlyn.command.mana")
public class ManaCommand extends BaseCommand {

	@Dependency
	EasterlynCore core;
	@Dependency
	EasterlynCaptchas captchas;

	@Subcommand("current")
	@CommandCompletion("")
	@Syntax("")
	public void current(@Flags(CoreContexts.SELF) Player player) {
		core.getLocaleManager().sendMessage(player, "sink.module.mana.current",
				"{exp}", String.valueOf(ExperienceUtil.getExp(player)));
	}

	@CommandAlias("mana")
	@CommandCompletion("@integer")
	@Syntax("<exp>")
	public void experience(long experience) {
		core.getLocaleManager().sendMessage(getCurrentCommandIssuer().getIssuer(), "sink.module.mana.exp_to_level",
				"{exp}", String.valueOf(experience), "{level}", getFormat().format(ExperienceUtil.getLevelFromExp(experience)));
	}

	@CommandAlias("mana")
	@CommandCompletion("@integer")
	@Syntax("<level>L")
	public void level(@Single String argument) {
		if (!argument.matches("\\d+[lL]")) {
			showSyntax(getCurrentCommandIssuer(), getLastCommandOperationContext().getRegisteredCommand());
			return;
		}
		int level = Integer.parseInt(argument.substring(0, argument.length() - 1));
		core.getLocaleManager().sendMessage(getCurrentCommandIssuer().getIssuer(), "sink.module.mana.exp_to_level",
				"{exp}", getFormat().format(ExperienceUtil.getExpFromLevel(level)), "{level}", String.valueOf(level));
	}

	@Subcommand("cost")
	@CommandCompletion("")
	@Syntax("")
	public void cost(@Flags(CoreContexts.SELF) Player player) {
		ItemStack hand = player.getInventory().getItemInMainHand();
		while (EasterlynCaptchas.isUsedCaptcha(hand)) {
			ItemStack oldHand = hand;
			hand = captchas.getItemByCaptcha(hand);
			if (hand == null || hand.isSimilar(oldHand)) {
				hand = oldHand;
				break;
			} else {
				hand.setAmount(hand.getAmount() * oldHand.getAmount());
			}
		}
		if (hand.getType().isAir()) {
			core.getLocaleManager().sendMessage(player, "sink.module.mana.air");
			return;
		}

		double worth;
		try {
			worth = EconomyUtil.getWorth(hand);
		} catch (ArithmeticException e) {
			player.sendMessage(e.getMessage());
			return;
		}

		BaseComponent component = new TextComponent();

		TextComponent itemComponent = StringUtil.getItemComponent(hand);
		if (hand.getAmount() > 1) {
			itemComponent.addExtra(Colors.getOrDefault("normal.b", ChatColor.DARK_AQUA) + "x" + hand.getAmount());
		}
		component.addExtra(itemComponent);

		String value = core.getLocaleManager().getValue("sink.module.mana.exp_to_level",
				core.getLocaleManager().getLocale(getCurrentCommandIssuer().getIssuer()),
				"{exp}", getFormat().format(worth), "{level}", String.valueOf(ExperienceUtil.getLevelFromExp((long) worth)));
		for (TextComponent text : StringUtil.toJSON(value)) {
			component.addExtra(text);
		}

		player.sendMessage(component);
	}

	private DecimalFormat getFormat() {
		return new DecimalFormat("#,###,###,###.###");
	}

}