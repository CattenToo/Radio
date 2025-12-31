package arnett.radio;

import arnett.radio.Items.Microphone.MicrophoneListener;
import arnett.radio.Items.Radio.FieldRadioVoiceChatListener;
import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
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
        encoders.get(id).close();
        return encoders.remove(id);
    }

    public static OpusDecoder removeEncoder(UUID id)
    {
        decoders.get(id).close();
        return decoders.remove(id);
    }

    @Override
    public void registerEvents(EventRegistration registration)
    {
        registration.registerEvent(MicrophonePacketEvent.class, FieldRadioVoiceChatListener::onMicrophone);
        registration.registerEvent(MicrophonePacketEvent.class, MicrophoneListener::onMicrophone);

        //create the volume category for speakers
        VolumeCategory speakers = api.volumeCategoryBuilder()
                .setId("speakers")
                .setName("Speakers")
                .setDescription("Volume of all speakers")
                .build();

        api.registerVolumeCategory(speakers);
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
