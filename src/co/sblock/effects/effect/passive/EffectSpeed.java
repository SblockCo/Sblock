package co.sblock.effects.effect.passive;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import co.sblock.effects.effect.Effect;
import co.sblock.effects.effect.EffectBehaviorCooldown;
import co.sblock.effects.effect.EffectBehaviorPassive;

/**
 * Effect for passively granting the speed PotionEffect.
 * 
 * @author Jikoo
 */
public class EffectSpeed extends Effect implements EffectBehaviorPassive, EffectBehaviorCooldown {

	public EffectSpeed() {
		super(500, 2, 10, "Speed");
	}

	@Override
	public String getCooldownName() {
		return "Effect:Speed";
	}

	@Override
	public long getCooldownDuration() {
		return 5000;
	}

	@Override
	public void applyEffect(Player player, int level) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, level), false);
	}

}