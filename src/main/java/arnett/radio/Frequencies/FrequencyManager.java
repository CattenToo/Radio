package arnett.radio.Frequencies;

import arnett.radio.Items.CustomItemManager;
import arnett.radio.Items.Radio.FieldRadioVoiceChat;
import arnett.radio.Items.Speaker.Speaker;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import arnett.radio.RadioVoiceChat;
import com.destroystokyo.paper.MaterialTags;
import com.google.common.collect.HashBiMap;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.ChatFormatting;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.units.qual.A;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class FrequencyManager {

    //stores which frequency an item is connected to
    public static final NamespacedKey radioFrequencyKey = new NamespacedKey(Radio.singleton, "frequency");

    //stores which dye tab belongs to which dye and vice versa
    //normal is Dye names
    //inverse is custom names
    public static HashBiMap<String, String> dyeMap = HashBiMap.create();

    //hashbimap fast for getting stuff
    static HashBiMap<String, Integer> numberedDyes = HashBiMap.create();

    //list of active broadcasters
    static ArrayList<FrequencyBroadcaster> broadcasters = new ArrayList<>(4);



    public static void reload()
    {
        dyeMap.clear();

        int i = 0;
        //sets up 2 way map for quick frequency color refrence
        for (String key : RadioConfig.frequencyRepresentationDyes.getKeys(false)) {
            dyeMap.put(key, RadioConfig.frequencyRepresentationDyes.getString(key));
            numberedDyes.put(key, i);
            i++;
        }

        broadcasters.forEach(e -> {
            e.stopPlaying();
        });

        if(RadioVoiceChat.api != null)
            setUpBroadcasters();
    }

    public static void setUpBroadcasters()
    {
        //fill broadcaster up
        RadioConfig.presetAudio.forEach((frequency, fileName) -> {
            // try to load the audio file
            try {

                //get the audio file
                AudioInputStream rawAudio = AudioSystem.getAudioInputStream(new File(Radio.singleton.getDataFolder(), fileName));

                AudioFormat scvFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        48000f,
                        16,
                        1,  // mono
                        2,  // frame size = 2 bytes (16-bit)
                        48000f,
                        false  // little-endian
                );

                AudioInputStream scvAudio = AudioSystem.getAudioInputStream(scvFormat, rawAudio);

                //create the audio broadcaster and add it to the list
                broadcasters.add(new FrequencyBroadcaster(frequency, scvAudio.readAllBytes(), true, 0));

                Radio.logger.info("Read Audio for " + frequency +" from : " + fileName);

                scvAudio.close();
                rawAudio.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    //used for shaped recipes
    public static ItemStack addFrequencyToCraft(ItemStack result, ItemStack[] mtx, List<String> shape)
    {
        //get position of dyes
        StringBuilder frequency = new StringBuilder();
        StringBuilder displayFrequency = new StringBuilder();

        //stores which dye was used for which frequency
        //useful if recipe required two or more dyes for the same sub frequency
        String[] dyesChecker = new String[8];

        //check matrix for dyes in correct places
        for(int i = 0; i < shape.size(); i++ )
        {
            for(int j = 0; j < shape.get(i).length(); j++)
            {
                char checked = shape.get(i).charAt(j);

                if(Character.isDigit(checked))
                {

                    Material item = mtx[i*3 + j].getType();
                    int digit = Character.getNumericValue(checked);
                    String dyeName = item.name().substring(0, item.name().length()-4);
                    //dye
                    if(dyesChecker[digit] == null)
                    {
                        //has not yet been added
                        //so just add dye to frequency and to dyes checker
                        frequency.append(dyeName);
                        frequency.append(RadioConfig.frequencySplitString);
                        displayFrequency.append(RadioConfig.frequencyRepresentationDyes.getString(dyeName));
                        displayFrequency.append(RadioConfig.frequencySplitString);
                        dyesChecker[digit] = RadioConfig.frequencyRepresentationDyes.getString(dyeName);
                    }
                    else if(!dyesChecker[digit].equals(RadioConfig.frequencyRepresentationDyes.getString(dyeName)))
                    {
                        //invalid recipe
                        return ItemStack.of(Material.AIR);
                    }
                }
            }
        }

        //chop off last bit
        frequency.setLength(frequency.length() - RadioConfig.frequencySplitString.length());
        displayFrequency.setLength(displayFrequency.length() - RadioConfig.frequencySplitString.length());

        tagFrequency(result, frequency.toString());

        result.lore(List.of(Component.text(displayFrequency.toString())));

        return result;
    }

    //used for shapeless recipes
    public static ItemStack addFrequencyToCraft(ItemStack result, ItemStack[] mtx)
    {
        //String builders since they're literally built for this
        StringBuilder frequency = new StringBuilder();
        StringBuilder displayFrequency = new StringBuilder();

        //check matrix for dyes
        for(ItemStack item : mtx)
        {
            if(item == null)
                continue;

            //if this item is a dye
            if(MaterialTags.DYES.isTagged(item))
            {
                //gets dye name
                String dyeName = item.getType().name().substring(0, item.getType().name().length()-4);

                //add to tags
                frequency.append(dyeName);
                frequency.append(RadioConfig.frequencySplitString);
                displayFrequency.append(RadioConfig.frequencyRepresentationDyes.getString(dyeName));
                displayFrequency.append(RadioConfig.frequencySplitString);
            }
        }

        //chop off last bit
        frequency.setLength(frequency.length() - RadioConfig.frequencySplitString.length());
        displayFrequency.setLength(displayFrequency.length() - RadioConfig.frequencySplitString.length());

        tagFrequency(result, frequency.toString());

        result.lore(List.of(Component.text(displayFrequency.toString())));

        return result;
    }

    public static TextComponent getColoredFrequencyTag(String frequency)
    {
        //only used when displaying frequencies, not for logic
        frequency = convertToDisplayFrequency(frequency);

        int splitFirstIndex = frequency.indexOf(RadioConfig.frequencySplitString);

        //if it doesn't have a split
        if(splitFirstIndex == -1)
            splitFirstIndex = frequency.length();

        //get main frequency now since it's used multiple times
        String mainFq = frequency.substring(0, splitFirstIndex);
        TextColor mainFqTextColor = CustomItemManager.getFrequencyTextColor(mainFq);

        TextComponent c = Component.text("<").color(mainFqTextColor);

        String[] split = frequency.split(RadioConfig.frequencySplitString);
        for(int i = 0; i < split.length; i++)
        {
            c = c.append(Component.text(split[i] + (i == split.length - 1 ? "" : RadioConfig.frequencySplitString))
                    .color(CustomItemManager.getFrequencyTextColor(split[i]))
            );

        }

        c = c.append(Component.text( "> ")).color(mainFqTextColor);

        return c;
    }

    public static BossBar.Color getBossBarColor(String frequency)
    {
        int splitFirstIndex = frequency.indexOf(RadioConfig.frequencySplitString);

        //if it doesn't have a split
        if(splitFirstIndex == -1)
            splitFirstIndex = frequency.length();

        String mainFq = frequency.substring(0, splitFirstIndex);

        try {
            return BossBar.Color.valueOf(mainFq);
        }
        catch (Exception e)
        {
            return BossBar.Color.WHITE;
        }
    }

    public static ChatFormatting getTextFormatColor(String frequency)
    {
        int splitFirstIndex = frequency.indexOf(RadioConfig.frequencySplitString);

        //if it doesn't have a split
        if(splitFirstIndex == -1)
            splitFirstIndex = frequency.length();

        String mainFq = frequency.substring(0, splitFirstIndex);

        try {
            //unfortunately, these colors don't match the ChatFormatting colors so we gotta do some mismatching
            return switch (mainFq)
            {
                case "LIGHT_GRAY" -> ChatFormatting.GRAY;
                case "GRAY" -> ChatFormatting.DARK_GRAY;
                case "BLACK" -> ChatFormatting.BLACK;
                case "BROWN" -> ChatFormatting.GOLD;
                case "RED" -> ChatFormatting.RED;
                case "ORANGE" -> ChatFormatting.GOLD;
                case "YELLOW" -> ChatFormatting.YELLOW;
                case "LIME" -> ChatFormatting.GREEN;
                case "GREEN" -> ChatFormatting.GREEN;
                case "CYAN" -> ChatFormatting.AQUA;
                case "LIGHT_BLUE" -> ChatFormatting.BLUE;
                case "BLUE" -> ChatFormatting.DARK_BLUE;
                case "PURPLE" -> ChatFormatting.DARK_PURPLE;
                case "MAGENTA" -> ChatFormatting.LIGHT_PURPLE;
                case "PINK" -> ChatFormatting.LIGHT_PURPLE;
                default -> ChatFormatting.WHITE;
            };
        }
        catch (Exception e)
        {
            return ChatFormatting.WHITE;
        }
    }

    public static TextComponent getColoredFrequencyMessage(String frequency, Player sender, Component message)
    {
        //almost the same as getColoredFrequencyTag but this also needs the colors from a couple more parts so It's reusing a lot of code

        //only used when displaying frequencies, not for logic
        frequency = convertToDisplayFrequency(frequency);

        int splitFirstIndex = frequency.indexOf(RadioConfig.frequencySplitString);

        //if it doesn't have a split
        if(splitFirstIndex == -1)
            splitFirstIndex = frequency.length();

        String mainFq = frequency.substring(0, splitFirstIndex);
        TextColor mainFqTextColor = CustomItemManager.getFrequencyTextColor(mainFq);

        TextComponent c = Component.text("<").color(mainFqTextColor);

        String[] split = frequency.split(RadioConfig.frequencySplitString);
        for(int i = 0; i < split.length; i++)
        {
            c = c.append(Component.text(split[i] + (i == split.length - 1 ? "" : RadioConfig.frequencySplitString))
                    .color(CustomItemManager.getFrequencyTextColor(split[i]))
            );

        }

        c = c.append(Component.text( "> ")).color(mainFqTextColor)
                .append(Component.text(sender.getName()).color(TextColor.color(CustomItemManager.getDulledFrequencyColor(split[split.length-1]).asRGB())))
                .append(Component.text(": ")
                        .append(message).color(TextColor.color(CustomItemManager.getDulledFrequencyColor(mainFq).asRGB())));;

        return c;
    }

    public static String convertToDisplayFrequency(String frequency)
    {
        StringBuilder displayFrequency = new StringBuilder();

        for(String str : frequency.split(RadioConfig.frequencySplitString))
        {
            String disp = FrequencyManager.dyeMap.get(str);

            if(disp == null) {
                displayFrequency.append(str).append(RadioConfig.frequencySplitString);
                continue;
            }

            displayFrequency.append(disp).append(RadioConfig.frequencySplitString);
        }

        displayFrequency.setLength(displayFrequency.length() - RadioConfig.frequencySplitString.length());

        return displayFrequency.toString();
    }

    public static Recipe getFrequencyIndependentShapedRecipe(NamespacedKey idKey, ItemStack result, List<String> shape, ConfigurationSection ingredients)
    {
        ShapedRecipe recipe = new ShapedRecipe(idKey, result);

        //get shape of recipe from config
        recipe.shape(shape.toArray(String[]::new));

        //allows for all dye types to be used in a slot
        RecipeChoice.MaterialChoice dyes = new RecipeChoice.MaterialChoice(MaterialTags.DYES);

        //defines the ingredients (the letters in the shape)
        if (ingredients != null) {
            for (String key : ingredients.getKeys(false)) {
                //just a basic material
                Material mat;
                try{
                    mat = Material.matchMaterial(ingredients.getString(key));
                }
                catch (Exception e)
                {
                    //material not found or something went wrong
                    Radio.logger.warning("Incorrectly registered Material For Radio basic recipe");
                    mat = Material.AIR;
                }
                if (mat != null) {
                    recipe.setIngredient(key.charAt(0), mat);
                }
            }
        }

        //add dyes
        for(int i = 0; i < 8; i++)
        {
            try
            {
                recipe.setIngredient((char)( i + '0'), dyes);
            }
            catch (Exception e)
            {
                //frequency not in recipe so exit
                break;
            }
        }

        return recipe;
    }

    public static String getFrequency(ItemStack item)
    {
        return item.getPersistentDataContainer().getOrDefault(radioFrequencyKey, PersistentDataType.STRING, "");
    }

    public static String getFrequency(Entity e)
    {
        return e.getPersistentDataContainer().getOrDefault(radioFrequencyKey, PersistentDataType.STRING, "");
    }

    public static int[] convertToIntFrequency(String frequency)
    {
        String[] split = frequency.split(RadioConfig.frequencySplitString);
        int[] arr = new int[split.length];

        //match the strings to their numbers
        for(int i = 0; i < split.length; i++)
        {
            arr[i] = numberedDyes.getOrDefault(split[i], 0);
        }

        return arr;
    }

    public static String convertIntToFrequency(int[] arr, int startIndex)
    {
        StringBuilder frequency = new StringBuilder();

        //match the strings to their numbers
        for(int i = startIndex; i < arr.length; i++)
        {
            frequency.append(numberedDyes.inverse().getOrDefault(arr[i], "WHITE"));
        }

        return frequency.toString();
    }

    public static ItemStack tagFrequency(ItemStack stack, String frequency)
    {
        stack.editPersistentDataContainer(pdc -> {
            pdc.set(FrequencyManager.radioFrequencyKey, PersistentDataType.STRING, frequency.toString());
        });

        stack.lore(List.of(Component.text(FrequencyManager.convertToDisplayFrequency(frequency))));

        return stack;
    }

    public static void sendToFrequency(UUID sender, byte[] audioData, String frequency, MicrophonePacket packet)
    {
        //send to speakers
        Speaker.sendMicrophonePacketToFrequency(sender, audioData, frequency);

        //send to field radios
        FieldRadioVoiceChat.sendPacketToFrequency(sender, audioData, frequency, packet);
    }

    public static void sendToFrequency(UUID sender, byte[] audioData, String frequency)
    {
        //send to speakers
        Speaker.sendMicrophonePacketToFrequency(sender, audioData, frequency);

        //does not send to field radios since those follow receiver's volume preferences for the sender which this cannot provide
    }
}
