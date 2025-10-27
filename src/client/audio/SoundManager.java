// src/client/audio/SoundManager.java
package client.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public class SoundManager {

    /**
     * Phát một file âm thanh từ thư mục resources.
     * @param fileName Tên file âm thanh (ví dụ: "invite.wav")
     */
    public static synchronized void playSound(final String fileName) {
        // Chạy trong một luồng mới để không làm block luồng giao diện
        new Thread(() -> {
            try {
                Clip clip = AudioSystem.getClip();
                URL soundURL = SoundManager.class.getResource("/resources/" + fileName);
                if (soundURL == null) {
                    System.err.println("Không tìm thấy file âm thanh: /resources/" + fileName);
                    return;
                }
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(soundURL);
                clip.open(inputStream);
                clip.start();
            } catch (Exception e) {
                System.err.println("Lỗi khi phát âm thanh: " + e.getMessage());
            }
        }).start();
    }
}