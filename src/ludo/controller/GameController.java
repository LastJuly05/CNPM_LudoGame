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
    private List<String> rankings = new ArrayList<>();

    public GameController() {
        initGame();
    }

    /**
     * Initialise or reset the game state.
     * Sets up board, dice, players, and clears any previous turn data.
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
        currentV1 = 0; currentV2 = 0;
        v1Used = false; v2Used = false;
        highlightedHorses.clear();
        rankings.clear();
    }

    public void setUI(GameUI ui) { this.ui = ui; }

    /**
     * Starts a new game session.
     * Resets the current player index to the first player, clears any rolled state,
     * and notifies the UI to display the starting message and board.
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
     * Restarts the game after it has ended or when the user requests a reset.
     * Re-initialises the game state and updates the UI accordingly.
     */
    public void restartGame() {
        initGame();
        if (ui != null) {
            ui.resetDiceDisplay();
            ui.showMessage("Trò chơi đã chơi lại! Lượt của: " + players[0].getName());
            ui.renderBoard(board, players);
        }
    }

    public void rollDiceRequest() {
        if (hasRolled) {
            ui.showMessage("Bạn đã đổ rồi! Hãy bấm chọn quân cờ để di chuyển.");
            return;
        }

        int[] result = dice.roll();
        currentV1 = result[0];
        currentV2 = result[1];
        hasRolled = true;

        v1Used = false;
        v2Used = false;

        ui.updateDiceDisplay(currentV1, currentV2);
        ui.showMessage(players[currentPlayerIndex].getName() + " đổ được: [" + currentV1 + "], [" + currentV2 + "] (tổng: " + (currentV1 + currentV2) + ")");

        // UC5: Kiểm tra cản đường và highlight ngựa hợp lệ
        updateHighlightedHorses();

        ui.renderBoard(board, players);

        if (highlightedHorses.isEmpty()) {
            ui.showMessage("Không có nước đi hợp lệ! Mất lượt sau 1.5 giây...");
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
                if (h.getState() == HorseState.IN_BASE) {
                    // Kiểm tra ô xuất phát: nếu có ngựa cùng màu thì không thể xuất quân
                    int startPos = board.getStartPosition(h.getColor());
                    Horse occupier = board.getHorseAt(startPos);
                    if (occupier != null && occupier.getColor() == h.getColor()) {
                        continue;
                    }
                    highlightedHorses.add(h);
                } else if (h.getState() == HorseState.IN_HOME) {
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
                endTurn(false);
            }).start();
        }
    }

    public void handleHorseClick(Horse clickedHorse) {
        if (!hasRolled) {
            ui.showMessage("Hãy đổ xúc xắc trước!");
            return;
        }
        if (clickedHorse.getColor() != players[currentPlayerIndex].getColor()) {
            ui.showMessage("Không phải quân cờ của bạn!");
            return;
        }
        if (!highlightedHorses.contains(clickedHorse)) {
            ui.showMessage("Quân này không thể đi với số điểm hiện tại!");
            return;
        }

        if (clickedHorse.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(clickedHorse.getColor());
            deployHorse(clickedHorse, startPos);
        } else if (clickedHorse.getState() == HorseState.ON_PATH) {
            int dist = clickedHorse.getDistanceTraveled();
            int sum = currentV1 + currentV2;
            int steps = (dist + sum <= 55) ? sum : ((dist + currentV1 == 55) ? currentV1 : currentV2);
            moveHorseOnPath(clickedHorse, steps);
        } else if (clickedHorse.getState() == HorseState.IN_HOME) {
            tryClimbHome(clickedHorse);
        }
    }

    private void deployHorse(Horse h, int pos) {
        Horse occupier = board.getHorseAt(pos);
        if (occupier != null && occupier.getColor() == h.getColor()) return;
        if (occupier != null) {
            board.clearPosition(pos);
            occupier.sendToBase();
            ui.showMessage("💥 Đá ngựa " + occupier.getColor() + " về chuồng!");
        }
        h.setCurrentPosition(pos);
        h.setState(HorseState.ON_PATH);
        h.setDistanceTraveled(0);
        board.setHorseAt(pos, h);
        ui.renderBoard(board, players);
        endTurn(dice.isDeployable());
    }

    private void moveHorseOnPath(Horse h, int steps) {
        int oldPos = h.getCurrentPosition();
        int newDist = h.getDistanceTraveled() + steps;

        if (newDist == 55) {
            board.clearPosition(oldPos);
            h.setCurrentPosition(-1);
            h.setDistanceTraveled(55);
            h.setState(HorseState.IN_HOME);
            h.setHomeStep(0);
            ui.showMessage("🚪 Ngựa đã đến cửa chuồng! Đổ xúc xắc tiếp.");
            ui.renderBoard(board, players);
            endTurn(dice.isDeployable());
            return;
        }

        int newPos = (oldPos + steps) % 56;
        Horse occupier = board.getHorseAt(newPos);
        if (occupier != null && occupier.getColor() == h.getColor()) return;
        if (occupier != null) {
            if (board.isSafeCell(newPos)) {
                ui.showMessage("Ô an toàn, không thể đá cờ!");
                return;
            }
            board.clearPosition(newPos);
            occupier.sendToBase();
            ui.showMessage("💥 Đá ngựa " + occupier.getColor() + " về chuồng!");
        }

        board.clearPosition(oldPos);
        h.setCurrentPosition(newPos);
        h.setDistanceTraveled(newDist);
        board.setHorseAt(newPos, h);
        ui.renderBoard(board, players);
        endTurn(dice.isDeployable());
    }

    private void tryClimbHome(Horse h) {
        int targetStep = h.getHomeStep() + 1;
        if (targetStep > 6) return;

        if (currentV1 == targetStep || currentV2 == targetStep || (currentV1 + currentV2) == targetStep) {
            h.setHomeStep(targetStep);
            if (targetStep == 6) {
                h.setState(HorseState.FINISHED);
                ui.showMessage("🎉 Quân cờ đã về đích HOÀN THÀNH!");
            }
            ui.renderBoard(board, players);
            if (players[currentPlayerIndex].hasWon()) {
                boolean isGameOver = checkWinCondition();
                if (!isGameOver) {
                    endTurn(false);
                }
            } else {
                endTurn(dice.isDouble());
            }
        }
    }

    /**
     * Evaluates if the current player has satisfied a win condition.
     * Updates rankings, shows appropriate pop-ups, and determines whether the match
     * should continue, advance to the next player, or restart entirely.
     */
    public boolean checkWinCondition() {
        Player current = players[currentPlayerIndex];
        if (current.hasWon() && !rankings.contains(current.getName())) {
            rankings.add(current.getName());
            ui.showPopup("🏆 Người chơi màu [" + current.getName() + "] đã VỀ ĐÍCH! Hạng #" + rankings.size());

            int finishedCount = 0;
            for (Player p : players) if (p.hasWon()) finishedCount++;

            if (finishedCount >= 3) {
                ui.showPopup("🎮 Trận đấu kết thúc hoàn toàn!\nThứ hạng:\n" +
                        String.join("\n", rankings.stream()
                                .map(r -> (rankings.indexOf(r) + 1) + ". " + r).toArray(String[]::new)));
                restartGame();
                return true;
            } else {
                nextPlayerTurn();
            }
        }
        return false;
    }

    private void endTurn(boolean extraTurn) {
        hasRolled = false;
        highlightedHorses.clear();

        boolean isDouble = dice.isDouble();
        boolean isOneSix = (currentV1 == 1 && currentV2 == 6) || (currentV1 == 6 && currentV2 == 1);

        dice.reset();
        if (extraTurn && !players[currentPlayerIndex].hasWon()) {
            ui.showMessage("🎲 Đổ trúng cặp đôi! " + players[currentPlayerIndex].getName() + " được THƯỞNG THÊM 1 lượt.");
        } else {
            nextPlayerTurn();
        }
        ui.renderBoard(board, players);
    }

    /**
     * Advances the turn to the next eligible player.
     * Skips players who have already won, and informs the UI whose turn it is.
     */
    public void nextPlayerTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;
        int attempts = 0;
        while (players[currentPlayerIndex].hasWon() && attempts < 4) {
            currentPlayerIndex = (currentPlayerIndex + 1) % 4;
            attempts++;
        }
        ui.showMessage("🎯 Lượt của: " + players[currentPlayerIndex].getName());
    }

    // Helper: UC5 - Kiểm tra xem quãng đường di chuyển có bị ngựa khác cản không
    private boolean canMove(Horse h, int steps) {
        if (h.getDistanceTraveled() + steps > 55) return false;

        int startPos = h.getCurrentPosition();
        if (startPos != -1) {
            for (int i = 1; i < steps; i++) {
                int checkPos = (startPos + i) % 56;
                if (board.getHorseAt(checkPos) != null) {
                    return false;
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