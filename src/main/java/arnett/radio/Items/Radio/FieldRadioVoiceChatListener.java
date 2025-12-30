package arnett.radio.Items.Radio;

import arnett.radio.FrequencyManager;
import arnett.radio.Items.Speaker.Speaker;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import arnett.radio.RadioVoiceChat;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FieldRadioVoiceChatListener implements Listener {

    @EventHandler
    public void onInventoryChange(PlayerInventorySlotChangeEvent e)
    {
        // so fun fact, when opening a GUI minecraft refreshes the inventory which means
        // this is getting set off, but it's not with the same new and old item because
        // when it is refreshed the old item is AIR and the new item is the actual item
        // which causes an issue because WHY! (syncing that's why)

        // only when a radio is involved and the same radio isn't changed by itself.
        if(FieldRadio.isRadio(e.getNewItemStack()))
        {
            if(e.getOldItemStack().getType().equals(Material.AIR))
            {
                // possible inventory refresh scenario (like opening a chest)
                // or item added to empty slot
                // how to tell the difference? NOT SCIENTIFICALLY POSSIBLE
                // so screw it, one tick later we'll just refresh the player
                // ONLY IF they already are connected to the frequency

                String frequency = FrequencyManager.getFrequency(e.getNewItemStack());

                if(FieldRadioVoiceChat.isOnFrequency(frequency, e.getPlayer()))
                    Bukkit.getScheduler().scheduleSyncDelayedTask(Radio.singleton, () -> {
                        FieldRadioVoiceChat.refresh(frequency, e.getPlayer());
                    }, 1);
            }

            //radio added to inventory
            //set player to listen to frequency
            FieldRadioVoiceChat.addToFrequency(FrequencyManager.getFrequency(e.getNewItemStack()), e.getPlayer().getUniqueId());
        }
        if(FieldRadio.isRadio(e.getOldItemStack()))
        {

            //radio removed from inventory
            //remove player from listen to frequency
            FieldRadioVoiceChat.removeFromFrequency(FrequencyManager.getFrequency(e.getOldItemStack()), e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e)
    {
        FieldRadioVoiceChat.removeFromGrace(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
    {
        // so when a player joins their inventory is refreshed by the server which means
        // the inventory slot change event will be called, so we just have to remove
        // all their existing entries if any (which would be the case if there is a serer stop)
        FieldRadioVoiceChat.removeFromFrequency(e.getPlayer().getUniqueId());
    }

    //voice chat events are static with this plugin because reworking that at this point would be a nightmare
    public static void onMicrophone(MicrophonePacketEvent e)
    {
        // The connection might be null if the event is caused by other means
        if (e.getSenderConnection() == null)
            return;

        // Cast the generic player object of the voice chat API to an actual bukkit player
        // This object should always be a bukkit player object on bukkit based servers
        if (!(e.getSenderConnection().getPlayer().getPlayer() instanceof Player player))
            return;

        //make sure player is actively holding a radio
        if(!FieldRadio.isHoldingRadio(player))
            return;

        //grace period so voice doesn't cut off at end
        if(!player.hasActiveItem() && !FieldRadioVoiceChat.playersInGracePeroid.containsKey(player.getUniqueId()))
        {
            return;
        }
        else if (player.hasActiveItem())
        {
            //update Grace period
            FieldRadioVoiceChat.playersInGracePeroid.put(player.getUniqueId(), System.nanoTime() + RadioConfig.fieldRadio_gracePeriod);
        }
        else
        {
            //they aren't using the radio and are on grace
            if(FieldRadioVoiceChat.playersInGracePeroid.get(player.getUniqueId()) < System.nanoTime())
            {
                //remove player from grace and stop packet
                FieldRadioVoiceChat.removeFromGrace(player.getUniqueId());
                return;
            }
        }


        String frequency = FrequencyManager.getFrequency(FieldRadio.getHeldRadio(player).get());

        //gets voice chat api for connections
        VoicechatServerApi serverVC = e.getVoicechat();

        byte[] audioData = e.getPacket().getOpusEncodedData();

        //apply the filter if needed
        if(RadioConfig.fieldRadio_audioFilter_enabled)
        {
            //modify packet
            audioData = RadioVoiceChat.getEncoder(player.getUniqueId()).encode(FieldRadioVoiceChat.applyFilter(RadioVoiceChat.getDecoder(player.getUniqueId()).decode(audioData)));
        }

        //send to speakers
        Speaker.sendMicrophonePacketToFrequency(player.getUniqueId(), audioData, frequency);

        // hash map of players already computed so we don't waste time with doing it again
        Set<UUID> processed = new HashSet<>((int)Math.sqrt(FieldRadioVoiceChat.frequencyListeners.get(frequency).size()));

        //so player doesn't hear themselves
        processed.add(player.getUniqueId());

        //sending to field radios
        for(UUID id : FieldRadioVoiceChat.frequencyListeners.get(frequency))
        {
            //skip if already added to set (they've already been sent the packet)
            if(!processed.add(id))
                continue;

            //grab connection
            VoicechatConnection connection = serverVC.getConnectionOf(id);

            //make sure connection is there
            if(connection == null || !connection.isConnected())
                continue;

            //send audio
            serverVC.sendStaticSoundPacketTo(connection, e.getPacket().staticSoundPacketBuilder().opusEncodedData(audioData).build());
        }
    }
}
