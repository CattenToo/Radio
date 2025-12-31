package arnett.radio.Items.Speaker;

import de.maxhenkel.voicechat.api.Entity;
import org.bukkit.Location;
import org.bukkit.World;


public record SpeakerSession(String frequency,
                             Location location,
                             Entity entity)
{ }