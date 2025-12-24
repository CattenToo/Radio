package arnett.radio.Commands.CommandTree.Monitor;

import arnett.radio.Commands.SubCommand;
import arnett.radio.RadioConfig;
import arnett.radio.FrequencyManager;
import arnett.radio.Items.Radio.FieldRadioVoiceChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

// todo THIS COMMAND WILL BE REWORKED LATER

@SuppressWarnings("UnstableApiUsage")
public class FrequencyDisplayCommand implements SubCommand {

    @Override
    public String getName() {
        return "frequency";
    }

    @Override
    public String getDescription() {
        return "Displays the players listening to given frequencies";
    }

    @Override
    public String getSyntax() {
        return "/radio frequency <main> <sub>";
    }

    @Override
    public boolean execute(Player player, String[] args, int level) {

        //this is just a permission check
        SubCommand.super.execute(player, args, level);

        Map<String, ArrayList<UUID>> map = FieldRadioVoiceChat.getFrequencys();

        //monitoring frequency
        StringBuilder argFrequency = new StringBuilder();

        for(String s : args)
            argFrequency.append(s).append(RadioConfig.frequencySplitString);

        StringBuilder playerList = new StringBuilder();
        map.forEach((frequency, players) -> {

            //check if args match frequency
            if(!frequency.startsWith(argFrequency.toString()))
                return;

            for(UUID id : players)
            {
                try {
                    playerList.append(Bukkit.getPlayer(id).getName());
                }
                catch (NullPointerException e)
                {
                    //player not online so Can't get name (this is slower btw)
                    playerList.append(Bukkit.getOfflinePlayer(id).getName());
                }
                playerList.append(", ");
            }

            //display
            player.sendMessage(FrequencyManager.getColoredFrequencyTag(frequency)
                    .append(Component.text(playerList.toString())));

            playerList.setLength(0);
        });

        player.sendMessage(Component.text("Search Complete").decorate(TextDecoration.BOLD));

        return true;
    }

    @Override
    public boolean canUse(Player player)
    {
        return player.hasPermission("radio.monitor");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args, int level) {
        // /DisplayRadioListeners <here> <here> <here> ...
        if (args.length >= level) {
            //returns all dye values
            return FrequencyManager.dyeMap.keySet().stream().toList();
        }

        return List.of();
    }
}
