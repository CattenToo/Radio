package arnett.radio;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

//general listening for voicechat setup
public class RadioVoiceChatListener implements Listener {
    @EventHandler
    public static void onPlayerLeave(PlayerQuitEvent e)
    {
        RadioVoiceChat.removeDecoder(e.getPlayer().getUniqueId());
        RadioVoiceChat.removeEncoder(e.getPlayer().getUniqueId());
    }
}
