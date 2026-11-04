package edu.hitsz.audio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioSettingsTest {
    @Test
    void channelsKeepTheirVolumeWhenMutedAndUnmuted() {
        AudioSettings settings = new AudioSettings();
        settings.setVolume(AudioSettings.Channel.MUSIC, 0.35);
        settings.setVolume(AudioSettings.Channel.EFFECTS, 0.8);
        settings.setMuted(AudioSettings.Channel.MUSIC, true);
        assertEquals(0.0, settings.snapshot().effectiveVolume(AudioSettings.Channel.MUSIC));
        assertEquals(0.35, settings.snapshot().musicVolume());
        assertEquals(0.8, settings.snapshot().effectiveVolume(AudioSettings.Channel.EFFECTS));
        settings.setMuted(AudioSettings.Channel.MUSIC, false);
        assertEquals(0.35, settings.snapshot().effectiveVolume(AudioSettings.Channel.MUSIC));
    }

    @Test
    void rejectsOutOfRangeOrNonFiniteLevels() {
        AudioSettings settings = new AudioSettings();
        for (double invalid : new double[]{-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> settings.setVolume(AudioSettings.Channel.MUSIC, invalid));
        }
    }

    @Test
    void simultaneousChannelChangesDoNotLoseTheOtherChannel() throws Exception {
        AudioSettings settings = new AudioSettings();
        Thread music = new Thread(() -> {
            for (int i = 0; i < 1000; i++) settings.setVolume(AudioSettings.Channel.MUSIC, 0.7);
        });
        Thread effects = new Thread(() -> {
            for (int i = 0; i < 1000; i++) settings.setVolume(AudioSettings.Channel.EFFECTS, 0.4);
        });
        music.start(); effects.start(); music.join(); effects.join();
        assertEquals(0.7, settings.snapshot().musicVolume());
        assertEquals(0.4, settings.snapshot().effectsVolume());
    }
}
