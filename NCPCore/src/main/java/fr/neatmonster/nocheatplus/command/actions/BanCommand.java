package fr.neatmonster.nocheatplus.command.actions;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.neatmonster.nocheatplus.command.AbstractCommand;
import fr.neatmonster.nocheatplus.command.BaseCommand;
import fr.neatmonster.nocheatplus.logging.LogUtil;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.utilities.IdUtil;

public class BanCommand extends BaseCommand {

	public BanCommand(JavaPlugin plugin) {
		super(plugin, "ban", Permissions.COMMAND_BAN);
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
		
		// TODO: Consider supporting vanilla syntax or removing this command :p.
		
		// Args contains "ban" as first arg.
		if (args.length < 2) {
			return false;
		}
		final String name = args[1].trim();
		final String reason;
		if (args.length > 2) {
			reason = AbstractCommand.join(args, 2);
		}
		else {
			reason = "";
		}
		ban(sender, name, reason);
		return true;
	}
	
	/**
	 * 
	 * @param sender
	 * @param name Trimmed name.
	 * @param reason
	 */
	void ban(CommandSender sender, String name, String reason) {
		final Server server = Bukkit.getServer();
		Player player = DataManager.getPlayer(name);
		// Pro logic below. 
		if (player == null && !IdUtil.isValidMinecraftUserName(name)) {
			UUID id = IdUtil.UUIDFromStringSafe(name);
			if (id != null) {
				LogUtil.logWarning("Banning by UUID might not work (" + id.toString()+"), relay to the vanilla command.");
			} else {
				LogUtil.logWarning("Might not be a valid user name: " + name);
			}
		}
		if (player != null){
			player.kickPlayer(reason);
		}
		// Relay to the server command for compatibility reasons.
		server.dispatchCommand(server.getConsoleSender(), "ban " + name);
		logBan(sender, player, name, reason);
		
	}
	
	private void logBan(CommandSender sender, Player player, String name, String reason) {
		LogUtil.logInfo("[NoCheatPlus] (" + sender.getName() + ") Banned " + name + (player != null ? ("/" + player.getName()) : "") + " : " + reason);
	}

	/* (non-Javadoc)
	 * @see fr.neatmonster.nocheatplus.command.AbstractCommand#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
	 */
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		// TODO: Consider adding player names and other.
		return null;
	}

}
