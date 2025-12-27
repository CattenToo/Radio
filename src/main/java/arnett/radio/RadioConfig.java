package arnett.radio;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

//yeah, this system is much better
//basically, all this does is allow for ease of use when coding
public class RadioConfig {

    // Top level
    public static boolean enabled;

    public static ConfigurationSection frequencyRepresentationDyes;
    public static String frequencySplitString;

    //field radio
        //  Audio Filter
        public static boolean fieldRadio_audioFilter_enabled;
        public static double fieldRadio_audioFilter_LPAlpha;
        public static double fieldRadio_audioFilter_HPAlpha;
        public static int fieldRadio_audioFilter_noiseFloor;
        public static int fieldRadio_audioFilter_crackleChance;

        // Radio Grace perood
        public static long fieldRadio_gracePeriod;

        // Radio Recipe
        public static boolean fieldRadio_recipe_basic_enabled;
        public static List<String> fieldRadio_recipe_basic_shape;
        public static ConfigurationSection fieldRadio_recipe_basic_ingredients;

    //speaker
    public static boolean speaker_useEntity;
    public static int speaker_maxSpeakerCacheSize;
    public static int speaker_soundRange;

        // Recipe
        public static boolean speaker_recipe_basic_enabled;
        public static List<String> speaker_recipe_basic_shape;
        public static ConfigurationSection speaker_recipe_basic_ingredients;

        // block
        public static Material speaker_block_headType;
        public static Material speaker_block_wallHeadType;

    // entity



    public static void refresh()
    {
         // Top level
         enabled = Radio.config.getBoolean("enabled");

         frequencyRepresentationDyes = Radio.config.getConfigurationSection("frequency-representation.dyes");
         frequencySplitString = Radio.config.getString("frequency-representation.separating-string");

        //field radio
             //  Audio Filter
             fieldRadio_audioFilter_enabled = Radio.config.getBoolean("fieldradio.audio-filter.enabled");
             fieldRadio_audioFilter_LPAlpha = Radio.config.getDouble("fieldradio.audio-filter.LP-alpha");
             fieldRadio_audioFilter_HPAlpha = Radio.config.getDouble("fieldradio.audio-filter.HP-alpha");
             fieldRadio_audioFilter_noiseFloor = Radio.config.getInt("fieldradio.audio-filter.noise-floor");
             fieldRadio_audioFilter_crackleChance = Radio.config.getInt("fieldradio.audio-filter.crackle-chance");

             // Radio Grace perood
             fieldRadio_gracePeriod = Radio.config.getLong("fieldradio.grace-period");

             // Radio Recipe
             fieldRadio_recipe_basic_enabled = Radio.config.getBoolean("fieldradio.recipe.basic.enabled");

        fieldRadio_recipe_basic_shape = Radio.config.getStringList("fieldradio.recipe.basic.shape");
        fieldRadio_recipe_basic_ingredients = Radio.config.getConfigurationSection("fieldradio.recipe.basic.ingredients");

        //speaker
        speaker_useEntity = Radio.config.getBoolean("speaker.use-entity");
        speaker_maxSpeakerCacheSize = Radio.config.getInt("speaker.max-speaker-cache-size");
        speaker_soundRange = Radio.config.getInt("speaker.sound-range");

            // Recipe
            speaker_recipe_basic_enabled = Radio.config.getBoolean("speaker.recipe.basic.enabled");
            speaker_recipe_basic_shape = Radio.config.getStringList("speaker.recipe.basic.shape");
            speaker_recipe_basic_ingredients = Radio.config.getConfigurationSection("speaker.recipe.basic.ingredients");

            //block
            speaker_block_headType = Material.matchMaterial(Radio.config.getString("speaker.block.head-type"));
            speaker_block_wallHeadType = Material.matchMaterial(Radio.config.getString("speaker.block.wall-head-type"));

            //entity



        //special cases that need reloading

    }
}
