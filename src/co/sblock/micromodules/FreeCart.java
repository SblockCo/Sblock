package co.sblock.micromodules;

import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import co.sblock.module.Module;
import co.sblock.utilities.Cooldowns;

/**
 * A Module for tracking minecarts which despawn on collisions or when the rider exits.
 * 
 * @author Jikoo
 */
public class FreeCart extends Module {

	private static FreeCart instance;

	private final HashSet<Minecart> carts = new HashSet<>();

	@Override
	protected void onEnable() {
		instance = this;
	}

	@Override
	protected void onDisable() {
		instance = null;
	}

	public void spawnCart(Player p, Location location, Vector startspeed) {
		Cooldowns cooldowns = Cooldowns.getInstance();
		if (cooldowns.getRemainder(p, "freecart") > 0) {
			return;
		}
		cooldowns.addCooldown(p, "freecart", 2000);
		Minecart m = (Minecart) location.getWorld().spawnEntity(location, EntityType.MINECART);
		m.setPassenger(p);
		m.setVelocity(startspeed);
		carts.add(m);
	}

	public boolean isFreeCart(Minecart cart) {
		return carts.contains(cart);
	}

	public boolean isOnFreeCart(Player p) {
		if (p.getVehicle() == null) {
			return false;
		}
		if (p.getVehicle().getType() != EntityType.MINECART) {
			return false;
		}
		return isFreeCart((Minecart) p.getVehicle());
	}

	public void remove(Player p) {
		if (p.getVehicle() == null) {
			return;
		}
		if (p.getVehicle().getType() != EntityType.MINECART) {
			return;
		}
		remove((Minecart) p.getVehicle());
	}

	public void remove(Minecart minecart) {
		if (!carts.remove(minecart)) {
			return;
		}
		minecart.eject();
		minecart.remove();
	}

	public void cleanUp() {
		for (Minecart cart : this.carts) {
			cart.eject();
			cart.remove();
		}
	}

	public static FreeCart getInstance() {
		return instance;
	}

	@Override
	protected String getModuleName() {
		return "Sblock FreeCart";
	}
}