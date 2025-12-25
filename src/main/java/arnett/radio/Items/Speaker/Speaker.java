package arnett.radio.Items.Speaker;

import arnett.radio.RadioConfig;
import arnett.radio.FrequencyManager;
import arnett.radio.Radio;
import arnett.radio.RadioVoiceChat;
import com.destroystokyo.paper.MaterialTags;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.Packet;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

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
    public static HashMap<String, ArrayList<AudioChannel>> activeSpeakers = new HashMap<>();


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

        speaker.lore(List.of(Component.text(FrequencyManager.convertToDisplayFrequency(frequency))));

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
        //todo don't forget to replace true here when entity version is ready
        if(item.getType() != (RadioConfig.speaker_useEntity ? true : RadioConfig.speaker_block_headType))
            return false;

        return item.getPersistentDataContainer().has(speakerIdentifierKey, PersistentDataType.STRING);
    }

    public static void addActiveSpeaker(Location location, String frequency)
    {
        LocationalAudioChannel newAudioChannel = RadioVoiceChat.api.createLocationalAudioChannel(
                UUID.randomUUID(),
                RadioVoiceChat.api.fromServerLevel(location.getWorld()),
                RadioVoiceChat.api.createPosition(location.getX(), location.getY(), location.getZ())
        );

        newAudioChannel.setDistance(RadioConfig.speaker_soundRange);

        //add it to the list (create entry if not present)
        activeSpeakers.computeIfAbsent(frequency, (fq) -> new ArrayList<>())
                .add(newAudioChannel);

        Radio.logger.info("Added Speaker to list " + frequency);
        Radio.logger.info("at " + location.getX() + ", " + location.getY() + ", " + location.getZ());
    }

    public static void removeActiveSpeaker(Location location)
    {
        //add it to the list (not an actual audio player since that will be handled per player)
        activeSpeakers.remove(location);

        Radio.logger.info("Removed Speaker from list");
        Radio.logger.info("at " + location.getX() + ", " + location.getY() + ", " + location.getZ());
    }

    public static void sendMicrophonePacketToFrequency(MicrophonePacketEvent e, String frequency)
    {
        //search active speakers for ones connected to frequency
        activeSpeakers.computeIfAbsent(frequency, (fq) -> new ArrayList<>()).forEach((channel) -> {

            Radio.logger.info("Speaker Received Packed On" + frequency);
            channel.send(e.getPacket());

        });


    }

    public static void tagChunkOfSpeaker(Chunk chunk, Location blockLocation){

        PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();

        //add to the list
        List<int[]> locationsList = chunkPdc.get(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays());

        //if it is not yet tagged
        if(locationsList == null)
            locationsList = List.of();

        ArrayList<int[]> locationsArray = new ArrayList<>(locationsList);

        locationsArray.add(new int[]{
                blockLocation.getBlockX(),
                blockLocation.getBlockY(),
                blockLocation.getBlockZ()
        });

        //tag the chunk or update the tag
        chunkPdc.set(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays(), locationsArray);
    }

    public static void untagChunkOfSpeaker(Chunk chunk, Location blockLocation){

        PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();

        //check if chunk is tagged in the first place
        List<int[]> locationsList = chunkPdc.get(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays());

        //if it is not yet tagged
        if(locationsList == null)
            return;

        ArrayList<int[]> locationsArray = new ArrayList<>(locationsList);

        // remove if the locations match
        // this is a little inefficient since we keep going after we remove the one at the location,
        // but just in case there are multiple from the same spot for some reason this will take care of it
        locationsArray.removeIf((location ->
                location[0] == blockLocation.getBlockX() &&
                location[1] == blockLocation.getBlockY() &&
                location[2] == blockLocation.getBlockZ()
        ));

        //tag the chunk or update the tag
        chunkPdc.set(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays(), locationsArray);
    }


    public static boolean isBlockSpeaker(Block block){
        //is the block the specified head type
        if(!block.getType().equals(RadioConfig.speaker_block_headType))
            return false;

        return true;
    }
}
