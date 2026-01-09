package arnett.radio;

import arnett.radio.Frequencies.FrequencyBroadcaster;
import arnett.radio.Frequencies.FrequencyManager;
import arnett.radio.Items.Microphone.MicrophoneListener;
import arnett.radio.Items.Radio.FieldRadioVoiceChatListener;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Random;
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

        FrequencyManager.setUpBroadcasters();
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

    public static void removeDecoder(UUID id)
    {
        if(!decoders.containsKey(id))
            return;

        decoders.get(id).close();
        decoders.remove(id);
    }

    public static void removeEncoder(UUID id)
    {
        if(!encoders.containsKey(id))
            return;

        encoders.get(id).close();
        encoders.remove(id);
    }

    @Override
    public void registerEvents(EventRegistration registration)
    {
        registration.registerEvent(MicrophonePacketEvent.class, FieldRadioVoiceChatListener::onMicrophone);
        registration.registerEvent(MicrophonePacketEvent.class, MicrophoneListener::onMicrophone);
    }

    public static short[] applyFilter(short[] decodedData, double LP_ALPHA, double HP_ALPHA, int NOISE_FLOOR, int CRACKLE_CHANCE)
    {
        // Filter states to maintain continuity across packets
        double lowPassState = 0;
        double highPassState = 0;
        double lastRawSample = 0;
        Random random = new Random();

        for (int i = 0; i < decodedData.length; i++) {
            double currentSample = decodedData[i];

            // 1. BANDPASS FILTER (EQ)
            // Low Pass (Cuts highs)
            lowPassState = LP_ALPHA * currentSample + (1 - LP_ALPHA) * lowPassState;
            double filtered = lowPassState;

            // High Pass (Cuts lows)
            highPassState = HP_ALPHA * highPassState + HP_ALPHA * (filtered - lastRawSample);
            lastRawSample = filtered;

            // 2. SATURATION (The "Crunch")
            // Convert to -1.0 to 1.0 range for math
            double x = highPassState / 32768.0;
            // Soft clipping formula: (3x - x^3) / 2
            double saturated = (3 * x - Math.pow(x, 3)) / 2.0;

            // 3. NOISE & INTERFERENCE
            // Constant low-level hiss
            int hiss = random.nextInt(NOISE_FLOOR * 2 + 1) - NOISE_FLOOR;

            // Random electrical "crackles"
            int crackle = (random.nextInt(CRACKLE_CHANCE) == 0) ? (random.nextInt(6000) - 3000) : 0;

            // 4. CLAMP & OUTPUT
            int finalSample = (int) (saturated * 32767) + hiss + crackle;
            decodedData[i] = (short) Math.max(-32768, Math.min(32767, finalSample));
        }

        return decodedData;
    }
}
