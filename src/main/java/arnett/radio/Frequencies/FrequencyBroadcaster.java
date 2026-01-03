package arnett.radio.Frequencies;

import arnett.radio.Items.Speaker.Speaker;
import arnett.radio.Radio;
import arnett.radio.RadioConfig;
import arnett.radio.RadioVoiceChat;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.units.qual.A;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class FrequencyBroadcaster {


    final String frequency;
    final ArrayList<byte[]> audio = new ArrayList<>();
    final long loopDelay;
    BukkitTask task;


    //parameters for task
    AtomicBoolean running = new AtomicBoolean(true);
    AtomicInteger progress = new AtomicInteger(0);

    static UUID brodcasterID = UUID.randomUUID();

    public FrequencyBroadcaster(String frequency, byte[] audioData, boolean autoStart, long loopDelay)
    {
        //initialize values
        this.frequency = frequency;
        this.loopDelay = loopDelay;

        //convert to short array since that's what the encoder needs
        ShortBuffer shortBuffer = ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

        short[] decodedAudio = new short[shortBuffer.remaining()];
        shortBuffer.get(decodedAudio);

        //create the encoder
        OpusEncoder encoder = RadioVoiceChat.api.createEncoder();
        int packetSize = 960;

        CompletableFuture.runAsync(() -> {
            //960 is the size of an audio packet for SVC
            for (int i = 0; i < decodedAudio.length; i += packetSize)
            {
                int size = Math.min(packetSize, decodedAudio.length - i);
                short[] packetFrame = new short[packetSize];

                //fill the packet
                for(int j = 0; j < size; j++)
                {
                    packetFrame[j] = decodedAudio[j + i];
                }

                //add packet to list
                audio.add(encoder.encode(packetFrame));
            }

            encoder.close();

            if(autoStart)
                startPlaying();
        });
    }

    public String getFrequency(){
        return frequency;
    }

    //starts the thread for playing the sound
    public void startPlaying()
    {
        Radio.logger.info("Starting Broadcast Task on " + frequency);

        task = Bukkit.getScheduler().runTaskAsynchronously(Radio.singleton, () -> {
            try {
                while (true)
                {
                    long start = System.nanoTime();
                    long wait = 20_000_000L; // 20ms

                    for (int i = 0; i < audio.size(); i++) {
                        //gets the time we should be at
                        long expected = start + i * wait;

                        //difference between that and the time we are at
                        long remaining = expected - System.nanoTime();

                        // if we are at a positive difference then we are ahead
                        // if we are negative then we are behind
                        if (remaining > 0) {
                            Thread.sleep(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                        }

                        //don't play any audio if we're paused
                        if (!running.get())
                            return;

                        //send out the packet
                        FrequencyManager.sendToFrequency(brodcasterID, audio.get(progress.get()), frequency);

                        //increment and end if we are at the end
                        if (progress.getAndAdd(1) >= audio.size())
                            break;
                    }

                //wait to reload the loop
                Thread.sleep(loopDelay);
                progress.set(0);
            }


            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void stopPlaying()
    {
        if(task != null)
            task.cancel();
        running.set(false);
        progress.set(0);
    }

    public void pause()
    {
        running.set(false);
    }

    public void resume()
    {
        running.set(true);
    }
}
