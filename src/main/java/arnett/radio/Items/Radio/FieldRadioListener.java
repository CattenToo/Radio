package arnett.radio.Items.Radio;

import arnett.radio.RadioConfig;
import arnett.radio.FrequencyManager;
import arnett.radio.Radio;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Keyed;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class FieldRadioListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onMessageSent(AsyncChatEvent e)
    {
        //todo config for disabling global chat

        Optional<ItemStack> sendingRadio = FieldRadio.getHeldRadio(e.getPlayer());

        //check for radio being held, if not, continue normally
        if(sendingRadio.isEmpty())
            return;

        //clears those without a radio (and non players)
        e.viewers().removeIf(audience -> {
            //only check for players
            if(!(audience instanceof Player player))
                //not a player
                return true;

            //check if they have radio
            ItemStack[] inventoryRadios = FieldRadio.getRadiosFromPlayer(player);

            for (ItemStack receivingRadio : inventoryRadios)
            {
                //frequency check
                if(FieldRadio.matchingFrequencies(sendingRadio.get(), receivingRadio))
                    return false;
            }

            //no match found
            return true;

        });

        String frequency = FrequencyManager.getFrequency(sendingRadio.get());

        //build a new message
        e.renderer((source, sourceDisplayName, message, viewer) ->
            FrequencyManager.getColoredFrequencyMessage(frequency, source, message));

    }

    @EventHandler
    public void onItemCraftered(CrafterCraftEvent e)
    {
        if (!FieldRadio.isRadio(e.getRecipe().getResult()))
            //not radio recipe so skip
            return;

        ItemStack result = e.getResult();

        //returns what is put in the crafting interface
        ItemStack[] mtx = ((Crafter)e.getBlock().getState()).getInventory().getContents();


        //basic craft
        if(e.getRecipe().getKey().equals(FieldRadio.radioCraftKey))
        {
            //update result (tbh not sure if this is necessary)
            e.setResult(FrequencyManager.addFrequencyToCraft(result, mtx, RadioConfig.fieldRadio_recipe_basic_shape));
        }

        //retuning
        else if(e.getRecipe().getKey().equals(FieldRadio.radioRetuneKey))
        {
            //update result (tbh not sure if this is necessary)
            e.setResult(FrequencyManager.addFrequencyToCraft(result, mtx));
        }

        //Rut-roh!
        else {
            Radio.logger.warning("COULD NOT FIND FIELD-RADIO CRAFTER RECIPE");
        }
    }

    @EventHandler
    public void onCraftPrepared(PrepareItemCraftEvent e)
    {
        if(e.getRecipe() == null)
            //invalid recipe so skip
            return;

        if (!FieldRadio.isRadio(e.getRecipe().getResult()))
            //not radio recipe so skip
            return;

        if(!(e.getRecipe() instanceof Keyed keyedRecipe))
            return;

        ItemStack result = e.getInventory().getResult();

        //returns what is put in the crafting interface
        ItemStack[] mtx = e.getInventory().getMatrix();

        if(keyedRecipe.getKey().equals(FieldRadio.radioCraftKey))
        {
            //update result
            e.getInventory().setResult(FrequencyManager.addFrequencyToCraft(result, mtx, RadioConfig.fieldRadio_recipe_basic_shape));
        }

        else if(keyedRecipe.getKey().equals(FieldRadio.radioRetuneKey))
        {
            //update result
            e.getInventory().setResult(FrequencyManager.addFrequencyToCraft(result, mtx));
        }

        //Rut-roh!
        else {
            Radio.logger.warning("COULD NOT FIND FIELD-RADIO CRAFT RECIPE");
        }
    }
}
