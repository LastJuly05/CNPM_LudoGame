package ludo.controller;

import ludo.model.*;
import ludo.view.GameUI;
import javax.swing.JOptionPane;
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
        ui.showMessage(players[currentPlayerIndex].getName() + " đổ được: [" + currentV1 + "], [" + currentV2 + "]");

        updateHighlightedHorses();
    }

    private void updateHighlightedHorses() {
        if (v1Used && v2Used) {
            highlightedHorses.clear();
        } else {
            int v1 = v1Used ? 0 : currentV1;
            int v2 = v2Used ? 0 : currentV2;

            List<Horse> rawList = players[currentPlayerIndex].getValidMoves(v1, v2);
            highlightedHorses.clear();

            int startPos = board.getStartPosition(players[currentPlayerIndex].getColor());

            for (Horse h : rawList) {
                if (h.getState() == HorseState.IN_BASE) {
                    Horse occupier = board.getHorseAt(startPos);
                    if (occupier == null || occupier.getColor() != players[currentPlayerIndex].getColor()) {
                        highlightedHorses.add(h);
                    }
                } else if (h.getState() == HorseState.IN_HOME) {
                    highlightedHorses.add(h);
                } else if (h.getState() == HorseState.ON_PATH) {
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

            Horse occupier = board.getHorseAt(startPos);
            if (occupier != null && occupier.getColor() == clickedHorse.getColor()) {
                ui.showMessage("Ô xuất phát đã có ngựa của bạn, không thể ra quân!");
                return;
            }

            if (currentV1 == currentV2) {
                if (!v1Used) v1Used = true;
                else v2Used = true;
            } else {
                v1Used = true;
                v2Used = true;
            }
            deployHorse(clickedHorse, startPos);
            checkTurnEnd();

        } else if (clickedHorse.getState() == HorseState.ON_PATH) {
            int dist = clickedHorse.getDistanceTraveled();
            int remainingToGate = 55 - dist; // Khoảng cách còn lại để đến cửa chuồng

            List<Integer> choices = new ArrayList<>();
            List<String> optionsList = new ArrayList<>();

            boolean canV1 = !v1Used && canMove(clickedHorse, currentV1);
            boolean canV2 = !v2Used && canMove(clickedHorse, currentV2);
            boolean canSum = !v1Used && !v2Used && canMove(clickedHorse, currentV1 + currentV2);

            if (canV1) {
                choices.add(currentV1);
                if (currentV1 == remainingToGate) {
                    optionsList.add("Về cửa chuồng (Dùng xúc xắc " + currentV1 + ")");
                } else {
                    optionsList.add("Đi " + currentV1 + " bước");
                }
            }
            if (canV2 && !choices.contains(currentV2)) {
                choices.add(currentV2);
                if (currentV2 == remainingToGate) {
                    optionsList.add("Về cửa chuồng (Dùng xúc xắc " + currentV2 + ")");
                } else {
                    optionsList.add("Đi " + currentV2 + " bước");
                }
            }
            if (canSum) {
                choices.add(currentV1 + currentV2);
                if (currentV1 + currentV2 == remainingToGate) {
                    optionsList.add("Gộp điểm về cửa chuồng (Dùng tổng " + (currentV1 + currentV2) + ")");
                } else {
                    optionsList.add("Gộp đi " + (currentV1 + currentV2) + " bước");
                }
            }

            if (choices.isEmpty()) return;

            // Nếu chỉ có 1 lựa chọn duy nhất và đó là điểm gộp
            if (highlightedHorses.size() == 1 && !dice.isDeployable() && canSum && choices.size() == 1) {
                choices.clear();
                optionsList.clear();
                choices.add(currentV1 + currentV2);
                if (currentV1 + currentV2 == remainingToGate) {
                    optionsList.add("Gộp điểm về cửa chuồng (Dùng tổng " + (currentV1 + currentV2) + ")");
                } else {
                    optionsList.add("Gộp đi " + (currentV1 + currentV2) + " bước");
                }
            }

            int chosenSteps = choices.get(0);

            // Nếu có nhiều hơn 1 lựa chọn, hiển thị bảng hỏi người chơi
            if (choices.size() > 1) {
                String[] optionsArray = optionsList.toArray(new String[0]);
                int choiceIdx = JOptionPane.showOptionDialog(ui,
                        "Bạn muốn dùng điểm nào cho ngựa này?", "Chọn Nước Đi Về Đích",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, optionsArray, optionsArray[0]);

                if (choiceIdx >= 0 && choiceIdx < choices.size()) {
                    chosenSteps = choices.get(choiceIdx);
                } else {
                    return; // Người chơi nhấn X hoặc Cancel để hủy lệnh
                }
            }

            // Thực hiện di chuyển quân cờ đi số bước đã chọn
            moveHorseOnPath(clickedHorse, chosenSteps);

            // Cập nhật trạng thái xúc xắc đã sử dụng
            if (chosenSteps == (currentV1 + currentV2) && !v1Used && !v2Used) {
                v1Used = true;
                v2Used = true;
            }
            else if (chosenSteps == currentV1 && !v1Used) {
                v1Used = true;
            }
            else if (chosenSteps == currentV2 && !v2Used) {
                v2Used = true;
            }
            else {
                if (!v1Used) v1Used = true;
                else v2Used = true;
            }

            checkTurnEnd();

        } else if (clickedHorse.getState() == HorseState.IN_HOME) {
            int currentStep = clickedHorse.getHomeStep();
            List<Integer> choices = new ArrayList<>();
            List<String> optionsList = new ArrayList<>();

            int v1 = v1Used ? 0 : currentV1;
            int v2 = v2Used ? 0 : currentV2;

            if (v1 > 0 && canClimb(clickedHorse.getColor(), currentStep + v1)) {
                choices.add(v1);
                optionsList.add("Lên bậc " + (currentStep + v1));
            }
            if (v2 > 0 && v2 != v1 && canClimb(clickedHorse.getColor(), currentStep + v2)) {
                choices.add(v2);
                optionsList.add("Lên bậc " + (currentStep + v2));
            }
            if (v1 > 0 && v2 > 0 && canClimb(clickedHorse.getColor(), currentStep + v1 + v2)) {
                choices.add(v1 + v2);
                optionsList.add("Gộp lên bậc " + (currentStep + v1 + v2));
            }

            if (choices.isEmpty()) return;

            int chosenSteps = choices.get(0);
            if (choices.size() > 1) {
                String[] optionsArray = optionsList.toArray(new String[0]);
                int choiceIdx = JOptionPane.showOptionDialog(ui,
                        "Chọn bậc muốn lên:", "Thăng Bậc Chuồng Đích",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, optionsArray, optionsArray[0]);
                if (choiceIdx >= 0 && choiceIdx < choices.size()) {
                    chosenSteps = choices.get(choiceIdx);
                } else {
                    return;
                }
            }

            tryClimbHome(clickedHorse, currentStep + chosenSteps);

            if (chosenSteps == (currentV1 + currentV2)) {
                v1Used = true; v2Used = true;
            } else if (chosenSteps == currentV1 && !v1Used) {
                v1Used = true;
            } else if (chosenSteps == currentV2 && !v2Used) {
                v2Used = true;
            } else {
                if (!v1Used) v1Used = true;
                else v2Used = true;
            }

            checkTurnEnd();
        }
    }

    private boolean canMove(Horse h, int steps) {
        int currentDist = h.getDistanceTraveled();
        int remainingToGate = 55 - currentDist;

        if (currentDist >= 55) return false;

        // LUẬT MỚI: Chỉ cho phép đi nếu số bước nhỏ hơn hoặc bằng đúng khoảng cách còn lại tới cửa chuồng
        if (steps > remainingToGate) {
            return false;
        }

        int startPos = h.getCurrentPosition();
        if (startPos != -1) {
            for (int i = 1; i < steps; i++) {
                int checkPos = (startPos + i) % 56;
                if (board.getHorseAt(checkPos) != null) {
                    return false; // Bị cản đường
                }
            }
        }
        return true;
    }

    private boolean canClimb(PlayerColor color, int targetStep) {
        return targetStep <= 6 && !isHomeStepOccupied(color, targetStep);
    }

    private void checkTurnEnd() {
        if (players[currentPlayerIndex].hasWon()) {
            checkWinCondition();
            return;
        }

        if (v1Used && v2Used) {
            endTurn(dice.isDeployable());
        } else {
            int remaining = !v1Used ? currentV1 : currentV2;
            ui.showMessage("Bạn còn điểm " + remaining + ". Hãy chọn ngựa đi tiếp!");
            updateHighlightedHorses();
        }
    }

    private void tryClimbHome(Horse h, int targetStep) {
        if (h.getState() == HorseState.ON_PATH) {
            board.clearPosition(h.getCurrentPosition());
            h.setCurrentPosition(-1);
        }

        h.setHomeStep(targetStep);
        if (targetStep == 6) {
            h.setState(HorseState.FINISHED);
            ui.showMessage("🎉 Quân cờ đã về đích HOÀN THÀNH!");
        } else {
            h.setState(HorseState.IN_HOME);
        }
        ui.renderBoard(board, players);
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

    public boolean checkWinCondition() {
        Player current = players[currentPlayerIndex];

        if (current.hasWon() && !rankings.contains(current.getName())) {
            rankings.add(current.getName());

            int currentRank = rankings.size();
            ui.showPopup("🏆 Chúc mừng người chơi [" + current.getName() + "] đã VỀ ĐÍCH! Đạt Hạng #" + currentRank);

            int finishedCount = 0;
            for (Player p : players) {
                if (p.hasWon()) finishedCount++;
            }

            if (finishedCount >= 3) {
                for (Player p : players) {
                    if (!rankings.contains(p.getName())) {
                        rankings.add(p.getName());
                    }
                }

                StringBuilder scoreboard = new StringBuilder("🎮 TRẬN ĐẤU KẾT THÚC HOÀN TOÀN! 🎮\n\n");
                scoreboard.append("🏆 BẢNG XẾP HẠNG CHUNG CUỘC:\n");
                for (int i = 0; i < rankings.size(); i++) {
                    scoreboard.append("  Hạng ").append(i + 1).append(": ").append(rankings.get(i)).append("\n");
                }

                ui.showPopup(scoreboard.toString());
                restartGame();
                return true;
            } else {
                nextPlayerTurn();
            }
        }
        return false;
    }

    private void deployHorse(Horse h, int pos) {
        Horse occupier = board.getHorseAt(pos);
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
    }

    private void moveHorseOnPath(Horse h, int steps) {
        int oldPos = h.getCurrentPosition();
        int newDist = h.getDistanceTraveled() + steps;
        int newPos = (oldPos + steps) % 56;

        if (newDist == 55) {
            board.clearPosition(oldPos);
            h.setCurrentPosition(newPos);
            h.setDistanceTraveled(55);
            board.clearPosition(newPos);
            board.setHorseAt(newPos, h);
            ui.showMessage("🚪 Ngựa đã đến cửa chuồng!");
            ui.renderBoard(board, players);
            return;
        }

        Horse occupier = board.getHorseAt(newPos);
        if (occupier != null) {
            board.clearPosition(newPos);
            occupier.sendToBase();
            ui.showMessage("💥 Đá ngựa " + occupier.getColor() + " về chuồng!");
        }

        board.clearPosition(oldPos);
        h.setCurrentPosition(newPos);
        h.setDistanceTraveled(newDist);
        board.setHorseAt(newPos, h);
        ui.renderBoard(board, players);
    }

    private void endTurn(boolean extraTurn) {
        hasRolled = false;
        v1Used = false;
        v2Used = false;
        highlightedHorses.clear();
        dice.reset();

        if (extraTurn && !players[currentPlayerIndex].hasWon()) {
            String reason = dice.isDouble() ? "CẶP ĐÔI TRÙNG NHAU" : "CẶP ĐẶC BIỆT 1-6";
            ui.showMessage("🎲 Đổ trúng [" + reason + "]! " + players[currentPlayerIndex].getName() + " được THƯỞNG THÊM 1 lượt.");
        } else {
            nextPlayerTurn();
        }
        ui.renderBoard(board, players);
    }

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
    public Dice getDice() { return dice; }
}