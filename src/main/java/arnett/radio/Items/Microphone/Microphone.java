package arnett.radio.Items.Microphone;

import arnett.radio.FrequencyManager;
import arnett.radio.Items.CustomItemManager;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import com.destroystokyo.paper.MaterialTags;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Microphone {
    //used both to identify microphone items and chunks that have microphones
    public static final NamespacedKey microphoneIdentifierKey = new NamespacedKey(Radio.singleton, "microphone");

    public static final NamespacedKey microphoneCraftKey = new NamespacedKey(Radio.singleton, "microphone_craft");
    public static final NamespacedKey microphoneRetuneKey = new NamespacedKey(Radio.singleton, "microphone_retune");

    public static final NamespacedKey microphoneModelKey = new NamespacedKey("radio", "microphone");
    public static final NamespacedKey microphoneDisplayModelKey = new NamespacedKey("radio", "microphone_display");
    public static final NamespacedKey microphoneWallDisplayModelKey = new NamespacedKey("radio", "microphone_wall_display");

    //this list should be relativity small since it only track player attached to microphones
    //hashmap in hashmap for the faster lookup time
    //to reduce overhead, when created in "attachPlayerToMicrophone" the HashMap is created with expected size of 3
    public static HashMap<Player, HashMap<String, Pair<Entity, BukkitTask>>> attachedPlayers = new HashMap<>();

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


    public static void attachPlayerToMicrophone(Player player, String frequency, Entity mic)
    {

        if (attachedPlayers.containsKey(player) && attachedPlayers.get(player).containsKey(frequency))
            //already attached to this frequency
            return;


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
            player.showBossBar(indicator);
        }
        catch (Exception ignored){};

        //add to attachments, will replace if already there and create a new entry if player isn't yet present
        attachedPlayers.computeIfAbsent(player, (p) -> new HashMap<>(3))
                .put(frequency, createEntityDistanceCheckerPair(player, mic));

        //link the player and the mic with a lead
        setLinkToMic(player, mic, frequency, true);
    }

    public static void movePlayerToMicrophone(Player player, String frequency, Entity mic)
    {
        HashMap<String, Pair<Entity, BukkitTask>> connectionsMap = attachedPlayers.get(player);
        detachPlayerFromMicrophone(player, frequency, connectionsMap.get(frequency).getLeft());
        attachPlayerToMicrophone(player, frequency, mic);
    }

    public static void detachPlayerFromMicrophone(Player player, String frequency, Entity mic)
    {
        //hide the boss bar if possible
        try {
            player.hideBossBar(indicatorBarDisplays.get(frequency));
        }
        catch (Exception ignore){
        }

        //remove the player if they're in the list
        if(attachedPlayers.containsKey(player))
        {
            HashMap<String, Pair<Entity, BukkitTask>> connectionMap = attachedPlayers.get(player);

            //cancel the runnable
            connectionMap.get(frequency).getRight().cancel();

            //remove the mic
            connectionMap.remove(frequency);

            //remove the player if they're no longer attached to anything
            if(connectionMap.isEmpty())
                attachedPlayers.remove(player);
        }

        //remove the link
        setLinkToMic(player, mic, frequency, false);
    }

    public static void removeMicrophoneAttachments(String frequency, Entity mic) {
        //check all entries to see if they are of this mic, if so remove them
        //normally this would be rough, but it's not something called often so it should be fine
        attachedPlayers.entrySet().removeIf((entry) ->{

            entry.getValue().entrySet().removeIf(e -> {
                if(e.getValue().getLeft().equals(mic))
                {
                    //player is attached to this mic

                    //hide the boss bar indicator
                    entry.getKey().hideBossBar(indicatorBarDisplays.get(frequency));

                    //stop the runnable
                    e.getValue().getRight().cancel();

                    //remove them from this frequency
                    return true;
                }

                return false;
            });

            return entry.getValue().isEmpty();
        });

        //all links would be removed with the entity's removal so no need to worry about that
    }

    public static boolean isAttached(Player player, String frequency)
    {
        return attachedPlayers.containsKey(player) && attachedPlayers.get(player).containsKey(frequency);
    }

    public static void setLinkToMic(Player player, Entity mic, String frequency, boolean linked)
    {
        if(!mic.getPersistentDataContainer().has(CustomItemManager.entityLinkKey))
            return;

        try {
            //get the display entity
            Entity dispalyEntity = Bukkit.getEntity(
                    UUID.fromString(
                            mic.getPersistentDataContainer().get(CustomItemManager.entityLinkKey, PersistentDataType.STRING)
                    )
            );

            CustomItemManager.setGlowForPlayer(player, dispalyEntity, linked, FrequencyManager.getTextFormatColor(frequency));
        }
        //was already removed somehow or can't find link
        catch (Exception ignored){}

        //play link sound
        mic.getLocation().getWorld().playSound(mic.getLocation(), linked? Sound.BLOCK_AMETHYST_BLOCK_RESONATE : Sound.BLOCK_AMETHYST_BLOCK_STEP, .5f, 1.5f);

    }

    public static Pair<Entity, BukkitTask> createEntityDistanceCheckerPair(Player player, Entity mic)
    {
        return Pair.of(mic, new BukkitRunnable() {
            @Override
            public void run() {
                //if the entity was removed somehow
                if(mic == null || player == null)
                    cancel();

                if(player.getLocation().distanceSquared(mic.getLocation()) > RadioConfig.microphone_squaredUseRange)
                {
                    //detach them
                    detachPlayerFromMicrophone(player, FrequencyManager.getFrequency(mic) ,mic);

                    //stop the distance checker
                    cancel();
                }
            }
        }.runTaskTimer(Radio.singleton, 0L, 10L));
    }
}
