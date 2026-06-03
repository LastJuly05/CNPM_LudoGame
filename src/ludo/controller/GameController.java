package ludo.controller;

import ludo.model.*;
import ludo.view.GameUI;
import java.util.ArrayList;
import java.util.List;

public class GameController {
    private Player[] players;
    private Board board;
    private Dice dice;
    private int currentPlayerIndex;
    private GameUI ui;

    private boolean hasRolled = false;
    private int currentV1 = 0, currentV2 = 0;
    private boolean v1Used = false;
    private boolean v2Used = false;
    private List<Horse> highlightedHorses = new ArrayList<>();
    private List<String> rankings = new ArrayList<>(); // Thứ hạng người thắng

    public GameController() {
        initGame();
    }

    private void initGame() {
        board = new Board();
        dice = new Dice();
        players = new Player[]{
            new Player("Đỏ", PlayerColor.RED),
            new Player("Xanh Dương", PlayerColor.BLUE),
            new Player("Xanh Lá", PlayerColor.GREEN),
            new Player("Vàng", PlayerColor.YELLOW)
        };
        currentPlayerIndex = 0;
        hasRolled = false;
        currentV1 = 0; currentV2 = 0;
        v1Used = false; v2Used = false;
        highlightedHorses.clear();
        rankings.clear();
    }

    public void setUI(GameUI ui) { this.ui = ui; }

    public void startGame() {
        currentPlayerIndex = 0;
        hasRolled = false;
        ui.showMessage("Lượt của: " + players[currentPlayerIndex].getName());
        ui.renderBoard(board, players);
    }

    public void restartGame() {
        initGame();
        ui.resetDiceDisplay();
        ui.showMessage("Game mới bắt đầu! Lượt của: " + players[0].getName());
        ui.renderBoard(board, players);
    }

    public void rollDiceRequest() {
        if (hasRolled) {
            ui.showMessage("Đã đổ rồi! Hãy chọn ngựa.");
            return;
        }
        // Kiểm tra người chơi hiện tại đã thắng chưa
        if (players[currentPlayerIndex].hasWon()) {
            nextPlayerTurn();
            return;
        }

        int[] result = dice.roll();
        currentV1 = result[0];
        currentV2 = result[1];
        hasRolled = true;

        v1Used = false; 
        v2Used = false;
        
        ui.updateDiceDisplay(currentV1, currentV2);
        ui.showMessage(players[currentPlayerIndex].getName() + " đổ được " + currentV1 + " và " + currentV2 + " (tổng: " + (currentV1 + currentV2) + ")");
        // Gọi hàm kiểm tra cản đường
        updateHighlightedHorses();
        // Tính ngựa hợp lệ và highlight
        highlightedHorses = players[currentPlayerIndex].getValidMoves(currentV1, currentV2);
        ui.renderBoard(board, players);

        if (highlightedHorses.isEmpty()) {
            ui.showMessage("Không có nước đi! Mất lượt.");
            new javax.swing.Timer(1500, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                endTurn(false);
            }).start();
        }
    }

    private void updateHighlightedHorses() {
        if (v1Used && v2Used) {
            highlightedHorses.clear();
        } else {
            int v1 = v1Used ? 0 : currentV1;
            int v2 = v2Used ? 0 : currentV2;
            
            // Lấy danh sách thô từ Player
            List<Horse> rawList = players[currentPlayerIndex].getValidMoves(v1, v2);
            highlightedHorses.clear();
            
            // UC5: Lọc lại danh sách, loại bỏ những con ngựa bị cản đường
            for (Horse h : rawList) {
                if (h.getState() == HorseState.IN_BASE || h.getState() == HorseState.IN_HOME) {
                    highlightedHorses.add(h);
                } else if (h.getState() == HorseState.ON_PATH) {
                    // Chỉ highlight nếu ít nhất 1 trong các hướng đi không bị cản
                    if ((v1 > 0 && canMove(h, v1)) || 
                        (v2 > 0 && canMove(h, v2)) || 
                        (v1 > 0 && v2 > 0 && canMove(h, v1 + v2))) {
                        highlightedHorses.add(h);
                    }
                }
            }
        }
        
        ui.renderBoard(board, players);

        if (highlightedHorses.isEmpty()) {
            ui.showMessage("Không có nước đi (hoặc bị cản đường)! Mất lượt.");
            new javax.swing.Timer(1500, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                endTurn(false); // Bị chặn/không đi được thì mất quyền thưởng lượt
            }).start();
        }
    }

    public void handleHorseClick(Horse clickedHorse) {
        if (!hasRolled) {
            ui.showMessage("Hãy đổ xúc xắc trước!");
            return;
        }
        if (clickedHorse.getColor() != players[currentPlayerIndex].getColor()) {
            ui.showMessage("Không phải ngựa của bạn!");
            return;
        }
        if (!highlightedHorses.contains(clickedHorse)) {
            ui.showMessage("Ngựa này không thể đi với kết quả này!");
            return;
        }

        // Xử lý từng trạng thái ngựa
        if (clickedHorse.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(clickedHorse.getColor());
            deployHorse(clickedHorse, startPos);
        } else if (clickedHorse.getState() == HorseState.ON_PATH) {
            int dist = clickedHorse.getDistanceTraveled();
            int sum = currentV1 + currentV2;
            if (dist < 55) {
                int steps;
                if (dist + sum <= 55) {
                    steps = sum;
                } else if (dist + currentV1 == 55) {
                    steps = currentV1;
                } else {
                    steps = currentV2;
                }
                moveHorseOnPath(clickedHorse, steps);
            } else {
                // dist == 55, vào chuồng đích ở bậc 1
                tryClimbHome(clickedHorse);
            }
        } else if (clickedHorse.getState() == HorseState.IN_HOME) {
            tryClimbHome(clickedHorse);
        }
    }

    private void tryClimbHome(Horse h) {
        int currentStep = (h.getState() == HorseState.ON_PATH) ? 0 : h.getHomeStep();
        int targetStep = currentStep + 1;
        int sum = currentV1 + currentV2;

        if (targetStep > 6) {
            ui.showMessage("Ngựa đã vào đích rồi!");
            return;
        }
        if (isHomeStepOccupied(h.getColor(), targetStep)) {
            ui.showMessage("Bậc " + targetStep + " đã có ngựa!");
            return;
        }
        if (currentV1 != targetStep && currentV2 != targetStep && sum != targetStep) {
            ui.showMessage("Cần đổ được " + targetStep + " để lên bậc này!");
            return;
        }

        // Xóa khỏi bàn cờ nếu đang ON_PATH
        if (h.getState() == HorseState.ON_PATH) {
            board.clearPosition(h.getCurrentPosition());
            h.setCurrentPosition(-1);
        }

        h.setHomeStep(targetStep);
        if (targetStep == 6) {
            h.setState(HorseState.FINISHED);
            ui.showMessage("🎉 Ngựa của " + players[currentPlayerIndex].getName() + " đã VỀ ĐÍCH!");
        } else {
            h.setState(HorseState.IN_HOME);
        }

        ui.renderBoard(board, players);

        if (players[currentPlayerIndex].hasWon()) {
            checkWinCondition();
        } else {
            endTurn(dice.isDouble());
        }
    }

    private boolean isHomeStepOccupied(PlayerColor color, int step) {
        for (Player p : players) {
            if (p.getColor() == color) {
                for (Horse h : p.getHorses()) {
                    if ((h.getState() == HorseState.IN_HOME || h.getState() == HorseState.FINISHED)
                            && h.getHomeStep() == step) return true;
                }
            }
        }
        return false;
    }

    private void checkWinCondition() {
        Player current = players[currentPlayerIndex];
        if (current.hasWon()) {
            rankings.add(current.getName());
            ui.showPopup("🏆 CHÚC MỪNG! " + current.getName() + " đã CHIẾN THẮNG!\nXếp hạng #" + rankings.size());
            // Kiểm tra tất cả đã xong chưa
            int finished = 0;
            for (Player p : players) { if (p.hasWon()) finished++; }
            if (finished >= 3) {
                // Tìm người cuối cùng
                for (Player p : players) { if (!p.hasWon()) rankings.add(p.getName() + " (chưa xong)"); }
                ui.showPopup("🎮 Game kết thúc!\nThứ hạng:\n" + String.join("\n", rankings.stream()
                    .map((r) -> (rankings.indexOf(r)+1) + ". " + r).toArray(String[]::new)));
            } else {
                nextPlayerTurn();
            }
        }
    }

    private void deployHorse(Horse h, int pos) {
        Horse occupier = board.getHorseAt(pos);
        if (occupier != null && occupier.getColor() == h.getColor()) {
            ui.showMessage("Ô xuất phát đã có ngựa của bạn!");
            return;
        }
        if (occupier != null) {
            board.clearPosition(pos);
            occupier.sendToBase();
            ui.showMessage("💥 Đã đá ngựa " + occupier.getColor() + " về chuồng!");
        }

        h.setCurrentPosition(pos);
        h.setState(HorseState.ON_PATH);
        h.setDistanceTraveled(0);
        board.setHorseAt(pos, h);

        ui.renderBoard(board, players);
        endTurn(dice.isDouble());
    }

    private void moveHorseOnPath(Horse h, int steps) {
        int oldPos = h.getCurrentPosition();
        int newDist = h.getDistanceTraveled() + steps;
        int newPos = (oldPos + steps) % 56;

        // Nếu đến đúng 55 bước: vào cửa chuồng, không chiếm ô ngoài
        if (newDist == 55) {
            board.clearPosition(oldPos);
            h.setCurrentPosition(newPos); // vẫn cập nhật pos để track
            h.setDistanceTraveled(55);
            // Không setHorseAt vì horse đang "ở cửa chuồng" không chiếm ô ngoài
            board.clearPosition(newPos); // xóa vị trí mới nếu có
            board.setHorseAt(newPos, h);
            ui.showMessage("🚪 Ngựa đã đến cửa chuồng! Đổ xúc xắc tiếp.");
            ui.renderBoard(board, players);
            endTurn(dice.isDouble());
            return;
        }

        // Kiểm tra va chạm
        Horse occupier = board.getHorseAt(newPos);
        if (occupier != null && occupier.getColor() == h.getColor()) {
            ui.showMessage("Ô đích đã có ngựa của bạn!");
            return;
        }
        if (occupier != null) {
            // Kiểm tra safe cell
            if (board.isSafeCell(newPos)) {
                ui.showMessage("Ô đó là ô an toàn, không thể đá ngựa!");
                return;
            }
            board.clearPosition(newPos);
            occupier.sendToBase();
            ui.showMessage("💥 Đã đá ngựa " + occupier.getColor() + " về chuồng!");
        }

        board.clearPosition(oldPos);
        h.setCurrentPosition(newPos);
        h.setDistanceTraveled(newDist);
        board.setHorseAt(newPos, h);

        ui.renderBoard(board, players);
        endTurn(dice.isDouble());
    }

    private void endTurn(boolean extraTurn) {
        hasRolled = false;
        highlightedHorses.clear();
        dice.reset();

        if (extraTurn) {
            ui.showMessage("🎲 Đổ được đôi! " + players[currentPlayerIndex].getName() + " được đổ thêm!");
        } else {
            nextPlayerTurn();
        }
        ui.renderBoard(board, players);
    }

    private void nextPlayerTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;
        // Bỏ qua người đã thắng
        int attempts = 0;
        while (players[currentPlayerIndex].hasWon() && attempts < 4) {
            currentPlayerIndex = (currentPlayerIndex + 1) % 4;
            attempts++;
        }
        ui.showMessage("🎯 Lượt của: " + players[currentPlayerIndex].getName());
    }

    // Helper: UC5 - Kiểm tra xem quãng đường di chuyển có bị ngựa khác cản không
    private boolean canMove(Horse h, int steps) {
        if (h.getDistanceTraveled() + steps > 55) return false; // Vượt quá cửa chuồng
        
        int startPos = h.getCurrentPosition();
        if (startPos != -1) { // Ngựa đang trên đường
            // Quét các ô từ ngay trước mặt đến sát ô đích
            for (int i = 1; i < steps; i++) {
                int checkPos = (startPos + i) % 56;
                if (board.getHorseAt(checkPos) != null) {
                    return false; // Bị cản đường bởi một quân ngựa khác
                }
            }
        }
        return true;
    }
    

    // Getters
    public Board getBoard() { return board; }
    public Player[] getPlayers() { return players; }
    public List<Horse> getHighlightedHorses() { return highlightedHorses; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public Dice getDice() { return dice; }
}