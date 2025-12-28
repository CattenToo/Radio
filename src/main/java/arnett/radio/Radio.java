package arnett.radio;

import arnett.radio.Commands.CommandManager;
import arnett.radio.Items.CustomItemManager;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.logging.Logger;

public final class Radio extends JavaPlugin {

    public static Logger logger;
    public static FileConfiguration config;
    public static JavaPlugin singleton;
    public static ArrayList<String> avaliablePlugins = new ArrayList<>();

    @Override
    public void onEnable() {

        //make sure config is present
        saveDefaultConfig();

        logger = getLogger();
        //setup fields for ease of use
        singleton = this;
        //Config is a custom class for ease of use which inherits from class of getConfig()
        config = getConfig();


        FrequencyManager.reload();

        if(!RadioConfig.enabled)
        {
            return;
        }


        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            // This prints the exact name needed for your plugin.yml
            avaliablePlugins.add(plugin.getName());
        }

        //sets up voicechat things if server has voicecehat
        setupVoicechatFunctionality();

        // registers listeners
        CustomItemManager.registerItemEvents(this);

        //registers recipes on reload config btw

        //register commands
        getCommand("fieldradio").setExecutor(new CommandManager(avaliablePlugins));
    }

    //sets up voice chat dependent things
    public void setupVoicechatFunctionality()
    {
        // check voicechat
        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);

        getLogger().info("status: " + (service != null));
        if(service == null)
        {
            getLogger().info("Running Without Simple Voice Chat");
            return;
        }


        //simple voice is present from here down

        service.registerPlugin(new RadioVoiceChat());

        getLogger().info("Using Simple Voice Chat");
        //register events specific to voice chat
        CustomItemManager.registerVoiceChatItemEvents(this);
    }

    @Override
    public void reloadConfig()
    {
        super.reloadConfig();

        config = getConfig();
        RadioConfig.refresh();

        if(logger != null)
            FrequencyManager.reload();

        CustomItemManager.reload();

        logger.info("Reloaded Config");
    }

}
