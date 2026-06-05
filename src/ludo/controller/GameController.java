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
        ui.showMessage(players[currentPlayerIndex].getName() + " đổ được " + currentV1 + " và " + currentV2);

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
            
            // FIX LỖI TỰ ĐÁ QUÂN MÌNH: Lấy ô xuất phát của người chơi hiện tại
            int startPos = board.getStartPosition(players[currentPlayerIndex].getColor());
            
            for (Horse h : rawList) {
                if (h.getState() == HorseState.IN_BASE) {
                    // Chỉ cho phép highlight ngựa trong chuồng nếu ô xuất phát KHÔNG bị chặn bởi quân mình
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
            ui.showMessage("Không phải ngựa của bạn!");
            return;
        }
        if (!highlightedHorses.contains(clickedHorse)) {
            ui.showMessage("Ngựa này không thể đi với kết quả hiện tại!");
            return;
        }

        if (clickedHorse.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(clickedHorse.getColor());
            
            // Failsafe: Chống đá quân mình lần 2 (dù đã chặn ở bước highlight)
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
            List<Integer> choices = new ArrayList<>();
            List<String> optionsList = new ArrayList<>();
            
            boolean canV1 = !v1Used && canMove(clickedHorse, currentV1);
            boolean canV2 = !v2Used && canMove(clickedHorse, currentV2);
            boolean canSum = !v1Used && !v2Used && canMove(clickedHorse, currentV1 + currentV2);
            
            if (canV1) { 
                choices.add(currentV1); 
                optionsList.add("Đi " + currentV1 + " bước"); 
            }
            if (canV2 && !choices.contains(currentV2)) { 
                choices.add(currentV2); 
                optionsList.add("Đi " + currentV2 + " bước"); 
            }
            if (canSum) { 
                choices.add(currentV1 + currentV2); 
                optionsList.add("Gộp đi " + (currentV1 + currentV2) + " bước"); 
            }
            
            if (choices.isEmpty()) return;

            // FIX LỖI POP-UP LÀM PHIỀN: Bỏ qua điều kiện isDeployable
            // Chỉ cần có 1 con ngựa duy nhất có thể đi và nó gộp được -> Tự động gộp luôn
            if (highlightedHorses.size() == 1 && canSum) {
                choices.clear();
                optionsList.clear();
                choices.add(currentV1 + currentV2);
                optionsList.add("Gộp đi " + (currentV1 + currentV2) + " bước");
            }
            
            int chosenSteps = choices.get(0); 
            
            if (choices.size() > 1) {
                String[] optionsArray = optionsList.toArray(new String[0]);
                int choiceIdx = JOptionPane.showOptionDialog(ui,
                    "Bạn muốn dùng điểm nào cho ngựa này?", "Chọn Nước Đi",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, optionsArray, optionsArray[0]);
                    
                if (choiceIdx >= 0 && choiceIdx < choices.size()) {
                    chosenSteps = choices.get(choiceIdx);
                } else {
                    return; // Nhấn X để hủy
                }
            }
            
            moveHorseOnPath(clickedHorse, chosenSteps);
            
            if (chosenSteps == currentV1 && !v1Used) { v1Used = true; }
            else if (chosenSteps == currentV2 && !v2Used) { v2Used = true; }
            else { v1Used = true; v2Used = true; }
            
            checkTurnEnd();
            
        } else if (clickedHorse.getState() == HorseState.IN_HOME) {
            int targetStep = clickedHorse.getHomeStep() + 1;
            List<Integer> choices = new ArrayList<>();
            
            if (!v1Used && currentV1 == targetStep && canClimb(clickedHorse.getColor(), targetStep)) {
                choices.add(currentV1);
            }
            if (!v2Used && currentV2 == targetStep && canClimb(clickedHorse.getColor(), targetStep)) {
                if (!choices.contains(currentV2)) choices.add(currentV2);
            }
            if (!v1Used && !v2Used && (currentV1 + currentV2) == targetStep && canClimb(clickedHorse.getColor(), targetStep)) {
                choices.add(currentV1 + currentV2);
            }
            
            if (choices.isEmpty()) return;

            // Auto-sum cho phần lên chuồng nếu chỉ có 1 ngựa
            if (highlightedHorses.size() == 1 && choices.contains(currentV1 + currentV2)) {
                choices.clear();
                choices.add(currentV1 + currentV2);
            }
            
            int chosenSteps = choices.get(0);
            if (choices.size() > 1) {
                String[] optionsArray = choices.stream().map(v -> "Dùng " + v + " điểm").toArray(String[]::new);
                int choiceIdx = JOptionPane.showOptionDialog(ui,
                    "Bạn muốn dùng điểm nào để lên chuồng?", "Chọn Nước Đi",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, optionsArray, optionsArray[0]);
                if (choiceIdx >= 0 && choiceIdx < choices.size()) chosenSteps = choices.get(choiceIdx);
                else return;
            }
            
            tryClimbHome(clickedHorse, targetStep);
            
            if (chosenSteps == currentV1 && !v1Used) { v1Used = true; }
            else if (chosenSteps == currentV2 && !v2Used) { v2Used = true; }
            else { v1Used = true; v2Used = true; }
            
            checkTurnEnd();
        }
    }

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
            ui.showMessage("🎉 Ngựa của " + players[currentPlayerIndex].getName() + " đã VỀ ĐÍCH!");
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

    private void checkWinCondition() {
        Player current = players[currentPlayerIndex];
        if (current.hasWon()) {
            rankings.add(current.getName());
            ui.showPopup("🏆 CHÚC MỪNG! " + current.getName() + " đã CHIẾN THẮNG!\nXếp hạng #" + rankings.size());
            int finished = 0;
            for (Player p : players) { if (p.hasWon()) finished++; }
            if (finished >= 3) {
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
            ui.showMessage("💥 Đã đá ngựa " + occupier.getColor() + " về chuồng!");
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

        if (extraTurn) {
            ui.showMessage("🎲 Đổ xúc xắc đặc biệt! " + players[currentPlayerIndex].getName() + " được lắc thêm lượt!");
        } else {
            nextPlayerTurn();
        }
        ui.renderBoard(board, players);
    }

    private void nextPlayerTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;
        int attempts = 0;
        while (players[currentPlayerIndex].hasWon() && attempts < 4) {
            currentPlayerIndex = (currentPlayerIndex + 1) % 4;
            attempts++;
        }
        ui.showMessage("🎯 Lượt của: " + players[currentPlayerIndex].getName());
    }

    public Board getBoard() { return board; }
    public Player[] getPlayers() { return players; }
    public List<Horse> getHighlightedHorses() { return highlightedHorses; }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public Dice getDice() { return dice; }
}