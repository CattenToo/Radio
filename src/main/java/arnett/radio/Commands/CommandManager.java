package arnett.radio.Commands;

import arnett.radio.Commands.CommandTree.Give.GiveFieldRadioCommand;
import arnett.radio.Commands.CommandTree.Give.GiveSpeakerCommand;
import arnett.radio.Commands.CommandTree.GiveBranch;
import arnett.radio.Commands.CommandTree.Manage.Config.SaveConfigCommand;
import arnett.radio.Commands.CommandTree.Manage.Config.SetConfigValueCommand;
import arnett.radio.Commands.CommandTree.Manage.ConfigBranch;
import arnett.radio.Commands.CommandTree.Manage.RefreshConnectionsCommand;
import arnett.radio.Commands.CommandTree.Manage.Config.ReloadConfigCommand;
import arnett.radio.Commands.CommandTree.ManageBranch;
import arnett.radio.Commands.CommandTree.Monitor.FrequencyDisplayCommand;
import arnett.radio.Commands.CommandTree.MonitorBranch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandManager implements CommandExecutor, TabCompleter {
    private final ArrayList<SubCommand> subCommands = new ArrayList<>();
    public static List<String> avaliablePlugins;


    public CommandManager(List<String> plugins) {

        //set plugins for subcommand reference
        avaliablePlugins = plugins;

        //add for commands that don't take a plugin
        avaliablePlugins.add("none");

        //add sub commands
        for(String s : avaliablePlugins)
            switch(s)
            {
                case "none" -> {
                    subCommands.add(new GiveBranch(new HashMap<>(Map.of(

                            new GiveFieldRadioCommand(), "none",
                            new GiveSpeakerCommand(), "voicechat"

                    ))));
                    subCommands.add(new MonitorBranch(new HashMap<>(Map.of(

                            new FrequencyDisplayCommand(), "voicechat"

                    ))));
                    subCommands.add(new ManageBranch(new HashMap<>(Map.of(

                            new RefreshConnectionsCommand(),"voicechat",

                            new ConfigBranch
                                    (new HashMap<>(Map.of(
                                            new ReloadConfigCommand(), "none",
                                            new SetConfigValueCommand(), "none",
                                            new SaveConfigCommand(), "none")))
                            , "none"
                    ))));
                }
            }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        //find subcommand
        if (args.length > 0)
        {
            for (SubCommand sub : subCommands){
                if (args[0].equalsIgnoreCase(sub.getName()))
                {
                    //preform sub command given
                    return sub.execute(player, args, 1);
                }
            }

            //command does not exist
            player.sendMessage(Component.text("Sub Command Does Not Exist").color(TextColor.color(255, 0, 0)));

        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {

        // /radio <here>
        if (args.length == 1)
            return subCommands.stream().map(SubCommand::getName).toList();

        // /radio subCommand <here>
        else if (args.length > 1)
            //search sub commands for their arguments
            for (SubCommand cmd : subCommands)
                if (args[0].equalsIgnoreCase(cmd.getName()))
                    //return the arguments of the sub command
                    return cmd.getSubcommandArguments((Player) sender, args, 2);

        //not in radio tree, handled by yml
        return List.of();
    }
}
