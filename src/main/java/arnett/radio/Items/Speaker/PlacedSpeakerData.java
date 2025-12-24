package arnett.radio.Items.Speaker;

import arnett.radio.RadioConfig;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

//basically just to store data
public class PlacedSpeakerData {

    public PlacedSpeakerData(Location location, String frequency){
        this.location = location;
        this.frequency = frequency;
    }

    private final Location location;
    private final String frequency;
    public LinkedHashMap<UUID, AudioChannel> channels = new LinkedHashMap<>(){

        //override this to auto delete the oldest entry when over the max
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, AudioChannel> eldest) {
            return size() > RadioConfig.speaker_maxSpeakerCacheSize;
        }

    };

    public boolean isOfFrequency(String fq)
    {
        return frequency.equals(fq);
    }

    public Location getLocation()
    {
        return location;
    }
    public String getFrequency()
    {
        return frequency;
    }
}
