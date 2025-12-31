package arnett.radio.Commands.CommandTree.Manage.Config;

import arnett.radio.Commands.SubCommand;
import arnett.radio.Items.Radio.FieldRadio;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class SaveConfigCommand implements SubCommand {
    @Override
    public String getName() {
        return "save";
    }

    @Override
    public String getDescription() {
        return "/radio manage config save";
    }

    @Override
    public String getSyntax() {
        return "Saves current config to plugins/Radio/config.yml then reloads it";
    }

    @Override
    public boolean execute(Player player, String[] args, int level) {

        SubCommand.super.execute(player, args, level);

        Radio.singleton.saveConfig();
        Radio.singleton.reloadConfig();

        player.sendMessage(Component.text("Saved & Reloaded").decorate(TextDecoration.BOLD));

        return true;
    }

    @Override
    public boolean canUse(Player player) {
        return player.hasPermission("radio.manage");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args, int level) {
        return List.of();
    }
}