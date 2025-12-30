package arnett.radio;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.List;

//yeah, this system is much better
//basically, all this does is allow for ease of use when coding
public class RadioConfig {

    // Top level
    public static boolean enabled;

    public static ConfigurationSection frequencyRepresentationDyes;
    public static String frequencySplitString;

    //field radio
    public static Material fieldRadio_baseMaterial;

        //  Audio Filter
        public static boolean fieldRadio_audioFilter_enabled;
        public static double fieldRadio_audioFilter_volume;
        public static double fieldRadio_audioFilter_LPAlpha;
        public static double fieldRadio_audioFilter_HPAlpha;
        public static int fieldRadio_audioFilter_noiseFloor;
        public static int fieldRadio_audioFilter_crackleChance;

        // Radio Grace perood
        public static long fieldRadio_gracePeriod;

        // Radio Recipe
            //basic
            public static boolean fieldRadio_recipe_basic_enabled;
            public static List<String> fieldRadio_recipe_basic_shape;
            public static ConfigurationSection fieldRadio_recipe_basic_ingredients;

            public static boolean fieldRadio_recipe_retune_enabled;
            public static List<String> fieldRadio_recipe_retune_ingredients;

    //speaker
    public static boolean speaker_useEntity;
    public static int speaker_cacheSize;
    public static int speaker_soundRange;

        //  Audio Filter
        public static boolean speaker_audioFilter_enabled;
        public static double speaker_audioFilter_volume;
        public static double speaker_audioFilter_LPAlpha;
        public static double speaker_audioFilter_HPAlpha;
        public static int speaker_audioFilter_noiseFloor;
        public static int speaker_audioFilter_crackleChance;

        // Recipe
        public static boolean speaker_recipe_basic_enabled;
        public static List<String> speaker_recipe_basic_shape;
        public static ConfigurationSection speaker_recipe_basic_ingredients;

        public static boolean speaker_recipe_retune_enabled;
        public static List<String> speaker_recipe_retune_ingredients;

        // block
        public static Material speaker_block_headType;
        public static Material speaker_block_wallHeadType;

        // entity
        public static Material speaker_entity_baseMaterial;
        public static float speaker_entity_interactionWidth;
        public static float speaker_entity_interactionHeight;
        public static Vector speaker_entity_displayOffset;



    public static void refresh()
    {
         // Top level
         enabled = Radio.config.getBoolean("enabled");

         frequencyRepresentationDyes = Radio.config.getConfigurationSection("frequency-representation.dyes");
         frequencySplitString = Radio.config.getString("frequency-representation.separating-string");

         //field radio


             //  Audio Filter
             fieldRadio_audioFilter_enabled = Radio.config.getBoolean("fieldradio.audio-filter.enabled");
             fieldRadio_audioFilter_volume = Radio.config.getDouble("fieldradio.audio-filter.volume");
             fieldRadio_audioFilter_LPAlpha = Radio.config.getDouble("fieldradio.audio-filter.LP-alpha");
             fieldRadio_audioFilter_HPAlpha = Radio.config.getDouble("fieldradio.audio-filter.HP-alpha");
             fieldRadio_audioFilter_noiseFloor = Radio.config.getInt("fieldradio.audio-filter.noise-floor");
             fieldRadio_audioFilter_crackleChance = Radio.config.getInt("fieldradio.audio-filter.crackle-chance");

         // Radio Grace period
         fieldRadio_gracePeriod = Radio.config.getLong("fieldradio.grace-period");

         //base material
         fieldRadio_baseMaterial = Material.matchMaterial(Radio.config.getString("fieldradio.base-material"));

             // Radio Recipe
                //basic
                fieldRadio_recipe_basic_enabled = Radio.config.getBoolean("fieldradio.recipe.basic.enabled");

                fieldRadio_recipe_basic_shape = Radio.config.getStringList("fieldradio.recipe.basic.shape");
                fieldRadio_recipe_basic_ingredients = Radio.config.getConfigurationSection("fieldradio.recipe.basic.ingredients");

                //retune
                fieldRadio_recipe_retune_enabled= Radio.config.getBoolean("fieldradio.recipe.retune.enabled");
                fieldRadio_recipe_retune_ingredients = Radio.config.getStringList("fieldradio.recipe.retune.ingredients");

        //speaker
        speaker_useEntity = Radio.config.getBoolean("speaker.use-entity");
        speaker_soundRange = Radio.config.getInt("speaker.sound-range");
        speaker_cacheSize = Radio.config.getInt("speaker.cache-size");


            //  Audio Filter
            speaker_audioFilter_enabled = Radio.config.getBoolean("speaker.audio-filter.enabled");
            speaker_audioFilter_volume = Radio.config.getDouble("speaker.audio-filter.volume");
            speaker_audioFilter_LPAlpha = Radio.config.getDouble("speaker.audio-filter.LP-alpha");
            speaker_audioFilter_HPAlpha = Radio.config.getDouble("speaker.audio-filter.HP-alpha");
            speaker_audioFilter_noiseFloor = Radio.config.getInt("speaker.audio-filter.noise-floor");
            speaker_audioFilter_crackleChance = Radio.config.getInt("speaker.audio-filter.crackle-chance");

            // Recipe
                //basic
                speaker_recipe_basic_enabled = Radio.config.getBoolean("speaker.recipe.basic.enabled");
                speaker_recipe_basic_shape = Radio.config.getStringList("speaker.recipe.basic.shape");
                speaker_recipe_basic_ingredients = Radio.config.getConfigurationSection("speaker.recipe.basic.ingredients");

                //retune
                speaker_recipe_retune_enabled= Radio.config.getBoolean("speaker.recipe.retune.enabled");
                speaker_recipe_retune_ingredients = Radio.config.getStringList("speaker.recipe.retune.ingredients");


            //block
            speaker_block_headType = Material.matchMaterial(Radio.config.getString("speaker.block.head-type"));
            speaker_block_wallHeadType = Material.matchMaterial(Radio.config.getString("speaker.block.wall-head-type"));

            //entity
            //base material
            speaker_entity_baseMaterial = Material.matchMaterial(Radio.config.getString("speaker.entity.base-material"));
            speaker_entity_interactionWidth = (float)Radio.config.getDouble("speaker.entity.interaction-width");
            speaker_entity_interactionHeight = (float)Radio.config.getDouble("speaker.entity.interaction-height");
            ConfigurationSection displayOffset = Radio.config.getConfigurationSection("speaker.entity.display-offset");
            speaker_entity_displayOffset = new Vector(
                    (float)displayOffset.getDouble("x"),
                    (float)displayOffset.getDouble("y"),
                    (float)displayOffset.getDouble("z")
            );

    }
}
