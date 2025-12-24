package arnett.radio.Commands;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

public interface SubCommand {

    //name of the command
    String getName();

    //description (for help)
    String getDescription();

    //syntax (for help)
    String getSyntax();

    //code that gets executed (i mean it's in the name)
    default boolean execute(Player player, String[] args, int level)
    {
        if(!canUse(player))
        {
            player.sendMessage(Component.text("You do not have permission to run this command!"));
            return true;
        }

        return false;
    }

    //permission check
    boolean canUse(Player player);

    //provides suggestions when writing args
    List<String> getSubcommandArguments(Player player, String[] args, int level);
}
