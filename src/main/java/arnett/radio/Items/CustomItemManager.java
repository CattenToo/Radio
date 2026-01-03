package arnett.radio.Items;

import arnett.radio.Items.Microphone.Microphone;
import arnett.radio.Items.Microphone.MicrophoneListener;
import arnett.radio.Items.Speaker.Speaker;
import arnett.radio.RadioConfig;
import arnett.radio.Items.Speaker.SpeakerListener;
import arnett.radio.Radio;
import arnett.radio.Frequencies.FrequencyManager;
import arnett.radio.Items.Radio.FieldRadio;
import arnett.radio.Items.Radio.FieldRadioListener;
import arnett.radio.Items.Radio.FieldRadioVoiceChatListener;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.*;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;


// tbh this class isn't used that much, but I'm keeping because I want to
// and because it looks better for when there are more items
public class CustomItemManager {

    public static final NamespacedKey entityLinkKey = new NamespacedKey("radio", "link");

    // accessor to get an entity's data
    // index zero are the entity flags
    // it's stored in binary so read it as a byte
    static EntityDataAccessor<Byte> entityFlagAccessor = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
    static Scoreboard glowScoreboard = new Scoreboard();

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

        //microphone
        plugin.getServer().getPluginManager().registerEvents(new MicrophoneListener(), plugin);
    }

    public static void registerRecipes()
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

        //microphone
        for(Recipe r : Microphone.getRecipes())
        {
            if(r instanceof Keyed keyedRecipe)
                Radio.logger.info("Added Recipe: " + keyedRecipe.getKey());
            else
                Radio.logger.info("Added Recipe for microphone" );
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

        //microphone
        for(NamespacedKey r : Microphone.getRecipeKeys())
        {
            Bukkit.removeRecipe(r);
        }

        //re add them
        registerRecipes();
    }

    public static void setGlowForPlayer(Player player, Entity e, boolean doGlow, ChatFormatting color)
    {
        //make sure entity is present
        if(e == null)
            return;

        ServerPlayer cPlayer = ((CraftPlayer)player).getHandle();
        net.minecraft.world.entity.Entity cEntity = ((CraftEntity)e).getHandle();

        //grab the data
        SynchedEntityData entityData = cEntity.getEntityData();

        //this contains the flags of the entity
        byte flags = entityData.get(entityFlagAccessor);

        // change the flag with a mask
        // 0x40 = 01000000

        if(doGlow)
            flags |= 0x40;
        else
            flags &= ~0x40;

        //data values which will be sent to the player
        List<SynchedEntityData.DataValue<?>> glowingData = List.of(SynchedEntityData.DataValue.create(entityFlagAccessor, flags));

        //create custom team
        PlayerTeam glowTeam = new PlayerTeam(glowScoreboard, (color.getName() + "_Glow"));

        //set the color of the team (which will be the glow color)
        glowTeam.setColor(color);

        //tell client to create or remove team
        cPlayer.connection.send(
                // ...Packet(team, add/update[true] or remove[false])
                ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(glowTeam, doGlow)
        );

        //add the display to the team
        cPlayer.connection.send(
                //this kinda just pretends like the entity is a player and ships it's uuid instead
                ClientboundSetPlayerTeamPacket.createPlayerPacket(
                        glowTeam,
                        e.getUniqueId().toString(),
                        doGlow ? ClientboundSetPlayerTeamPacket.Action.ADD : ClientboundSetPlayerTeamPacket.Action.REMOVE
                )
        );

        //ship glow to the player
        cPlayer.connection.send(
                new ClientboundSetEntityDataPacket(
                    e.getEntityId(),
                    glowingData
        ));


        //set it back for everyone else
        flags &= ~0x40;
        entityData.set(entityFlagAccessor, flags);
    }
}
