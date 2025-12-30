package arnett.radio.Items;

import arnett.radio.Items.Speaker.Speaker;
import arnett.radio.RadioConfig;
import arnett.radio.Items.Speaker.SpeakerListener;
import arnett.radio.Radio;
import arnett.radio.FrequencyManager;
import arnett.radio.Items.Radio.FieldRadio;
import arnett.radio.Items.Radio.FieldRadioListener;
import arnett.radio.Items.Radio.FieldRadioVoiceChatListener;
import arnett.radio.RadioVoiceChat;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;


// tbh this class isn't used that much, but I'm keeping because I want to
// and because it looks better for when there are more items
public class CustomItemManager {

    public static void registerItemEvents(JavaPlugin plugin)
    {
        //radio
        plugin.getServer().getPluginManager().registerEvents(new FieldRadioListener(), plugin);
    }

    public static void registerVoiceChatItemEvents(JavaPlugin plugin)
    {
        //radio
        plugin.getServer().getPluginManager().registerEvents(new FieldRadioVoiceChatListener(), plugin);

        //speaker
        plugin.getServer().getPluginManager().registerEvents(new SpeakerListener(), plugin);
    }

    public static void registerRecipies()
    {
        //radio
        for(Recipe r : FieldRadio.getRecipes())
        {
            if(r instanceof Keyed keyedRecipe)
                Radio.logger.info("Added Recipe: " + keyedRecipe.getKey());
            else
                Radio.logger.info("Added Recipe for field radio" );
            Bukkit.addRecipe(r);
        }

        //speaker
        for(Recipe r : Speaker.getRecipes())
        {
            if(r instanceof Keyed keyedRecipe)
                Radio.logger.info("Added Recipe: " + keyedRecipe.getKey());
            else
                Radio.logger.info("Added Recipe for speaker" );
            Bukkit.addRecipe(r);
        }
    }

    //returns recipe without choice items
    public static String[] getIndependentRecipe(String[] recipe)
    {
        StringBuilder newRecipeLine = new StringBuilder();

        for(int i = 0; i < recipe.length; i++)
        {
            for (char c : recipe[i].toCharArray())
            {
                if(Character.isDigit(c))
                    newRecipeLine.append(" ");
                else
                    newRecipeLine.append(c);
            }

            recipe[i] = newRecipeLine.toString();
            newRecipeLine.setLength(0);
        }

        return recipe;
    }

    public static TextColor getFrequencyTextColor(String subFrequency)
    {
        return TextColor.color(getFrequencyColor(subFrequency).asRGB());
    }

    //runs using display frequencies
    public static Color getFrequencyColor(String subFrequency)
    {
        String dye = FrequencyManager.dyeMap.inverse().get(subFrequency);

        try
        {
            if(RadioConfig.frequencyRepresentationDyes.getString(dye).equals(subFrequency))
            {
                return DyeColor.valueOf(dye).getColor();
            }
        }
        catch (Exception ignored){
            Radio.logger.info(subFrequency + " Fialed to get color: " + dye);
        }

        return Color.WHITE;
    }

    public static Color getDulledFrequencyColor(String subFrequency)
    {
        return getFrequencyColor(subFrequency).mixColors(Color.WHITE);
    }

    public static void reload() {
        //refresh recipes
        //field radio
        for(NamespacedKey r : FieldRadio.getRecipekeys())
        {
            Bukkit.removeRecipe(r);
        }

        //speaker
        for(NamespacedKey r : Speaker.getRecipekeys())
        {
            Bukkit.removeRecipe(r);
        }

        //re add them
        registerRecipies();
    }
}
