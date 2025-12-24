package arnett.radio.Items.Radio;

import arnett.radio.RadioConfig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

//mostly just a library class for functions relating to the radio through voicechat
public class FieldRadioVoiceChat {

    //stores all players that are listening to which frequency
    public static HashMap<String, ArrayList<UUID>> frequencyListeners = new HashMap<>();
    //stores all players who are on grace period so mic doesn't cut off
    // uses long because I feel like it and ticks can slow which isn't really ideal for this
    public static HashMap<UUID, Long> playersInGracePeroid = new HashMap<>();

    public static void addToFrequency(String frequency, UUID id)
    {
        frequencyListeners.computeIfAbsent(frequency, key -> new ArrayList<UUID>()).add(id);
    }

    public static void removeFromFrequency(String frequency, UUID id)
    {
        frequencyListeners.get(frequency).remove(id);

        if (frequencyListeners.get(frequency).isEmpty())
            frequencyListeners.remove(frequency);
    }

    public static void removeFromFrequency(UUID id)
    {
        frequencyListeners.entrySet().removeIf(((s) -> {
            s.getValue().removeIf((e) ->
                    e.equals(id)
            );

            if (s.getValue().isEmpty())
                return true;
            return false;
        }));
    }

    //cleans frequencies
    public static void clearFrequencies()
    {
        frequencyListeners.clear();
    }

    //overload for clearing one player from frequencies
    public static void clearFrequency(Player player)
    {
        clearFrequency(player.getUniqueId());
    }

    //overload for clearing one player from frequencies
    public static void clearFrequency(UUID target)
    {
        for(String s : frequencyListeners.keySet())
            clearFrequency(s, target);
    }

    //overload for clearing one player from frequencies
    public static void clearFrequency(String frequency, UUID target)
    {
        //remove the player from the list and remove the list entry if it is empty

        frequencyListeners.get(frequency).removeIf(k -> k.equals(target));

        if(frequencyListeners.get(frequency).isEmpty())
            frequencyListeners.remove(frequency);
    }

    //overload for clearing one player from frequencies
    public static void clearFrequency(String frequency, Player player)
    {
        clearFrequency(frequency, player.getUniqueId());
    }


    public static boolean isOnFrequency(Player player)
    {
        UUID target = player.getUniqueId();
        return isOnFrequency(target);
    }

    //overload
    public static boolean isOnFrequency(UUID target)
    {
         for(String s : frequencyListeners.keySet())
             if(isOnFrequency(s, target))
                 return true;

        return false;
    }

    public static boolean isOnFrequency(String frequency, UUID target)
    {
        return frequencyListeners.getOrDefault(frequency, new ArrayList<>()).contains(target);
    }

    public static boolean isOnFrequency(String frequency, Player player)
    {
        UUID target = player.getUniqueId();
        return isOnFrequency(frequency, target);
    }

    public static Map<String, ArrayList<UUID>> getFrequencys()
    {
        return Collections.unmodifiableMap(frequencyListeners);
    }

    public static void removeFromGrace(UUID id)
    {
        playersInGracePeroid.remove(id);
    }

    public static void refresh(Player target){

        clearFrequency(target);

        ItemStack[] radios = FieldRadio.getRadiosFromPlayer(target);

        for(ItemStack radio : radios)
            addToFrequency(FieldRadio.getFrequency(radio), target.getUniqueId());
    }

    public static void refresh(String frequency, Player target){

        clearFrequency(frequency, target);

        ItemStack[] radios = FieldRadio.getRadiosFromPlayer(target);

        for(ItemStack radio : radios)
            if(FieldRadio.getFrequency(radio).equalsIgnoreCase(frequency))
                addToFrequency(FieldRadio.getFrequency(radio), target.getUniqueId());
    }

    public static short[] applyFilter(short[] decodedData)
    {
        // Filter states to maintain continuity across packets
        double lowPassState = 0;
        double highPassState = 0;
        double lastRawSample = 0;
        Random random = new Random();

        // Configuration constants
        double LP_ALPHA = RadioConfig.fieldRadio_audioFilter_LPAlpha; // Lower = more muffled
        double HP_ALPHA = RadioConfig.fieldRadio_audioFilter_HPAlpha; // Higher = less bass
        int NOISE_FLOOR = RadioConfig.fieldRadio_audioFilter_noiseFloor;  // Constant hiss volume
        int CRACKLE_CHANCE = RadioConfig.fieldRadio_audioFilter_crackleChance; // 1 in 2000 samples

        // no, I did not actually code the audio manipulation part of the filter since I'm not the best at working with audio

        for (int i = 0; i < decodedData.length; i++) {
            double currentSample = decodedData   [i];

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
