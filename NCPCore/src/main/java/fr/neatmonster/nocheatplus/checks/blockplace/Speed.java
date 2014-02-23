package fr.neatmonster.nocheatplus.checks.blockplace;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;

/**
 * This check verifies if the player isn't throwing items too quickly, like eggs or arrows.
 */
public class Speed extends Check {

    /**
     * Instantiates a new speed check.
     */
    public Speed() {
        super(CheckType.BLOCKPLACE_SPEED);
    }

    /**
     * Checks a player.
     * 
     * @param player
     *            the player
     * @return true, if successful
     */
    public boolean check(final Player player) {
        final BlockPlaceConfig cc = BlockPlaceConfig.getConfig(player);
        final BlockPlaceData data = BlockPlaceData.getData(player);

        boolean cancel = false;

        // Has the player thrown items too quickly?
        if (data.speedLastTime != 0 && System.currentTimeMillis() - data.speedLastTime < cc.speedInterval) {
            if (data.speedLastRefused) {
                final double difference = cc.speedInterval - System.currentTimeMillis() + data.speedLastTime;

                // They failed, increase this violation level.
                data.speedVL += difference;

                // Execute whatever actions are associated with this check and the violation level and find out if we
                // should cancel the event.
                cancel = executeActions(player, data.speedVL, difference, cc.speedActions);
            }

            data.speedLastRefused = true;
        } else {
            // Reward them by lowering their violation level.
            data.speedVL *= 0.9D;

            data.speedLastRefused = false;
        }

        data.speedLastTime = System.currentTimeMillis();

        return cancel;
    }
}
