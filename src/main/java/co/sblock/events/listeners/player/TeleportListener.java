package co.sblock.events.listeners.player;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

import co.sblock.Sblock;
import co.sblock.chat.Language;
import co.sblock.events.listeners.SblockListener;
import co.sblock.users.Region;
import co.sblock.users.User;
import co.sblock.users.Users;

/**
 * Listener for PlayerTeleportEvents.
 * 
 * @author Jikoo
 */
public class TeleportListener extends SblockListener {

	private final Users users;

	public TeleportListener(Sblock plugin) {
		super(plugin);
		this.users = plugin.getModule(Users.class);
	}

	/**
	 * The event handler for PlayerTeleportEvents.
	 * <p>
	 * This method is for events that are guaranteed to be completed.
	 * 
	 * @param event the PlayerTeleportEvent
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (event.getCause() != TeleportCause.SPECTATE || event.getPlayer().hasPermission("sblock.felt")) {
			return;
		}
		for (Player player : event.getTo().getWorld().getPlayers()) {
			if (!player.getLocation().equals(event.getTo())) {
				continue;
			}
			if (player.getGameMode() == GameMode.SPECTATOR) {
				if (player.getSpectatorTarget() != null
						&& player.getSpectatorTarget() instanceof Player) {
					player = (Player) player.getSpectatorTarget();
				} else {
					continue;
				}
			}
			if (users.getUser(player.getUniqueId()).getSpectatable()) {
				return;
			}
			event.setCancelled(true);
			event.getPlayer().sendMessage(Language.getColor("player.bad") + player.getDisplayName() + Language.getColor("bad")
					+ " has disallowed spectating! You'll need to send a tpa.");
			return;
		}
	}

	/**
	 * The event handler for PlayerTeleportEvents.
	 * <p>
	 * This method is for events that are guaranteed to be completed.
	 * 
	 * @param event the PlayerTeleportEvent
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleportHasOccurred(final PlayerTeleportEvent event) {
		// People keep doing stupid stuff like /home while falling from spawn
		event.getPlayer().setFallDistance(0);

		if (event.getTo().getWorld().equals(event.getFrom().getWorld())) {
			return;
		}

		final UUID uuid = event.getPlayer().getUniqueId();

		new BukkitRunnable() {
			@Override
			public void run() {
				Player player = Bukkit.getPlayer(uuid);
				if (player == null) {
					// Player has logged out.
					return;
				}
				User user = users.getUser(uuid);
				// Update region
				Region target;
				if (player.getWorld().getName().equals("Derspit")) {
					target = user.getDreamPlanet();
				} else {
					target = Region.getRegion(event.getTo().getWorld().getName());
				}
				user.updateCurrentRegion(target, false);
				user.updateFlight();
			}
		}.runTask(getPlugin());
	}
}
