package cc.co.evenprime.bukkit.nocheat.data;


/**
 * 
 * @author Evenprime
 * 
 */
public class ChatData extends Data {

    public int                 messageCount = 0;
    public int                 spamLasttime = 0;
    public final ExecutionHistory history      = new ExecutionHistory();

}