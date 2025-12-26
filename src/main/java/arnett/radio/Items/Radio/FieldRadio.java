package arnett.radio.Items.Radio;

import arnett.radio.RadioConfig;
import arnett.radio.FrequencyManager;
import arnett.radio.Radio;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class FieldRadio {

    // shouldn't ever check the value of the key, only that the radio has it
    public static final NamespacedKey radioIdentifierKey = new NamespacedKey(Radio.singleton, "field_radio");

    //namspace key in resource pack for custom model
    public static final NamespacedKey radioModelKey = new NamespacedKey("radio", "field_radio");

    public static final Material RadioMaterial = Material.MUSIC_DISC_13;

    public static ArrayList<Recipe> getRecipes()
    {
        ArrayList<Recipe> recipes = new ArrayList<Recipe>();

        // Plain Radio
        if(RadioConfig.fieldRadio_recipe_basic_enabled)
            recipes.add(FrequencyManager.getFrequencyIndependentShapedRecipe(radioIdentifierKey, getRadio(),
                RadioConfig.fieldRadio_recipe_basic_shape, RadioConfig.fieldRadio_recipe_basic_ingredients));

        return  recipes;
    }

    public static ItemStack getRadio()
    {
        //creates Item (off of music disk because of minimal use cases)
        final ItemStack radio = ItemStack.of(RadioMaterial);

        //sets Item visuals
        radio.setData(DataComponentTypes.ITEM_NAME, Component.text("Field Radio"));
        radio.setData(DataComponentTypes.ITEM_MODEL, radioModelKey);

        //sets Item Component Data
        radio.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                        .consumeSeconds(Float.MAX_VALUE)
                        .animation(ItemUseAnimation.TOOT_HORN)
                        .hasConsumeParticles(false)
                        .build());

        radio.editMeta(meta -> {
            meta.getUseCooldown().setCooldownSeconds(5f);
        });

        //removes jukebox functionality
        radio.unsetData(DataComponentTypes.JUKEBOX_PLAYABLE);

        //Adds Identifier tag
        radio.editPersistentDataContainer(pdc -> {
            pdc.set(radioIdentifierKey, PersistentDataType.STRING, "field_radio");
        });

        return radio;
    }

    public static ItemStack getRadio(String frequency)
    {
        ItemStack radio = FieldRadio.getRadio();

        radio.editPersistentDataContainer(pdc -> {
            pdc.set(FrequencyManager.radioFrequencyKey, PersistentDataType.STRING, frequency);
        });

        radio.lore(List.of(Component.text(FrequencyManager.convertToDisplayFrequency(frequency))));

        return radio;
    }

    public static boolean isHoldingRadio(Player player)
    {
        return isRadio(player.getInventory().getItemInMainHand()) || isRadio(player.getInventory().getItemInOffHand());
    }

    public static Optional<ItemStack> getHeldRadio(Player player)
    {
        if (isRadio(player.getInventory().getItemInMainHand()))
            return Optional.of(player.getInventory().getItemInMainHand());

        else if (isRadio(player.getInventory().getItemInOffHand()))
            return  Optional.of(player.getInventory().getItemInOffHand());

        return Optional.empty();
    }

    public static boolean isRadio(ItemStack item)
    {
        //exit this IMEADITEALY if not related to radio through the fastest possible method
        if(item.getType() != RadioMaterial)
            return false;

        return item.getPersistentDataContainer().has(radioIdentifierKey, PersistentDataType.STRING);
    }

    public static boolean hasRadio(Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .anyMatch(FieldRadio::isRadio);
    }

    public static ItemStack[] getRadiosFromPlayer(Player player) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(FieldRadio::isRadio)
                .toArray(ItemStack[]::new);
    }

    public static ItemStack[] getRadiosFromPlayer(Player player, String frequency) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(FieldRadio::isRadio)
                .toArray(ItemStack[]::new);
    }

    public static Boolean matchingFrequencies(ItemStack radio1, ItemStack radio2)
    {
        return FrequencyManager.getFrequency(radio1).equals(FrequencyManager.getFrequency(radio2));
    }

    public static int getFrequencyColor(String frequency)
    {
        return  DyeColor.valueOf(frequency).getColor().asRGB();
    }

}
