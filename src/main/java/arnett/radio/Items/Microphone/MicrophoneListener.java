package arnett.radio.Items.Microphone;

import arnett.radio.FrequencyManager;
import arnett.radio.Items.CustomItemManager;
import arnett.radio.Items.Speaker.Speaker;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import arnett.radio.RadioVoiceChat;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class MicrophoneListener implements Listener {
    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent e)
    {
        if(!Microphone.isMicrophone(e.getItemInHand()))
            return;

        String frequency = FrequencyManager.getFrequency(e.getItemInHand());

        //microphone has been placed

        //get the location
        Location placeSpot = e.getBlockPlaced().getLocation().add(RadioConfig.microphone_entity_displayOffset);

        // get block is the one it was placed on and block place is the block which was placed
        // so this gets the side it was placed on
        BlockFace face = e.getBlockPlaced().getFace(e.getBlockAgainst());
        boolean againstWall = !(face == BlockFace.DOWN || face == BlockFace.UP || face == BlockFace.SELF);
        NamespacedKey displayModel = againstWall ? Microphone.microphoneWallDisplayModelKey : Microphone.microphoneDisplayModelKey;

        Vector hitboxOffset = new Vector(0, -.5, 0);

        if(againstWall)
        {
            //set the rotation according to the block face placed on
            placeSpot.setYaw(switch (face){
                case NORTH -> 0f;
                case EAST -> 90f;
                case SOUTH -> 180f;
                case WEST -> 270f;
                default -> 0f;
            });

            // move the hitbox
            switch (face)
            {
                case NORTH -> hitboxOffset.setZ(-.4f);
                case EAST -> hitboxOffset.setX(.4f);
                case SOUTH -> hitboxOffset.setZ(.4f);
                case WEST -> hitboxOffset.setX(-.4f);
            }

            hitboxOffset.setY(-.25f);
        }
        else
        {
            //set the rotation according to player direction
            placeSpot.setYaw(switch (e.getPlayer().getFacing()){
                case NORTH -> 0f;
                case EAST -> 90f;
                case SOUTH -> 180f;
                case WEST -> 270f;
                default -> 0f;
            });
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
            box.setInteractionWidth(RadioConfig.microphone_entity_interactionWidth);
            box.setInteractionHeight(RadioConfig.microphone_entity_interactionHeight);


            //tag it with the UUID of the display for when it needs to be removed
            box.getPersistentDataContainer().set(CustomItemManager.entityLinkKey,
                    PersistentDataType.STRING,
                    display.getUniqueId().toString());

            //tag it as a mic
            box.getPersistentDataContainer().set(Microphone.microphoneIdentifierKey,
                    PersistentDataType.STRING,
                    "microphone");

            //tag it's frequency
            box.getPersistentDataContainer().set(FrequencyManager.radioFrequencyKey,
                    PersistentDataType.STRING,
                    frequency);
        });

        //cancel the actual placement
        e.getBlockPlaced().setType(Material.AIR);
    }

    @EventHandler
    public void onPlayerEntityInteraction(PlayerInteractEntityEvent e)
    {
        // did they interact with a microphone
        if(!e.getRightClicked().getPersistentDataContainer().has(Microphone.microphoneIdentifierKey))
            return;

        String frequency = FrequencyManager.getFrequency(e.getRightClicked());

        // are they attached to this frequency already
        if(Microphone.attachedPlayers.containsKey(e.getPlayer()) &&
                Microphone.attachedPlayers.get(e.getPlayer()).containsKey(frequency))
        {
            // is it this mic or another one
            if(Microphone.attachedPlayers.get(e.getPlayer()).get(frequency).equals(e.getRightClicked()))
            {
                // this mic so remove them
                Microphone.detachPlayerFromMicrophone(e.getPlayer(), frequency);
                return;
            }
            else {
                // they are attached to a different mic so just move them to this one
                Microphone.movePlayerToMicrophone(e.getPlayer(), frequency, e.getRightClicked());
            }
        }

        //it was a mic that was interacted
        //add the player to the mic list
        Microphone.attachPlayerToMicrophone(e.getPlayer(), frequency, e.getRightClicked());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        //was it the speaker which was damaged
        if(!e.getEntity().getPersistentDataContainer().has(Microphone.microphoneIdentifierKey))
            return;

        //was it by a player or explosion?
        if(!(e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) &&
                !(e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) &&
                !(e.getDamager() instanceof Player)){
            return;
        }

        //well it should be removed if it was one of the above, so

        String frequency = FrequencyManager.getFrequency(e.getEntity());

        //remove all attachments
        Microphone.removeMicrophoneAttachments(frequency, e.getEntity());

        //and delete the linked display
        String stringLinkedId = e.getEntity().getPersistentDataContainer().get(CustomItemManager.entityLinkKey, PersistentDataType.STRING);

        try {
            if(stringLinkedId != null)
                Bukkit.getEntity(UUID.fromString(stringLinkedId)).remove();
        }
        catch (Exception ex)
        {
            Radio.logger.warning("Could Not Remove Linked Microphone for UUID: " + stringLinkedId);
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
                Microphone.getMicrophone(FrequencyManager.getFrequency(e.getEntity()))
        );


    }

    public static void onMicrophone(MicrophonePacketEvent e)
    {
        //makes sure it's a player, which it obviously is, but mostly cast it to a bukkit player
        if (!(e.getSenderConnection().getPlayer().getPlayer() instanceof Player player))
            return;

        //is the player attached
        if(!Microphone.attachedPlayers.containsKey(player))
            return;



        UUID playerId = e.getSenderConnection().getPlayer().getUuid();

        //send it out to all attached
        //this is a minimal enough case where I think it's okay to loop through each entry
        Microphone.attachedPlayers.get(player).forEach((frequency, mic) -> {

            // get the data
            byte[] audioData = e.getPacket().getOpusEncodedData();

            //apply the filter if needed
            if(RadioConfig.microphone_audioFilter_enabled)
            {
                short[] filteredAudio = RadioVoiceChat.applyFilter(
                        RadioVoiceChat.getDecoder(playerId).decode(audioData),
                        RadioConfig.microphone_audioFilter_LPAlpha,
                        RadioConfig.microphone_audioFilter_HPAlpha,
                        RadioConfig.microphone_audioFilter_noiseFloor,
                        RadioConfig.microphone_audioFilter_crackleChance
                );

                //modify packet
                audioData = RadioVoiceChat.getEncoder(playerId).encode(filteredAudio);
            }


            //send it out
            FrequencyManager.sendToFrequency(
                    playerId,
                    audioData,
                    frequency,
                    e.getPacket()
            );
        });
    }

    //todo test
    @EventHandler
    public void onItemCraftered(CrafterCraftEvent e)
    {
        if (!Microphone.isMicrophone(e.getRecipe().getResult()))
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

        if (!Microphone.isMicrophone(e.getRecipe().getResult()))
            //not radio recipe so skip
            return;

        if(!(e.getRecipe() instanceof Keyed keyedRecipe))
            return;

        ItemStack result = e.getInventory().getResult();

        //returns what is put in the crafting interface
        ItemStack[] mtx = e.getInventory().getMatrix();

        if(keyedRecipe.getKey().equals(Microphone.microphoneCraftKey))
        {
            //update result
            e.getInventory().setResult(FrequencyManager.addFrequencyToCraft(result, mtx, RadioConfig.microphone_recipe_basic_shape));
        }

        else if(keyedRecipe.getKey().equals(Microphone.microphoneRetuneKey))
        {
            //update result
            e.getInventory().setResult(FrequencyManager.addFrequencyToCraft(result, mtx));
        }

        //Rut-roh!
        else {
            Radio.logger.warning("COULD NOT FIND MICROPHONE CRAFT RECIPE");
        }
    }
}
