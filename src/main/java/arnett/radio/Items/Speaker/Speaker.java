package arnett.radio.Items.Speaker;

import arnett.radio.RadioConfig;
import arnett.radio.FrequencyManager;
import arnett.radio.Radio;
import arnett.radio.RadioVoiceChat;
import com.destroystokyo.paper.MaterialTags;
import de.maxhenkel.voicechat.api.Position;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class Speaker {
    //todo use Heads to monitor the block placement

    //used both to identify speaker items and chunks that have speakers
    public static final NamespacedKey speakerIdentifierKey = new NamespacedKey(Radio.singleton, "speaker");

    public static final NamespacedKey speakerCraftKey = new NamespacedKey(Radio.singleton, "speaker_craft");
    public static final NamespacedKey speakerRetuneKey = new NamespacedKey(Radio.singleton, "speaker_retune");

    public static final NamespacedKey speakerModelKey = new NamespacedKey("radio", "speaker");

    // this is used to track active locational channels if using blocks since it's easier on the server
    // or active entity channels if not since they need to be entities anyway
    // active meaning that they are not in an unloaded chunk.
    // Frequencies map to maps of Worlds which contain the list of channels, it's not that bad
    public static HashMap<String, HashMap<World, ArrayList<AudioChannel>>> activeSpeakers = new HashMap<>();


    public static ArrayList<Recipe> getRecipes()
    {
        ArrayList<Recipe> recipes = new ArrayList<Recipe>();

        // Plain Speaker
        if(RadioConfig.speaker_recipe_basic_enabled)
            recipes.add(FrequencyManager.getFrequencyIndependentShapedRecipe(speakerCraftKey, getSpeaker(),
                RadioConfig.speaker_recipe_basic_shape, RadioConfig.speaker_recipe_basic_ingredients));

        //retuning
        if(RadioConfig.speaker_recipe_retune_enabled)
        {
            ShapelessRecipe recipe = new ShapelessRecipe(speakerRetuneKey, getSpeaker());

            List<String> ingredients = RadioConfig.speaker_recipe_retune_ingredients;

            ingredients.forEach(i -> {
                //special case RADIO
                if(i.equals("SPEAKER"))
                    recipe.addIngredient(RadioConfig.speaker_block_headType);

                    //special case DYE for frequency
                else if (i.equals("DYE"))
                    recipe.addIngredient(new RecipeChoice.MaterialChoice(MaterialTags.DYES));

                else {
                    try {
                        recipe.addIngredient(Material.matchMaterial(i));
                    }
                    catch (Exception e)
                    {
                        Radio.logger.warning("Invalid material provided for Speaker retune recipe: " + i);
                    }
                }
            });

            recipes.add(recipe);
        }
        return  recipes;
    }

    public static NamespacedKey[] getRecipekeys()
    {
        return new NamespacedKey[]{speakerCraftKey, speakerRetuneKey};
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

        speaker.setData(DataComponentTypes.ITEM_NAME, Component.text("Speaker"));

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
        activeSpeakers.computeIfAbsent(frequency, (fq) -> new HashMap<>())
                .computeIfAbsent(location.getWorld(), (world) -> new ArrayList<>())
                    .add(newAudioChannel);

        Radio.logger.info("Added Speaker to list " + frequency);
        Radio.logger.info("at " + location.getX() + ", " + location.getY() + ", " + location.getZ());
    }

    public static void removeActiveSpeaker(Location location)
    {
        //remove it from the list by checking the x y z and world
        activeSpeakers.entrySet().removeIf((frequencyMapEntry) ->{
            frequencyMapEntry.getValue().entrySet().removeIf((worldEnrty) -> {
                worldEnrty.getValue().removeIf((audioChannel) -> {

                    return audioChannel instanceof LocationalAudioChannel channel &&
                            location.getBlockX() == (int)channel.getLocation().getX() &&
                            location.getBlockY() == (int)channel.getLocation().getY() &&
                            location.getBlockZ() == (int)channel.getLocation().getZ() &&
                            location.getWorld().equals(worldEnrty.getKey());
                });

                //clear any empty entries
                return worldEnrty.getValue().isEmpty();
            });

            //clear any empty entries
            return frequencyMapEntry.getValue().isEmpty();
        });

        Radio.logger.info("Removed Speaker from list ");
        Radio.logger.info("at " + location.getX() + ", " + location.getY() + ", " + location.getZ());
    }

    public static void sendMicrophonePacketToFrequency(MicrophonePacketEvent e, String frequency)
    {
        short[] decodedAudio = applyFilter(RadioVoiceChat.getDecoder(e.getSenderConnection().getPlayer().getUuid()).decode(e.getPacket().getOpusEncodedData()));

        //search active speakers for ones connected to frequency
        activeSpeakers.getOrDefault(frequency, new HashMap<>()).forEach((world, channelList)->{
            channelList.forEach((channel)->{
                channel.send(RadioVoiceChat.getEncoder(e.getSenderConnection().getPlayer().getUuid()).encode(decodedAudio));
            });
        });
    }

    public static void tagChunkOfSpeaker(Chunk chunk, Location blockLocation, String frequency){

        PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();

        //add to the list
        List<int[]> locationsList = chunkPdc.get(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays());

        //if it is not yet tagged
        if(locationsList == null)
            locationsList = List.of();

        ArrayList<int[]> locationsArray = new ArrayList<>(locationsList);

        //get the int representation of the frequency so we can tag the frequency as well
        int[] freqInts = FrequencyManager.convertToIntFrequency(frequency);

        int[] arr = new int[3 + freqInts.length];

        //location
        arr[0] = blockLocation.getBlockX();
        arr[1] = blockLocation.getBlockY();
        arr[2] = blockLocation.getBlockZ();

        //add the frequency
        for(int i = 0; i < freqInts.length; i++)
        {
            arr[3 + i] = freqInts[i];
        }

        locationsArray.add(arr);

        //tag the chunk or update the tag
        chunkPdc.set(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays(), locationsArray);

        Radio.logger.info("Chunk Tagged");
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

        //update the tag
        if(!locationsArray.isEmpty())
        {
            chunkPdc.set(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays(), locationsArray);
        }
        else {
            //out of speakers
            chunkPdc.remove(Speaker.speakerIdentifierKey);
        }

        Radio.logger.info("Chunk Untagged");
    }

    public static String getFrequencyOfSpeakerBlock(Block block)
    {
        if(!block.getChunk().getPersistentDataContainer().has(speakerIdentifierKey))
            return "none";

        List<int[]> speakers = block.getChunk().getPersistentDataContainer().get(speakerIdentifierKey, PersistentDataType.LIST.integerArrays());

        for (int[] arr : speakers)
        {
            if(arr[0] == (block.getLocation().blockX()) &&
                arr[1] == (block.getLocation().blockY()) &&
                arr[2] == (block.getLocation().blockZ())) {

                return FrequencyManager.convertIntToFrequency(arr, 3);
            }
        }

        //shouldn't reach this
        Radio.logger.warning("No Freqeucny found when scanning for Speaker");
        return "none";
    }

    public static String getFrequencyOfSpeakerBlock(Location location)
    {
        if(!location.getChunk().getPersistentDataContainer().has(speakerIdentifierKey))
            return "none";

        List<int[]> speakers = location.getChunk().getPersistentDataContainer().get(speakerIdentifierKey, PersistentDataType.LIST.integerArrays());

        for (int[] arr : speakers)
        {
            if(arr[0] == (location.blockX()) &&
                    arr[1] == (location.blockY()) &&
                    arr[2] == (location.blockZ())) {

                return FrequencyManager.convertIntToFrequency(arr, 3);
            }
        }

        //shouldn't reach this
        Radio.logger.warning("No Freqeucny found when scanning for Speaker");
        return "none";
    }


    public static short[] applyFilter(short[] decodedData)
    {
        // Filter states to maintain continuity across packets
        double lowPassState = 0;
        double highPassState = 0;
        double lastRawSample = 0;
        Random random = new Random();

        // Configuration constants
        double Volume = RadioConfig.speaker_audioFilter_volume; // Lower = more muffled
        double LP_ALPHA = RadioConfig.speaker_audioFilter_LPAlpha; // Lower = more muffled
        double HP_ALPHA = RadioConfig.speaker_audioFilter_HPAlpha; // Higher = less bass
        int NOISE_FLOOR = RadioConfig.speaker_audioFilter_noiseFloor;  // Constant hiss volume
        int CRACKLE_CHANCE = RadioConfig.speaker_audioFilter_crackleChance; // 1 in 2000 samples

        // no, I did not actually code the audio manipulation part of the filter since I'm not the best at working with audio
        //actually, I did the volume part myself, ya know, the easiest part; i'm so good

        for (int i = 0; i < decodedData.length; i++) {
            double currentSample = decodedData[i];

            //Volume multiplier
            decodedData[i] *= Volume;

            // 1. BANDPASS FILTER (EQ)
            // Low Pass (Cuts highs)
            lowPassState = LP_ALPHA * currentSample + (1 - LP_ALPHA) * lowPassState;
            double filtered = lowPassState;

            // High Pass (Cuts lows)
            highPassState = HP_ALPHA * highPassState + HP_ALPHA * (filtered - lastRawSample);
            lastRawSample = filtered;

            // 2. SATURATION (The "Crunch")
            // Convert to -1.0 to 1.0 range for math
            double x = highPassState / 32768.0;
            // Soft clipping formula: (3x - x^3) / 2
            double saturated = (3 * x - Math.pow(x, 3)) / 2.0;

            // 3. NOISE & INTERFERENCE
            // Constant low-level hiss
            int hiss = random.nextInt(NOISE_FLOOR * 2 + 1) - NOISE_FLOOR;

            // Random electrical "crackles"
            int crackle = (random.nextInt(CRACKLE_CHANCE) == 0) ? (random.nextInt(6000) - 3000) : 0;

            // 4. CLAMP & OUTPUT
            int finalSample = (int) (saturated * 32767) + hiss + crackle;
            decodedData[i] = (short) Math.max(-32768, Math.min(32767, finalSample));
        }

        return decodedData;
    }

    public static boolean isBlockSpeaker(Block block){
        //is the block the specified head type
        return block.getType().equals(RadioConfig.speaker_block_headType) || block.getType().equals(RadioConfig.speaker_block_wallHeadType);
    }

    public static boolean isBlockSpeaker(Material block){
        //is the block the specified head type
        return block.equals(RadioConfig.speaker_block_headType) || block.equals(RadioConfig.speaker_block_wallHeadType);
    }
}
