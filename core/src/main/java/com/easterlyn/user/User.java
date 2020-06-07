package com.easterlyn.user;

import com.easterlyn.EasterlynCore;
import com.easterlyn.event.PlayerNameChangeEvent;
import com.easterlyn.event.UserCreationEvent;
import com.easterlyn.util.Colors;
import com.easterlyn.util.GenericUtil;
import com.easterlyn.util.PermissionUtil;
import com.easterlyn.util.PlayerUtil;
import com.easterlyn.util.Request;
import com.easterlyn.util.StringUtil;
import com.easterlyn.util.command.Group;
import com.easterlyn.util.wrapper.ConcurrentConfiguration;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Data storage for a user.
 *
 * @author Jikoo
 */
public class User implements Group {

	private final EasterlynCore plugin;
	private final UUID uuid;
	private final ConcurrentConfiguration storage;
	private final Map<String, Object> tempStore;

	User(@NotNull EasterlynCore plugin, @NotNull UUID uuid, @NotNull ConcurrentConfiguration storage) {
		this.plugin = plugin;
		this.uuid = uuid;
		this.storage = storage;
		tempStore = new ConcurrentHashMap<>();
	}

	protected User(User user) {
		plugin = user.plugin;
		uuid = user.uuid;
		storage = user.storage;
		tempStore = user.tempStore;
	}

	@NotNull
	public UUID getUniqueId() {
		return uuid;
	}

	@Override
	@NotNull
	public Collection<UUID> getMembers() {
		return Collections.singleton(getUniqueId());
	}

	@Nullable
	public Player getPlayer() {
		try {
			return PlayerUtil.getPlayer(plugin, getUniqueId());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	@NotNull
	public String getDisplayName() {
		return GenericUtil.orDefault(getStorage().getString("displayName"),
				GenericUtil.orDefault(GenericUtil.functionAs(Player.class, getPlayer(), Player::getDisplayName),
						getUniqueId().toString()));
	}

	@NotNull
	public ChatColor getColor() {
		return Colors.getOrDefault(getStorage().getString("color"), getRank().getColor());
	}

	public void setColor(@NotNull ChatColor color) {
		if (color == ChatColor.RESET) {
			getStorage().set("color", null);
			return;
		}
		if (!color.isColor()) {
			throw new IllegalArgumentException("Color must be a color, not a format code!");
		}
		getStorage().set("color", color.name());
	}

	public boolean isOnline() {
		return plugin.getServer().getPlayer(getUniqueId()) != null;
	}

	public boolean hasPermission(String permission) {
		if (isOnline()) {
			Player player = getPlayer();
			if (player != null) {
				return player.hasPermission(permission);
			}
		}
		return PermissionUtil.hasPermission(getUniqueId(), permission);
	}

	@NotNull
	public UserRank getRank() {
		UserRank[] userRanks = UserRank.values();
		for (int i = userRanks.length - 1; i > 0; --i) {
			if (hasPermission(userRanks[i].getPermission())) {
				return userRanks[i];
			}
		}
		return UserRank.MEMBER;
	}

	@NotNull
	protected Pattern getMentionPattern() {
		Object storedPattern = getTemporaryStorage().get("mentionPattern");
		if (storedPattern instanceof Pattern) {
			return (Pattern) storedPattern;
		}
		StringBuilder builder = new StringBuilder("^@?(");
		OfflinePlayer player = Bukkit.getOfflinePlayer(getUniqueId());
		if (player.getName() != null) {
			builder.append(player.getName()).append('|');
			Player online = player.isOnline() ? player.getPlayer() : null;
			if (online != null && !online.getDisplayName().isEmpty() && !online.getDisplayName().equals(player.getName())) {
				builder.append("\\Q").append(online.getDisplayName()).append("\\E|");
			}
		}
		builder.append(getUniqueId()).append(")([\\\\W&&[^" + ChatColor.COLOR_CHAR + "}]])?$");
		Pattern pattern = Pattern.compile(builder.toString());
		getTemporaryStorage().put("mentionPattern", pattern);
		return pattern;
	}

	@Override
	public TextComponent getMention() {
		TextComponent component = new TextComponent("@" + getDisplayName());
		component.setColor(getColor().asBungee());
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(getUniqueId());
		component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
				(offlinePlayer.isOnline() ? "/msg " : "/mail send ") + (offlinePlayer.getName() != null ? offlinePlayer.getName() : getUniqueId())));

		List<TextComponent> hovers = new ArrayList<>();
		TextComponent line = new TextComponent(getDisplayName());
		line.setColor(getColor().asBungee());
		if (offlinePlayer.getName() != null && !offlinePlayer.getName().equals(line.getText())) {
			TextComponent realName = new TextComponent(" (" + offlinePlayer.getName() + ")");
			realName.setColor(net.md_5.bungee.api.ChatColor.WHITE);
			line.addExtra(realName);
		}
		TextComponent extra = new TextComponent(" - ");
		extra.setColor(net.md_5.bungee.api.ChatColor.WHITE);
		line.addExtra(extra);
		extra = new TextComponent("Click to message!");
		extra.setColor(Colors.COMMAND.asBungee());
		line.addExtra(extra);
		hovers.add(line);

		UserRank rank = getRank();
		line = new TextComponent("\n" + rank.getFriendlyName());
		line.setColor(rank.getColor().asBungee());
		hovers.add(line);
		// TODO class and affinity
		// TODO could cache in temp store, but needs to be deleted on perm change (login/command)

		component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hovers.toArray(new TextComponent[0])));
		return component;
	}

	public void sendMessage(@NotNull String message) {
		sendMessage(StringUtil.toJSON(message).toArray(new TextComponent[0]));
	}

	public void sendMessage(@NotNull BaseComponent... components) {
		Player player = getPlayer();
		if (player != null) {
			player.sendMessage(components);
		}
	}

	@NotNull
	public ConcurrentConfiguration getStorage() {
		return storage;
	}

	@NotNull
	public Map<String, Object> getTemporaryStorage() {
		return tempStore;
	}

	/**
	 * Gets and clears the pending request, if any.
	 *
	 * @return the Request or null if not present
	 */
	@Nullable
	public Request pollPendingRequest() {
		Object stored = tempStore.remove("core.request");
		if (!(stored instanceof Request)) {
			return null;
		}
		Request request = (Request) stored;
		return request.getExpiry() > System.currentTimeMillis() ? request : null;
	}

	/**
	 * Sets a user's pending request if nothing is currently pending.
	 *
	 * @param request the Request
	 * @return true if the Request was successfully added
	 */
	public boolean setPendingRequest(@NotNull Request request) {
		Object stored = tempStore.get("core.request");
		if (stored instanceof Request && ((Request) stored).getExpiry() > System.currentTimeMillis()) {
			return false;
		}
		tempStore.put("core.request", request);
		return true;
	}

	/**
	 * The String representation of the Player's total time in game.
	 *
	 * @return the Player's time in game
	 */
	@NotNull
	private String getTimePlayed() {
		Player player = getPlayer();
		if (player == null) {
			return "0 days, 00:00";
		}
		int time = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60;
		int days = time / (24 * 60);
		time -= days * 24 * 60;
		int hours = time / (60);
		time -= hours * 60;
		DecimalFormat decimalFormat = new DecimalFormat("00");
		return days + " days, " + decimalFormat.format(hours) + ':' + decimalFormat.format(time);
	}

	public EasterlynCore getPlugin() {
		return plugin;
	}

	void save() {
		File file = new File(plugin.getDataFolder().getPath() + File.separatorChar + "users", getUniqueId().toString() + ".yml");
		try {
			getStorage().save(file);
		} catch (IOException e) {
			throw new RuntimeException("Unable to save data for " + getUniqueId(), e);
		}
	}

	@NotNull
	static User load(@NotNull EasterlynCore plugin, @NotNull final UUID uuid) {
		PluginManager pluginManager = plugin.getServer().getPluginManager();
		File file = new File(plugin.getDataFolder().getPath() + File.separatorChar + "users", uuid.toString() + ".yml");
		if (file.exists()) {
			ConcurrentConfiguration storage = ConcurrentConfiguration.load(file);
			User user = new User(plugin, uuid, storage);
			Player player = user.getPlayer();

			if (player != null && player.getAddress() != null) {
				storage.set("ip", player.getAddress().getHostString());
				String previousName = storage.getString("name");
				if (previousName != null && !previousName.equals(player.getName())) {
					storage.set("previousName", previousName);
					storage.set("name", player.getName());
					pluginManager.callEvent(new PlayerNameChangeEvent(player, previousName, player.getName()));
				}
			}

			return user;
		}

		Player player = Bukkit.getPlayer(uuid);

		User user = new User(plugin, uuid, new ConcurrentConfiguration());
		if (player != null) {
			user.getStorage().set("name", player.getName());
			if (player.getAddress() != null) {
				user.getStorage().set("ip", player.getAddress().getHostString());
			}

			if (!player.hasPlayedBefore()) {
				pluginManager.callEvent(new UserCreationEvent(user));
			}
		}

		return user;
	}

}