package fr.neatmonster.nocheatplus.checks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ActionList;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.components.MCAccessHolder;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import fr.neatmonster.nocheatplus.hooks.NCPHookManager;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.ExecutionHistory;
import fr.neatmonster.nocheatplus.utilities.TickTask;

/**
 * The parent class of all checks. Don't let this implement Listener without knowing that this might be registered as component with NCP before the check-listeners.
 */
public abstract class Check implements MCAccessHolder{

    // TODO: Do these get cleaned up ?
    /** The execution histories of each check. */
    protected static Map<String, ExecutionHistory> histories = new HashMap<String, ExecutionHistory>();

    /**
     * Gets the player's history.
     *
     * @param player
     *            the player
     * @return the history
     */
    protected static ExecutionHistory getHistory(final Player player) {
        if (!histories.containsKey(player.getName())) {
            histories.put(player.getName(), new ExecutionHistory());
        }
        return histories.get(player.getName());
    }

    /** The type. */
    protected final CheckType type;

    protected MCAccess mcAccess;

    /**
     * Instantiates a new check.
     *
     * @param type
     *            the type
     */
    public Check(final CheckType type) {
        this.type = type;
        mcAccess = NCPAPIProvider.getNoCheatPlusAPI().getMCAccess();
        ViolationHistory.checkTypeMap.put(getClass().getName(), type);
        DataManager.registerExecutionHistory(type, histories);
    }

    /**
     * Execute actions, possibly thread safe according to the isMainThread flag.<br>
     * This does not use extra synchronization.
     *
     * @param player
     *            the player
     * @param VL
     *            the vL
     * @param actions
     *            the actions
     * @param isMainThread
     *            if the thread the main thread
     * @return true, if successful
     */
    public boolean executeActions(final Player player, final double vL, final double addedVL,
                                  final ActionList actions, boolean isMainThread) {
        // Sync it into the main thread by using an event.
        return executeActions(new ViolationData(this, player, vL, addedVL, actions), isMainThread);
    }

    /**
     * Convenience method: Execute actions from the main thread only.
     *
     * @param player
     *            the player
     * @param vL
     *            the violation level
     * @param addedVL
     *            the violation level added
     * @param actions
     *            the actions
     * @return true, if the event should be cancelled
     */
    protected boolean executeActions(final Player player, final double vL, final double addedVL,
                                     final ActionList actions) {
        return executeActions(new ViolationData(this, player, vL, addedVL, actions), true);
    }

    /**
     * Execute some actions for the specified player, only for the main thread.
     *
     * @param violationData
     *            the violation data
     * @return true, if the event should be cancelled
     */
    protected boolean executeActions(final ViolationData violationData) {
        return executeActions(violationData, true);
    }

    /**
     * Execute some actions for the specified player.
     *
     * @param violationData
     *            the violation data
     * @param isMainThread If this is called from within the main thread. If true, this must really be the main thread and not from synchronized code coming form another thread.
     * @return true, if the event should be cancelled
     */
    protected boolean executeActions(final ViolationData violationData, final boolean isMainThread) {

        // Dispatch the VL processing to the hook manager (now thread safe).
        if (NCPHookManager.shouldCancelVLProcessing(violationData)) {
            // One of the hooks has decided to cancel the VL processing, return false.
            return false;
        }

        final boolean hasCancel = violationData.hasCancel();

        if (isMainThread) {
            return violationData.executeActions();
        }
        else {
            // Always schedule to add to ViolationHistory.
            TickTask.requestActionsExecution(violationData);
        }

        // (Design change: Permission checks are moved to cached permissions, lazily updated.)
        return hasCancel;
    }

    /**
     * Fill in parameters for creating violation data. 
     * Individual checks should override this to fill in check specific parameters,
     * which then are fetched by the violation data instance.
     * @param player
     * @return
     */
    protected Map<ParameterName, String> getParameterMap(final ViolationData violationData) {
        final Map<ParameterName, String> params = new HashMap<ParameterName, String>();
        // (Standard parameters like player, vl, check name are filled in in ViolationData.getParameter!)
        return params;
    }

    /**
     * Gets the type of the check.
     *
     * @return the type
     */
    public CheckType getType() {
        return type;
    }

    /**
     * Checks if this check is enabled for the specified player.
     *
     * @param player
     *            the player
     * @return true, if the check is enabled
     */
    public boolean isEnabled(final Player player) {
        if (!type.isEnabled(player)) {
            return false;
        }
        // TODO: Checking for the thread might be a temporary measure.
        if (Bukkit.isPrimaryThread()) {
            // Check permissions directly.
            if (player.hasPermission(type.getPermission())) {
                return false;
            }
        }
        else if (type.hasCachedPermission(player)) {
            // Assume asynchronously running check.
            return false;
        }
        return !NCPExemptionManager.isExempted(player, type);
    }

    @Override
    public void setMCAccess(MCAccess mcAccess) {
        this.mcAccess = mcAccess;
    }

    @Override
    public MCAccess getMCAccess() {
        return mcAccess;
    }

}