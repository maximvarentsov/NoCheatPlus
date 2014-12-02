package fr.neatmonster.nocheatplus.checks.fight;

import java.util.Iterator;

import fr.neatmonster.nocheatplus.logging.Streams;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckListener;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.combined.Combined;
import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.checks.inventory.Items;
import fr.neatmonster.nocheatplus.checks.moving.LocationTrace;
import fr.neatmonster.nocheatplus.checks.moving.LocationTrace.TraceEntry;
import fr.neatmonster.nocheatplus.checks.moving.MediumLiftOff;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.MovingListener;
import fr.neatmonster.nocheatplus.compat.BridgeHealth;
import fr.neatmonster.nocheatplus.components.JoinLeaveListener;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.stats.Counters;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.build.BuildParameters;

/**
 * Central location to listen to events that are relevant for the fight checks.<br>
 * This listener is registered after the CombinedListener.
 * 
 * @see FightEvent
 */
public class FightListener extends CheckListener implements JoinLeaveListener{

    /** The angle check. */
    private final Angle       angle       = addCheck(new Angle());

    /** The critical check. */
    private final Critical    critical    = addCheck(new Critical());

    /** The direction check. */
    private final Direction   direction   = addCheck(new Direction());
    
    /** Faster health regeneration check. */
    private final FastHeal fastHeal		  = addCheck(new FastHeal());

    /** The god mode check. */
    private final GodMode     godMode     = addCheck(new GodMode());

    /** The knockback check. */
    private final Knockback   knockback   = addCheck(new Knockback());

    /** The no swing check. */
    private final NoSwing     noSwing     = addCheck(new NoSwing());

    /** The reach check. */
    private final Reach       reach       = addCheck(new Reach());
    
    /** The self hit check */
    private final SelfHit     selfHit     = addCheck(new SelfHit());

    /** The speed check. */
    private final Speed       speed       = addCheck(new Speed());
    
    /** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
	private final Location useLoc1 = new Location(null, 0, 0, 0);
	
	/** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
	private final Location useLoc2 = new Location(null, 0, 0, 0);
	
	private final Counters counters = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(Counters.class);
    private final int idCancelDead = counters.registerKey("canceldead");
    
    public FightListener(){
    	super(CheckType.FIGHT);
    }

    /**
     * A player attacked something with DamageCause ENTITY_ATTACK. That's most likely what we want to really check.
     * 
     * @param event
     *            The EntityDamageByEntityEvent
     * @return 
     */
    private boolean handleNormalDamage(final Player player, final Entity damaged, final double damage, final int tick, final FightData data) {
        final FightConfig cc = FightConfig.getConfig(player);
        
        // Hotfix attempt for enchanted books.
        // TODO: maybe a generaluzed version for the future...
        final ItemStack stack = player.getItemInHand();
        // Illegal enchantments hotfix check.
        if (Items.checkIllegalEnchantments(player, stack)) return true;
        
        boolean cancelled = false;
        
        final String worldName = player.getWorld().getName();
        final long now = System.currentTimeMillis();
        final boolean worldChanged = !worldName.equals(data.lastWorld);
        
        final Location loc =  player.getLocation(useLoc1);
//        // Bad pitch/yaw, just in case.
// 		if (LocUtil.needsDirectionCorrection(useLoc1.getYaw(), useLoc1.getPitch())) {
// 			mcAccess.correctDirection(player);
// 			player.getLocation(useLoc1);
// 		}
        final Location damagedLoc = damaged.getLocation(useLoc2);
//        final double targetDist = CheckUtils.distance(loc, targetLoc); // TODO: Calculate distance as is done in fight.reach !
        final double targetMove;
        final int tickAge;
        final long msAge; // Milliseconds the ticks actually took.
        final double normalizedMove; // Blocks per second.
        // TODO: relative distance (player - target)!
        // TODO: Use trace for this ?
        if (data.lastAttackedX == Double.MAX_VALUE || tick < data.lastAttackTick || worldChanged || tick - data.lastAttackTick > 20){
        	// TODO: 20 ?
        	tickAge = 0;
        	targetMove = 0.0;
        	normalizedMove = 0.0;
        	msAge = 0;
        }
        else{
        	tickAge = tick - data.lastAttackTick;
        	// TODO: Maybe use 3d distance if dy(normalized) is too big. 
        	targetMove = TrigUtil.distance(data.lastAttackedX, data.lastAttackedZ, damagedLoc.getX(), damagedLoc.getZ());
        	msAge = (long) (50f * TickTask.getLag(50L * tickAge, true) * (float) tickAge);
        	normalizedMove = msAge == 0 ? targetMove : targetMove * Math.min(20.0, 1000.0 / (double) msAge);
        }
        // TODO: calculate factor for dists: ticks * 50 * lag
        
        // TODO: dist < width => skip some checks (direction, ..)
    	
        final LocationTrace damagedTrace;
        final Player damagedPlayer;
        if (damaged instanceof Player){
        	damagedPlayer = (Player) damaged;
//        	// Bad pitch/yaw, just in case.
//     		if (LocUtil.needsDirectionCorrection(useLoc2.getYaw(), useLoc2.getPitch())) {
//     			mcAccess.correctDirection(damagedPlayer);
//     			damagedPlayer.getLocation(useLoc2);
//     		}
     		// Log.
        	if (cc.debug && damagedPlayer.hasPermission(Permissions.ADMINISTRATION_DEBUG)){
        		damagedPlayer.sendMessage("Attacked by " + player.getName() + ": inv=" + mcAccess.getInvulnerableTicks(damagedPlayer) + " ndt=" + damagedPlayer.getNoDamageTicks());
        	}
        	// Check for self hit exploits (mind that projectiles are excluded from this.)
        	if (selfHit.isEnabled(player) && selfHit.check(player, damagedPlayer, data, cc)) {
        		cancelled = true;
        	}
        	// Get+update the damaged players.
        	// TODO: Problem with NPCs: data stays (not a big problem).
        	// (This is done even if the event has already been cancelled, to keep track, if the player is on a horse.)
        	damagedTrace = MovingData.getData(damagedPlayer).updateTrace(damagedPlayer, damagedLoc, tick);
        } else {
        	damagedPlayer = null; // TODO: This is a temporary workaround.
        	// Use a fake trace.
        	// TODO: Provide for entities too? E.g. one per player, or a fully fledged bookkeeping thing (EntityData).
        	//final MovingConfig mcc = MovingConfig.getConfig(damagedLoc.getWorld().getName());
        	damagedTrace = null; //new LocationTrace(mcc.traceSize, mcc.traceMergeDist);
        	//damagedTrace.addEntry(tick, damagedLoc.getX(), damagedLoc.getY(), damagedLoc.getZ());
        }
        
        if (cc.cancelDead){
        	if (damaged.isDead()) {
        		cancelled = true;
        	}
        	// Only allow damaging others if taken damage this tick.
            if (player.isDead() && data.damageTakenByEntityTick != TickTask.getTick()){
            	cancelled = true;
            }
        }
        
        if (damage <= 4.0 && tick == data.damageTakenByEntityTick && data.thornsId != Integer.MIN_VALUE && data.thornsId == damaged.getEntityId()){
        	// Don't handle further, but do respect selfhit/canceldead.
        	// TODO: Remove soon.
        	data.thornsId = Integer.MIN_VALUE;
        	return cancelled;
        }
        else {
        	data.thornsId = Integer.MIN_VALUE;
        }

        // Run through the main checks.
        if (!cancelled && speed.isEnabled(player)){
        	if (speed.check(player, now)){
        		cancelled = true;
        		// Still feed the improbable.
        		if (data.speedVL > 50){
        			Improbable.check(player, 2f, now, "fight.speed");
        		}
        		else{
        			Improbable.feed(player, 2f, now);
        		}
        	}
        	else if (normalizedMove > 2.0 && Improbable.check(player, 1f, now, "fight.speed")){
        		// Feed improbable in case of ok-moves too.
        		// TODO: consider only feeding if attacking with higher average speed (!)
        		cancelled = true;
        	}
        }

        if (!cancelled && critical.isEnabled(player) && critical.check(player, loc, data, cc)) {
        	cancelled = true;
        }
        
        if (!cancelled && knockback.isEnabled(player) && knockback.check(player, data, cc)) {
        	cancelled = true;
        }
        
        if (!cancelled && noSwing.isEnabled(player) && noSwing.check(player, data, cc)) {
        	cancelled = true;
        }
        
        if (!cancelled && player.isBlocking() && !player.hasPermission(Permissions.MOVING_SURVIVALFLY_BLOCKING)) {
        	cancelled = true;
        }
        
        // TODO: Order of all these checks ...
        // Checks that use LocationTrace.

        // TODO: Later optimize (...), should reverse check window ?
        
        // First loop through reach and direction, to determine a window.
        final boolean reachEnabled = !cancelled && reach.isEnabled(player);
        final boolean directionEnabled = !cancelled && direction.isEnabled(player);
        
        if (reachEnabled || directionEnabled) {
        	if (damagedPlayer != null) {
        		// TODO: Move to a method (trigonometric checks).
                final ReachContext reachContext = reachEnabled ? reach.getContext(player, loc, damaged, damagedLoc, data, cc) : null;
                final DirectionContext directionContext = directionEnabled ? direction.getContext(player, loc, damaged, damagedLoc, data, cc) : null;
                
                final long traceOldest = tick; // - damagedTrace.getMaxSize(); // TODO: Set by window.
                // TODO: Iterating direction: could also start from latest, be it on occasion.
                Iterator<TraceEntry> traceIt = damagedTrace.maxAgeIterator(traceOldest);
                
                boolean violation = true; // No tick with all checks passed.
                boolean reachPassed = !reachEnabled; // Passed individually for some tick.
                boolean directionPassed = !directionEnabled; // Passed individually for some tick.
                // TODO: Maintain a latency estimate + max diff and invalidate completely (i.e. iterate from latest NEXT time)], or just max latency.
                while (traceIt.hasNext()) {
                	final TraceEntry entry = traceIt.next();
                	// Simplistic just check both until end or hit.
                	// TODO: Other default distances/tolerances.
                	boolean thisPassed = true;
                	if (reachEnabled) {
                		if (reach.loopCheck(player, loc, damagedPlayer, entry, reachContext, data, cc)) {
                			thisPassed = false;
                		} else {
                			reachPassed = true;
                		}
                	}
                	// TODO: For efficiency one could omit checking at all if reach is failed all the time.
                	if (directionEnabled && (reachPassed || !directionPassed)) {
                		if (direction.loopCheck(player, damagedLoc, damagedPlayer, entry, directionContext, data, cc)) {
                			thisPassed = false;
                		} else {
                			directionPassed = true;
                		}
                	}
                	if (thisPassed) {
                		// TODO: Log/set estimated latency.
                		violation = false;
                		break;
                	}
                }
                // TODO: How to treat mixed state: violation && reachPassed && directionPassed [current: use min violation // thinkable: silent cancel, if actions have cancel (!)]
                // TODO: Adapt according to strictness settings?
                if (reachEnabled) {
                	// TODO: Might ignore if already cancelled by mixed/silent cancel.
                	if (reach.loopFinish(player, loc, damagedPlayer, reachContext, violation, data, cc)) {
                		cancelled = true;
                	}
                }
                if (directionEnabled) {
                	// TODO: Might ignore if already cancelled.
                	if (direction.loopFinish(player, loc, damagedPlayer, directionContext, violation, data, cc)) {
                		cancelled = true;
                	}
                }
                // TODO: Log exact state, probably record min/max latency (individually).
        	} else {
        		// Still use the classic methods for non-players. maybe[]
        		if (reachEnabled && reach.check(player, loc, damaged, damagedLoc, data, cc)) {
                	cancelled = true;
                }
                
                if (directionEnabled && direction.check(player, loc, damaged, damagedLoc, data, cc)) {
                	cancelled = true;
                }
        	}
        }
        
        // Check angle with allowed window.
        if (angle.isEnabled(player)) {
        	// TODO: Revise, use own trace.
			// The "fast turning" checks are checked in any case because they accumulate data.
			// Improbable yaw changing: Moving events might be missing up to a ten degrees change.
			if (Combined.checkYawRate(player, loc.getYaw(), now, worldName, cc.yawRateCheck)) {
				// (Check or just feed).
				// TODO: Work into this somehow attacking the same aim and/or similar aim position (not cancel then).
				cancelled = true;
			}
			// Angle check.
			if (angle.check(player, worldChanged, data, cc)) {
				if (!cancelled && cc.debug) {
					NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, player.getName() + " fight.angle cancel without yawrate cancel.");
				}
				cancelled = true;
			}
		}
        
        // Set values.
        data.lastWorld = worldName;
    	data.lastAttackTick = tick;
    	data.lastAttackedX = damagedLoc.getX();
    	data.lastAttackedY = damagedLoc.getY();
    	data.lastAttackedZ = damagedLoc.getZ();
//    	data.lastAttackedDist = targetDist;
    	
    	// Care for the "lost sprint problem": sprint resets, client moves as if still...
    	// TODO: Use stored distance calculation same as reach check?
    	// TODO: For pvp: make use of "player was there" heuristic later on.
    	// TODO: Confine further with simple pre-conditions.
    	// TODO: Evaluate if moving traces can help here.
    	if (!cancelled && TrigUtil.distance(loc.getX(), loc.getZ(), damagedLoc.getX(), damagedLoc.getZ()) < 4.5){
    		final MovingData mData = MovingData.getData(player);
			// Check if fly checks is an issue at all, re-check "real sprinting".
    		if (mData.fromX != Double.MAX_VALUE && mData.mediumLiftOff != MediumLiftOff.LIMIT_JUMP){
    			final double hDist = TrigUtil.distance(loc.getX(), loc.getZ(), mData.fromX, mData.fromZ);
    			if (hDist >= 0.23) {
    				// TODO: Might need to check hDist relative to speed / modifiers.
    				final MovingConfig mc = MovingConfig.getConfig(player);
    				if (now <= mData.timeSprinting + mc.sprintingGrace && MovingListener.shouldCheckSurvivalFly(player, mData, mc)){
    					// Judge as "lost sprint" problem.
    					// TODO: What would mData.lostSprintCount > 0  mean here?
        				mData.lostSprintCount = 7;
        				if ((cc.debug || mc.debug) && BuildParameters.debugLevel > 0){
							NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, player.getName() + " (lostsprint) hDist to last from: " + hDist + " | targetdist=" + TrigUtil.distance(loc.getX(), loc.getZ(), damagedLoc.getX(), damagedLoc.getZ()) + " | sprinting=" + player.isSprinting() + " | food=" + player.getFoodLevel() +" | hbuf=" + mData.sfHorizontalBuffer);
        				}
    				}
    			}
    		}
    	}
    	
    	// Generic attacking penalty.
    	// (Cancel after sprinting hacks, because of potential fp).
        if (!cancelled && data.attackPenalty.isPenalty(now)) {
        	cancelled = true;
        	if (cc.debug) {
				NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, player.getName() + " ~ attack penalty.");
        	}
        }
        
    	// Cleanup.
        useLoc1.setWorld(null);
        useLoc2.setWorld(null);
        
        return cancelled;
    }
    
    /**
     * Check if a player might return some damage due to the "thorns" enchantment.
     * @param player
     * @return
     */
    public static final boolean hasThorns(final Player player){
    	final PlayerInventory inv = player.getInventory();
    	final ItemStack[] contents = inv.getArmorContents();
    	for (int i = 0; i < contents.length; i++){
    		final ItemStack stack = contents[i];
    		if (stack != null && stack.getEnchantmentLevel(Enchantment.THORNS) > 0){
    			return true;
    		}
    	}
    	return false;
    }

    /**
     * We listen to EntityDamage events for obvious reasons.
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamage(final EntityDamageEvent event) {
    	
    	final Entity damaged = event.getEntity();
    	final Player damagedPlayer = damaged instanceof Player ? (Player) damaged : null;
    	final FightData damagedData = damagedPlayer == null ? null : FightData.getData(damagedPlayer);
    	final boolean damagedIsDead = damaged.isDead();
    	if (damagedPlayer != null && !damagedIsDead) {
            if (!damagedPlayer.isDead() && godMode.isEnabled(damagedPlayer) && godMode.check(damagedPlayer, BridgeHealth.getDamage(event), damagedData)){
                // It requested to "cancel" the players invulnerability, so set their noDamageTicks to 0.
            	damagedPlayer.setNoDamageTicks(0);
            }
            if (BridgeHealth.getHealth(damagedPlayer) >= BridgeHealth.getMaxHealth(damagedPlayer)){
            	// TODO: Might use the same FightData instance for GodMode.
            	if (damagedData.fastHealBuffer < 0){
            		// Reduce negative buffer with each full health.
            		damagedData.fastHealBuffer /= 2;
            	}
            	// Set reference time.
            	damagedData.fastHealRefTime = System.currentTimeMillis();
            }
        }
//    	NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(LogManager.TRACE_FILE, event.getCause());
    	// Attacking entities.
        if (event instanceof EntityDamageByEntityEvent) {
            final EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
            final Entity damager = e.getDamager();
            final int tick = TickTask.getTick();
        	if (damagedPlayer != null && !damagedIsDead){
        	    // TODO: check once more when to set this (!) in terms of order.
        		FightData.getData(damagedPlayer).damageTakenByEntityTick = tick;
                if (hasThorns(damagedPlayer)){
            		// TODO: Cleanup here.
                	// Remember the id of the attacker to allow counter damage.
                	damagedData.thornsId = damager.getEntityId();
            	}
                else{
                	damagedData.thornsId = Integer.MIN_VALUE;
                }
        	}
        	final DamageCause damageCause = event.getCause();
        	final Player player = damager instanceof Player ? (Player) damager : null;
        	Player attacker = player;
        	// TODO: deobfuscate.
        	if (damager instanceof TNTPrimed) {
        		final Entity source = ((TNTPrimed) damager).getSource();
        		if (source instanceof Player) {
        			attacker = (Player) source;
        		}
        	}
        	if (attacker != null && (damageCause == DamageCause.BLOCK_EXPLOSION || damageCause == DamageCause.ENTITY_EXPLOSION)) {
        		// NOTE: Pigs don't have data.
				final FightData data = FightData.getData(attacker);
            	data.lastExplosionEntityId = damaged.getEntityId();
    			data.lastExplosionDamageTick = tick;
    			return;
    		}
            if (player != null){
                final double damage = BridgeHealth.getDamage(e);
                final FightData data = FightData.getData(player);
                if (damageCause == DamageCause.ENTITY_ATTACK){
    				// TODO: Might/should skip the damage comparison, though checking on lowest priority.
                	if (damaged.getEntityId() == data.lastExplosionEntityId && tick == data.lastExplosionDamageTick) {
                		data.lastExplosionDamageTick = -1;
                		data.lastExplosionEntityId = Integer.MAX_VALUE;
                	} else if (handleNormalDamage(player, damaged, damage, tick, data)){
                		e.setCancelled(true);
                	}
                }
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDamageMonitor(final EntityDamageEvent event) {
    	final Entity damaged = event.getEntity();
    	if (damaged instanceof Player){
    		final Player player = (Player) damaged;
    		final FightData data = FightData.getData(player);
    		final int ndt = player.getNoDamageTicks();
    		if (data.lastDamageTick == TickTask.getTick() && data.lastNoDamageTicks != ndt){
    			// Plugin compatibility thing.
    			data.lastNoDamageTicks = ndt;
    		}
    	}
    }

    /**
     * We listen to death events to prevent a very specific method of doing godmode.
     * 
     * @param event
     *            the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    protected void onEntityDeathEvent(final EntityDeathEvent event) {
        // Only interested in dying players.
        final Entity entity = event.getEntity();
        if (entity instanceof Player){
            final Player player = (Player) entity;
            if (godMode.isEnabled(player)) {
            	godMode.death(player);
            }
        }
    }

    /**
     * We listen to PlayerAnimation events because it is used for arm swinging.
     * 
     * @param event
     *            the event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    protected void onPlayerAnimation(final PlayerAnimationEvent event) {
        // Set a flag telling us that the arm has been swung.
        FightData.getData(event.getPlayer()).noSwingArmSwung = true;
    }

    /**
     * We listen to the PlayerToggleSprint events for the Knockback check.
     * 
     * @param event
     *            the event
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerToggleSprint(final PlayerToggleSprintEvent event) {
        if (event.isSprinting()) {
        	FightData.getData(event.getPlayer()).knockbackSprintTime = System.currentTimeMillis();
        }
    }
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityRegainHealthLow(final EntityRegainHealthEvent event){
    	final Entity entity = event.getEntity();
    	if (!(entity instanceof Player)) return;
    	final Player player = (Player) entity;
    	if (player.isDead() && BridgeHealth.getHealth(player) <= 0.0) {
    		// Heal after death.
    		event.setCancelled(true);
    		counters.addPrimaryThread(idCancelDead, 1);
    		return;
    	}
    	if (event.getRegainReason() != RegainReason.SATIATED) {
    		return;
    	}
    	if (fastHeal.isEnabled(player) && fastHeal.check(player)) {
    		// TODO: Can clients force events with 0-re-gain ?
    		event.setCancelled(true);
    	}
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(final EntityRegainHealthEvent event){
    	final Entity entity = event.getEntity();
    	if (!(entity instanceof Player)) return;
    	final Player player = (Player) entity;
    	final FightData data = FightData.getData(player);
    	// Adjust god mode data:
    	// Remember the time.
    	data.regainHealthTime = System.currentTimeMillis();
    	// Set god-mode health to maximum.
    	// TODO: Mind that health regain might half the ndt.
    	final double health = Math.min(BridgeHealth.getHealth(player) + BridgeHealth.getAmount(event), BridgeHealth.getMaxHealth(player));
    	data.godModeHealth = Math.max(data.godModeHealth, health);
    }

	@Override
	public void playerJoins(final Player player) {
	}

	@Override
	public void playerLeaves(final Player player) {
		final FightData data = FightData.getData(player);
		data.angleHits.clear();
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event){
		FightData.getData(event.getPlayer()).onWorldChange();
	}
	
	@EventHandler(ignoreCancelled = false, priority = EventPriority.MONITOR)
    public void onItemHeld(final PlayerItemHeldEvent event) {
		final Player player = event.getPlayer();
		final long penalty = FightConfig.getConfig(player).toolChangeAttackPenalty;
		if (penalty > 0 ) {
			FightData.getData(player).attackPenalty.applyPenalty(penalty);
		}
	}

}
