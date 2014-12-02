package fr.neatmonster.nocheatplus.compat;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;


/**
 * Various bridge methods not enough for an own class.
 * @author mc_dev
 *
 */
public class BridgeMisc {

	/**
	 * Return a shooter of a projectile if we get an entity, null otherwise.
	 */
	public static Player getShooterPlayer(Projectile projectile) {
		Object source;
		try {
			source = projectile.getClass().getMethod("getShooter").invoke(projectile);
		} catch (IllegalArgumentException e) {
			return null;
		} catch (SecurityException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		} catch (InvocationTargetException e) {
			return null;
		} catch (NoSuchMethodException e) {
			return null;
		}
		if (source instanceof Player) {
			return (Player) source;
		} else {
			return null;
		}
	}

	/**
	 * Retrieve a player from projectiles or cast to player, if possible.
	 * @param damager
	 * @return
	 */
	public static Player getAttackingPlayer(Entity damager) {
		if (damager instanceof Player) {
			return (Player) damager;
		} else if (damager instanceof Projectile) {
			return getShooterPlayer((Projectile) damager);
		} else {
			return null;
		}
	}

	/**
	 * Get online players as an array (convenience for reducing IDE markers :p).
	 * @return
	 */
	public static Player[] getOnlinePlayers() {
		Object obj = Bukkit.getOnlinePlayers();
		if (obj instanceof Collection<?>) {
			@SuppressWarnings("unchecked")
			Collection<? extends Player> players = (Collection<? extends Player>) obj;
			return players.toArray(new Player[players.size()]);
		}
		else if (obj instanceof Player[]) {
			return (Player[]) obj;
		} else {
			return new Player[0];
		}
	}

}