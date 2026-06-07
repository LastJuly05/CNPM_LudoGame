package ludo.view;

import ludo.controller.GameController;
import ludo.model.Board;
import ludo.model.Player;

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
        SwingUtilities.invokeLater(this::showPlayerSelectionDialog);
    }

    private void initUI() {
        setTitle("Cờ Cá Ngựa - Ludo Game");
        setSize(950, 750);
        setMinimumSize(new Dimension(800, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(35, 35, 50));

        boardPanel = new BoardPanel();
        boardPanel.setController(controller);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
        add(boardPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(45, 45, 65));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 15));
        rightPanel.setPreferredSize(new Dimension(230, 700));

        JLabel title = new JLabel("CỜ CÁ NGỰA", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(255, 215, 0));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(title);

        JLabel subtitle = new JLabel("LUDO GAME", SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 11));
        subtitle.setForeground(new Color(200, 200, 200));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(subtitle);
        rightPanel.add(Box.createVerticalStrut(10));

        statusLabel = new JLabel("<html><center>Chọn số người chơi<br>để bắt đầu!</center></html>", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setPreferredSize(new Dimension(200, 70));
        statusLabel.setMaximumSize(new Dimension(210, 90));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 120), 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        rightPanel.add(statusLabel);
        rightPanel.add(Box.createVerticalStrut(12));

        dicePanel = new DicePanel();
        dicePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        dicePanel.setMaximumSize(new Dimension(210, 100));
        rightPanel.add(dicePanel);
        rightPanel.add(Box.createVerticalStrut(12));

        JButton rollButton = makeButton("[ Đổ Xúc Xắc ]", new Color(70, 130, 200));
        rollButton.addActionListener(e -> controller.rollDiceRequest());
        rightPanel.add(rollButton);
        rightPanel.add(Box.createVerticalStrut(8));

        JButton restartButton = makeButton("[ Chơi Lại ]", new Color(80, 160, 80));
        restartButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc muốn bắt đầu game mới?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) controller.restartGame();
        });
        rightPanel.add(restartButton);
        rightPanel.add(Box.createVerticalStrut(16));

        JTextArea hint = new JTextArea(
                "LUẬT CHƠI:\n" +
                        "- Đổ đôi hoặc 1+6\n" +
                        "  => Xuất quân\n" +
                        "- Đổ được đôi\n" +
                        "  => Đổ thêm lượt\n" +
                        "- Ô xanh nhạt = an toàn\n" +
                        "- Nhấn vào ngựa sáng\n" +
                        "  để di chuyển\n" +
                        "- 4 ngựa về đích = thắng"
        );
        hint.setEditable(false);
        hint.setFont(new Font("Arial", Font.PLAIN, 11));
        hint.setBackground(new Color(55, 55, 75));
        hint.setForeground(new Color(180, 200, 220));
        hint.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(80, 80, 120)),
                        "Hướng dẫn", 0, 0,
                        new Font("Arial", Font.BOLD, 11),
                        new Color(180, 200, 220)
                ),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        hint.setMaximumSize(new Dimension(210, 180));
        rightPanel.add(hint);

        add(rightPanel, BorderLayout.EAST);
    }

    // =========================================================================
    // DIALOG CHỌN CHẾ ĐỘ CHƠI
    // =========================================================================
    public void showPlayerSelectionDialog() {
        JDialog dialog = new JDialog(this, "Chọn chế độ chơi", true);
        dialog.setSize(420, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(new Color(35, 35, 50));

        JLabel titleLbl = new JLabel("CỜ CÁ NGỰA", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Arial", Font.BOLD, 22));
        titleLbl.setForeground(new Color(255, 215, 0));
        titleLbl.setBorder(BorderFactory.createEmptyBorder(18, 0, 4, 0));
        dialog.add(titleLbl, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(new Color(35, 35, 50));
        center.setBorder(BorderFactory.createEmptyBorder(6, 24, 6, 24));

        // ── Nhóm 1: Nhiều người chơi ─────────────────────────────────────────
        addSectionLabel(center, "NHIỀU NGƯỜI CHƠI");

        String[][] pvpModes = {
                {"2 Người Chơi",  "Đỏ vs Lục"},
                {"3 Người Chơi",  "Đỏ vs Lam vs Lục"},
                {"4 Người Chơi",  "Đỏ vs Lam vs Lục vs Vàng"}
        };
        String[] pvpCodes = {"2p", "3p", "4p"};
        Color[] pvpColors = {new Color(60, 120, 190), new Color(80, 150, 80), new Color(160, 80, 80)};
        for (int i = 0; i < pvpModes.length; i++) {
            addModeButton(center, dialog, pvpModes[i][0], pvpModes[i][1], pvpCodes[i], pvpColors[i]);
            if (i < pvpModes.length - 1) center.add(Box.createVerticalStrut(6));
        }

        center.add(Box.createVerticalStrut(12));

        // ── Nhóm 2: Người vs Máy ─────────────────────────────────────────────
        addSectionLabel(center, "NGƯỜI vs MÁY");

        String[][] pvbModes = {
                {"1 Người + 1 Máy",  "Bạn (Đỏ) vs Máy (Lục)"},
                {"1 Người + 2 Máy",  "Bạn (Đỏ) vs 2 Máy"},
                {"1 Người + 3 Máy",  "Bạn (Đỏ) vs 3 Máy"}
        };
        String[] pvbCodes = {"2b", "3b", "4b"};
        Color[] pvbColors = {new Color(140, 80, 160), new Color(150, 90, 50), new Color(60, 130, 130)};
        for (int i = 0; i < pvbModes.length; i++) {
            addModeButton(center, dialog, pvbModes[i][0], pvbModes[i][1], pvbCodes[i], pvbColors[i]);
            if (i < pvbModes.length - 1) center.add(Box.createVerticalStrut(6));
        }

        center.add(Box.createVerticalStrut(12));

        // ── Nhóm 3: Máy vs Máy ───────────────────────────────────────────────
        addSectionLabel(center, "MÁY vs MÁY");
        addModeButton(center, dialog, "Xem Máy Tự Đấu", "4 Máy tự chơi với nhau", "bot", new Color(80, 80, 100));

        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(new Color(35, 35, 50));
        dialog.add(scroll, BorderLayout.CENTER);

        JButton exitBtn = new JButton("Thoát game");
        exitBtn.setFont(new Font("Arial", Font.PLAIN, 11));
        exitBtn.setBackground(new Color(80, 40, 40));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setFocusPainted(false);
        exitBtn.setBorderPainted(false);
        exitBtn.addActionListener(e -> System.exit(0));
        JPanel south = new JPanel();
        south.setBackground(new Color(35, 35, 50));
        south.add(exitBtn);
        dialog.add(south, BorderLayout.SOUTH);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setVisible(true);
    }

    private void addSectionLabel(JPanel parent, String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 11));
        lbl.setForeground(new Color(160, 160, 180));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 2, 3, 0));
        parent.add(lbl);
    }

    private void addModeButton(JPanel parent, JDialog dialog,
                               String mainText, String subText,
                               String modeCode, Color color) {
        JPanel btn = new JPanel(new BorderLayout(8, 0));
        btn.setBackground(color.darker());
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        btn.setMaximumSize(new Dimension(370, 46));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel mainLbl = new JLabel(mainText);
        mainLbl.setFont(new Font("Arial", Font.BOLD, 13));
        mainLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel(subText);
        subLbl.setFont(new Font("Arial", Font.PLAIN, 11));
        subLbl.setForeground(new Color(200, 200, 200));

        btn.add(mainLbl, BorderLayout.WEST);
        btn.add(subLbl, BorderLayout.EAST);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dialog.dispose();
                controller.startNewGame(modeCode);
            }
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(color.brighter()); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(color.darker()); }
        });

        parent.add(btn);
    }

    // =========================================================================
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
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    public void renderBoard(Board board, Player[] players) { boardPanel.updateData(board, players); }

    public void updateDiceDisplay(int v1, int v2) {
        dicePanel.setValues(v1, v2);
        boolean special = (v1 == v2) || (v1 == 1 && v2 == 6) || (v1 == 6 && v2 == 1);
        dicePanel.setSpecial(special);
    }

    public void resetDiceDisplay() { dicePanel.setValues(0, 0); dicePanel.setSpecial(false); }

    public void showMessage(String message) {
        String safe = message
                .replace("⭐", "[*]").replace("🎲", "[~]").replace("🚀", "[>]")
                .replace("🚪", "[D]").replace("💥", "[!]").replace("🎉", "[v]")
                .replace("✅", "[+]").replace("🎯", "[o]").replace("🏆", "[W]").replace("🎮", "[G]");
        statusLabel.setText("<html><center>" + safe.replace("\n", "<br>") + "</center></html>");
    }

    public void showPopup(String message) {
        JOptionPane.showMessageDialog(this, message, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── DicePanel ─────────────────────────────────────────────────────────────
    static class DicePanel extends JPanel {
        private int v1 = 0, v2 = 0;
        private boolean special = false;

        DicePanel() { setPreferredSize(new Dimension(210, 95)); setBackground(new Color(45, 45, 65)); }
        void setValues(int v1, int v2) { this.v1 = v1; this.v2 = v2; repaint(); }
        void setSpecial(boolean s) { this.special = s; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(180, 180, 200));
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            g2d.drawString("Kết quả xúc xắc:", 10, 15);
            int diceSize = 70, gap = 10;
            int totalW = diceSize * 2 + gap;
            int startX = (getWidth() - totalW) / 2;
            Color diceColor = special ? new Color(255, 220, 80) : Color.WHITE;
            drawDiceFace(g2d, startX, 20, diceSize, v1, diceColor);
            drawDiceFace(g2d, startX + diceSize + gap, 20, diceSize, v2, diceColor);
        }

        private void drawDiceFace(Graphics2D g2d, int x, int y, int size, int value, Color bg) {
            g2d.setColor(bg);
            g2d.fillRoundRect(x, y, size, size, 12, 12);
            g2d.setColor(new Color(100, 100, 130));
            g2d.setStroke(new java.awt.BasicStroke(1.5f));
            g2d.drawRoundRect(x, y, size, size, 12, 12);
            g2d.setStroke(new java.awt.BasicStroke(1f));
            if (value < 1 || value > 6) {
                g2d.setColor(new Color(180, 180, 180));
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("?", x + size/2 - 7, y + size/2 + 7);
                return;
            }
            g2d.setColor(new Color(40, 40, 60));
            int r = size / 8;
            for (int[] dot : getDotPositions(value, x, y, size))
                g2d.fillOval(dot[0] - r, dot[1] - r, r * 2, r * 2);
        }

        private int[][] getDotPositions(int value, int x, int y, int s) {
            int p1 = s/4, p2 = s/2, p3 = s*3/4;
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