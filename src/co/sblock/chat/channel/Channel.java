package co.sblock.chat.channel;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import co.sblock.chat.ChatMsgs;
import co.sblock.chat.ColorDef;
import co.sblock.chat.SblockChat;
import co.sblock.data.SblockData;
import co.sblock.users.Region;
import co.sblock.users.User;
import co.sblock.users.UserManager;
import co.sblock.utilities.Log;


/**
 * Defines default channel behavior
 * 
 * @author Dublek, Jikoo
 */
public abstract class Channel {

	/**
	 * 
	 * @author ted
	 *
	 * A wrapper for the Channels because frankly, they are managed in a rediculous, absurd way
	 * this is just a temporary solution for serializing them until I get around to refactoring
	 * that too.
	 *
	 */
	public static class ChannelSerialiser {

		private final ChannelType ct;
		private final String name;
		private final AccessLevel access;

		private final Set<UUID> approvedPlayers;
		private final Set<UUID> moderators;
		private final Set<UUID> mutedPlayers;
		private final Set<UUID> bannedPlayers;
		private final Set<UUID> listeningPlayers;

		private final UUID owner;

		/**
		 * oh boy, there are alot of params...
		 * 
		 * @param ct
		 * @param name
		 * @param access
		 * @param owner
		 * @param approvedPlayers
		 * @param moderators
		 * @param mutedPlayers
		 * @param bannedPlayers
		 * @param listeningPlayers
		 */
		public ChannelSerialiser(ChannelType ct, String name, AccessLevel access, UUID owner,
				Set<UUID> approvedPlayers,
				Set<UUID> moderators,
				Set<UUID> mutedPlayers,
				Set<UUID> bannedPlayers,
				Set<UUID> listeningPlayers) {
			this.approvedPlayers = approvedPlayers;
			this.moderators = moderators;
			this.mutedPlayers = mutedPlayers;
			this.bannedPlayers = bannedPlayers;
			this.listeningPlayers = listeningPlayers;
			this.ct = ct;
			this.name = name;
			this.access = access;
			this.owner = owner;
		}

		public ChannelSerialiser(ChannelType ct, String name, AccessLevel access, UUID owner) {
			this.approvedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
			this.moderators = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
			this.mutedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
			this.bannedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
			this.listeningPlayers = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
			this.ct = ct;
			this.name = name;
			this.access = access;
			this.owner = owner;
		}

		public Channel build() {
			Channel chan;
			switch(ct) {
				case NICK:
					chan = new NickChannel(name, access, owner);
					break;
				case NORMAL:
					chan = new NormalChannel(name, access, owner);
					break;
				case REGION:
					chan = new RegionChannel(name, access, owner);
					break;
				case RP:
					chan = new RPChannel(name, access, owner);
					break;
				default:
					throw new RuntimeException("if this gets called you have some SERIOUS issues");
			}
			chan.approvedList = approvedPlayers;
			chan.modList = moderators;
			chan.muteList = mutedPlayers;
			chan.banList = bannedPlayers;
			chan.listening = listeningPlayers;
			return chan;
		}

	}

	/*
	 * Immutable Data regardign the channel
	 */
	protected String name;
	protected AccessLevel access;
	protected Set<UUID> approvedList;
	protected Set<UUID> modList;
	protected Set<UUID> muteList;
	protected Set<UUID> banList;
	protected Set<UUID> listening;

	/*
	 * The owner is mutable (for some reason)
	 */
	protected UUID owner;

	/**
	 * @param name the name of the channel
	 * @param a the access level of the channel
	 * @param creator the owner of the channel
	 */
	public Channel(String name, AccessLevel a, UUID creator) {
		this.name = name;
		this.access = a;
		this.owner = creator;
		approvedList = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
		modList = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
		if (creator != null) {
			this.modList.add(creator);
		}
		muteList = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());;
		banList = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());;
		listening = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());;
		if (creator != null) {
			SblockData.getDB().saveChannelData(this);
		}
	}

	public String getName() {
		return this.name;
	}

	public AccessLevel getAccess() {
		return this.access;
	}

	public Set<UUID> getListening() {
		return this.listening;
	}

	public abstract ChannelType getType();

	/**
	 * ONLY CALL FROM CHATUSER
	 * 
	 * @param userID the user UUID to add listening
	 */
	public void addListening(UUID userID) {
		this.listening.add(userID);
	}

	/**
	 * ONLY CALL FROM CHATUSER
	 * 
	 * @param user the UUID to remove from listening.
	 */
	public void removeListening(UUID userID) {
		this.listening.remove(userID);
	}

	public abstract void setNick(User sender, String nick);

	public abstract void removeNick(User sender, boolean warn);

	public abstract String getNick(User sender);

	public abstract boolean hasNick(User sender);

	public abstract User getNickOwner(String nick);

	public abstract ChannelSerialiser toSerialiser();

	public void setOwner(User sender, UUID newOwner) {
		if (sender.equals(this.owner)) {
			this.owner = newOwner;
		}
	}

	public UUID getOwner() {
		return this.owner;
	}

	public boolean isOwner(User sender) {
		return sender.getUUID().equals(owner) || sender.getPlayer().hasPermission("group.denizen");
	}

	/**
	 * Method used by database to load a moderator silently.
	 * 
	 * @param user the name to add as a moderator
	 */
	public void loadMod(UUID id) {
		this.modList.add(id);
	}

	public void addMod(User sender, UUID userID) {
		if (!this.isChannelMod(sender)) {
			sender.sendMessage(ChatMsgs.onChannelCommandFail(this.name));
			return;
		}
		if (UserManager.getUser(userID) == null) {
			sender.sendMessage(ChatMsgs.errorInvalidUser(userID.toString()));
			return;
		}
		User user = UserManager.getUser(userID);
		String message = ChatMsgs.onChannelModAdd(user.getPlayerName(), this.name);
		if (!this.isChannelMod(UserManager.getUser(userID))) {
			this.modList.add(userID);
			this.sendMessage(message);
			if (!this.listening.contains(userID)) {
				user.sendMessage(message);
			}
		} else {
			sender.sendMessage(message);
		}
	}

	public void removeMod(User sender, UUID userID) {
		if (!this.isChannelMod(sender)) {
			sender.sendMessage(ChatMsgs.onChannelCommandFail(this.name));
			return;
		}
		if (UserManager.getUser(userID) == null) {
			sender.sendMessage(ChatMsgs.errorInvalidUser(userID.toString()));
			return;
		}
		User user = UserManager.getUser(userID);
		String message = ChatMsgs.onChannelModRm(user.getPlayerName(), this.name);
		if (this.modList.contains(userID) && !this.isOwner(user)) {
			this.modList.remove(userID);
			this.sendMessage(message);
			if (!this.listening.contains(userID)) {
				user.sendMessage(message);
			}
		} else {
			sender.sendMessage(message);
		}
	}

	public Set<UUID> getModList() {
		return this.modList;
	}

	public boolean isChannelMod(User sender) {
		return isMod(sender) || sender.getPlayer().hasPermission("group.felt");
	}

	public boolean isMod(User sender) {
		return modList.contains(sender.getUUID()) || sender.getPlayer().hasPermission("group.denizen");
	}

	public void kickUser(User sender, UUID userID) {
		if (!this.isChannelMod(sender)) {
			sender.sendMessage(ChatMsgs.onChannelCommandFail(this.name));
			return;
		}
		if (UserManager.getUser(userID) == null) {
			sender.sendMessage(ChatMsgs.errorInvalidUser(userID.toString()));
			return;
		}
		User user = UserManager.getUser(userID);
		String message = ChatMsgs.onUserKickAnnounce(user.getPlayerName(), this.name);
		if (this.isOwner(user)) {
			sender.sendMessage(ChatMsgs.onChannelCommandFail(this.name));
		} else if (listening.contains(user.getPlayerName())) {
			this.sendMessage(message);
			this.listening.remove(user.getUUID());
			user.removeListening(this.getName());
		} else {
			sender.sendMessage(message);
		}

	}

	/**
	 * Method used by database to load a ban silently.
	 * 
	 * @param user the UUID to add as a ban
	 */
	public void loadBan(UUID userID) {
		this.banList.add(userID);
	}

	public void banUser(User sender, UUID userID) {
		if (!this.isChannelMod(sender)) {
			sender.sendMessage(ChatMsgs.onChannelCommandFail(this.name));
			return;
		}
		if (UserManager.getUser(userID) == null) {
			sender.sendMessage(ChatMsgs.errorInvalidUser(userID.toString()));
			return;
		}
		User user = UserManager.getUser(userID);
		String message = ChatMsgs.onUserBanAnnounce(Bukkit.getPlayer(userID).getName(), this.name);
		if (this.isOwner(user)) {
			sender.sendMessage(ChatMsgs.onChannelCommandFail(this.name));
		} else if (!this.isBanned(user)) {
			if (modList.contains(userID)) {
				modList.remove(userID);
			}
			this.approvedList.remove(userID);
			this.banList.add(userID);
			this.sendMessage(message);
			if (listening.contains(userID)) {
				user.removeListening(this.getName());
			}
		} else {
			sender.sendMessage(message);
		}
	}

	public void unbanUser(User sender, UUID userID) {
		if (!this.isOwner(sender)) {
			sender.sendMessage(ChatMsgs.onChannelCommandFail(this.name));
			return;
		}
		if (UserManager.getUser(userID) == null) {
			sender.sendMessage(ChatMsgs.errorInvalidUser(userID.toString()));
			return;
		}
		User user = UserManager.getUser(userID);
		String message = ChatMsgs.onUserUnbanAnnounce(user.getPlayerName(), this.name);
		if (banList.contains(userID)) {
			this.banList.remove(userID);
			user.sendMessage(message);
			this.sendMessage(message);
		} else {
			sender.sendMessage(message);
		}
	}

	public Set<UUID> getBanList() {
		return banList;
	}

	public boolean isBanned(User user) {
		return banList.contains(user.getUUID());
	}

	public void loadApproval(UUID userID) {
		this.approvedList.add(userID);
	}

	public void approveUser(User sender, UUID target) {
		if (this.getAccess().equals(AccessLevel.PUBLIC)) {
			sender.sendMessage(ChatMsgs.unsupportedOperation(this.name));
			return;
		} else {
			User targ = UserManager.getUser(target);
			String message = ChatMsgs.onUserApproved(targ.getPlayerName(), this.name);
			if (this.isApproved(targ)) {
				sender.sendMessage(message);
				return;
			}
			approvedList.add(target);
			this.sendMessage(message);
			targ.sendMessage(message);
		}
	}

	public void deapproveUser(User sender, UUID target) {
		if (this.getAccess().equals(AccessLevel.PUBLIC)) {
			sender.sendMessage(ChatMsgs.unsupportedOperation(this.name));
			return;
		} else {
			User targ = UserManager.getUser(target);
			String message = ChatMsgs.onUserDeapproved(targ.getPlayerName(), this.name);
			if (!this.isApproved(targ)) {
				sender.sendMessage(message);
				return;
			}
			approvedList.remove(target);
			this.sendMessage(message);
			targ.removeListeningSilent(this);
		}
	}

	public Set<UUID> getApprovedUsers() {
		return approvedList;
	}

	public boolean isApproved(User user) {
		return approvedList.contains(user.getUUID()) || isChannelMod(user);
	}

	public void disband(User sender) {
		if (this.owner == null) {
			sender.sendMessage(ChatMsgs.errorDisbandDefault());
			return;
		}
		if (!this.isOwner(sender)) {
			sender.sendMessage(ChatMsgs.onChannelCommandFail(this.name));
			return;
		}
		this.sendMessage(ChatMsgs.onChannelDisband(this.getName()));
		for (UUID userID : this.listening.toArray(new UUID[0])) {
			UserManager.getUser(userID).removeListeningSilent(this);
		}
		SblockChat.getChat().getChannelManager().dropChannel(this.name);
	}

	/**
	 * For sending a channel message, not for chat! Chat should be handled by getting a Message from
	 * ChannelManager.
	 * 
	 * @param message the message to send the channel.
	 */
	public void sendMessage(String message) {
		for (UUID userID : this.listening.toArray(new UUID[0])) {
			User u = UserManager.getUser(userID);
			if (u == null) {
				listening.remove(userID);
				continue;
			}
			u.sendMessage(message);
		}
		Log.anonymousInfo(message);
	}

	/**
	 * Gets chat channel name prefix.
	 * 
	 * @param sender the User sending the message
	 * 
	 * @return the channel prefix
	 */
	public String formatMessage(User sender, boolean isThirdPerson) {

		ChatColor guildRank;
		ChatColor channelRank;
		ChatColor globalRank = null;
		ChatColor region;
		String nick;
		String prepend = new String();

		if (sender != null) {
			Player player = sender.getPlayer();

			// Guild leader color
			if (player.hasPermission("sblock.guildleader")) {
				guildRank = sender.getAspect().getColor();
			} else {
				guildRank = ColorDef.RANK_HERO;
			}

			// Chat rank color
			if (this.isOwner(sender)) {
				channelRank = ColorDef.CHATRANK_OWNER;
			} else if (this.isChannelMod(sender)) {
				channelRank = ColorDef.CHATRANK_MOD;
			} else {
				channelRank = ColorDef.CHATRANK_MEMBER;
			}

			// Message coloring provided by additional perms
			for (ChatColor c : ChatColor.values()) {
				if (player.hasPermission("sblockchat.color")
						&& player.hasPermission("sblockchat." + c.name().toLowerCase())) {
					prepend += c;
					break;
				}
			}

			// Name color fetched from scoreboard, if team invalid perm-based instead.
			try {
				globalRank = ChatColor.valueOf(Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player).getName());
			} catch (IllegalStateException | IllegalArgumentException | NullPointerException e) {
				if (sender.getPlayer().hasPermission("group.horrorterror"))
					globalRank = ColorDef.RANK_HORRORTERROR;
				else if (sender.getPlayer().hasPermission("group.denizen"))
					globalRank = ColorDef.RANK_DENIZEN;
				else if (sender.getPlayer().hasPermission("group.felt"))
					globalRank = ColorDef.RANK_FELT;
				else if (sender.getPlayer().hasPermission("group.helper"))
					globalRank = ColorDef.RANK_HELPER;
				else if (sender.getPlayer().hasPermission("group.godtier"))
					globalRank = ColorDef.RANK_GODTIER;
				else if (sender.getPlayer().hasPermission("group.donator"))
					globalRank = ColorDef.RANK_DONATOR;
				else {
					globalRank = ColorDef.RANK_HERO;
				}
			}

			nick = this.getNick(sender);

			Region sRegion = sender.getCurrentRegion();
			if (sRegion == null) {
				region = ChatColor.GOLD;
			} else {
				region = sRegion.getRegionColor();
			}
		} else {
			guildRank = ColorDef.RANK_HERO;
			channelRank = ColorDef.CHATRANK_OWNER;
			globalRank = ColorDef.RANK_HORRORTERROR;
			region = ColorDef.WORLD_AETHER;
			nick = "<nonhuman>";
		}

		return guildRank+ "[" + channelRank + this.name + guildRank + "]" + region
				+ (isThirdPerson ? "> " : " <") + globalRank + nick
				+ (isThirdPerson ? "" : region + ">") + ChatColor.WHITE + ' ' + prepend;
	}

	public String toString() {
		return ChatColor.GOLD + this.getName() + ChatColor.GREEN + ": Access: " + ChatColor.GOLD
				+ this.getAccess() + ChatColor.GREEN + " Type: " + ChatColor.GOLD + this.getType()
				+ "\n" + ChatColor.GREEN + "Owner: " + ChatColor.GOLD
				+ Bukkit.getOfflinePlayer(this.getOwner()).getName();
	}

	// this would probably get me fired
	public boolean equals(Object o) {
		if (o instanceof String) {
			return this.name.equals(o);
		}
		return super.equals(o);
	}

	
}
