package arnett.radio.Commands;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class BranchCommand implements SubCommand {

    protected HashMap<SubCommand, String> subCommands = new HashMap<>();

    public BranchCommand(HashMap<SubCommand, String> map)
    {
        //fill sub commands
        map.forEach((command, plugin)->{
            if(CommandManager.avaliablePlugins.contains(plugin))
                subCommands.put(command, plugin);
        });
    }

    @Override
    public boolean execute(Player player, String[] args, int level)
    {
        for(SubCommand cmd : subCommands.keySet())
        {
            if(args[level].equalsIgnoreCase(cmd.getName()))
            {
                //preform sub command given
                return cmd.execute(player, args, level + 1);
            }
        }

        return false;
    }


    @Override
    //provides suggestions when writing args
    public List<String> getSubcommandArguments(Player player, String[] args, int level)
    {
        //does player have permission for this branch
        if(!canUse(player))
            return List.of();

        //if the length of the args is equal to the level, we have found the current end node
        if(args.length == level)
        {
            //display this node's sub commands
            ArrayList<String> commandNames = new ArrayList<>();
            subCommands.forEach((command, plugin) -> {
                commandNames.add(command.getName());
            });

            return commandNames;
        }

        //doesn't end on this command
        for(SubCommand cmd : subCommands.keySet())
        {
            if(cmd.getName().equalsIgnoreCase(args[level-1]))
                return cmd.getSubcommandArguments(player, args, level+1);
        }

        return List.of();
    }
}
