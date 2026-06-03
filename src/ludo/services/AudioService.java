package ludo.services;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class AudioService {

    /**
     * Hàm phát âm thanh từ một đường dẫn file (.wav)
     * @param soundFileName Tên file âm thanh nằm trong thư mục resources (Ví dụ: "dice_roll.wav")
     */
    public void playSound(String soundFileName) {
        // Chạy âm thanh trên một Thread riêng biệt để không làm đóng băng giao diện (UI) khi chơi game
        new Thread(() -> {
            try {
                // Tìm file âm thanh trong thư mục tài nguyên của dự án
                URL soundURL = getClass().getClassLoader().getResource("sounds/" + soundFileName);
                AudioInputStream audioInputStream;

                if (soundURL != null) {
                    audioInputStream = AudioSystem.getAudioInputStream(soundURL);
                } else {
                    // Phương án dự phòng nếu chưa cấu hình thư mục resource: Tìm trực tiếp từ thư mục ngoài
                    File file = new File("resources/sounds/" + soundFileName);
                    if (!file.exists()) {
                        System.out.println("❌ Không tìm thấy file âm thanh: " + soundFileName);
                        return;
                    }
                    audioInputStream = AudioSystem.getAudioInputStream(file);
                }

                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start(); // Phát âm thanh

            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                System.out.println("⚠️ Lỗi khi phát âm thanh " + soundFileName + ": " + e.getMessage());
            }
        }).start();
    }
}