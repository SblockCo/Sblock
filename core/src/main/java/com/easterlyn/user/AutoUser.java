package com.easterlyn.user;

import com.easterlyn.EasterlynCore;
import com.easterlyn.util.Colors;
import com.easterlyn.util.GenericUtil;
import com.easterlyn.util.StringUtil;
import com.easterlyn.util.wrapper.ConcurrentConfiguration;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoUser extends User {

	private final Map<String, String> userData;

	public AutoUser(@NotNull EasterlynCore core, @NotNull Map<String, String> userData) {
		super(core, new UUID(0, 0), new ConcurrentConfiguration());
		this.userData = userData;
	}

	@Nullable
	public Player getPlayer() {
		return null;
	}

	@NotNull
	public String getDisplayName() {
		return ChatColor.translateAlternateColorCodes('&',
				GenericUtil.orDefault(userData.get("name"), "Auto User"));
	}

	@NotNull
	public ChatColor getColor() {
		return Colors.getOrDefault(userData.get("color"), getRank().getColor());
	}

	public boolean isOnline() {
		return false;
	}

	public boolean hasPermission(String permission) {
		Permission perm = getPlugin().getServer().getPluginManager().getPermission(permission);
		return perm == null || perm.getDefault() == PermissionDefault.TRUE || perm.getDefault() == PermissionDefault.OP;
	}

	@NotNull
	public UserRank getRank() {
		return UserRank.ADMIN;
	}

	public TextComponent getMention() {
		TextComponent component = new TextComponent("@" + getDisplayName());
		component.setColor(getColor().asBungee());

		String click = userData.get("click");
		if (click != null && !click.isEmpty()) {
			component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, click));
		}

		String hover = userData.get("hover");
		if (hover != null && !hover.isEmpty()) {
			hover = ChatColor.translateAlternateColorCodes('&', hover);
			component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, StringUtil.toJSON(hover).toArray(new TextComponent[0])));
		}

		return component;
	}

	public void sendMessage(@NotNull String message) {
		Bukkit.getConsoleSender().sendMessage(message);
	}

	public void sendMessage(@NotNull BaseComponent... components) {
		Bukkit.getConsoleSender().sendMessage(components);
	}

	void save() {}

}