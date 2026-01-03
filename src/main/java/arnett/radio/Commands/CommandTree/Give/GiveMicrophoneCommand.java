package arnett.radio.Commands.CommandTree.Give;

import arnett.radio.Commands.SubCommand;
import arnett.radio.Frequencies.FrequencyManager;
import arnett.radio.Items.Microphone.Microphone;
import arnett.radio.RadioConfig;
import org.bukkit.entity.Player;

import java.util.List;

public class GiveMicrophoneCommand implements SubCommand {

    @Override
    public boolean execute(Player player, String[] args, int level) {
        if(args.length <= level)
        {
            //no frequency provided
            player.give(Microphone.getMicrophone("DECORATION"));
            return true;
        }

        StringBuilder frequency = new StringBuilder();

        for(int i = level; i < args.length; i++)
        {
            frequency.append(args[i]).append(RadioConfig.frequencySplitString);
        }

        frequency.setLength(frequency.length() - RadioConfig.frequencySplitString.length());

        player.give(Microphone.getMicrophone(frequency.toString()));
        return true;
    }

    @Override
    public String getName() {
        return "microphone";
    }

    @Override
    public String getDescription() {
        return "gives microphone of frequency";
    }

    @Override
    public String getSyntax() {
        return "/radio give microphone <frequency>";
    }

    @Override
    public boolean canUse(Player player)
    {
        return player.hasPermission("radio.give");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args, int level) {
        return FrequencyManager.dyeMap.keySet().stream().toList();
    }
}
