package arnett.radio.Commands.CommandTree.Manage.Config;

import arnett.radio.Commands.SubCommand;
import arnett.radio.Radio;
import org.bukkit.entity.Player;

import java.util.List;

public class ReloadConfigCommand implements SubCommand {

    @Override
    public boolean execute(Player player, String[] args, int level) {

        SubCommand.super.execute(player, args, level);

        Radio.singleton.reloadConfig();
        return true;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "reloads the config file";
    }

    @Override
    public String getSyntax() {
        return "/radio manage config reload";
    }

    @Override
    public boolean canUse(Player player)
    {
        return player.hasPermission("radio.manage");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args, int level) {
        return List.of();
    }
}