package ludo.views;

import ludo.controllers.GameController;
import ludo.models.Board;
import ludo.models.Player;
import javax.swing.*;
import java.awt.*;

public class GameUI extends JFrame {
    private BoardPanel boardPanel;
    private JLabel lblStatus;
    private JLabel lblDiceDisplay;
    private JButton btnRoll;
    private JButton btnRestart;

    public GameUI(GameController controller) {
        setTitle("Dự án Game Ludo - Mô hình MVC Công nghệ Phần mềm");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        boardPanel = new BoardPanel(controller);
        add(boardPanel, BorderLayout.CENTER);

        // Thanh công cụ điều khiển phía dưới màn hình
        JPanel pnlControl = new JPanel(new FlowLayout());
        lblStatus = new JLabel("Trạng thái: Sẵn sàng vào trận.");
        lblDiceDisplay = new JLabel("Xúc xắc: [1] [1]");

        btnRoll = new JButton("Đổ Xúc Xắc");
        btnRoll.addActionListener(e -> controller.rollDiceRequest());

        btnRestart = new JButton("Chơi lại trận mới");
        btnRestart.addActionListener(e -> controller.restartGame());

        pnlControl.add(lblStatus);
        pnlControl.add(lblDiceDisplay);
        pnlControl.add(btnRoll);
        pnlControl.add(btnRestart);
        add(pnlControl, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    public void renderBoard(Board board, Player[] players) {
        boardPanel.repaint(); // Ép hệ thống vẽ lại đồ họa toàn bộ bàn cờ
    }

    public void updateDiceDisplay(int v1, int v2) {
        lblDiceDisplay.setText("Xúc xắc: [" + v1 + "] [" + v2 + "]");
    }

    public void resetDiceDisplay() {
        lblDiceDisplay.setText("Xúc xắc: [1] [1]");
    }

    public void showMessage(String msg) {
        lblStatus.setText(msg);
    }

    public void showPopup(String content) {
        JOptionPane.showMessageDialog(this, content);
    }
}