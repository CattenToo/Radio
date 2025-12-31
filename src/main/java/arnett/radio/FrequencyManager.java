package arnett.radio;

import arnett.radio.Items.CustomItemManager;
import arnett.radio.Items.Radio.FieldRadio;
import arnett.radio.Items.Radio.FieldRadioVoiceChat;
import arnett.radio.Items.Speaker.Speaker;
import com.destroystokyo.paper.MaterialTags;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.w3c.dom.Text;

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
            Radio.logger.info(split[i]);
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
                    Radio.logger.info("Incorrectly registered Material For Radio basic recipe");
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
        return item.getPersistentDataContainer().getOrDefault(radioFrequencyKey, PersistentDataType.STRING, "none");
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
}
