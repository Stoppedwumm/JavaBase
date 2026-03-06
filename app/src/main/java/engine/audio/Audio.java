// This shit is anything but tested, all changes here are temporary and might be removed
package engine.audio;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Audio {

    private static final Map<String, Clip> cache = new HashMap<>();
    private static Clip backgroundMusic;

    /**
     * Plays a short sound effect once.
     * @param path Path to the file in resources (e.g., "/sounds/jump.wav")
     */
    public static void playSound(String path) {
        try {
            Clip clip = getClip(path);
            if (clip != null) {
                clip.setFramePosition(0); // Rewind to start
                clip.start();
            }
        } catch (Exception e) {
            System.err.println("Error playing sound: " + path);
        }
    }

    /**
     * Plays a file on loop. Only one music track can play at a time.
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
            System.err.println("Error playing music: " + path);
        }
    }

    public static void stopMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }

    /**
     * Adjusts volume of a clip.
     * @param volume 0.0 to 1.0
     */
    private static void setVolume(Clip clip, float volume) {
        if (volume < 0f || volume > 1f) return;
        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
        gainControl.setValue(dB);
    }

    private static Clip getClip(String path) throws Exception {
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        InputStream is = Audio.class.getResourceAsStream(path);
        if (is == null) throw new RuntimeException("Sound not found: " + path);
        
        // Use BufferedInputStream to support mark/reset required by AudioSystem
        InputStream bufferedIn = new BufferedInputStream(is);
        AudioInputStream audioIn = AudioSystem.getAudioInputStream(bufferedIn);
        
        Clip clip = AudioSystem.getClip();
        clip.open(audioIn);
        
        cache.put(path, clip);
        return clip;
    }
}
