package net.minecraft.client.sounds;

import com.mojang.serialization.Codec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

/**
 * The MusicManager class manages the playing of music in Minecraft.
 */
@OnlyIn(Dist.CLIENT)
public class MusicManager {
    /**
     * The delay before starting to play the next song.
     */
    private static final int STARTING_DELAY = 100;
    private final RandomSource random = RandomSource.create();
    private final Minecraft minecraft;
    private @Nullable SoundInstance currentMusic;
    private MusicManager.MusicFrequency gameMusicFrequency;
    private float currentGain = 1.0F;
    /**
     * The delay until the next song starts.
     */
    private int nextSongDelay = 100;
    private boolean toastShown = false;

    public MusicManager(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.gameMusicFrequency = minecraft.options.musicFrequency().get();
    }

    public void tick() {
        float f = this.minecraft.getMusicVolume();
        if (this.currentMusic != null && this.currentGain != f) {
            boolean flag = this.fadePlaying(f);
            if (!flag) {
                return;
            }
        }

        Music music = net.neoforged.neoforge.client.ClientHooks.selectMusic(this.minecraft.getSituationalMusic(), this.currentMusic);
        if (music == null) {
            if (this.currentMusic != null) {
                this.stopPlaying();
            }
            this.nextSongDelay = 0;
            return;
        }

        if (music == null) {
            this.nextSongDelay = Math.max(this.nextSongDelay, 100);
        } else {
            if (this.currentMusic != null) {
                if (canReplace(music, this.currentMusic)) {
                    this.minecraft.getSoundManager().stop(this.currentMusic);
                    this.nextSongDelay = Mth.nextInt(this.random, 0, music.minDelay() / 2);
                }

                if (!this.minecraft.getSoundManager().isActive(this.currentMusic)) {
                    this.currentMusic = null;
                    this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
                }
            }

            this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
            if (this.currentMusic == null && this.nextSongDelay-- <= 0) {
                this.startPlaying(music);
            }
        }
    }

    private static boolean canReplace(Music music, SoundInstance sound) {
        return music.replaceCurrentMusic() && !music.sound().value().location().equals(sound.getIdentifier());
    }

    public void startPlaying(Music music) {
        SoundEvent soundevent = music.sound().value();
        this.currentMusic = SimpleSoundInstance.forMusic(soundevent);
        switch (this.minecraft.getSoundManager().play(this.currentMusic)) {
            case STARTED:
                this.minecraft.getToastManager().showNowPlayingToast();
                this.toastShown = true;
                break;
            case STARTED_SILENTLY:
                this.toastShown = false;
        }

        this.nextSongDelay = Integer.MAX_VALUE;
    }

    public void showNowPlayingToastIfNeeded() {
        if (!this.toastShown) {
            this.minecraft.getToastManager().showNowPlayingToast();
            this.toastShown = true;
        }
    }

    /**
     * Stops playing the specified {@linkplain Music} selector.
     *
     * @param music the {@linkplain Music} selector to stop playing
     */
    public void stopPlaying(Music music) {
        if (this.isPlayingMusic(music)) {
            this.stopPlaying();
        }
    }

    public void stopPlaying() {
        if (this.currentMusic != null) {
            this.minecraft.getSoundManager().stop(this.currentMusic);
            this.currentMusic = null;
            this.minecraft.getToastManager().hideNowPlayingToast();
        }

        this.nextSongDelay += 100;
    }

    private boolean fadePlaying(float volume) {
        if (this.currentMusic == null) {
            return false;
        } else if (this.currentGain == volume) {
            return true;
        } else {
            if (this.currentGain < volume) {
                this.currentGain = this.currentGain + Mth.clamp(this.currentGain, 5.0E-4F, 0.005F);
                if (this.currentGain > volume) {
                    this.currentGain = volume;
                }
            } else {
                this.currentGain = 0.03F * volume + 0.97F * this.currentGain;
                if (Math.abs(this.currentGain - volume) < 1.0E-4F || this.currentGain < volume) {
                    this.currentGain = volume;
                }
            }

            this.currentGain = Mth.clamp(this.currentGain, 0.0F, 1.0F);
            if (this.currentGain <= 1.0E-4F) {
                this.stopPlaying();
                return false;
            } else {
                this.minecraft.getSoundManager().updateCategoryVolume(SoundSource.MUSIC, this.currentGain);
                return true;
            }
        }
    }

    /**
     * {@return {@code true} if the {@linkplain Music} selector is currently playing, {@code false} otherwise}
     *
     * @param selector the {@linkplain Music} selector to check for
     */
    public boolean isPlayingMusic(Music selector) {
        return this.currentMusic == null ? false : selector.sound().value().location().equals(this.currentMusic.getIdentifier());
    }

    public @Nullable String getCurrentMusicTranslationKey() {
        if (this.currentMusic != null) {
            Sound sound = this.currentMusic.getSound();
            if (sound != null) {
                return sound.getLocation().toShortLanguageKey();
            }
        }

        return null;
    }

    public void setMinutesBetweenSongs(MusicManager.MusicFrequency musicFrequency) {
        this.gameMusicFrequency = musicFrequency;
        this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic(), this.random);
    }

    @OnlyIn(Dist.CLIENT)
    public static enum MusicFrequency implements StringRepresentable {
        DEFAULT("DEFAULT", "options.music_frequency.default", 20),
        FREQUENT("FREQUENT", "options.music_frequency.frequent", 10),
        CONSTANT("CONSTANT", "options.music_frequency.constant", 0);

        public static final Codec<MusicManager.MusicFrequency> CODEC = StringRepresentable.fromEnum(MusicManager.MusicFrequency::values);
        private final String name;
        private final int maxFrequency;
        private final Component caption;

        private MusicFrequency(String name, String captionKey, int maxFrequency) {
            this.name = name;
            this.maxFrequency = maxFrequency * 1200;
            this.caption = Component.translatable(captionKey);
        }

        int getNextSongDelay(@Nullable Music music, RandomSource random) {
            if (music == null) {
                return this.maxFrequency;
            } else if (this == CONSTANT) {
                return 100;
            } else {
                int i = Math.min(music.minDelay(), this.maxFrequency);
                int j = Math.min(music.maxDelay(), this.maxFrequency);
                return Mth.nextInt(random, i, j);
            }
        }

        public Component caption() {
            return this.caption;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
