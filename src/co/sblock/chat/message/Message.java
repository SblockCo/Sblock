package co.sblock.chat.message;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import co.sblock.chat.ChannelManager;
import co.sblock.chat.channel.Channel;
import co.sblock.chat.channel.RegionChannel;
import co.sblock.users.OfflineUser;
import co.sblock.users.Users;
import co.sblock.utilities.Cooldowns;
import co.sblock.utilities.JSONUtil;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Used to better clarify a message's destination prior to formatting.
 * 
 * @author Jikoo
 */
public class Message {

	private static final String DISCORD_FORMAT = "**%s**: %s";
	private static final String DISCORD_FORMAT_THIRD = "***** **%s** *%s*";

	private final OfflineUser sender;
	private final Channel channel;
	private final String name;
	private final boolean thirdPerson;
	private final TextComponent channelComponent, channelHighlightComponent, nameComponent;
	private String consoleFormat, unformattedMessage;
	private TextComponent messageComponent;

	Message(OfflineUser sender, String name, Channel channel, String message, String consoleFormat,
			boolean thirdPerson, TextComponent channelComponent,
			TextComponent channelHighlightComponent, TextComponent nameComponent,
			TextComponent messageComponent) {
		this.sender = sender;
		this.name = name;
		this.channel = channel;
		this.thirdPerson = thirdPerson;
		this.unformattedMessage = message;
		this.consoleFormat = consoleFormat;
		this.channelComponent = channelComponent;
		this.channelHighlightComponent = channelHighlightComponent;
		this.nameComponent = nameComponent;
		this.messageComponent = messageComponent;
	}

	public OfflineUser getSender() {
		return sender;
	}

	public String getSenderName() {
		return name;
	}

	public Channel getChannel() {
		return channel;
	}

	public String getMessage() {
		return unformattedMessage;
	}

	public void setMessage(String message) {
		if (message == null) {
			throw new IllegalArgumentException("Message cannot be null!");
		}
		this.unformattedMessage = message;
		this.messageComponent = new TextComponent(JSONUtil.fromLegacyText(message));
	}

	public String getConsoleMessage() {
		return String.format(getConsoleFormat(), name, unformattedMessage);
	}

	public String getDiscordMessage() {
		// Names will only contain underscores, messages may contain additional formatting.
		// In the future we may allow formatting for all users.
		return String.format(thirdPerson ? DISCORD_FORMAT_THIRD : DISCORD_FORMAT,
				name.replace("_", "\\_"), unformattedMessage.replaceAll("([\\_~*])", "\\\\$1"));
	}

	public void setConsoleFormat(String consoleFormat) {
		this.consoleFormat = consoleFormat;
	}

	public String getConsoleFormat() {
		return consoleFormat;
	}

	public boolean isThirdPerson() {
		return thirdPerson;
	}

	public Channel parseReplyChannel() {
		// All Messages have a click event
		String atChannel = channelComponent.getClickEvent().getValue();

		if (atChannel.length() < 2 || atChannel.charAt(0) != '@') {
			return getChannel();
		}

		int end = atChannel.indexOf(' ');
		if (end == -1) {
			end = atChannel.length();
		}

		Channel channel = ChannelManager.getChannelManager().getChannel(atChannel.substring(1, end));

		return channel == null ? getChannel() : channel;
	}

	public void send() {
		this.send(getChannel().getListening());
	}

	public <T> void send(Collection<T> recipients) {
		this.send(recipients, false);
	}

	public <T> void send(Collection<T> recipients, boolean normalChat) {
		if (!normalChat || !(channel instanceof RegionChannel)) {
			Logger.getLogger("Minecraft").info(getConsoleMessage());
		}

		for (T object : recipients) {
			UUID uuid;
			Player player;
			if (object instanceof UUID) {
				uuid = (UUID) object;
				player = Bukkit.getPlayer(uuid);
			} else if (object instanceof Player) {
				player = (Player) object;
				uuid = player.getUniqueId();
			} else {
				throw new RuntimeException("Invalid recipient type: " + object.getClass());
			}

			OfflineUser u = Users.getGuaranteedUser(uuid);
			if (player == null || !u.isOnline() || player.spigot() == null
					|| channel instanceof RegionChannel && u.getSuppression()) {
				continue;
			}

			BaseComponent message = messageComponent.duplicate();

			if (channel.equals(u.getCurrentChannel())) {
				message.setColor(ChatColor.WHITE);
			} else {
				message.setColor(ChatColor.GRAY);
			}

			if (sender != null && (sender.equals(u) || !sender.getHighlight())) {
				// No self-highlight.
				player.spigot().sendMessage(channelComponent, nameComponent, message);
				continue;
			}

			boolean highlight = false;

			StringBuilder patternString = new StringBuilder("(^|\\W)(");
			for (String highlightString : u.getHighlights(getChannel())) {
				if (patternString.length() > 8) {
					patternString.append('|');
				}
				patternString.append(highlightString);
			}
			patternString.append(")(\\W|$)");
			Pattern pattern = Pattern.compile(patternString.toString(), Pattern.CASE_INSENSITIVE);
			for (BaseComponent component : message.getExtra()) {
				TextComponent text = (TextComponent) component;
				String componentMessage = text.getText();
				Matcher match = pattern.matcher(text.getText());
				List<BaseComponent> components = new LinkedList<>();
				int lastEnd = 0;
				while (match.find()) {
					components.add(new TextComponent(componentMessage.substring(lastEnd, match.start())));
					TextComponent highlightComponent = new TextComponent(match.group());
					highlightComponent.setColor(ChatColor.AQUA);
					components.add(highlightComponent);
					lastEnd = match.end();
				}
				if (lastEnd == 0) {
					continue;
				}
				highlight = true;
				components.add(new TextComponent(componentMessage.substring(lastEnd)));
				text.setText("");
				text.setExtra(components);
			}

			if (highlight && Cooldowns.getInstance().getRemainder(player, "highlight") == 0
					&& !channel.getName().equals("#pm")) {
				// Fun sound effects! Sadly, ender dragon kill is a little long even at 2x
				switch ((int) (Math.random() * 20)) {
				case 0:
					player.playSound(player.getLocation(), Sound.ENDERMAN_STARE, 1, 2);
					break;
				case 1:
					player.playSound(player.getLocation(), Sound.WITHER_SPAWN, 1, 2);
					break;
				case 2:
				case 3:
					player.playSound(player.getLocation(), Sound.ANVIL_LAND, 1, 1);
					break;
				default:
					player.playSound(player.getLocation(), Sound.LEVEL_UP, 1, 2);
				}
				Cooldowns.getInstance().addCooldown(player, "highlight", 30000);
			}
			player.spigot().sendMessage(highlight ? channelHighlightComponent : channelComponent, nameComponent, message);
		}
	}
}
