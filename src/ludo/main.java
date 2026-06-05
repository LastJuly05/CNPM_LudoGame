package ludo;

import ludo.controller.GameController;
import ludo.view.GameUI;
import javax.swing.SwingUtilities;

public class main {
    public static void main(String[] args) {
        // Đảm bảo UI chạy trên Event Dispatch Thread của Swing
        SwingUtilities.invokeLater(() -> {
            GameController controller = new GameController();
            GameUI ui = new GameUI(controller);
            ui.setVisible(true);
            
            // Bắt đầu game
            controller.startGame();
        });
    }
}