package ludo.views;

import ludo.controllers.GameController;
import ludo.models.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BoardPanel extends JPanel {
    private GameController controller;

    public BoardPanel(GameController controller) {
        this.controller = controller;
        setPreferredSize(new Dimension(500, 500));
        setBackground(new Color(245, 245, 245));

        // Bắt sự kiện người chơi click vào màn hình để chọn quân đi
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleBoardClick(e.getX(), e.getY());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Vẽ phác thảo cấu trúc bàn cờ 4 vùng màu cơ bản
        g2d.setColor(Color.WHITE);
        g2d.fillRect(20, 20, 460, 460);

        g2d.setColor(new Color(255, 102, 102)); // Vùng Đỏ (Top-Left)
        g2d.fillRect(20, 20, 180, 180);
        g2d.setColor(new Color(102, 178, 255)); // Vùng Xanh Dương (Top-Right)
        g2d.fillRect(300, 20, 180, 180);
        g2d.setColor(new Color(102, 255, 102)); // Vùng Xanh Lá (Bottom-Left)
        g2d.fillRect(20, 300, 180, 180);
        g2d.setColor(new Color(255, 255, 102)); // Vùng Vàng (Bottom-Right)
        g2d.fillRect(300, 300, 180, 180);

        // 2. Quét qua mảng dữ liệu để render hiển thị các quân cờ lên khung hình
        if (controller != null) {
            Player[] players = controller.getPlayers();
            for (Player p : players) {
                for (Horse h : p.getHorses()) {
                    drawHorseIcon(g2d, h);
                }
            }
        }
    }

    private void drawHorseIcon(Graphics2D g2d, Horse h) {
        Point pt = getCoordinates(h);
        // Thiết lập màu sắc đồ họa tương ứng với màu thực tế quân cờ
        switch (h.getColor()) {
            case RED -> g2d.setColor(Color.RED);
            case BLUE -> g2d.setColor(Color.BLUE);
            case GREEN -> g2d.setColor(Color.GREEN);
            case YELLOW -> g2d.setColor(Color.YELLOW);
        }

        // Báo sáng (Highlight) nếu quân cờ này được phép di chuyển hợp lệ
        if (controller.getHighlightedHorses().contains(h)) {
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(pt.x - 18, pt.y - 18, 36, 36);
        }

        g2d.fillOval(pt.x - 12, pt.y - 12, 24, 24);
        g2d.setColor(Color.WHITE);
        g2d.drawString(h.getId().substring(h.getId().length() - 1), pt.x - 4, pt.y + 5);
    }

    private Point getCoordinates(Horse h) {
        // Thuật toán giả lập phân bổ vị trí đồ họa dựa trên trạng thái dữ liệu lõi
        if (h.getState() == HorseState.IN_BASE) {
            int idx = Integer.parseInt(h.getId().substring(h.getId().length() - 1)) - 1;
            int offset = idx * 30;
            if (h.getColor() == PlayerColor.RED) return new Point(50 + offset, 60);
            if (h.getColor() == PlayerColor.BLUE) return new Point(330 + offset, 60);
            if (h.getColor() == PlayerColor.GREEN) return new Point(50 + offset, 350);
            return new Point(330 + offset, 350);
        }

        if (h.getState() == HorseState.ON_PATH) {
            int pos = h.getCurrentPosition();
            return new Point(220 + (pos % 3) * 20, 30 + (pos / 3) * 8); // Giả lập hiển thị tịnh tiến trên mảng đường đi công cộng
        }

        return new Point(250, 250); // Mặc định hiển thị tại tâm chuồng đích
    }

    private void handleBoardClick(int x, int y) {
        if (controller == null) return;
        // Quét tìm xem tọa độ click chuột có trúng vào vòng tròn của quân cờ nào không
        for (Player p : controller.getPlayers()) {
            for (Horse h : p.getHorses()) {
                Point pt = getCoordinates(h);
                if (Math.abs(pt.x - x) < 15 && Math.abs(pt.y - y) < 15) {
                    controller.handleHorseClick(h);
                    return;
                }
            }
        }
    }
}