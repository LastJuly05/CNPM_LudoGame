package ludo.controllers; // Đồng bộ chuẩn theo ảnh thực tế của bạn

import ludo.models.*; // Import toàn bộ model (Board, Player, Horse, Dice, HorseState)
import ludo.views.GameUI;
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
    private List<Horse> highlightedHorses = new ArrayList<>();
    private List<String> rankings = new ArrayList<>(); // Lưu thứ hạng người thắng theo thời gian

    public GameController() {
        initGame();
    }

    /**
     * [Nhiệm vụ Sang] 1. KHỞI TẠO BÀN CHƠI (System Setup)
     */
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
        currentV1 = 0;
        currentV2 = 0;
        highlightedHorses.clear();
        rankings.clear();
    }

    public void setUI(GameUI ui) {
        this.ui = ui;
    }

    /**
     * [Nhiệm vụ Sang] 2. KÍCH HOẠT VÒNG LẶP TRẬN ĐẤU BAN ĐẦU
     */
    public void startGame() {
        currentPlayerIndex = 0;
        hasRolled = false;
        if (ui != null) {
            ui.showMessage("Trận đấu bắt đầu! Lượt của: " + players[currentPlayerIndex].getName());
            ui.renderBoard(board, players);
        }
    }

    /**
     * [Nhiệm vụ Sang] 3. RESET / RESTART TRẬN ĐẤU MỚI TỪ GIAO DIỆN
     */
    public void restartGame() {
        initGame();
        if (ui != null) {
            ui.resetDiceDisplay();
            ui.showMessage("Trò chơi đã được thiết lập lại! Lượt của: " + players[0].getName());
            ui.renderBoard(board, players);
        }
    }

    /**
     * YÊU CẦU TUNG XÚC XẮC (Nhận tín hiệu từ View)
     */
    public void rollDiceRequest() {
        if (hasRolled) {
            ui.showMessage("Bạn đã đổ xúc xắc rồi! Hãy chọn ngựa để đi.");
            return;
        }

        if (players[currentPlayerIndex].hasWon()) {
            nextPlayerTurn();
            return;
        }

        int[] result = dice.roll();
        currentV1 = result[0];
        currentV2 = result[1];
        hasRolled = true;

        ui.updateDiceDisplay(currentV1, currentV2);
        ui.showMessage(players[currentPlayerIndex].getName() + " đổ được: [" + currentV1 + "], [" + currentV2 + "] (Tổng: " + (currentV1 + currentV2) + ")");

        // Gọi phân hệ xử lý tính nước đi hợp lệ của Vy để highlight quân cờ
        highlightedHorses = players[currentPlayerIndex].getValidMoves(currentV1, currentV2);
        ui.renderBoard(board, players);

        // Trường hợp không có quân nào đi được thì tự động chuyển lượt
        if (highlightedHorses.isEmpty()) {
            ui.showMessage("Không có nước đi hợp lệ! Mất lượt sau 1.5 giây...");
            new javax.swing.Timer(1500, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                endTurn(false);
            }).start();
        }
    }

    /**
     * SỰ KIỆN CLICK CHỌN QUÂN CỜ TRÊN VIEW
     */
    public void handleHorseClick(Horse clickedHorse) {
        if (!hasRolled) {
            ui.showMessage("Vui lòng đổ xúc xắc trước!");
            return;
        }
        if (clickedHorse.getColor() != players[currentPlayerIndex].getColor()) {
            ui.showMessage("Đây không phải là quân cờ của bạn!");
            return;
        }
        if (!highlightedHorses.contains(clickedHorse)) {
            ui.showMessage("Quân cờ này không thể di chuyển với số điểm hiện tại!");
            return;
        }

        // Tích hợp xử lý các trạng thái di chuyển của quân cờ
        if (clickedHorse.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(clickedHorse.getColor());
            deployHorse(clickedHorse, startPos);
        } else if (clickedHorse.getState() == HorseState.ON_PATH) {
            int dist = clickedHorse.getDistanceTraveled();
            int sum = currentV1 + currentV2;

            if (dist < 55) {
                int steps = (dist + sum <= 55) ? sum : ((dist + currentV1 == 55) ? currentV1 : currentV2);
                moveHorseOnPath(clickedHorse, steps);
            } else {
                tryClimbHome(clickedHorse); // Vào chuồng đích
            }
        } else if (clickedHorse.getState() == HorseState.IN_HOME) {
            tryClimbHome(clickedHorse);
        }
    }

    private void deployHorse(Horse h, int pos) {
        Horse occupier = board.getHorseAt(pos);
        if (occupier != null && occupier.getColor() == h.getColor()) {
            ui.showMessage("Ô xuất phát đang bị chiếm bởi quân cùng màu!");
            return;
        }
        if (occupier != null) {
            board.clearPosition(pos);
            occupier.sendToBase();
            ui.showMessage("💥 Tuyệt vời! Đã đá ngựa màu " + occupier.getColor() + " về chuồng gốc!");
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

        if (newDist == 55) {
            board.clearPosition(oldPos);
            h.setCurrentPosition(newPos);
            h.setDistanceTraveled(55);
            board.setHorseAt(newPos, h);
            ui.showMessage("🚪 Quân cờ đã tiến sát đến cửa chuồng đích!");
            ui.renderBoard(board, players);
            endTurn(dice.isDouble());
            return;
        }

        Horse occupier = board.getHorseAt(newPos);
        if (occupier != null && occupier.getColor() == h.getColor()) {
            ui.showMessage("Ô đích đến đã có quân cờ của bạn đứng chặn!");
            return;
        }
        if (occupier != null) {
            if (board.isSafeCell(newPos)) {
                ui.showMessage("Đây là ô AN TOÀN hệ thống, không thể thực hiện đá cờ!");
                return;
            }
            board.clearPosition(newPos);
            occupier.sendToBase();
            ui.showMessage("💥 Đã đá quân cờ " + occupier.getColor() + " về chuồng!");
        }

        board.clearPosition(oldPos);
        h.setCurrentPosition(newPos);
        h.setDistanceTraveled(newDist);
        board.setHorseAt(newPos, h);

        ui.renderBoard(board, players);
        endTurn(dice.isDouble());
    }

    private void tryClimbHome(Horse h) {
        int currentStep = (h.getState() == HorseState.ON_PATH) ? 0 : h.getHomeStep();
        int targetStep = currentStep + 1;
        int sum = currentV1 + currentV2;

        if (targetStep > 6) return;
        if (isHomeStepOccupied(h.getColor(), targetStep)) {
            ui.showMessage("Bậc số " + targetStep + " của chuồng đích đã có ngựa chặn!");
            return;
        }
        if (currentV1 != targetStep && currentV2 != targetStep && sum != targetStep) {
            ui.showMessage("Bạn cần đổ trúng điểm số " + targetStep + " để leo bậc!");
            return;
        }

        if (h.getState() == HorseState.ON_PATH) {
            board.clearPosition(h.getCurrentPosition());
            h.setCurrentPosition(-1);
        }

        h.setHomeStep(targetStep);
        if (targetStep == 6) {
            h.setState(HorseState.FINISHED);
            ui.showMessage("🎉 Quân cờ của " + players[currentPlayerIndex].getName() + " đã về đích bậc 6 HOÀN THÀNH!");
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

    /**
     * [Nhiệm vụ Sang] 4. XÁC ĐỊNH THỨ HẠNG VÀ KIỂM TRA ĐIỀU KIỆN THẮNG CUỘC
     */
    public void checkWinCondition() {
        Player current = players[currentPlayerIndex];
        if (current.hasWon() && !rankings.contains(current.getName())) {
            rankings.add(current.getName());
            ui.showPopup("🏆 CHÚC MỪNG! Người chơi màu [" + current.getName() + "] đã hoàn thành trận đấu!\nXếp hạng: #" + rankings.size());

            int finishedCount = 0;
            for (Player p : players) {
                if (p.hasWon()) finishedCount++;
            }

            // Nếu 3/4 người chơi hoàn thành trận đấu, game kết thúc hoàn toàn
            if (finishedCount >= 3) {
                for (Player p : players) {
                    if (!p.hasWon()) rankings.add(p.getName() + " (Chưa hoàn thành)");
                }

                StringBuilder sb = new StringBuilder("🎮 TRẬN ĐẤU KẾT THÚC!\nBẢNG XẾP HẠNG CHUNG CUỘC:\n");
                for (int i = 0; i < rankings.size(); i++) {
                    sb.append((i + 1)).append(". ").append(rankings.get(i)).append("\n");
                }
                ui.showPopup(sb.toString());
                restartGame(); // Tự động reset bàn chơi mới
            } else {
                nextPlayerTurn();
            }
        }
    }

    private void endTurn(boolean extraTurn) {
        hasRolled = false;
        highlightedHorses.clear();
        dice.reset();

        if (extraTurn && !players[currentPlayerIndex].hasWon()) {
            ui.showMessage("🎲 Đổ trúng cặp đặc biệt! " + players[currentPlayerIndex].getName() + " được THƯỞNG THÊM LƯỢT!");
        } else {
            nextPlayerTurn();
        }
        ui.renderBoard(board, players);
    }

    /**
     * [Nhiệm vụ Sang] 5. CHUYỂN LƯỢT CHƠI LUÂN PHIÊN VÀ TỰ ĐỘNG BỎ QUA NGƯỜI THẮNG
     */
    public void nextPlayerTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;

        int safetyCounter = 0;
        // Vòng lặp bỏ qua (skip) người chơi nếu hasWon() trả về true
        while (players[currentPlayerIndex].hasWon() && safetyCounter < 4) {
            currentPlayerIndex = (currentPlayerIndex + 1) % 4;
            safetyCounter++;
        }

        ui.showMessage("🎯 Lượt chơi hiện tại thuộc về: " + players[currentPlayerIndex].getName());
    }

    // --- Getters phục vụ hiển thị đồ họa ---
    public Board getBoard() { return board; }
    public Player[] getPlayers() { return players; }
    public List<Horse> getHighlightedHorses() { return highlightedHorses; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public Dice getDice() { return dice; }
}