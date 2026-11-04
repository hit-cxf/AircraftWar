package edu.hitsz.audio;

import java.util.concurrent.atomic.AtomicReference;

/** 本次运行共享的音量状态；EDT 可更新，JavaFX 播放入口读取一致快照。 */
public final class AudioSettings {
    public enum Channel { MUSIC, EFFECTS }

    public record Snapshot(double musicVolume, double effectsVolume,
                           boolean musicMuted, boolean effectsMuted) {
        public double volume(Channel channel) {
            return channel == Channel.MUSIC ? musicVolume : effectsVolume;
        }

        public boolean muted(Channel channel) {
            return channel == Channel.MUSIC ? musicMuted : effectsMuted;
        }

        public double effectiveVolume(Channel channel) {
            return muted(channel) ? 0.0 : volume(channel);
        }
    }

    private static final AudioSettings GLOBAL = new AudioSettings();
    private final AtomicReference<Snapshot> state =
            new AtomicReference<>(new Snapshot(0.60, 0.75, false, false));

    public static AudioSettings global() {
        return GLOBAL;
    }

    public Snapshot snapshot() {
        return state.get();
    }

    public void setVolume(Channel channel, double volume) {
        if (channel == null || !Double.isFinite(volume) || volume < 0 || volume > 1) {
            throw new IllegalArgumentException("音量必须在 0～1 之间");
        }
        state.updateAndGet(s -> channel == Channel.MUSIC
                ? new Snapshot(volume, s.effectsVolume(), s.musicMuted(), s.effectsMuted())
                : new Snapshot(s.musicVolume(), volume, s.musicMuted(), s.effectsMuted()));
    }

    public void setMuted(Channel channel, boolean muted) {
        if (channel == null) {
            throw new IllegalArgumentException("音频通道不能为空");
        }
        state.updateAndGet(s -> channel == Channel.MUSIC
                ? new Snapshot(s.musicVolume(), s.effectsVolume(), muted, s.effectsMuted())
                : new Snapshot(s.musicVolume(), s.effectsVolume(), s.musicMuted(), muted));
    }
}
