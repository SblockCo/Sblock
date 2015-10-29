package co.sblock.micromodules;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.EntityFallingBlock;

/**
 * Wrapper for EntityFallingBlock to allow easier detection of Meteorite components.
 * 
 * @author Jikoo
 */
public class MeteoriteComponent extends EntityFallingBlock {

	private final boolean explode;
	private final boolean bore;
	@SuppressWarnings("deprecation")
	public MeteoriteComponent(Location l, Material material, boolean explode, boolean bore) {
		super(((CraftWorld) l.getWorld()).getHandle(), l.getBlockX(), l.getBlockY(), l.getBlockZ(), Block.getById(material.getId()).fromLegacyData(0));
		this.explode = explode;
		this.bore = bore;
		this.ticksLived = 1;
		this.dropItem = false;
		((CraftWorld) l.getWorld()).getHandle().addEntity(this, SpawnReason.CUSTOM);
	}

	public boolean shouldExplode() {
		return explode;
	}

	@Override
	public boolean aW() {
		/*
		 * Cancelling damage no longer prevents physics applying to EntityTNTPrimed and
		 * EntityFallingBlock. As a workaround, we set the MeteoriteComponent invincible.
		 */
		return bore;
	}
}
