package arnett.radio.Items.Speaker;

import arnett.radio.RadioConfig;
import arnett.radio.FrequencyManager;
import arnett.radio.Radio;
import arnett.radio.RadioVoiceChat;
import com.destroystokyo.paper.MaterialTags;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.Packet;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class Speaker {
    //todo use Heads to monitor the block placement

    //used both to identify speaker items and chunks that have speakers
    public static final NamespacedKey speakerIdentifierKey = new NamespacedKey(Radio.singleton, "speaker");
    public static final NamespacedKey speakerModelKey = new NamespacedKey("radio", "speaker");

    // this is used to track active locational channels if using blocks since it's easier on the server
    // or active entity channels if not since they need to be entities anyway
    // active meaning that they are not in an unloaded chunk
    public static ArrayList<PlacedSpeakerData> activeSpeakers = new ArrayList<>();


    public static ArrayList<Recipe> getRecipes()
    {
        ArrayList<Recipe> recipes = new ArrayList<Recipe>();

        // Plain Speaker
        if(RadioConfig.speaker_recipe_basic_enabled)
            recipes.add(FrequencyManager.getFrequencyIndependentShapedRecipe(speakerIdentifierKey, getSpeaker(),
                RadioConfig.speaker_recipe_basic_shape, RadioConfig.speaker_recipe_basic_ingredients));

        return  recipes;
    }

    public static ItemStack getSpeaker(String frequency)
    {
        ItemStack speaker = getSpeaker();

        //set frequency
        speaker.editPersistentDataContainer(pdc -> {
            pdc.set(FrequencyManager.radioFrequencyKey, PersistentDataType.STRING, frequency);
        });

        return speaker;
    }

    public static ItemStack getSpeaker()
    {
        if(RadioConfig.speaker_useEntity)
            return getSpeakerEntityItem();
        else
            return getspeakerBlockItem();
    }

    static ItemStack getspeakerBlockItem()
    {
        ItemStack speaker;

        try {
            speaker = new ItemStack(RadioConfig.speaker_block_headType);
        }
        catch (Exception e)
        {
            Radio.logger.info("INVALID CONFIG TYPE FOR SPEAKER HEAD: " + RadioConfig.speaker_block_headType);
            return ItemStack.of(Material.AIR);
        }

        //sets Item visuals
        speaker.setData(DataComponentTypes.ITEM_NAME, Component.text("Speaker"));
        speaker.setData(DataComponentTypes.ITEM_MODEL, speakerModelKey);

        //Adds Identifier tag
        speaker.editPersistentDataContainer(pdc -> {
            pdc.set(speakerIdentifierKey, PersistentDataType.STRING, "speaker");
        });

        return speaker;
    }

    static ItemStack getSpeakerEntityItem()
    {
        ItemStack speaker = new ItemStack(Material.AIR);

        //todo entity version of the speaker

        return speaker;
    }

    public static boolean isSpeaker(ItemStack item)
    {
        //todo don't forfet to replace true when entity version is ready
        if(item.getType() != (RadioConfig.speaker_useEntity ? true : RadioConfig.speaker_block_headType))
            return false;

        return item.getPersistentDataContainer().has(speakerIdentifierKey, PersistentDataType.STRING);
    }

    public static void addActiveSpeaker(Location location, String frequency)
    {
        Radio.logger.info("Added Speaker to list" + frequency);
        //add it to the list (not an actual audio player since that will be handled per player)
        activeSpeakers.add(new PlacedSpeakerData(location, frequency));
    }

    public static void removeActiveSpeaker(Location location, String frequency)
    {
        //add it to the list (not an actual audio player since that will be handled per player)
        activeSpeakers.remove(location);
    }

    public static void sendMicrophonePacketToFrequency(MicrophonePacketEvent e, String frequency)
    {
        //search active speakers for ones connected to frequency
        activeSpeakers.forEach((speaker) -> {

            //correct frequency?
            if(!speaker.isOfFrequency(frequency))
                return;

            UUID sender = e.getSenderConnection().getPlayer().getUuid();

            if(sender == null)
                return;

            Radio.logger.info("Found Speaker to " + frequency);

            Location location = speaker.getLocation();

            //if sender is not cached, cache the sender and create the channel
            if(!speaker.channels.containsKey(sender))
            {
                speaker.channels.put(sender, RadioVoiceChat.api.createLocationalAudioChannel(
                        sender,

                        //screw this one line of code specifically
                        RadioVoiceChat.api.fromServerLevel(((CraftWorld)speaker.getLocation().getWorld()).getHandle()),

                        RadioVoiceChat.api.createPosition(location.getX(), location.getY(), location.getZ()))
                );

                Radio.logger.info("Chached new Player" + Bukkit.getPlayer(sender));
            }

            //send the packet
            speaker.channels.get(sender).send(e.getPacket());
        });

    }

}
