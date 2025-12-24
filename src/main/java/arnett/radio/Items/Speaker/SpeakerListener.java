package arnett.radio.Items.Speaker;

import arnett.radio.FrequencyManager;
import arnett.radio.Items.Radio.FieldRadio;
import arnett.radio.RadioConfig;
import org.bukkit.Location;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SpeakerListener implements Listener {
    @EventHandler
    public void OnBlockPlaced(BlockPlaceEvent e)
    {
        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        //is the block the specified head type
        if(!e.getBlock().getType().equals(RadioConfig.speaker_block_headType))
            return;

        String frequency = e.getItemInHand().getPersistentDataContainer().getOrDefault(
                FrequencyManager.radioFrequencyKey, PersistentDataType.STRING, ""
        );

        //check if this has been tagged with a frequency, if not it's probably just a regular head
        if(frequency.isEmpty())
            return;

        //speaker has been placed
        Location blockLocation = e.getBlock().getLocation();

        //tag the chunk
        {
            PersistentDataContainer chunkPdc = e.getBlock().getChunk().getPersistentDataContainer();

            //add to the list
            List<int[]> locations = chunkPdc.get(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays());

            //if it is not yet tagged
            if(locations == null)
                locations = List.of();


            locations.add(new int[]{
                    blockLocation.getBlockX(),
                    blockLocation.getBlockY(),
                    blockLocation.getBlockZ()
            });

            //tag the chunk or update the tag
            chunkPdc.set(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays(), locations);
        }

        //add it to the list
        Speaker.addActiveSpeaker(blockLocation, frequency);
    }

    @EventHandler
    public void onItemCraftered(CrafterCraftEvent e)
    {
        if (!Speaker.isSpeaker(e.getRecipe().getResult()))
            //not radio recipe so skip
            return;

        ItemStack result = e.getResult();

        //returns what is put in the crafting interface
        ItemStack[] mtx = ((Crafter)e.getBlock().getState()).getInventory().getContents();

        //update result (tbh not sure if this is necessary)
        e.setResult(FrequencyManager.addFrequencyToCraft(result, mtx, RadioConfig.speaker_recipe_basic_shape));
    }

    @EventHandler
    public void onCraftPrepared(PrepareItemCraftEvent e)
    {
        if(e.getRecipe() == null)
            //invalid recipe so skip
            return;

        if (!Speaker.isSpeaker(e.getRecipe().getResult()))
            //not radio recipe so skip
            return;

        ItemStack result = e.getInventory().getResult();

        //returns what is put in the crafting interface
        ItemStack[] mtx = e.getInventory().getMatrix();

        //update result (tbh not sure if this is necessary)
        e.getInventory().setResult(FrequencyManager.addFrequencyToCraft(result, mtx, RadioConfig.speaker_recipe_basic_shape));
    }
}
