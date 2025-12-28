package arnett.radio;

import arnett.radio.Items.Radio.FieldRadioVoiceChatListener;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class RadioVoiceChat implements VoicechatPlugin {

    public static VoicechatServerApi api;
    public static HashMap<UUID, OpusDecoder> decoders = new HashMap<>();
    public static HashMap<UUID, OpusEncoder> encoders = new HashMap<>();


    @Override
    public String getPluginId() {
        return "FieldRadio";
    }

    @Override
    public void initialize(VoicechatApi api)
    {
        if (api instanceof VoicechatServerApi serverApi) {
            RadioVoiceChat.api = serverApi;
        }
    }

    public static OpusEncoder getEncoder(UUID id)
    {
        OpusEncoder encoder = encoders.get(id);
        if(encoder == null)
        {
            encoder = api.createEncoder();
            encoders.put(id, encoder);
        }
        return encoder;
    }

    public static OpusDecoder getDecoder(UUID id)
    {
        OpusDecoder decoder = decoders.get(id);
        if(decoder == null)
        {
            decoder = api.createDecoder();
            decoders.put(id, decoder);
        }
        return decoder;
    }

    public static OpusEncoder removeDecoder(UUID id)
    {
        Radio.logger.info("Removed Encoder");
        return encoders.remove(id);
    }

    public static OpusDecoder removeEncoder(UUID id)
    {
        Radio.logger.info("Removed Decoder");
        return decoders.remove(id);
    }

    @Override
    public void registerEvents(EventRegistration registration)
    {
        registration.registerEvent(MicrophonePacketEvent.class, FieldRadioVoiceChatListener::onMicrophone);
    }
}
