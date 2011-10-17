package cc.co.evenprime.bukkit.nocheat.checks.chat;

import org.bukkit.entity.Player;

import cc.co.evenprime.bukkit.nocheat.NoCheat;
import cc.co.evenprime.bukkit.nocheat.config.Permissions;
import cc.co.evenprime.bukkit.nocheat.config.cache.ConfigurationCache;
import cc.co.evenprime.bukkit.nocheat.data.BaseData;

/**
 * 
 * @author Evenprime
 * 
 */
public class ChatCheck {

    private final NoCheat plugin;

    public ChatCheck(NoCheat plugin) {

        this.plugin = plugin;
    }

    public boolean check(Player player, String message, ConfigurationCache cc) {

        boolean cancel = false;

        boolean spamCheck = cc.chat.spamCheck && !player.hasPermission(Permissions.CHAT_SPAM);

        if(spamCheck) {

            int time = plugin.getIngameSeconds();

            BaseData data = plugin.getPlayerData(player);

            if(data.chat.spamLasttime + cc.chat.spamTimeframe <= time) {
                data.chat.spamLasttime = time;
                data.chat.messageCount = 0;
            }

            data.chat.messageCount++;

            if(data.chat.messageCount > cc.chat.spamLimit) {

                // Prepare some event-specific values for logging and custom
                // actions

                data.log.check = "chat.spam";
                data.log.text = message;

                cancel = plugin.getActionManager().executeActions(player, cc.chat.spamActions, data.chat.messageCount - cc.chat.spamLimit, data.chat.history, cc);
            }
        }

        return cancel;
    }

}