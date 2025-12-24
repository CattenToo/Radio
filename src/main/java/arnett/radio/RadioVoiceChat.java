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

public class RadioVoiceChat implements VoicechatPlugin {

    public static VoicechatServerApi api;
    public static OpusDecoder decoder;
    public static OpusEncoder encoder;


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

        decoder = api.createDecoder();
        encoder = api.createEncoder();
    }

    @Override
    public void registerEvents(EventRegistration registration)
    {
        registration.registerEvent(MicrophonePacketEvent.class, FieldRadioVoiceChatListener::onMicrophone);
    }
}
