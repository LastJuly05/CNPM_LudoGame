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

        ui.updateDiceDisplay(currentV1, currentV2);
        ui.showMessage(players[currentPlayerIndex].getName() + " đổ được: [" + currentV1 + "], [" + currentV2 + "]");

        highlightedHorses = players[currentPlayerIndex].getValidMoves(currentV1, currentV2);
        ui.renderBoard(board, players);

        if (highlightedHorses.isEmpty()) {
            ui.showMessage("Không có nước đi hợp lệ! Mất lượt sau 1.5 giây...");
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
        endTurn(dice.isDouble());
    }

    private void moveHorseOnPath(Horse h, int steps) {
        int oldPos = h.getCurrentPosition();
        int newDist = h.getDistanceTraveled() + steps;

        if (newDist == 55) { // Tiến đến sát cửa chuồng đích
            board.clearPosition(oldPos);
            h.setCurrentPosition(-1);
            h.setDistanceTraveled(55);
            h.setState(HorseState.IN_HOME);
            h.setHomeStep(0);
            endTurn(dice.isDouble());
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
        endTurn(dice.isDouble());
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
     * Updates rankings, shows appropriate pop‑ups, and determines whether the match
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
                ui.showPopup("🎮 Trận đấu kết thúc hoàn toàn!");
                restartGame();
                return true;
            }
        }
        return false;
    }

    private void endTurn(boolean extraTurn) {
        hasRolled = false;
        highlightedHorses.clear();
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

    // --- Getters ---
    public Board getBoard() { return board; }
    public Player[] getPlayers() { return players; }
    public List<Horse> getHighlightedHorses() { return highlightedHorses; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
}