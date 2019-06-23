package com.easterlyn.chat.channel;

import com.easterlyn.chat.AccessLevel;
import com.easterlyn.users.User;
import com.easterlyn.util.command.UUIDTarget;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Channel implements UUIDTarget {

	private final String name;
	private final UUID owner;
	private final Set<UUID> members;

	public Channel(@NotNull String name, @NotNull UUID owner) {
		this.name = name;
		this.owner = owner;
		this.members = Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	/**
	 * Gets the name of the channel.
	 *
	 * @return the name
	 */
	@NotNull
	public final String getName() {
		return name;
	}

	/**
	 * Gets the display name of the channel.
	 *
	 * @return the channel's display name
	 */
	public final String getDisplayName() {
		return "#" + name;
	}

	/**
	 * Gets the AccessLevel of the channel.
	 *
	 * @return the AccessLevel
	 */
	@NotNull
	public AccessLevel getAccess() {
		return AccessLevel.PUBLIC;
	}

	/**
	 * Sets the AccessLevel of the channel.
	 *
	 * @param access the AccessLevel
	 */
	public void setAccess(@NotNull AccessLevel access) {}

	/**
	 * Gets the password for the channel.
	 *
	 * @return the channel's password or null if no password is set
	 */
	@Nullable
	public String getPassword() {
		return null;
	}

	/**
	 * Sets the password for the channel.
	 *
	 * @param password the new password
	 */
	public void setPassword(@Nullable String password) {}

	/**
	 * Gets the channel owner's UUID.
	 *
	 * @return the UUID
	 */
	@NotNull
	public final UUID getOwner() {
		return this.owner;
	}

	/**
	 * Gets whether or not a user is a channel's owner.
	 *
	 * @param user a user
	 * @return if this user is an owner
	 */
	public boolean isOwner(@NotNull User user) {
		return user.getUniqueId().equals(getOwner()) || user.hasPermission("easterlyn.chat.channel.owner");
	}

	/**
	 * Gets whether or not a user is a channel moderator.
	 *
	 * @param user a user
	 * @return whether this user has permission to moderate the channel
	 */
	public boolean isModerator(@NotNull User user) {
		return isOwner(user) || user.hasPermission("easterlyn.chat.channel.moderator");
	}

	/**
	 * Sets whether or not a user is a moderator.
	 *
	 * @param user the user
	 * @param moderator whether or not the user is a moderator
	 */
	public void setModerator(@NotNull User user, boolean moderator) {}

	/**
	 * Check if the user allowed to enter the channel.
	 *
	 * @param user a user
	 * @return whether or not the user is allowed to join
	 */
	public boolean isWhitelisted(@NotNull User user) {
		return !isBanned(user) && (getAccess() == AccessLevel.PUBLIC || isModerator(user));
	}

	/**
	 * Sets whether or not a user is allowed to enter the channel.
	 *
	 * @param user the user
	 * @param whitelisted whether or not the user is allowed to join the channel
	 */
	public void setWhitelisted(@NotNull User user, boolean whitelisted) {}

	/**
	 * Check if the given user is banned.
	 *
	 * @param user the user
	 * @return true if the user is banned
	 */
	public boolean isBanned(@NotNull User user) {
		return false;
	}

	/**
	 * Sets whether or not a user is banned.
	 *
	 * @param user the user
	 * @param banned whether or not the user is a moderator
	 */
	public void setBanned(@NotNull User user, boolean banned) {}

	/**
	 * Gets a set of all listening users' UUIDs.
	 *
	 * @return all relevant UUIDs
	 */
	@NotNull
	public final Set<UUID> getMembers() {
		return this.members;
	}

	/**
	 * Check if the channel has been recently accessed and should not be deleted.
	 *
	 * @return true if the channel should not be deleted
	 */
	public boolean isRecentlyAccessed() {
		return true;
	}

	/**
	 * Update the last access time.
	 */
	public void updateLastAccess() {}

	/**
	 * Loads additional data out of a ConfigurationSection specific to this channel.
	 *
	 * @param data the ConfigurationSection for the channel
	 */
	public void load(@NotNull ConfigurationSection data) {}

	/**
	 * Saves the Channel's data.
	 *
	 * @param channelStorage the Configuration storing channels
	 */
	public void save(@NotNull Configuration channelStorage) {
		channelStorage.set(getName() + ".class", getClass().getName());
		channelStorage.set(getName() + ".owner", getOwner().toString());
	}

}
