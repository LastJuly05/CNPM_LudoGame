package ludo;

import ludo.controller.GameController;
import ludo.view.GameUI;
import javax.swing.SwingUtilities;

public class main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameController controller = new GameController();
            GameUI ui = new GameUI(controller);
            ui.setVisible(true);
            // startGame() không còn dùng — GameUI tự hỏi số người chơi
            // rồi gọi controller.startNewGame(playerCount)
        });
    }
}