package co.sblock.users;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import co.sblock.Sblock;
import co.sblock.chat.Color;
import co.sblock.module.Module;
import co.sblock.utilities.CollectionConversions;

/**
 * Class that keeps track of players currently logged on to the game.
 * 
 * @author FireNG, Jikoo
 */
public class Users extends Module {

	/* The Cache of Player UUID and relevant Users. */
	private final LoadingCache<UUID, User> userCache;

	public Users(Sblock plugin) {
		super(plugin);
		this.userCache = CacheBuilder.newBuilder()
				.expireAfterAccess(30L, TimeUnit.MINUTES)
				.removalListener(new RemovalListener<UUID, User>() {
					@Override
					public void onRemoval(RemovalNotification<UUID, User> notification) {
						User user = notification.getValue();
						user.save();
						unteam(user.getPlayer());
					}
				}).build(new CacheLoader<UUID, User>() {
					@Override
					public User load(UUID uuid) throws Exception {
						if (uuid == null) {
							throw new NullPointerException();
						}
						User user = User.load(getPlugin(), uuid);
						team(user.getPlayer());
						return user;
					}
				});
	}

	@Override
	protected void onEnable() {
		for (Player p : Bukkit.getOnlinePlayers()) {
			getUser(p.getUniqueId());
			team(p);
		}
	}

	@Override
	protected void onDisable() {
		for (User u : userCache.asMap().values()) {
			unteam(u.getUUID().toString().replace("-", "").substring(0, 16));
		}
		// Invalidating and cleaning up causes our removal listener to save all cached users.
		userCache.invalidateAll();
		userCache.cleanUp();
	}

	@Override
	public String getName() {
		return "Sblock UserManager";
	}

	/**
	 * Fetch a User. A User is always returned, even if the Player by the given UUID is not online.
	 * 
	 * @param uuid
	 * @return the User
	 */
	public User getUser(UUID uuid) {
		try {
			return userCache.get(uuid);
		} catch (ExecutionException e) {
			// This shouldn't be possible, fail hard and fast.
			throw new RuntimeException(e);
		}
	}

	public Set<User> getOnlineUsers() {
		return CollectionConversions.toSet(Bukkit.getOnlinePlayers(), player -> getUser(player.getUniqueId()));
	}

	/**
	 * Add a Player to their group's Team.
	 * 
	 * @param player the Player
	 */
	public static void team(Player player) {
		if (player == null) {
			return;
		}
		StringBuilder prefixBuilder = new StringBuilder();
		for (net.md_5.bungee.api.ChatColor color : Color.COLORS) {
			if (player.hasPermission("sblockchat." + color.name().toLowerCase())) {
				prefixBuilder.append(color);
				break;
			}
		}
		if (prefixBuilder.length() > 0) {
			// Do nothing, we've got a fancy override going on
		} else if (player.hasPermission("sblock.horrorterror")) {
			prefixBuilder.append(Color.RANK_HORRORTERROR.toString());
		} else if (player.hasPermission("sblock.denizen")) {
			prefixBuilder.append(Color.RANK_DENIZEN.toString());
		} else if (player.hasPermission("sblock.felt")) {
			prefixBuilder.append(Color.RANK_FELT.toString());
		} else if (player.hasPermission("sblock.helper")) {
			prefixBuilder.append(Color.RANK_HELPER.toString());
		} else if (player.hasPermission("sblock.donator")) {
			prefixBuilder.append(Color.RANK_DONATOR.toString());
		} else if (player.hasPermission("sblock.godtier")) {
			prefixBuilder.append(Color.RANK_GODTIER.toString());
		} else {
			prefixBuilder.append(Color.RANK_HERO.toString());
		}
		for (net.md_5.bungee.api.ChatColor color : Color.FORMATS) {
			if (player.hasPermission("sblockchat." + color.name().toLowerCase())) {
				prefixBuilder.append(color);
			}
		}
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		String teamName = player.getName();
		Team team = board.getTeam(teamName);
		if (team == null) {
			team = board.registerNewTeam(teamName);
		}
		team.setPrefix(prefixBuilder.toString());
		team.addEntry(player.getName());

		Objective objective = board.getObjective("deaths");
		if (objective == null) {
			objective = board.registerNewObjective("deaths", "deathCount");
			objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		}

		// Since Mojang doesn't, we'll force deathcount to persist - it's been a feature for ages
		Score score = objective.getScore(player.getName());
		score.setScore(player.getStatistic(Statistic.DEATHS));
	}

	public static void unteam(Player player) {
		unteam(player.getName());
	}

	private static void unteam(String teamName) {
		Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
		if (team != null) {
			team.unregister();
		}
	}

	public static Location getSpawnLocation() {
		return new Location(Bukkit.getWorld("Earth"), -3.5, 20, 6.5, 179.99F, 1F);
	}
}
