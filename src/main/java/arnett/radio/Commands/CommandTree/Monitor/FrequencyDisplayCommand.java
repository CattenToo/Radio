package arnett.radio.Commands.CommandTree.Monitor;

import arnett.radio.Commands.SubCommand;
import arnett.radio.Items.Microphone.Microphone;
import arnett.radio.Items.Speaker.Speaker;
import arnett.radio.RadioConfig;
import arnett.radio.Frequencies.FrequencyManager;
import arnett.radio.Items.Radio.FieldRadioVoiceChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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

        //monitoring frequency
        StringBuilder argFrequency = new StringBuilder();

        for(int i = level; i < args.length; i++)
            argFrequency.append(args[level]).append(RadioConfig.frequencySplitString);

        //cut off last split char
        if(argFrequency.length() > RadioConfig.frequencySplitString.length())
            argFrequency.setLength(argFrequency.length() - RadioConfig.frequencySplitString.length());

        StringBuilder receiverList = new StringBuilder();

        //field radios
        FieldRadioVoiceChat.frequencyListeners.forEach((frequency, players) -> {

            //check if args match frequency
            if(!frequency.startsWith(argFrequency.toString()))
            {
                return;
            }

            //list players
            for(UUID id : players)
            {
                try {
                    receiverList.append(Bukkit.getPlayer(id).getName());
                }
                catch (NullPointerException e)
                {
                    //player not online so Can't get name (this is slower btw)
                    receiverList.append(Bukkit.getOfflinePlayer(id).getName());
                }
                receiverList.append(", ");
            }

            //display
            player.sendMessage(FrequencyManager.getColoredFrequencyTag(frequency)
                    .append(Component.text(receiverList.toString())));

            receiverList.setLength(0);
        });

        //speakers
        {
            //display
                player.sendMessage(Component.text("Speakers: " + Speaker.activeSpeakers.size()));
        }

        //microphones
        {
            //total attachments
            AtomicInteger total = new AtomicInteger();

            Microphone.attachedPlayers.forEach((ignored, connection) -> {
                total.addAndGet(connection.size());
            });

            //display
            player.sendMessage(Component.text("Microphone Connections: " + total));
        }

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