/**
 * 
 */
package co.sblock.Sblock.Machines.Type;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * @author Jikoo
 *
 */
public class PGOMachine extends Machine {

	public PGOMachine(Location l, String data) {
		super(l, data);
	}

	public List<Location> getLocations() {
		return new ArrayList<Location>();
	}

	public MachineType getType() {
		return MachineType.PERFECTLY_GENERIC_OBJECT;
	}

	@SuppressWarnings("deprecation")
	public void assemble(BlockPlaceEvent event) {
		Material placedOn = event.getBlockAgainst().getType();
		if (isValid(placedOn)) {
			event.getBlockPlaced().setTypeIdAndData(placedOn.getId(), event.getBlockAgainst().getData(), false);
		} else {
			event.getBlockPlaced().setType(Material.DIRT);
		}
		// Future features: Make wall signs etc. valid and copy text
	}

	/* (non-Javadoc)
	 * @see co.sblock.Sblock.Machines.Type.Machine#meetsAdditionalBreakConditions(org.bukkit.event.block.BlockPlaceEvent)
	 */
	public boolean meetsAdditionalBreakConditions(BlockBreakEvent event) {
		return true;
	}

	private boolean isValid(Material type) {
		switch (type) {
		case BIRCH_WOOD_STAIRS:
		case BOOKSHELF:
		case BRICK:
		case BRICK_STAIRS:
		case CACTUS:
		case CAKE_BLOCK:
		case CAULDRON:
		case COAL_BLOCK:
		case COAL_ORE:
		case COBBLESTONE:
		case COBBLESTONE_STAIRS:
		case COBBLE_WALL:
		case DIAMOND_BLOCK:
		case DIAMOND_ORE:
		case DIRT:
		case DOUBLE_STEP:
		case EMERALD_BLOCK:
		case EMERALD_ORE:
		case ENCHANTMENT_TABLE:
		case ENDER_STONE:
		case FENCE:
		case FENCE_GATE:
		case GLASS:
		case GLOWING_REDSTONE_ORE:
		case GLOWSTONE:
		case GOLD_BLOCK:
		case GOLD_ORE:
		case GRASS:
		case GRAVEL:
		case HARD_CLAY:
		case HAY_BLOCK:
		case HUGE_MUSHROOM_1:
		case HUGE_MUSHROOM_2:
		case ICE:
		case IRON_BLOCK:
		case IRON_FENCE:
		case IRON_ORE:
		case JACK_O_LANTERN:
		case JUKEBOX:
		case JUNGLE_WOOD_STAIRS:
		case LAPIS_BLOCK:
		case LAPIS_ORE:
		case LEAVES:
		case LOG:
		case MELON_BLOCK:
		case MOSSY_COBBLESTONE:
		case MYCEL:
		case NETHERRACK:
		case NETHER_BRICK:
		case NETHER_BRICK_STAIRS:
		case NETHER_FENCE:
		case NOTE_BLOCK:
		case OBSIDIAN:
		case PUMPKIN:
		case QUARTZ_BLOCK:
		case QUARTZ_ORE:
		case QUARTZ_STAIRS:
		case REDSTONE_BLOCK:
		case REDSTONE_ORE:
		case SAND:
		case SANDSTONE:
		case SANDSTONE_STAIRS:
		case SMOOTH_BRICK:
		case SMOOTH_STAIRS:
		case SNOW_BLOCK:
		case SOUL_SAND:
		case SPONGE:
		case SPRUCE_WOOD_STAIRS:
		case STAINED_CLAY:
		case STEP:
		case STONE:
		case THIN_GLASS:
		case TNT:
		case TRIPWIRE:
		case WEB:
		case WOOD:
		case WOOD_DOUBLE_STEP:
		case WOOD_STAIRS:
		case WOOD_STEP:
		case WOOL:
		case WORKBENCH:
			return true;
		default:
			return false;
		}
	}
}
