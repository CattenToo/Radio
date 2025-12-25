package arnett.radio.Items.Speaker;

import arnett.radio.FrequencyManager;
import arnett.radio.Items.Radio.FieldRadio;
import arnett.radio.RadioConfig;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.Location;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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
        Speaker.tagChunkOfSpeaker(e.getBlock().getChunk(), blockLocation);

        //add it to the list
        Speaker.addActiveSpeaker(blockLocation, frequency);
    }

    //breakage events
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e)
    {
        // this is whenever a PLAYER breaks a block

        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        //Was it a Speaker block destroyed
        if(!Speaker.isBlockSpeaker(e.getBlock()))
            return;

        //Speaker block was destroyed
        Speaker.removeActiveSpeaker(e.getBlock().getLocation());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e)
    {
        // this is whenever an ENTITY breaks a block through an EXPLOSION

        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        e.blockList().forEach((block) -> {
            //Was it a Speaker block destroyed
            if(!Speaker.isBlockSpeaker(block))
                return;

            //Speaker block was destroyed
            Speaker.removeActiveSpeaker(block.getLocation());
        });
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e)
    {
        // this is whenever a BLOCK breaks another block through an EXPLOSION
        // (i.e. beds in nether/end because for some reason that's separate)

        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        e.blockList().forEach((block) -> {
            //Was it a Speaker block destroyed
            if(!Speaker.isBlockSpeaker(block))
                return;

            //Speaker block was destroyed
            Speaker.removeActiveSpeaker(block.getLocation());
        });
    }

    @EventHandler
    public void onBlockDestroyed(BlockDestroyEvent e)
    {
        // edge cases like piston breaking block, physics, and other things.
        // This is more so being used just in case, since while using the head type,
        // a lot of the things that trigger this don't apply

        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        //Was it a Speaker block destroyed
        if(!Speaker.isBlockSpeaker(e.getBlock()))
            return;

        //Speaker block was destroyed
        Speaker.removeActiveSpeaker(e.getBlock().getLocation());
    }

    // was going to do piston extend / retract checks but then realized that
    // heads just get destroyed so that's already covered by onBlockDestroyed

    //chunk loading and deloading check
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e)
    {
        if(!e.getChunk().getPersistentDataContainer().has(Speaker.speakerIdentifierKey))
            return;

        //tag is present
        int[] pos = e.getChunk().getPersistentDataContainer().get(Speaker.speakerIdentifierKey, PersistentDataType.INTEGER_ARRAY);

        // should always exist and equal three but im going to check anyway
        // maybe someone screwed with the world data, idk
        if(pos == null || pos.length != 3)
            return;

        //check location of the tag to see if there is a speaker there
        // the (# & 15) part is mostly equivalent to (# % 16) but faster since it's a bit mask
        if(!e.getChunk().getBlock(pos[0] & 15, pos[1], pos[2] & 15).getType().equals(RadioConfig.speaker_block_headType))
        {
            //add it to the list
            //todo add this and test the block removal events
        }
    }

    //todo this one was last second addition, needs double checking later
    //chunk loading and deloading check
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e)
    {
        if(!e.getChunk().getPersistentDataContainer().has(Speaker.speakerIdentifierKey))
            return;

        //tag is present
        int[] pos = e.getChunk().getPersistentDataContainer().get(Speaker.speakerIdentifierKey, PersistentDataType.INTEGER_ARRAY);

        // should always exist and equal three but im going to check anyway
        // maybe someone screwed with the world data, idk
        if(pos == null || pos.length != 3)
            return;

        // just remove it, don't need to check if it's the right block since
        // if nothing is present in the active list nothing will happen anyway
        Speaker.removeActiveSpeaker(new Location(e.getWorld(), pos[0], pos[1], pos[2]));
    }

    //todo test
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
