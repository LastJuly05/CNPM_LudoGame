package ludo.view;

import ludo.controller.GameController;
import ludo.model.Board;
import ludo.model.Player;
import ludo.model.PlayerColor;

import javax.swing.*;
import java.awt.*;

public class GameUI extends JFrame {
    private GameController controller;
    private BoardPanel boardPanel;
    private JLabel statusLabel;
    private DicePanel dicePanel;

    public GameUI(GameController controller) {
        this.controller = controller;
        this.controller.setUI(this);
        initUI();
    }

    private void initUI() {
        setTitle("🎲 Cờ Cá Ngựa - Ludo Game");
        setSize(950, 750);
        setMinimumSize(new Dimension(800, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(35, 35, 50));

        // ---- Bàn cờ giữa ----
        boardPanel = new BoardPanel();
        boardPanel.setController(controller);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
        add(boardPanel, BorderLayout.CENTER);

        // ---- Panel bên phải ----
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(45, 45, 65));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 15));
        rightPanel.setPreferredSize(new Dimension(230, 700));

        // Tiêu đề
        JLabel title = new JLabel("🎯 CỜ CÁ NGỰA", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(255, 215, 0));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(title);
        rightPanel.add(Box.createVerticalStrut(10));

        // Nhãn lượt chơi
        statusLabel = new JLabel("<html><center>Chào mừng!</center></html>", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setPreferredSize(new Dimension(200, 60));
        statusLabel.setMaximumSize(new Dimension(210, 80));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 120), 1, true),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        rightPanel.add(statusLabel);
        rightPanel.add(Box.createVerticalStrut(15));

        // Panel xúc xắc
        dicePanel = new DicePanel();
        dicePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        dicePanel.setMaximumSize(new Dimension(210, 100));
        rightPanel.add(dicePanel);
        rightPanel.add(Box.createVerticalStrut(15));

        // Nút đổ xúc xắc
        JButton rollButton = makeButton("🎲 Đổ Xúc Xắc", new Color(70, 130, 200));
        rollButton.addActionListener(e -> controller.rollDiceRequest());
        rightPanel.add(rollButton);
        rightPanel.add(Box.createVerticalStrut(10));

        // Nút restart
        JButton restartButton = makeButton("🔄 Chơi Lại", new Color(80, 160, 80));
        restartButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn bắt đầu game mới?", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                controller.restartGame();
            }
        });
        rightPanel.add(restartButton);
        rightPanel.add(Box.createVerticalStrut(20));

        // Hướng dẫn
        JTextArea hint = new JTextArea(
            "📖 Luật chơi:\n" +
            "• Đổ đôi hoặc 1+6\n  → Xuất quân\n" +
            "• Đổ được đôi\n  → Đổ thêm lượt\n" +
            "• Ô xanh nhạt = an toàn\n" +
            "• Vòng tròn vàng = ngựa\n  có thể di chuyển\n" +
            "• 4 ngựa về đích = thắng"
        );
        hint.setEditable(false);
        hint.setFont(new Font("Arial", Font.PLAIN, 11));
        hint.setBackground(new Color(55, 55, 75));
        hint.setForeground(new Color(180, 200, 220));
        hint.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        hint.setMaximumSize(new Dimension(210, 200));
        rightPanel.add(hint);

        add(rightPanel, BorderLayout.EAST);
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 40));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(bg.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    public void renderBoard(Board board, Player[] players) {
        boardPanel.updateData(board, players);
    }

    public void updateDiceDisplay(int v1, int v2) {
        dicePanel.setValues(v1, v2);
        boolean special = (v1 == v2) || (v1 == 1 && v2 == 6) || (v1 == 6 && v2 == 1);
        dicePanel.setSpecial(special);
    }

    public void resetDiceDisplay() {
        dicePanel.setValues(0, 0);
        dicePanel.setSpecial(false);
    }

    public void showMessage(String message) {
        statusLabel.setText("<html><center>" + message.replace("\n", "<br>") + "</center></html>");
    }

    public void showPopup(String message) {
        JOptionPane.showMessageDialog(this, message, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---- Inner class vẽ xúc xắc ----
    static class DicePanel extends JPanel {
        private int v1 = 0, v2 = 0;
        private boolean special = false;

        DicePanel() {
            setPreferredSize(new Dimension(210, 95));
            setBackground(new Color(45, 45, 65));
        }

        void setValues(int v1, int v2) { this.v1 = v1; this.v2 = v2; repaint(); }
        void setSpecial(boolean s) { this.special = s; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Label "Xúc Xắc"
            g2d.setColor(new Color(180, 180, 200));
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            g2d.drawString("Kết quả xúc xắc:", 10, 15);

            int diceSize = 70;
            int gap = 10;
            int totalW = diceSize * 2 + gap;
            int startX = (getWidth() - totalW) / 2;
            int startY = 20;

            Color diceColor = special ? new Color(255, 220, 80) : Color.WHITE;
            drawDiceFace(g2d, startX, startY, diceSize, v1, diceColor);
            drawDiceFace(g2d, startX + diceSize + gap, startY, diceSize, v2, diceColor);
        }

        private void drawDiceFace(Graphics2D g2d, int x, int y, int size, int value, Color bg) {
            // Thân xúc xắc
            g2d.setColor(bg);
            g2d.fillRoundRect(x, y, size, size, 12, 12);
            g2d.setColor(new Color(100, 100, 130));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(x, y, size, size, 12, 12);
            g2d.setStroke(new BasicStroke(1f));

            if (value < 1 || value > 6) {
                g2d.setColor(new Color(180, 180, 180));
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("?", x + size/2 - 7, y + size/2 + 7);
                return;
            }

            // Vẽ chấm
            g2d.setColor(new Color(40, 40, 60));
            int r = size / 8;
            int[][] dots = getDotPositions(value, x, y, size);
            for (int[] dot : dots) {
                g2d.fillOval(dot[0] - r, dot[1] - r, r * 2, r * 2);
            }
        }

        private int[][] getDotPositions(int value, int x, int y, int s) {
            int p1 = s / 4, p2 = s / 2, p3 = s * 3 / 4;
            switch (value) {
                case 1: return new int[][]{{x+p2, y+p2}};
                case 2: return new int[][]{{x+p1, y+p1}, {x+p3, y+p3}};
                case 3: return new int[][]{{x+p1, y+p1}, {x+p2, y+p2}, {x+p3, y+p3}};
                case 4: return new int[][]{{x+p1,y+p1},{x+p3,y+p1},{x+p1,y+p3},{x+p3,y+p3}};
                case 5: return new int[][]{{x+p1,y+p1},{x+p3,y+p1},{x+p2,y+p2},{x+p1,y+p3},{x+p3,y+p3}};
                case 6: return new int[][]{{x+p1,y+p1},{x+p3,y+p1},{x+p1,y+p2},{x+p3,y+p2},{x+p1,y+p3},{x+p3,y+p3}};
                default: return new int[][]{};
            }
        }
    }
}