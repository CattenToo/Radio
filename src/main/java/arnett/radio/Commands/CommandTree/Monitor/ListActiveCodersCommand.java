package arnett.radio.Commands.CommandTree.Monitor;

import arnett.radio.Commands.SubCommand;
import arnett.radio.Radio;
import arnett.radio.RadioVoiceChat;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.event.KeyValuePair;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class ListActiveCodersCommand implements SubCommand{
    @Override
    public String getName() {
        return "coders";
    }

    @Override
    public String getDescription() {
        return "/radio monitor coders";
    }

    @Override
    public String getSyntax() {
        return "Lists the active encoders and decoders";
    }

    @Override
    public boolean execute(Player player, String[] args, int level) {

        SubCommand.super.execute(player, args, level);

        StringBuilder list = new StringBuilder();

        list.append(RadioVoiceChat.encoders.size()).append(": ");

        RadioVoiceChat.encoders.forEach((key, value) -> {
            try{
                list.append(Bukkit.getPlayer(key).getName()).append(", ");
            }
            catch (Exception e)
            {
                list.append(Component.text("NOT FINDABLE"));
            }
        });

        player.sendMessage("Encoders: " + list.toString());
        list.setLength(0);

        list.append(RadioVoiceChat.decoders.size()).append(": ");

        RadioVoiceChat.decoders.forEach((key, value) -> {
            try{
                list.append(Bukkit.getPlayer(key).getName());
            }
            catch (Exception e)
            {
                list.append(Component.text("NOT FINDABLE"));
            }
        });

        player.sendMessage("Decoders: " + list.toString());

        return true;
    }

    @Override
    public boolean canUse(Player player) {
        return player.hasPermission("radio.monitor");
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args, int level) {
        return List.of();
    }
}