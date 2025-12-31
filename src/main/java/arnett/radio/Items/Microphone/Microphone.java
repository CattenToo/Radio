package arnett.radio.Items.Microphone;

import arnett.radio.FrequencyManager;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import arnett.radio.RadioVoiceChat;
import com.destroystokyo.paper.MaterialTags;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.BuildableComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Microphone {
    //used both to identify microphone items and chunks that have microphones
    public static final NamespacedKey microphoneIdentifierKey = new NamespacedKey(Radio.singleton, "microphone");

    public static final NamespacedKey microphoneCraftKey = new NamespacedKey(Radio.singleton, "microphone_craft");
    public static final NamespacedKey microphoneRetuneKey = new NamespacedKey(Radio.singleton, "microphone_retune");

    public static final NamespacedKey microphoneModelKey = new NamespacedKey("radio", "microphone");
    public static final NamespacedKey microphoneDisplayModelKey = new NamespacedKey("radio", "microphone_display");
    public static final NamespacedKey microphoneWallDisplayModelKey = new NamespacedKey("radio", "microphone_wall_display");

    //this list should be relativity small since it only track player attached to microphones
    public static HashMap<UUID, Entity> attachedPlayers = new HashMap<>();
    static HashMap<String, BossBar> indicatorBarDisplays = new HashMap<>();

    public static ArrayList<Recipe> getRecipes()
    {
        ArrayList<Recipe> recipes = new ArrayList<Recipe>();

        // Plain Microphone
        if(RadioConfig.microphone_recipe_basic_enabled)
            recipes.add(FrequencyManager.getFrequencyIndependentShapedRecipe(microphoneCraftKey, getMicrophone(),
                    RadioConfig.microphone_recipe_basic_shape, RadioConfig.microphone_recipe_basic_ingredients));

        //retuning
        if(RadioConfig.microphone_recipe_retune_enabled)
        {
            ShapelessRecipe recipe = new ShapelessRecipe(microphoneRetuneKey, getMicrophone());

            List<String> ingredients = RadioConfig.microphone_recipe_retune_ingredients;

            ingredients.forEach(i -> {
                //special case Microphone
                if(i.equals("MICROPHONE"))
                    recipe.addIngredient(RadioConfig.microphone_entity_baseMaterial);

                    //special case DYE for frequency
                else if (i.equals("DYE"))
                    recipe.addIngredient(new RecipeChoice.MaterialChoice(MaterialTags.DYES));

                else {
                    try {
                        recipe.addIngredient(Material.matchMaterial(i));
                    }
                    catch (Exception e)
                    {
                        Radio.logger.warning("Invalid material provided for Microphone retune recipe: " + i);
                    }
                }
            });

            recipes.add(recipe);
        }
        return  recipes;
    }

    public static NamespacedKey[] getRecipeKeys()
    {
        return new NamespacedKey[]{microphoneCraftKey, microphoneRetuneKey};
    }

    public static ItemStack getMicrophone(String frequency)
    {
        ItemStack speaker = getMicrophone();

        //set frequency
        speaker.editPersistentDataContainer(pdc -> {
            pdc.set(FrequencyManager.radioFrequencyKey, PersistentDataType.STRING, frequency);
        });

        speaker.lore(List.of(Component.text(FrequencyManager.convertToDisplayFrequency(frequency))));

        return speaker;
    }

    public static ItemStack getMicrophone()
    {
        ItemStack microphone = new ItemStack(RadioConfig.microphone_entity_baseMaterial);

        //sets Item visuals
        microphone.setData(DataComponentTypes.ITEM_NAME, Component.text("Microphone", NamedTextColor.YELLOW));
        microphone.setData(DataComponentTypes.ITEM_MODEL, microphoneModelKey);

        //Adds Identifier tag
        microphone.editPersistentDataContainer(pdc -> {
            pdc.set(microphoneIdentifierKey, PersistentDataType.STRING, "microphone");
        });

        //removes jukebox functionality
        microphone.unsetData(DataComponentTypes.JUKEBOX_PLAYABLE);

        microphone.setData(DataComponentTypes.ITEM_NAME, Component.text("Microphone"));

        return microphone;
    }


    public static boolean isMicrophone(ItemStack item)
    {
        if(item.getType() != RadioConfig.microphone_entity_baseMaterial)
            return false;

        return item.getPersistentDataContainer().has(microphoneIdentifierKey, PersistentDataType.STRING);
    }

    public static boolean isMicrophone(Block block)
    {
        return block.getType() == RadioConfig.microphone_entity_baseMaterial;
    }


    public static void attachPlayerToMicrophone(UUID player, Entity mic)
    {
        if (attachedPlayers.containsKey(player))
            //already attached
            return;

        String frequency = FrequencyManager.getFrequency(mic);

        //get the bar or create it if it is new
        BossBar indicator = indicatorBarDisplays.computeIfAbsent(
                frequency, (e) ->
                BossBar.bossBar(
                        Component.text("Connected To: ").append(FrequencyManager.getColoredFrequencyTag(e)),
                        1f,
                        FrequencyManager.getBossBarColor(e),
                        BossBar.Overlay.NOTCHED_12
                ));

        //store the bar for later
        indicatorBarDisplays.put(frequency, indicator);

        try {
            // show the bar
            Bukkit.getPlayer(player).showBossBar(indicator);
        }
        catch (Exception ignored){};

        attachedPlayers.put(player, mic);
    }

    public static void detachPlayerFromMicrophone(UUID player)
    {
        try {
            Bukkit.getPlayer(player).hideBossBar(indicatorBarDisplays.get(attachedPlayers.get(player)));
        }
        catch (Exception ignore){
        }

        attachedPlayers.remove(player);
    }

    public static void removeMicrophoneAttachments(Entity mic) {
        attachedPlayers.entrySet().removeIf((entry) ->{
            Bukkit.getPlayer(entry.getKey()).hideBossBar(indicatorBarDisplays.get(FrequencyManager.getFrequency(mic)));
            return entry.getValue().equals(mic);
        });
    }
}
