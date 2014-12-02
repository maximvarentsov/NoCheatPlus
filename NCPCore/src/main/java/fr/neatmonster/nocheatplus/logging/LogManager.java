package fr.neatmonster.nocheatplus.logging;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.logging.details.AbstractLogManager;
import fr.neatmonster.nocheatplus.logging.details.BukkitLogNodeDispatcher;
import fr.neatmonster.nocheatplus.logging.details.ContentLogger;
import fr.neatmonster.nocheatplus.logging.details.FileLoggerAdapter;
import fr.neatmonster.nocheatplus.logging.details.LogOptions;
import fr.neatmonster.nocheatplus.logging.details.LogOptions.CallContext;


/**
 * Central access point for logging. The default loggers use the stream names,
 * at least as prefixes).<br>
 * Note that logging to the init/plugin/server with debug/fine or finer, might
 * result in the server loggers suppressing those. As long as default file is
 * activated, logging to init will log all levels to the file.
 *
 * @author dev1mc
 *
 */
public class LogManager extends AbstractLogManager {

    // TODO: Make LogManager an interface <- AbstractLogManager <- BukkitLogManager (hide some / instanceof).

    // TODO: ingame logging [ingame needs api to keep track of players who receive notifications.].
    // TODO: Later: Custom loggers (file, other), per-player-streams (debug per player), custom ingame loggers (one or more players).

    protected final Plugin plugin;

    /**
     * This will create all default loggers as well.
     * @param plugin
     */
    public LogManager(Plugin plugin) {
        super(new BukkitLogNodeDispatcher(plugin), Streams.defaultPrefix, Streams.INIT);
        this.plugin = plugin;
        ConfigFile config = ConfigManager.getConfigFile();
        createDefaultLoggers(config);
        getLogNodeDispatcher().setMaxQueueSize(config.getInt(ConfPaths.LOGGING_MAXQUEUESIZE));
    }

    @Override
    protected void registerInitLogger() {
        synchronized (registryCOWLock) {
            if (!hasStream(Streams.INIT)) {
                createInitStream();
            }
            else if (hasLogger(Streams.INIT.name)) {
                // Shallow check.
                return;
            }
            // Attach a new restrictive init logger.
            boolean bukkitLoggerAsynchronous = ConfigManager.getConfigFile().getBoolean(ConfPaths.LOGGING_BACKEND_CONSOLE_ASYNCHRONOUS);
            LoggerID initLoggerID = registerStringLogger(new ContentLogger<String>() {

                @Override
                public void log(Level level, String content) {
                    try {
                        Bukkit.getLogger().log(level, content);
                    } catch (Throwable t) {}
                }

            }, new LogOptions(Streams.INIT.name, bukkitLoggerAsynchronous ? CallContext.ANY_THREAD_DIRECT : CallContext.PRIMARY_THREAD_ONLY));
            attachStringLogger(initLoggerID, Streams.INIT);
        }
    }

    /**
     * Create default loggers and streams.
     */
    protected void createDefaultLoggers(ConfigFile config) {
        // Default streams.
        for (StreamID streamID : new StreamID[] {
                Streams.STATUS,
                Streams.SERVER_LOGGER, Streams.PLUGIN_LOGGER,
                Streams.NOTIFY_INGAME,
                Streams.DEFAULT_FILE, Streams.TRACE_FILE,

        }) {
            createStringStream(streamID);
        }

        // Variables for temporary use.
        LoggerID tempID;
        File file;

        // Configuration/defaults.
        // TODO: More configurability.
        // TODO: Might attempt to detect if a thread-safe logging framework is in use ("default" instead of false/true).
        boolean bukkitLoggerAsynchronous = config.getBoolean(ConfPaths.LOGGING_BACKEND_CONSOLE_ASYNCHRONOUS);
        // TODO: Do keep considering: AYNCHRONOUS_DIRECT -> ASYNCHRONOUS_TASK (not to delay async. event handling).
        CallContext defaultAsynchronousContext = CallContext.ASYNCHRONOUS_DIRECT; // Plugin runtime + asynchronous.

        // Server logger.
        tempID = registerStringLogger(Bukkit.getLogger(), new LogOptions(Streams.SERVER_LOGGER.name, bukkitLoggerAsynchronous ? defaultAsynchronousContext : CallContext.PRIMARY_THREAD_TASK));
        attachStringLogger(tempID, Streams.SERVER_LOGGER);

        // Plugin logger.
        tempID = registerStringLogger(plugin.getLogger(), new LogOptions(Streams.PLUGIN_LOGGER.name, bukkitLoggerAsynchronous ? defaultAsynchronousContext : CallContext.PRIMARY_THREAD_TASK));
        attachStringLogger(tempID, Streams.PLUGIN_LOGGER);

        // Ingame logger (assume not thread-safe at first).
        // TODO: Thread-safe ProtocolLib-based implementation?
        tempID = registerStringLogger(new ContentLogger<String>() {

            @Override
            public void log(Level level, String content) {
                // Ignore level for now.
                NCPAPIProvider.getNoCheatPlusAPI().sendAdminNotifyMessage(content);
            }

        }, new LogOptions(Streams.NOTIFY_INGAME.name, CallContext.PRIMARY_THREAD_DIRECT)); // TODO: Consider task.
        attachStringLogger(tempID, Streams.NOTIFY_INGAME);

        // Default file logger.
        file = new File(config.getString(ConfPaths.LOGGING_BACKEND_FILE_FILENAME));
        if (!file.isAbsolute()) {
            file = new File(plugin.getDataFolder(), file.getPath());
        }
        // TODO: Sanity check file+extensions and fall-back if not valid [make an auxiliary method doing all this at once]!
        ContentLogger<String> defaultFileLogger = new FileLoggerAdapter(file); // TODO: Method to get-or-create these.
        tempID = registerStringLogger(defaultFileLogger, new LogOptions(Streams.DEFAULT_FILE.name, defaultAsynchronousContext));
        attachStringLogger(tempID, Streams.DEFAULT_FILE);

        // Trace file logger.
        // TODO: Create a dedicated file, if "needed".
        attachStringLogger(getLoggerID(Streams.DEFAULT_FILE.name), Streams.TRACE_FILE); // Direct to default file for now.

        // Abstract INIT stream (attach file logger).
        // TODO: Consider configurability of skipping, depending on bukkitLoggerAsynchronous.
        tempID = registerStringLogger(defaultFileLogger, new LogOptions(Streams.DEFAULT_FILE.name +".init", CallContext.ANY_THREAD_DIRECT));
        attachStringLogger(tempID, Streams.INIT);

        // Abstract STATUS stream (efficient version of INIT during plugin runtime).
        attachStringLogger(getLoggerID(Streams.SERVER_LOGGER.name), Streams.STATUS);
        attachStringLogger(getLoggerID(Streams.DEFAULT_FILE.name), Streams.STATUS);

        //
    }

    /**
     * Not "official". TODO: Hide.
     */
    public void onReload() {
        // Hard clear and re-do loggers. Might result in loss of content.
        clear(0L, true); // Can not afford to wait.
        createDefaultLoggers(ConfigManager.getConfigFile());
    }

    /**
     * Necessary logging to a primary thread task (TickTask).
     */
    public void startTasks() {
        // TODO: Schedule / hide (redundant calls mean no harm, at present). 
        ((BukkitLogNodeDispatcher) getLogNodeDispatcher()).startTasks();
    }

}