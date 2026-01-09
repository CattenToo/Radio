package arnett.radio.Commands.CommandTree.Manage.Config;

import arnett.radio.Commands.SubCommand;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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

        // args.length - 1 because last argument should be value to set to
        for(int i = index; i < args.length - 1; i++)
        {
            curr.append(args[i]).append(".");
        }

        curr.setLength(curr.length() - 1);

        Object previous = Radio.singleton.getConfig().get(curr.toString());
        Object setArgument;

        try
        {
            setArgument = parseValue(previous.getClass(), args[args.length - 1]);
        }
        catch (Exception e)
        {
            player.sendMessage(Component.text("Unable to set Type, Either wrong input type or, set it in config on restart instead"));
            return true;
        }

        Radio.singleton.getConfig().set(curr.toString(), setArgument);
        Radio.singleton.saveConfig();
        player.sendMessage("Config Saved: " + curr.toString() + " set from " + previous + " to " + args[args.length - 1] + " (Reload Required to take effect)");
        return true;
    }

    //Class<?> means any class
    private Object parseValue(Class<?> type, String s)
    {
        //most used first
        if(type == String.class)
            return s;
        if(type == Boolean.class || type == boolean.class)
            return Boolean.parseBoolean(s);
        if(type == Integer.class || type == int.class)
            return Integer.parseInt(s);
        if(type == Float.class || type == float.class)
            return Float.parseFloat(s);
        if(type == Double.class || type == double.class)
            return Double.parseDouble(s);
        if(type == Character.class || type == char.class)
            return s.charAt(0);

        return null;
    }

    @Override
    public boolean canUse(Player player) {
        return player.hasPermission("radio.manage");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args, int level) {
        if(args.length == level)
        {
            Set<String> keys = Radio.singleton.getConfig().getKeys(false);

            //remove ignored entries (things that shouldn't be changed at runtime)
            for (String key : keys.toArray(new String[0]))
            {
                if(key.charAt(0) == '~')
                {
                    for (String ignore : key.split("/"))
                        if(key.equals("~"))
                            continue;
                        else
                            keys.remove(ignore);

                    keys.remove(key);
                }
            }

            return keys.stream().toList();
        }
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

                curr.setLength(curr.length() - 1);

                return Radio.singleton.getConfig().getConfigurationSection(curr.toString()).getKeys(false).stream().toList();
            }
            catch (Exception ignored){}
            // no further keys
        else if (args.length > level + 1)
        {
            //next argument present
            return getSubcommandArguments(player, args, level + 1);
        }


        return List.of();
    }
}