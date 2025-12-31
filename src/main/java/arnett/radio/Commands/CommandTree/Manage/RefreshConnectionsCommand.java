package arnett.radio.Commands.CommandTree.Manage;

import arnett.radio.Commands.SubCommand;
import arnett.radio.Items.Radio.FieldRadioVoiceChat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class RefreshConnectionsCommand implements SubCommand {

    @Override
    public boolean execute(Player player, String[] args, int level) {

        SubCommand.super.execute(player, args, level);

        switch (args.length)
        {
            case 1 -> {

                //refresh for one person
                Player target = Bukkit.getPlayer(args[level]);

                FieldRadioVoiceChat.refresh(target);

            }

            //default assumes no arguments
            default -> {
                FieldRadioVoiceChat.clearFrequencies();

                //refresh for everyone
                for (Player target : Bukkit.getOnlinePlayers())
                {
                    FieldRadioVoiceChat.refresh(target);
                }
            }
        }

        return true;
    }

    @Override
    public String getName() {
        return "refresh";
    }

    @Override
    public String getDescription() {
        return "refresh radios in player inventories";
    }

    @Override
    public String getSyntax() {
        return "/radio manage refresh <player>";
    }

    @Override
    public boolean canUse(Player player)
    {
        return player.hasPermission("radio.manage");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args, int level) {
        // /RefreshRadioConnections <here>
        if (args.length == level) {
            //returns all online players
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }

        return List.of();
    }
}