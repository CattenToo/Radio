package arnett.radio.Items.Radio;

import arnett.radio.Frequencies.FrequencyManager;
import arnett.radio.RadioVoiceChat;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

//mostly just a library class for functions relating to the radio through voicechat
public class FieldRadioVoiceChat {

    //stores all players that are listening to which frequency
    public static HashMap<String, ArrayList<UUID>> frequencyListeners = new HashMap<>();
    //stores all players who are on grace period so mic doesn't cut off
    // uses long because I feel like it and ticks can slow which isn't really ideal for this
    public static HashMap<UUID, Long> playersInGracePeroid = new HashMap<>();

    public static void addToFrequency(String frequency, UUID id)
    {
        frequencyListeners.computeIfAbsent(frequency, key -> new ArrayList<UUID>()).add(id);
    }

    public static void removeFromFrequency(String frequency, UUID id)
    {
        frequencyListeners.get(frequency).remove(id);

        if (frequencyListeners.get(frequency).isEmpty())
            frequencyListeners.remove(frequency);
    }

    public static void removeFromFrequency(UUID id)
    {
        frequencyListeners.entrySet().removeIf(((s) -> {
            s.getValue().removeIf((e) ->
                    e.equals(id)
            );

            if (s.getValue().isEmpty())
                return true;
            return false;
        }));
    }

    //cleans frequencies
    public static void clearFrequencies()
    {
        frequencyListeners.clear();
    }

    //overload for clearing one player from frequencies
    public static void clearFrequency(Player player)
    {
        clearFrequency(player.getUniqueId());
    }

    //overload for clearing one player from frequencies
    public static void clearFrequency(UUID target)
    {
        for(String s : frequencyListeners.keySet())
            clearFrequency(s, target);
    }

    //overload for clearing one player from frequencies
    public static void clearFrequency(String frequency, UUID target)
    {
        //remove the player from the list and remove the list entry if it is empty

        frequencyListeners.get(frequency).removeIf(k -> k.equals(target));

        if(frequencyListeners.get(frequency).isEmpty())
            frequencyListeners.remove(frequency);
    }

    //overload for clearing one player from frequencies
    public static void clearFrequency(String frequency, Player player)
    {
        clearFrequency(frequency, player.getUniqueId());
    }


    public static boolean isOnFrequency(Player player)
    {
        UUID target = player.getUniqueId();
        return isOnFrequency(target);
    }

    //overload
    public static boolean isOnFrequency(UUID target)
    {
         for(String s : frequencyListeners.keySet())
             if(isOnFrequency(s, target))
                 return true;

        return false;
    }

    public static boolean isOnFrequency(String frequency, UUID target)
    {
        return frequencyListeners.getOrDefault(frequency, new ArrayList<>()).contains(target);
    }

    public static boolean isOnFrequency(String frequency, Player player)
    {
        UUID target = player.getUniqueId();
        return isOnFrequency(frequency, target);
    }

    public static void removeFromGrace(UUID id)
    {
        playersInGracePeroid.remove(id);
    }

    public static void refresh(Player target){

        clearFrequency(target);

        ItemStack[] radios = FieldRadio.getRadiosFromPlayer(target);

        for(ItemStack radio : radios)
            addToFrequency(FrequencyManager.getFrequency(radio), target.getUniqueId());
    }

    public static void refresh(String frequency, Player target){

        clearFrequency(frequency, target);

        ItemStack[] radios = FieldRadio.getRadiosFromPlayer(target);

        for(ItemStack radio : radios)
            if(FrequencyManager.getFrequency(radio).equalsIgnoreCase(frequency))
                addToFrequency(FrequencyManager.getFrequency(radio), target.getUniqueId());
    }

    public static void sendPacketToFrequency(UUID sender, byte[] audioData, String frequency, MicrophonePacket packet)
    {
        //do these radios exist
        if (FieldRadioVoiceChat.frequencyListeners.get(frequency) == null)
            return;

        // hash map of players already computed so we don't waste time with doing it again
        Set<UUID> processed = new HashSet<>((int)Math.sqrt(FieldRadioVoiceChat.frequencyListeners.get(frequency).size()));

        //so player doesn't hear themselves
        processed.add(sender);

        //sending to field radios
        for(UUID id : FieldRadioVoiceChat.frequencyListeners.get(frequency))
        {
            //skip if already added to set (they've already been sent the packet)
            if(!processed.add(id))
                continue;

            //grab connection
            VoicechatConnection connection = RadioVoiceChat.api.getConnectionOf(id);

            //make sure connection is there
            if(connection == null || !connection.isConnected())
                continue;

            //send audio
            RadioVoiceChat.api.sendStaticSoundPacketTo(connection, packet.staticSoundPacketBuilder().opusEncodedData(audioData).build());
        }
    }
}
