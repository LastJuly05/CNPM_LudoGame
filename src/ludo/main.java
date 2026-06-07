package ludo;

import javax.swing.SwingUtilities;
import ludo.controller.GameController;
import ludo.view.GameUI;

public class main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameController controller = new GameController();
            GameUI ui = new GameUI(controller);
            controller.setUI(ui);
            ui.setVisible(true);
            // startGame() không còn dùng — GameUI tự hỏi số người chơi
            // rồi gọi controller.startNewGame(playerCount)
        });
    }
}