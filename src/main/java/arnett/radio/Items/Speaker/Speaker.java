package arnett.radio.Items.Speaker;

import arnett.radio.FrequencyManager;
import arnett.radio.Items.Speaker.SpeakerSession;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import arnett.radio.RadioVoiceChat;
import com.destroystokyo.paper.MaterialTags;
import de.maxhenkel.voicechat.api.Entity;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class Speaker {
    //todo use Heads to monitor the block placement

    //used both to identify speaker items and chunks that have speakers
    public static final NamespacedKey speakerIdentifierKey = new NamespacedKey(Radio.singleton, "speaker");

    public static final NamespacedKey speakerCraftKey = new NamespacedKey(Radio.singleton, "speaker_craft");
    public static final NamespacedKey speakerRetuneKey = new NamespacedKey(Radio.singleton, "speaker_retune");

    public static final NamespacedKey speakerModelKey = new NamespacedKey("radio", "speaker");
    public static final NamespacedKey speakerDisplayModelKey = new NamespacedKey("radio", "speaker_display");
    public static final NamespacedKey speakerWallDisplayModelKey = new NamespacedKey("radio", "speaker_wall_display");

    // this is used to track active locational channels if using blocks since it's easier on the server
    // or active entity channels if not since they need to be entities anyway
    // active meaning that they are not in an unloaded chunk.
    // Frequencies map to maps of Worlds which contains a list of players and a list of their channels, it's not that bad
    public static HashMap<SpeakerSession, LinkedHashMap<UUID, AudioChannel>> activeSpeakers = new HashMap<>();


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

    public static ItemStack getSpeakerEntityItem(String frequency)
    {
        ItemStack speaker = getSpeakerEntityItem();

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
            return getSpeakerBlockItem();
    }

    static ItemStack getSpeakerBlockItem()
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
        speaker.setData(DataComponentTypes.ITEM_NAME, Component.text("Speaker", NamedTextColor.YELLOW));
        speaker.setData(DataComponentTypes.ITEM_MODEL, speakerModelKey);

        //unfortunately this head stuff isn't going to work overriding a mob head so it's an entity exclusive feature
        speaker.unsetData(DataComponentTypes.EQUIPPABLE);

        //Adds Identifier tag
        speaker.editPersistentDataContainer(pdc -> {
            pdc.set(speakerIdentifierKey, PersistentDataType.STRING, "speaker");
        });

        speaker.setData(DataComponentTypes.ITEM_NAME, Component.text("Speaker"));

        return speaker;
    }

    static ItemStack getSpeakerEntityItem()
    {
        ItemStack speaker = new ItemStack(RadioConfig.speaker_entity_baseMaterial);

        //sets Item visuals
        speaker.setData(DataComponentTypes.ITEM_NAME, Component.text("Speaker", NamedTextColor.YELLOW));
        speaker.setData(DataComponentTypes.ITEM_MODEL, speakerModelKey);

        //Adds Identifier tag
        speaker.editPersistentDataContainer(pdc -> {
            pdc.set(speakerIdentifierKey, PersistentDataType.STRING, "speaker");
        });

        //make it equipable so it can be placed on head
        Equippable.Builder equipBuilder = Equippable.equippable(EquipmentSlot.HEAD);
        equipBuilder.assetId(null);
        speaker.setData(DataComponentTypes.EQUIPPABLE, equipBuilder.build());

        //removes jukebox functionality
        speaker.unsetData(DataComponentTypes.JUKEBOX_PLAYABLE);

        speaker.setData(DataComponentTypes.ITEM_NAME, Component.text("Speaker"));

        return speaker;
    }

    public static boolean isSpeaker(ItemStack item)
    {
        return item != null && item.getPersistentDataContainer().has(speakerIdentifierKey);
    }

    public static void addActiveSpeaker(Location location, String frequency)
    {
        LocationalAudioChannel newAudioChannel = RadioVoiceChat.api.createLocationalAudioChannel(
                UUID.randomUUID(),
                RadioVoiceChat.api.fromServerLevel(location.getWorld()),
                RadioVoiceChat.api.createPosition(location.getX(), location.getY(), location.getZ())
        );

        newAudioChannel.setDistance(RadioConfig.speaker_soundRange);

        addSpeakerToActiveList(location, frequency);
    }

    //used when using entities for speakers
    public static void addActiveSpeaker(Location location, String frequency, Entity entity)
    {
        EntityAudioChannel newAudioChannel = RadioVoiceChat.api.createEntityAudioChannel(
                UUID.randomUUID(),
                entity
        );

        newAudioChannel.setDistance(RadioConfig.speaker_soundRange);

        addSpeakerToActiveList(location, frequency, entity);
    }

    private static void addSpeakerToActiveList(Location location, String frequency)
    {
        //add it to the list (create entry if not present)
        activeSpeakers.put(new SpeakerSession(frequency, location, null), new LinkedHashMap<>(RadioConfig.speaker_cacheSize + 1, .75f, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, AudioChannel> eldest) {
                return size() > RadioConfig.speaker_cacheSize; // Removes oldest when size exceeds 8
            }
        });
    }

    private static void addSpeakerToActiveList(Location location, String frequency, Entity entity)
    {
        //add it to the list (create entry if not present)
        activeSpeakers.put(new SpeakerSession(frequency, location, entity), new LinkedHashMap<>(RadioConfig.speaker_cacheSize + 1, .75f, true)
        {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, AudioChannel> eldest) {
                return size() > RadioConfig.speaker_cacheSize; // Removes oldest when size exceeds 8
            }
        });
    }

    public static void removeActiveSpeaker(Location location)
    {
        //remove it from the list by checking the location
        activeSpeakers.entrySet().removeIf((session) -> {
            return Objects.equals(session.getKey().location(), location);
        });
    }

    public static void removeActiveSpeaker(Entity entity)
    {
        //remove it from the list by checking the location
        activeSpeakers.entrySet().removeIf((session) -> {
            return session.getKey().entity() == null || session.getKey().entity().equals(entity);
        });
    }

    public static void sendMicrophonePacketToFrequency(UUID sender, byte[] encodedAudio, String frequency)
    {
        //send the packet out to speakers
        try {
            activeSpeakers.forEach((session, map) -> {
                //if they are the correct frequency
                if(session.frequency().equals(frequency))
                {
                    //get the channel belonging to the player, or create one if it isn't present
                    map.computeIfAbsent(sender, (id) -> createAudioChannelForSession(session)).send(encodedAudio);
                }
            });
        }
        //if something was changed while iterating through
        catch (ConcurrentModificationException ignored){};

    }

    public static AudioChannel createAudioChannelForSession(SpeakerSession session)
    {
        if(session.entity() == null)
        {
            //we are dealing with a block speaker
            Location location = session.location();

            AudioChannel channel = RadioVoiceChat.api.createLocationalAudioChannel(

                    //random, NOT player because client only allows one channel that way
                    UUID.randomUUID(),

                    //world
                    RadioVoiceChat.api.fromServerLevel(
                            location.getWorld()),

                    //position
                    RadioVoiceChat.api.createPosition(
                            location.blockX(), location.blockY(), location.blockZ()
                    ));
            channel.setCategory("speakers");
            return channel;
        }
        else
        {
            //we are dealing with an entity speaker
            AudioChannel channel = RadioVoiceChat.api.createEntityAudioChannel(UUID.randomUUID(),session.entity());
            channel.setCategory("speakers");
            return channel;
        }
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

    public static boolean isBlockSpeaker(Block block){
        //is the block the specified head type
        return block.getType().equals(RadioConfig.speaker_block_headType) || block.getType().equals(RadioConfig.speaker_block_wallHeadType);
    }

    public static boolean isBlockSpeaker(Material block){
        //is the block the specified head type
        return block.equals(RadioConfig.speaker_block_headType) || block.equals(RadioConfig.speaker_block_wallHeadType);
    }

    public static boolean isEntitySpeaker(ItemStack item){
        //is the block the specified head type
        return item != null && item.getType().equals(RadioConfig.speaker_entity_baseMaterial) && item.getPersistentDataContainer().has(speakerIdentifierKey);
    }
}