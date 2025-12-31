package arnett.radio.Items.Speaker;

import arnett.radio.FrequencyManager;
import arnett.radio.Items.CustomItemManager;
import arnett.radio.Items.Radio.FieldRadio;
import arnett.radio.Items.Radio.FieldRadioVoiceChat;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import arnett.radio.RadioVoiceChat;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import de.maxhenkel.voicechat.api.Entity;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.checkerframework.checker.units.qual.N;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public class SpeakerListener implements Listener {
    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent e)
    {
        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
        {
            if(!Speaker.isEntitySpeaker(e.getItemInHand()))
                return;

            String frequency = FrequencyManager.getFrequency(e.getItemInHand());

            //check if this has been tagged with a frequency, if not it's probably just a regular head
            if(frequency.isEmpty())
                return;

            onSpeakerEntityPlaced(e, frequency);
        }
        else {
            //is the block the specified head type
            if(!Speaker.isBlockSpeaker(e.getBlock()))
                return;

            String frequency = FrequencyManager.getFrequency(e.getItemInHand());

            //check if this has been tagged with a frequency, if not it's probably just a regular head
            if(frequency.isEmpty())
                return;

            onSpeakerBlockPlaced(e, frequency);
        }
    }

    private void onSpeakerBlockPlaced(BlockPlaceEvent e, String frequency)
    {
        //speaker has been placed
        Location blockLocation = e.getBlock().getLocation();

        //tag the chunk (to easily find later)
        Speaker.tagChunkOfSpeaker(e.getBlock().getChunk(), blockLocation, frequency);

        //add it to the list
        Speaker.addActiveSpeaker(blockLocation, frequency);
    }

    private void onSpeakerEntityPlaced(BlockPlaceEvent e, String frequency)
    {
        //speaker has been placed

        //get the location
        Location placeSpot = e.getBlockPlaced().getLocation().add(RadioConfig.speaker_entity_displayOffset);

        // get block is the one it was placed on and block place is the block which was placed
        // so this gets the side it was placed on
        BlockFace face = e.getBlockPlaced().getFace(e.getBlockAgainst());
        boolean againstWall = !(face == BlockFace.DOWN || face == BlockFace.UP || face == BlockFace.SELF);
        NamespacedKey displayModel = againstWall ? Speaker.speakerWallDisplayModelKey : Speaker.speakerDisplayModelKey;

        //set the rotation
        placeSpot.setYaw(switch (face){
            case NORTH -> 0f;
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        });

        Vector hitboxOffset = new Vector(0, -.5, 0);

        if(againstWall)
        {
            switch (face)
            {
                case NORTH -> hitboxOffset.setZ(-.4f);
                case EAST -> hitboxOffset.setX(.4f);
                case SOUTH -> hitboxOffset.setZ(.4f);
                case WEST -> hitboxOffset.setX(-.4f);
            }

            hitboxOffset.setY(-.25f);
        }

        //create the item display to show the item
        ItemDisplay display = placeSpot.getWorld().spawn(placeSpot, ItemDisplay.class, itemDisplay -> {
            // this needs an item stack to display so we create a new one
            ItemStack displayStack = new ItemStack(Material.STICK);
            // and then set the model
            displayStack.setData(DataComponentTypes.ITEM_MODEL, displayModel);
            // then display it
            itemDisplay.setItemStack(displayStack);
        });

        //create the hitbox so it can be interacted with
        //this is going to be the main entity
        Interaction hitbox = placeSpot.getWorld().spawn(placeSpot.add(hitboxOffset), Interaction.class, box ->{

            //set size
            box.setInteractionWidth(RadioConfig.speaker_entity_interactionWidth);
            box.setInteractionHeight(RadioConfig.speaker_entity_interactionHeight);


            //tag it with the UUID of the display for when it needs to be removed
            box.getPersistentDataContainer().set(CustomItemManager.entityLinkKey,
                    PersistentDataType.STRING,
                    display.getUniqueId().toString());

            //tag it as a speaker
            box.getPersistentDataContainer().set(Speaker.speakerIdentifierKey,
                    PersistentDataType.STRING,
                    "speaker");

            //tag it's frequency
            box.getPersistentDataContainer().set(FrequencyManager.radioFrequencyKey,
                    PersistentDataType.STRING,
                    frequency);
        });

        //add it to the speaker list since it was just placed
        Speaker.addActiveSpeaker(e.getBlockPlaced().getLocation(), frequency, RadioVoiceChat.api.fromEntity(hitbox));

        //cancel the actual placement
        e.getBlockPlaced().setType(Material.AIR);
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

        //wait one tick to untag it because of item drops
        Bukkit.getScheduler().scheduleSyncDelayedTask(Radio.singleton, () -> {
            //untag the chunk
            Speaker.untagChunkOfSpeaker(e.getBlock().getChunk(), e.getBlock().getLocation());

            //Speaker block was destroyed
            Speaker.removeActiveSpeaker(e.getBlock().getLocation());
        }, 1);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e)
    {
        // this is whenever an ENTITY breaks a block through an EXPLOSION

        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        e.blockList().removeIf((block) -> {
            //Was it a Speaker block destroyed
            if(!Speaker.isBlockSpeaker(block))
                return false;

            //wait one tick to untag it because of item drops
            Bukkit.getScheduler().scheduleSyncDelayedTask(Radio.singleton, () -> {
                //untag the chunk
                Speaker.untagChunkOfSpeaker(block.getChunk(), block.getLocation());

                //Speaker block was destroyed
                Speaker.removeActiveSpeaker(block.getLocation());
            }, 1);

            //clear the block
            block.setType(Material.AIR);

            //drop the speaker item
            block.getLocation().getWorld().dropItemNaturally(block.getLocation(), Speaker.getSpeaker(Speaker.getFrequencyOfSpeakerBlock(block.getLocation())));

            //don't drop the natural item (remove it from the list)
            return true;
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

        e.blockList().removeIf((block) -> {
            //Was it a Speaker block destroyed
            if(!Speaker.isBlockSpeaker(block))
                return false;

            //wait one tick to untag it because of item drops
            Bukkit.getScheduler().scheduleSyncDelayedTask(Radio.singleton, () -> {
                //untag the chunk
                Speaker.untagChunkOfSpeaker(block.getChunk(), block.getLocation());

                //Speaker block was destroyed
                Speaker.removeActiveSpeaker(block.getLocation());
            }, 1);

            //clear the block
            block.setType(Material.AIR);

            //drop the speaker item
            block.getLocation().getWorld().dropItemNaturally(block.getLocation(), Speaker.getSpeaker(Speaker.getFrequencyOfSpeakerBlock(block.getLocation())));

            //don't drop the natural item (remove it from the list)
            return true;
        });


    }

    @EventHandler
    public void onBlockDestroyed(BlockDestroyEvent e)
    {
        // tbh idk when this gets called, but it maybe does sometimes so might as well handle it.

        //are we even using blocks for this project
        if(RadioConfig.speaker_useEntity)
            return;

        //Was it a Speaker block destroyed
        if(!Speaker.isBlockSpeaker(e.getBlock()))
            return;

        //wait one tick to untag it because of item drops
        Bukkit.getScheduler().scheduleSyncDelayedTask(Radio.singleton, () -> {
            //untag the chunk
            Speaker.untagChunkOfSpeaker(e.getBlock().getChunk(), e.getBlock().getLocation());

            //Speaker block was destroyed
            Speaker.removeActiveSpeaker(e.getBlock().getLocation());
        }, 1);

        e.setWillDrop(false);

        //spawn it ourselves
        e.getBlock().getLocation().getWorld().dropItemNaturally(e.getBlock().getLocation(), Speaker.getSpeaker(Speaker.getFrequencyOfSpeakerBlock(e.getBlock().getLocation())));
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e)
    {
        //piston extends obviously

        e.getBlocks().forEach(block -> {
            //Was it a Speaker block destroyed
            if(!Speaker.isBlockSpeaker(block))
                return;

            //clear the block
            block.setType(Material.AIR);

            //drop the speaker item
            block.getLocation().getWorld().dropItemNaturally(block.getLocation(), Speaker.getSpeaker(Speaker.getFrequencyOfSpeakerBlock(block.getLocation())));
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

        //wait one tick to untag it because of item drops
        Bukkit.getScheduler().scheduleSyncDelayedTask(Radio.singleton, () -> {
            //untag the chunk
            Speaker.untagChunkOfSpeaker(e.getBlock().getChunk(), e.getBlock().getLocation());

            //Speaker block was destroyed
            Speaker.removeActiveSpeaker(e.getBlock().getLocation());
        }, 1);
    }




    //item drops
    //todo Water placed on directly breaks without drop, player breaks doesn't register frequency, break detections that are working don't add correct frequencies


    @EventHandler
    public void onPlayerBreakBlock(BlockDropItemEvent e)
    {
        //get block state used for broken block
        if(!Speaker.isBlockSpeaker(e.getBlockState().getType()))
            return;

        //tag the drop
        e.getItems().forEach(item -> {
            item.setItemStack(Speaker.getSpeaker(Speaker.getFrequencyOfSpeakerBlock(e.getBlock().getLocation())));
        });
    }


    @EventHandler
    public void onBlockBreakBlock(BlockBreakBlockEvent e)
    {
        if(!Speaker.isBlockSpeaker(e.getBlock()))
            return;

        //tag the drop
        e.getDrops().forEach(item -> {
            item = Speaker.getSpeaker(Speaker.getFrequencyOfSpeakerBlock(e.getBlock().getLocation()));
        });
    }


    //chunk loading



    //chunk loading and deloading check
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e)
    {
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


        for(int[] tag : speakers)
        {

            // btw tag is stored as the first three numbers being the location
            // and everything else being the numerical representation of the frequency

            //make sure it exists and isn't too small
            if(tag == null || tag.length < 3)
                return;

            //check location of the tag to see if there is a speaker there
            // the (# & 15) part is mostly equivalent to (# % 16) but faster since it's a bit mask
            if(Speaker.isBlockSpeaker(e.getChunk().getBlock(tag[0] & 15, tag[1], tag[2] & 15)))
            {
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


        for(int[] tag : speakers)
        {

            //make sure it exists and isn't too small
            if(tag == null || tag.length < 3)
                return;

            // just remove it, don't need to check if it's the right block since
            // if nothing is present in the active list nothing will happen anyway
            Speaker.removeActiveSpeaker(new Location(e.getWorld(), tag[0], tag[1], tag[2]));
        }
    }


    //entity loading


    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e)
    {

        //are we checking entities
        if(!RadioConfig.speaker_useEntity)
            return;

        //is an entity tagged with the identifier
        e.getEntities().forEach(entity -> {
            if(entity.getPersistentDataContainer().has(Speaker.speakerIdentifierKey))
            {
                //it is a speaker so we should add it to the active list
                //thankfully, since it's an entity, it already has the stored frequency so no issue there

                Speaker.addActiveSpeaker(

                        entity.getLocation(),

                        entity.getPersistentDataContainer().get(FrequencyManager.radioFrequencyKey, PersistentDataType.STRING),

                        //this converts it to the vc entity
                        RadioVoiceChat.api.fromEntity(entity)
                );

            }
        });
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent e)
    {

        //are we checking entities
        if(!RadioConfig.speaker_useEntity)
            return;

        //is an entity tagged with the identifier
        e.getEntities().forEach(entity -> {
            if(entity.getPersistentDataContainer().has(Speaker.speakerIdentifierKey))
            {

                //writing this was SOOO much simpler than for blocks :)
                Speaker.removeActiveSpeaker(
                        RadioVoiceChat.api.fromEntity(entity)
                );
            }
        });
    }


    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        //was it the speaker which was damaged
        if(!e.getEntity().getPersistentDataContainer().has(Speaker.speakerIdentifierKey))
            return;

        //was it by a player or explosion?
        if(!(e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) &&
                !(e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) &&
                !(e.getDamager() instanceof Player player)){
            return;
        }

        //well it should be removed if it was one of the above, so
        //remove it
        Speaker.removeActiveSpeaker(RadioVoiceChat.api.fromEntity(e.getEntity()));

        //and delete the linked display
        String stringLinkedId = e.getEntity().getPersistentDataContainer().get(CustomItemManager.entityLinkKey, PersistentDataType.STRING);

        try {
            if(stringLinkedId != null)
                Bukkit.getEntity(UUID.fromString(stringLinkedId)).remove();
        }
        catch (Exception ex)
        {
            Radio.logger.warning("Could Not Remove Linked Speaker for UUID: " + stringLinkedId);
        }

        //remove the entity
        e.getEntity().remove();

        Location centerSpot = e.getEntity().getLocation();

        //show particles
        centerSpot.getWorld().spawnParticle(Particle.BLOCK, centerSpot, 30, 0.3, 0.3, 0.3, Material.BLACKSTONE.createBlockData());

        //play the break sound
        centerSpot.getWorld().playSound(centerSpot, Sound.BLOCK_DEEPSLATE_BRICKS_BREAK, 1f, 1f);

        //drop the item
        centerSpot.getWorld().dropItemNaturally(
                centerSpot,
                Speaker.getSpeaker(e.getEntity().getPersistentDataContainer().get(
                        FrequencyManager.radioFrequencyKey, PersistentDataType.STRING
                ))
        );
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

        //basic craft
        if(e.getRecipe().getKey().equals(Speaker.speakerCraftKey))
        {
            //update result
            e.setResult(FrequencyManager.addFrequencyToCraft(result, mtx, RadioConfig.speaker_recipe_basic_shape));
        }

        //retuning
        else if(e.getRecipe().getKey().equals(Speaker.speakerRetuneKey))
        {
            //update result
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

        if (!Speaker.isSpeaker(e.getRecipe().getResult()))
            //not radio recipe so skip
            return;

        if(!(e.getRecipe() instanceof Keyed keyedRecipe))
            return;

        ItemStack result = e.getInventory().getResult();

        //returns what is put in the crafting interface
        ItemStack[] mtx = e.getInventory().getMatrix();

        if(keyedRecipe.getKey().equals(Speaker.speakerCraftKey))
        {
            //update result
            e.getInventory().setResult(FrequencyManager.addFrequencyToCraft(result, mtx, RadioConfig.fieldRadio_recipe_basic_shape));
        }

        else if(keyedRecipe.getKey().equals(Speaker.speakerRetuneKey))
        {
            //update result
            e.getInventory().setResult(FrequencyManager.addFrequencyToCraft(result, mtx));
        }

        //Rut-roh!
        else {
            Radio.logger.warning("COULD NOT FIND SPEAKER CRAFT RECIPE");
        }
    }

}
