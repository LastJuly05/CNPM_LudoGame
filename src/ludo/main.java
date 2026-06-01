package ludo.view;

import ludo.controller.GameController;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class BoardPanel extends JPanel {
    private GameController controller;

    public BoardPanel(GameController controller) {
        this.controller = controller;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 1. Phân hệ của Phát: Tự động chạy thuật toán vẽ nền bàn cờ 4 màu
        drawLudoBoardBackground(g2d);

        // 2. Tự động lấy dữ liệu từ các lớp Model để cập nhật và vẽ quân cờ lên màn hình
        if (controller != null) {
            drawAllHorses(g2d);
        }
    }

    private void drawLudoBoardBackground(Graphics2D g2d) {
        // Thuật toán vẽ 56 ô cờ và chuồng của Phát viết ở đây
    }

    private void drawAllHorses(Graphics2D g2d) {
        // Quét mảng Player/Horse từ controller để vẽ đúng vị trí tọa độ của quân cờ
    }
}