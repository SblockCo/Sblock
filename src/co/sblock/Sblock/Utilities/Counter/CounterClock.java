package co.sblock.Sblock.Utilities.Counter;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CounterClock extends BukkitRunnable {
	
	public CounterClock() {
		
	}

	@Override
	public void run() {
		Set<Player> deletionSet = new HashSet<Player>();
		for(Counter c : CounterModule.counterMap.values())	{
			if(c.getCurrent() > 0){
				c.tick();
			}
			else if(c.getCurrent() == 0 && c.getCooldown() > 0)	{
				c.tickCooldown();
			}
			else if(c.getCurrent() == 0 && c.getCooldown() == 0)	{
				c.stopCounter();
				deletionSet.add(c.getPlayer());
			}
		}
		for(Player p : deletionSet)	{
			CounterModule.deleteCounter(p);
		}

	}

}
