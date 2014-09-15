package fr.neatmonster.nocheatplus.compat;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

import fr.neatmonster.nocheatplus.compat.bukkit.MCAccessBukkit;
import fr.neatmonster.nocheatplus.compat.cbdev.MCAccessCBDev;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.logging.LogUtil;

/**
 * Factory class to hide potentially dirty stuff.
 * @author mc_dev
 *
 */
public class MCAccessFactory {
	
	private final String[] updateLocs = new String[]{
		"[NoCheatPlus]  Check for updates and support at BukkitDev: http://dev.bukkit.org/server-mods/nocheatplus/",
		"[NoCheatPlus]  Development builds (unsupported by the Bukkit Staff, at your own risk): http://ci.md-5.net/job/NoCheatPlus/changes",
	};
	
	/**
	 * Get a new MCAccess instance using the config value for ConfPaths.COMPATIBILITY_BUKKITONLY.
	 * @return MCAccess instance.
	 * @throws RuntimeException if no access can be set.
	 */
	public MCAccess getMCAccess() {
		return getMCAccess(ConfigManager.getConfigFile().getBoolean(ConfPaths.COMPATIBILITY_BUKKITONLY));
	}
	
	/**
	 * Get a new MCAccess instance.
	 * @param bukkitOnly Set to true to force using an API-only module.
	 * @return
	 * @throws RuntimeException if no access can be set.
	 */
	public MCAccess getMCAccess(final boolean bukkitOnly) {
		final List<Throwable> throwables = new ArrayList<Throwable>();
		
		// Try to set up native access.
		if (!bukkitOnly) {
			
			// TEMP //
			// Only add as long as no stable module has been added.
			// 1.7.10
			try{
				return new MCAccessCBDev();
			}
			catch(Throwable t) {
				throwables.add(t);
			}
			// TEMP END //
		}
		
		// Try to set up api-only access (since 1.4.6).
		try{
			final String msg;
			if (bukkitOnly) {
				msg = "[NoCheatPlus] The plugin is configured for Bukkit-API-only access.";
			}
			else{
				msg = "[NoCheatPlus] Could not set up native access for the server-mod (" + Bukkit.getServer().getVersion() + "). Please check for updates and consider to request support.";
				for (String uMsg : updateLocs) {
					LogUtil.logWarning(uMsg);
				}
			}
			LogUtil.logWarning(msg);
			final MCAccess mcAccess = new MCAccessBukkit();
			LogUtil.logWarning("[NoCheatPlus] Bukkit-API-only access: Some features will likely not function properly, performance might suffer.");
			return mcAccess;
		}
		catch(Throwable t) {
			throwables.add(t);
		};
		
		// All went wrong.
		// TODO: Fall-back solution (disable plugin, disable checks).
		LogUtil.logSevere("[NoCheatPlus] Your version of NoCheatPlus is not compatible with the version of the server-mod (" + Bukkit.getServer().getVersion() + "). Please check for updates and consider to request support.");
		for (String msg : updateLocs) {
			LogUtil.logSevere(msg);
		}
		LogUtil.logSevere("[NoCheatPlus] >>> Failed to set up MCAccess <<<");
		for (Throwable t : throwables ) {
			LogUtil.logSevere(t);
		}
		// TODO: Schedule disabling the plugin or running in circles.
		throw new RuntimeException("Could not set up native access to the server mod, neither to the Bukkit-API.");
	}
}
