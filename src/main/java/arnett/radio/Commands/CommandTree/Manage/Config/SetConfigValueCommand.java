package arnett.radio.Commands.CommandTree.Manage.Config;

import arnett.radio.Commands.SubCommand;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class SetConfigValueCommand implements SubCommand {
    @Override
    public String getName() {
        return "set";
    }

    @Override
    public String getDescription() {
        return "/radio manage config set <setting> <value>";
    }

    @Override
    public String getSyntax() {
        return "Sets config values, REQUIRES RELOAD";
    }

    @Override
    public boolean execute(Player player, String[] args, int level) {

        SubCommand.super.execute(player, args, level);

        int index;
        //this part is pretty expense, but it's rarely intended to run so it's fine
        for(index = 0; index < args.length; index++)
        {
            if(args[index].equals("set"))
            {
                index++;
                break;
            }
        }

        StringBuilder curr = new StringBuilder();

        for(int i = index; i < args.length; i++)
        {
            curr.append(args[i]).append(".");
        }

        curr.setLength(curr.length() - RadioConfig.frequencySplitString.length());

        Object previous = Radio.singleton.getConfig().get(curr.toString());
        Radio.singleton.getConfig().set(curr.toString(), args[level+1]);
        player.sendMessage(curr.toString() + " set from " + previous + " to " + args[args.length - 1]);
        return true;
    }

    @Override
    public boolean canUse(Player player) {
        return player.hasPermission("radio.manage");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args, int level) {
        if(args.length == level)
            return Radio.singleton.getConfig().getKeys(false).stream().toList();
        else if(args.length == level + 1)
            try {
                int index;
                //this part is pretty expense, but it's rarely intended to run so it's fine
                for(index = 0; index < args.length; index++)
                {
                    if(args[index].equals("set"))
                    {
                        index++;
                        break;
                    }
                }

                StringBuilder curr = new StringBuilder();

                for(int i = index; i < args.length; i++)
                {
                    curr.append(args[i]).append(".");
                }

                curr.setLength(curr.length() - RadioConfig.frequencySplitString.length());

                return Radio.singleton.getConfig().getConfigurationSection(curr.toString()).getKeys(false).stream().toList();
            }
            catch (Exception ignored){}
            // no further keys
        else if (args.length > level + 1)
        {
            //next argument present
            return getSubcommandArguments(player, args, level);
        }

        return List.of();
    }
}
