package ludo;

import ludo.controllers.GameController;
import ludo.views.GameUI;
import javax.swing.SwingUtilities;

public class main {
    public static void main(String[] args) {
        // Bảo vệ luồng đồ họa chạy ổn định
        SwingUtilities.invokeLater(() -> {
            GameController controller = new GameController();
            GameUI ui = new GameUI(controller);
            controller.setUI(ui);
            ui.setVisible(true);

            // Kích hoạt luồng chạy trận đấu cốt lõi của Sang
            controller.startGame();
        });
    }
}