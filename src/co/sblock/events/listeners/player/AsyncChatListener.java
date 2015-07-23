package co.sblock.events.listeners.player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitRunnable;

import co.sblock.Sblock;
import co.sblock.chat.Chat;
import co.sblock.chat.Color;
import co.sblock.chat.ai.HalMessageHandler;
import co.sblock.chat.ai.Halper;
import co.sblock.chat.channel.RegionChannel;
import co.sblock.chat.message.Message;
import co.sblock.chat.message.MessageBuilder;
import co.sblock.events.event.SblockAsyncChatEvent;
import co.sblock.users.Users;
import co.sblock.utilities.general.Cooldowns;
import co.sblock.utilities.messages.JSONUtil;
import co.sblock.utilities.messages.RegexUtils;
import co.sblock.utilities.player.DummyPlayer;

import me.ryanhamshire.GriefPrevention.ClaimsMode;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;

import net.md_5.bungee.api.ChatColor;

/**
 * Listener for PlayerAsyncChatEvents.
 * 
 * @author Jikoo
 */
public class AsyncChatListener implements Listener {

	private final LinkedHashSet<HalMessageHandler> halFunctions;
	private final String[] tests = new String[] {"It is certain.", "It is decidedly so.",
			"Without a doubt.", "Yes, definitely.", "You may rely on it.", "As I see, yes.",
			"Most likely.", "Outlook good.", "Yes.", "Signs point to yes.",
			"Reply hazy, try again.", "Ask again later.", "Better not tell you now.",
			"Cannot predict now.", "Concentrate and ask again.", "Don't count on it.",
			"My reply is no.", "My sources say no.", "Outlook not so good.", "Very doubtful.",
			"Testing complete. Proceeding with operation.", "A critical fault has been discovered while testing.",
			"Error: Test results contaminated.", "tset", "PONG."};
	private final boolean handleGriefPrevention;
	private final Pattern claimPattern, trappedPattern;

	public AsyncChatListener() {
		halFunctions = new LinkedHashSet<>();
		halFunctions.add(new Halper());
		halFunctions.add(Chat.getChat().getHalculator());
		// MegaHal function should be last as it (by design) handles any message passed to it.
		// Insert any additional functions above.
		halFunctions.add(Chat.getChat().getHal());

		handleGriefPrevention = Bukkit.getPluginManager().isPluginEnabled("GriefPrevention");
		if (handleGriefPrevention) {
			unregisterGPChatListener();
			claimPattern = Pattern.compile(GriefPrevention.instance.dataStore.getMessage(Messages.HowToClaimRegex), Pattern.CASE_INSENSITIVE);
			trappedPattern = Pattern.compile("(^|\\s)(stuck|trapped)(\\s|$)", Pattern.CASE_INSENSITIVE);
		} else {
			claimPattern = null;
			trappedPattern = null;
		}
	}

	/**
	 * Because we send JSON messages, we actually have to remove all recipients from the event and
	 * manually send each one the message.
	 * 
	 * To prevent IRC and other chat loggers from picking up chat sent to non-regional channels,
	 * non-regional chat must be cancelled.
	 * 
	 * @param event the SblockAsyncChatEvent
	 */
	@EventHandler(ignoreCancelled = true)
	public void onAsyncPlayerChat(final AsyncPlayerChatEvent event) {
		Message message;
		if (event instanceof SblockAsyncChatEvent) {
			message = ((SblockAsyncChatEvent) event).getSblockMessage();
		} else {
			try {
				MessageBuilder mb = new MessageBuilder().setSender(Users.getGuaranteedUser(event.getPlayer().getUniqueId()))
						.setMessage(event.getMessage());
				// Ensure message can be sent
				if (!mb.canBuild(true) || !mb.isSenderInChannel(true)) {
					event.setCancelled(true);
					return;
				}
				message = mb.toMessage();

				event.getRecipients().removeIf(p -> !message.getChannel().getListening().contains(p.getUniqueId()));
			} catch (Exception e) {
				event.setCancelled(true);
				e.printStackTrace();
				return;
			}
		}

		String cleaned = ChatColor.stripColor(message.getMessage());

		if (cleaned.equalsIgnoreCase("test")) {
			event.getPlayer().sendMessage(ChatColor.RED + tests[(int) (Math.random() * 25)]);
			event.setCancelled(true);
			return;
		}

		for (Player player : event.getRecipients()) {
			if (cleaned.equalsIgnoreCase(player.getName())) {
				event.getPlayer().sendMessage(
						ChatColor.RED + "Names are short and easy to include in a sentence, "
								+ event.getPlayer().getDisplayName() + ". Please do it.");
				event.setCancelled(true);
				return;
			}
		}

		if (Chat.getChat().getHal().isOnlyTrigger(cleaned)) {
			event.getPlayer().sendMessage(Color.HAL + "What?");
			event.setCancelled(true);
			return;
		}

		if (message.getChannel() instanceof RegionChannel && rpMatch(cleaned)) {
			event.getPlayer().sendMessage(Color.HAL + "RP is not allowed in the main chat. Join #rp or #fanrp using /focus!");
			event.setCancelled(true);
			return;
		}

		if (handleGriefPrevention) {
			handleGPChat(event, message);
			if (event.isCancelled()) {
				return;
			}
		}

		event.setFormat(message.getConsoleFormat());
		event.setMessage(cleaned);

		// Flag soft muted messages
		if (event.getRecipients().size() < message.getChannel().getListening().size()) {
			event.setFormat("[SoftMute] " + event.getFormat());
		}

		// Region channels are the only ones that should be appearing in certain plugins
		if (!(message.getChannel() instanceof RegionChannel)) {
			if (!event.isCancelled() && event instanceof SblockAsyncChatEvent) {
				((SblockAsyncChatEvent) event).setGlobalCancelled(true);
			} else {
				event.setCancelled(true);
			}
		}

		// Flag channel as having been used so it is not deleted.
		message.getChannel().updateLastAccess();

		// Manually send messages to each player so we can wrap links, etc.
		message.send(event.getRecipients(), !(event instanceof SblockAsyncChatEvent));

		// Dummy player should not trigger Hal; he may become one.
		if (event.getPlayer() instanceof DummyPlayer) {
			event.getRecipients().clear();
			return;
		}

		// Handle Hal functions
		for (HalMessageHandler handler : halFunctions) {
			if (handler.handleMessage(message, event.getRecipients())) {
				break;
			}
		}

		// No one should receive the final message if it is not cancelled.
		event.getRecipients().clear();
	}

	public boolean rpMatch(String message) {
		if (message.matches("([hH][oO][nN][kK] ?)+")) {
			return true;
		}
		return false;
	}

	private void handleGPChat(final AsyncPlayerChatEvent event, final Message message) {
		if (GriefPrevention.instance == null) {
			return;
		}

		DataStore dataStore = GriefPrevention.instance.dataStore;
		final Player player = event.getPlayer();

		if (claimPattern.matcher(message.getMessage()).find()) {
			if (GriefPrevention.instance.config_claims_worldModes.get(player.getWorld().getName()) == ClaimsMode.Creative) {
				sendMessageOnDelay(player, Color.GOOD + dataStore.getMessage(Messages.CreativeBasicsVideo2,
						"https://www.youtube.com/watch?v=of88cxVmfSM&list=PL8YpI023Cthye5jUr-KGHGfczlNwgkdHM&index=3"));
			} else {
				sendMessageOnDelay(player, Color.GOOD + dataStore.getMessage(Messages.SurvivalBasicsVideo2,
						"https://www.youtube.com/watch?v=VDsjXB-BaE0&list=PL8YpI023Cthye5jUr-KGHGfczlNwgkdHM&index=1"));
			}
		} else if (trappedPattern.matcher(message.getMessage()).find()) {
			// Improvement over GP: Pattern ignores case and matches in substrings of words
			sendMessageOnDelay(player, Color.GOOD + dataStore.getMessage(Messages.TrappedInstructions));
		}

		PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());

		// Hard mute chat to just sender in the event of spam matches
		boolean spam = !message.getChannel().getName().equals("#halchat") && detectGPSpam(event, message, playerData);
		playerData.lastMessage = message.getMessage().toLowerCase();
		message.getChannel().setLastMessage(playerData.lastMessage);
		if (spam) {
			event.getRecipients().clear();
			event.getRecipients().add(player);
			if (playerData.spamCount > 3 && !playerData.spamWarned) {
				sendMessageOnDelay(player, Color.BAD + GriefPrevention.instance.config_spam_warningMessage);
				playerData.spamWarned = true;
				return;
			}
			if (playerData.spamCount > 8 && playerData.spamWarned) {
				sendMessageOnDelay(player, Color.HAL.replace("#", message.getChannel().getName()) + "You were asked not to spam. This mute will last 5 minutes.");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("mute %s 5m", player.getName()));
				event.setCancelled(true);
			}
			return;
		}

		// Soft-muted chat
		if (dataStore.isSoftMuted(player.getUniqueId())) {
			event.setFormat("[SoftMute] " + event.getFormat());
			String soft = new StringBuilder().append(ChatColor.GRAY).append("[SoftMute] ")
					.append(ChatColor.stripColor(message.getConsoleMessage())).toString();
			Iterator<Player> iterator = event.getRecipients().iterator();
			while (iterator.hasNext()) {
				Player recipient = iterator.next();
				if (dataStore.isSoftMuted(recipient.getUniqueId())) {
					continue;
				}
				iterator.remove();
				if (recipient.hasPermission("griefprevention.eavesdrop")) {
					recipient.sendMessage(soft);
				}
			}
		}

		// Fix for GP issue: SoftMuted players cannot ignore others - don't return as soon as softmute is handled.

		// Ignore lists are not currently accessible. This is a problem.
		try {
			Iterator<Player> iterator = event.getRecipients().iterator();
			PlayerData data = dataStore.getPlayerData(player.getUniqueId());
			Field field = dataStore.getClass().getField("ignoredPlayers");
			field.setAccessible(true);
			Object object = field.get(data);
			Method method = object.getClass().getMethod("containsKey", UUID.class);
			while (iterator.hasNext()) {
				UUID uuid = iterator.next().getUniqueId();
				if ((boolean) method.invoke(object, uuid)) {
					iterator.remove();
					continue;
				}
				if ((boolean) method.invoke(field.get(dataStore.getPlayerData(uuid)), player.getUniqueId())) {
					iterator.remove();
					continue;
				}
			}
		} catch (NoSuchFieldException | SecurityException | NoSuchMethodException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// Silently fail to ignore if an error occurs
		}
	}

	private boolean detectGPSpam(AsyncPlayerChatEvent event, Message message, PlayerData playerData) {
		Player player = event.getPlayer();
		if (player.hasPermission("griefprevention.spam")) {
			return false;
		}

		// Rather than ensure player has moved since login, check against achievement and spawn location
		// Disabled for now, as I don't recall ever seeing spammers of this kind. We're a small server.
//		if (!player.hasAchievement(Achievement.MINE_WOOD)
//				&& player.getLocation().getBlock().equals(Users.getSpawnLocation().getBlock())) {
//			sendMessageOnDelay(player, Color.GOOD + dataStore.getMessage(Messages.NoChatUntilMove));
//			return true;
//		}

		String msg = message.getMessage();

		// Caps filter only belongs in regional channels.
		if (message.getChannel() instanceof RegionChannel && msg.length() > 3
				&& StringUtils.getLevenshteinDistance(msg, msg.toUpperCase()) < msg.length() * .25) {
			message.setMessage(msg.toLowerCase());
		}

		msg = msg.toLowerCase();

		// Mute repeat messages
		if (msg.equals(playerData.lastMessage) || message.equals(message.getChannel().getLastMessage())) {
			// In event of exact duplicates, reach penalization levels at a much faster rate
			playerData.spamCount += playerData.spamCount > 0 ? playerData.spamCount : 1;
			event.setFormat("[RepeatChat] " + event.getFormat());
			return true;
		}

		long lastChat = Cooldowns.getInstance().getRemainder(player.getUniqueId(), "chat");
		Cooldowns.getInstance().addCooldown(player.getUniqueId(), "chat", 3000);

		// Cooldown of 1.5 seconds between messages, 3 seconds between short messages.
		if (lastChat > 1500 || msg.length() < 5 && lastChat > 0) {
			playerData.spamCount++;
			event.setFormat("[FastChat] " + event.getFormat());
			return true;
		}

		// Sans links, messages should contain a good symbol/space to length ratio
		String[] words = msg.split(" ");
		int spaces = words.length - 1;
		int length = msg.length();
		int symbols = 0;
		for (String word : words) {
			if (RegexUtils.URL_PATTERN.matcher(word).find()) {
				length -= word.length();
				spaces--;
				continue;
			}
			for (char character : word.toCharArray()) {
				if (!Character.isLetterOrDigit(character)) {
					symbols++;
				}
			}
		}
		if (symbols > length / 2 || length > 15 && spaces < length / 10) {
			playerData.spamCount++;
			event.setFormat("[Gibberish] " + event.getFormat());
			return true;
		}

		// Must be more than 25% different from last message
		if (StringUtils.getLevenshteinDistance(msg, playerData.lastMessage) < msg.length() * .25) {
			playerData.spamCount++;
			event.setFormat("[SimilarChat] " + event.getFormat());
			return true;
		}

		playerData.spamCount = 0;
		playerData.spamWarned = false;
		return false;
	}

	private void sendMessageOnDelay(final Player player, final String message) {
		if (player.spigot() == null) {
			return;
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				if (player != null) {
					player.spigot().sendMessage(JSONUtil.fromLegacyText(message));
				}
			}
		}.runTaskLater(Sblock.getInstance(), 10L);
	}

	private void unregisterGPChatListener() {
		RegisteredListener chatListener = null;
		for (RegisteredListener listener : AsyncPlayerChatEvent.getHandlerList().getRegisteredListeners()) {
			if (listener.getPlugin().getName().equals("GriefPrevention")) {
				chatListener = listener;
				break;
			}
		}
		AsyncPlayerChatEvent.getHandlerList().unregister(chatListener);
	}
}
