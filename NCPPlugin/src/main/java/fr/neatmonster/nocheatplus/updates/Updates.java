package fr.neatmonster.nocheatplus.updates;

import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.DefaultConfig;

public class Updates {
	
	/**
	 * 
	 * @param config
	 * @return null if everything is fine, a string with a message stating problems otherwise.
	 */
	public static String isConfigUpToDate(ConfigFile config){
        Object created = config.get(ConfPaths.CONFIGVERSION_CREATED);
        if (created != null && created instanceof Integer){
        	int buildCreated = ((Integer) created).intValue();
        	if (buildCreated < DefaultConfig.buildNumber){
        		// Potentially outdated Configuration.
        		return "Your configuration might be outdated.\n" + "Some settings could have changed, you should regenerate it!";
        	}
        	else if (buildCreated > DefaultConfig.buildNumber){
        		// Installed an older version of NCP.
        		return "Your configuration seems to be created by a newer plugin version.\n" + "Some settings could have changed, you should regenerate it!";
        	}
        	else{
        		return null;
        	}
        }
        // Error or not: could not determine versions, thus ignore.
        return null;
	}
}
