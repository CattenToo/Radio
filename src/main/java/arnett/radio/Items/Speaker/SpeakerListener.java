package arnett.radio.Items.Speaker;

import arnett.radio.FrequencyManager;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SpeakerListener implements Listener {
    @EventHandler
    public void OnBlockPlaced(BlockPlaceEvent e)
    {
        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        //is the block the specified head type
        if(!Speaker.isBlockSpeaker(e.getBlock()))
            return;

        String frequency = FrequencyManager.getFrequency(e.getItemInHand());

        //check if this has been tagged with a frequency, if not it's probably just a regular head
        if(frequency.isEmpty())
            return;

        //speaker has been placed
        Location blockLocation = e.getBlock().getLocation();

        //tag the chunk (to easily find later)
        Speaker.tagChunkOfSpeaker(e.getBlock().getChunk(), blockLocation, frequency);

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

        //untag the chunk
        Speaker.untagChunkOfSpeaker(e.getBlock().getChunk(), e.getBlock().getLocation());

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

            //untag the chunk
            Speaker.untagChunkOfSpeaker(block.getChunk(), block.getLocation());

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

            //untag the chunk
            Speaker.untagChunkOfSpeaker(block.getChunk(), block.getLocation());

            //Speaker block was destroyed
            Speaker.removeActiveSpeaker(block.getLocation());
        });
    }

    @EventHandler
    public void onBlockDestroyed(BlockDestroyEvent e)
    {
        // tbh idk when this gets called, but it maybe does some times so might as well handle it.

        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        //Was it a Speaker block destroyed
        if(!Speaker.isBlockSpeaker(e.getBlock()))
            return;

        //untag the chunk
        Speaker.untagChunkOfSpeaker(e.getBlock().getChunk(), e.getBlock().getLocation());

        //Speaker block was destroyed
        Speaker.removeActiveSpeaker(e.getBlock().getLocation());

        //tbh i'm not sure when this will even get called, so I'm not dropping an item for this
        e.setWillDrop(false);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e)
    {
        //piston extends obviously

        e.getBlocks().forEach(block -> {
            //Was it a Speaker block destroyed
            if(!Speaker.isBlockSpeaker(block))
                return;

            //untag the chunk
            Speaker.untagChunkOfSpeaker(block.getChunk(), block.getLocation());

            //Speaker block was destroyed
            Speaker.removeActiveSpeaker(block.getLocation());
        });
    }

    @EventHandler
    public void onBlockReplace(BlockFromToEvent e)
    {
        //block is replaced by another, like when water breaks redstone

        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        //Was it a Speaker block destroyed
        if(!Speaker.isBlockSpeaker(e.getToBlock()))
            return;

        //untag the chunk
        Speaker.untagChunkOfSpeaker(e.getToBlock().getChunk(), e.getToBlock().getLocation());

        //Speaker block was destroyed
        Speaker.removeActiveSpeaker(e.getToBlock().getLocation());

    }




    //item drops



    @EventHandler
    public void onPlayerBreakBlock(BlockDropItemEvent e)
    {
        if(!Speaker.isBlockSpeaker(e.getBlock()))
            return;

        //tag the drop
        e.getItems().forEach(item -> {
            FrequencyManager.tagFrequency(item.getItemStack(), Speaker.getFrequencyOfSpeakerBlock(e.getBlock()));
        });
    }


    @EventHandler
    public void onBlockBreakBlock(BlockBreakBlockEvent e)
    {
        if(!Speaker.isBlockSpeaker(e.getBlock()))
            return;

        //tag the drop
        e.getDrops().forEach(item -> {
            FrequencyManager.tagFrequency(item, Speaker.getFrequencyOfSpeakerBlock(e.getBlock()));
        });
    }


    //chunk loading




    //chunk loading and deloading check
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e)
    {
        if(!e.getChunk().getPersistentDataContainer().has(Speaker.speakerIdentifierKey))
            return;

        Radio.logger.info("Found Tagged Chunk on LOAD");

        //tag is present
        List<int[]> speakers = e.getChunk().getPersistentDataContainer().get(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays());

        if(speakers == null || speakers.isEmpty())
        {
            //some error happened in storage, anyway, there's no speakers so remove it
            e.getChunk().getPersistentDataContainer().remove(Speaker.speakerIdentifierKey);
            return;
        }

        Radio.logger.info("Found Speaker Data");

        for(int[] tag : speakers)
        {
            Radio.logger.info("checking tags");

            // btw tag is stored as the first three numbers being the location
            // and everything else being the numerical representation of the frequency

            //make sure it exists and isn't too small
            if(tag == null || tag.length < 3)
                return;

            Radio.logger.info(e.getChunk().getBlock(tag[0] & 15, tag[1], tag[2] & 15).getType().name());
            //check location of the tag to see if there is a speaker there
            // the (# & 15) part is mostly equivalent to (# % 16) but faster since it's a bit mask
            if(Speaker.isBlockSpeaker(e.getChunk().getBlock(tag[0] & 15, tag[1], tag[2] & 15)))
            {
                Radio.logger.info("found");
                //get the frequency
                String frequency = FrequencyManager.convertIntToFrequency(tag, 3);

                //add it to the list
                Speaker.addActiveSpeaker(new Location(e.getWorld(), tag[0], tag[1], tag[2]), frequency);
            }
        }
    }

    //todo this one was last second addition, needs double checking later
    //chunk loading and deloading check
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e)
    {
        if(!e.getChunk().getPersistentDataContainer().has(Speaker.speakerIdentifierKey))
            return;

        Radio.logger.info("Found Tagged Chunk on UNLOAD");

        if(!e.getChunk().getPersistentDataContainer().has(Speaker.speakerIdentifierKey))
            return;

        //tag is present
        List<int[]> speakers = e.getChunk().getPersistentDataContainer().get(Speaker.speakerIdentifierKey, PersistentDataType.LIST.integerArrays());

        if(speakers == null || speakers.isEmpty())
        {
            //some error happened in storage, anyway, there's no speakers so remove it
            e.getChunk().getPersistentDataContainer().remove(Speaker.speakerIdentifierKey);
            return;
        }

        Radio.logger.info("Found Speaker Data");

        for(int[] tag : speakers)
        {
            Radio.logger.info("checking tags");

            //make sure it exists and isn't too small
            if(tag == null || tag.length < 3)
                return;

            // just remove it, don't need to check if it's the right block since
            // if nothing is present in the active list nothing will happen anyway
            Speaker.removeActiveSpeaker(new Location(e.getWorld(), tag[0], tag[1], tag[2]));
        }
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
