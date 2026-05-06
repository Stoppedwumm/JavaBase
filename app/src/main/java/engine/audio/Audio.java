package engine.audio;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap; // Added this import
import java.util.Map;

/**
 * <b>EXPERIMENTAL AUDIO ENGINE</b>
 * <p>
 * <b>WARNING:</b> This class is currently in an experimental state. The API, internal 
 * implementation, and resource management strategies are not finalized. 
 * </p>
 * 
 * <p>Current Limitations:</p>
 * <ul>
 *   <li>Only supports 16-bit PCM .WAV files natively.</li>
 *   <li>The cache grows to a maximum size (LRU), then disposes of old clips.</li>
 * </ul>
 *
 * @author Stoppedwumm
 * @version 0.1-ALPHA
 */
public class Audio {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private Audio() {}

    private static final int MAX_CACHE_SIZE = 50;

    // Use LinkedHashMap with accessOrder = true for LRU (Least Recently Used) behavior
    private static final Map<String, Clip> cache = new LinkedHashMap<String, Clip>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Clip> eldest) {
            if (size() > MAX_CACHE_SIZE) {
                Clip clip = eldest.getValue();
                if (clip != null) {
                    if (clip.isRunning()) {
                        clip.stop();
                    }
                    clip.close(); // Important: Close the line to free system audio resources
                }
                return true;
            }
            return false;
        }
    };

    private static Clip backgroundMusic;

    /**
     * Plays a short sound effect once.
     * @param path Path to the file in resources (e.g., "/sounds/jump.wav")
     */
    public static void playSound(String path) {
        try {
            Clip clip = getClip(path);
            if (clip != null) {
                clip.setFramePosition(0);
                clip.start();
            }
        } catch (Exception e) {
            System.err.println("Audio Error [SFX]: " + path + " - " + e.getMessage());
        }
    }

    /**
     * Plays a file on loop. Only one music track can play at a time.
     * @param path Path to the file in resources.
     * @param volume Linear volume scale from 0.0 to 1.0.
     */
    public static void playMusic(String path, float volume) {
        stopMusic();
        try {
            backgroundMusic = getClip(path);
            if (backgroundMusic != null) {
                setVolume(backgroundMusic, volume);
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                backgroundMusic.start();
            }
        } catch (Exception e) {
            System.err.println("Audio Error [Music]: " + path + " - " + e.getMessage());
        }
    }

    /**
     * Stops the currently playing background music track.
     */
    public static void stopMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }

    private static void setVolume(Clip clip, float volume) {
        try {
            if (volume < 0f) volume = 0f;
            if (volume > 1f) volume = 1f;
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        } catch (Exception e) {
            System.err.println("Volume Control not supported.");
        }
    }

    private static Clip getClip(String path) throws Exception {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        InputStream is = Audio.class.getResourceAsStream(path);
        if (is == null) throw new RuntimeException("Resource not found: " + path);
        
        try (InputStream bufferedIn = new BufferedInputStream(is);
             AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedIn)) {
            
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            
            cache.put(path, clip);
            return clip;
        }
    }
}
