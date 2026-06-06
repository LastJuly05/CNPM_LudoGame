package ludo.controller;

import ludo.model.*;
import ludo.view.GameUI;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public class GameController {
    private Player[] players;
    private Board board;
    private Dice dice;
    private int currentPlayerIndex;
    private GameUI ui;

    private int playerCount = 4;
    private boolean[] isBot;
    private static final int BOT_DELAY_MS = 800;

    // Quản lý trạng thái xúc xắc độc lập cho từng người chơi
    private boolean hasRolled = false;
    private int currentV1 = 0, currentV2 = 0;
    private boolean v1Used = false;
    private boolean v2Used = false;
    
    private boolean bonusTurnEarned = false; 
    private boolean canDeployThisTurn = false; // Đúng luật gốc: đổ ra 1 hoặc 6 mới được xuất quân
    private boolean botThinking = false;
    private boolean gameOver = false;

    private List<Horse> highlightedHorses = new ArrayList<>();
    private List<String> rankings = new ArrayList<>();

    public GameController() {
        initGame("4p");
    }

    public void setUI(GameUI ui) { 
        this.ui = ui; 
    }

    public void startGame() {
        if (ui != null) {
            ui.resetDiceDisplay();
            ui.showMessage("Trận đấu bắt đầu! Lượt của: " + players[0].getName() + " (Mời Đổ)");
            ui.renderBoard(board, players);
        }
    }

    public void startNewGame(String mode) {
        initGame(mode);
        if (ui != null) {
            ui.resetDiceDisplay();
            String label = isCurrentBot() ? " (Máy)" : " (Mời Đổ)";
            ui.showMessage("Trận đấu bắt đầu! Lượt của: " + players[0].getName() + label);
            ui.renderBoard(board, players);
            scheduleBot();
        }
    }

    public void startNewGame(int count) { 
        startNewGame(count + "p"); 
    }

    private void initGame(String mode) {
        board = new Board();
        dice  = new Dice();
        botThinking = false;
        gameOver = false;
        
        hasRolled = false;
        currentV1 = 0; currentV2 = 0;
        v1Used = false; v2Used = false;
        bonusTurnEarned = false;
        canDeployThisTurn = false;
        highlightedHorses.clear();
        rankings.clear();
        currentPlayerIndex = 0;

        switch (mode) {
            case "2p": // Chế độ nhiều người chơi thật 1 máy
                playerCount = 2;
                players = new Player[]{
                    new Player("Đỏ",  PlayerColor.RED),
                    new Player("Lục", PlayerColor.GREEN)
                };
                isBot = new boolean[]{false, false};
                break;
            case "3p":
                playerCount = 3;
                players = new Player[]{
                    new Player("Đỏ",  PlayerColor.RED),
                    new Player("Lam", PlayerColor.BLUE),
                    new Player("Lục", PlayerColor.GREEN)
                };
                isBot = new boolean[]{false, false, false};
                break;
            case "2b":
                playerCount = 2;
                players = new Player[]{
                    new Player("Người (Đỏ)", PlayerColor.RED),
                    new Player("Máy (Lục)",  PlayerColor.GREEN)
                };
                isBot = new boolean[]{false, true};
                break;
            case "3b":
                playerCount = 3;
                players = new Player[]{
                    new Player("Người (Đỏ)", PlayerColor.RED),
                    new Player("Máy (Lam)",  PlayerColor.BLUE),
                    new Player("Máy (Lục)",  PlayerColor.GREEN)
                };
                isBot = new boolean[]{false, true, true};
                break;
            case "4b":
                playerCount = 4;
                players = new Player[]{
                    new Player("Người (Đỏ)",  PlayerColor.RED),
                    new Player("Máy (Lam)",   PlayerColor.BLUE),
                    new Player("Máy (Lục)",   PlayerColor.GREEN),
                    new Player("Máy (Vàng)",  PlayerColor.YELLOW)
                };
                isBot = new boolean[]{false, true, true, true};
                break;
            case "4p":
            default:
                playerCount = 4;
                players = new Player[]{
                    new Player("Đỏ",   PlayerColor.RED),
                    new Player("Lam",  PlayerColor.BLUE),
                    new Player("Lục",  PlayerColor.GREEN),
                    new Player("Vàng", PlayerColor.YELLOW)
                };
                isBot = new boolean[]{false, false, false, false};
                break;
        }
    }

    public void restartGame() {
        if (ui != null) ui.showPlayerSelectionDialog();
    }

    private void scheduleBot() {
        if (gameOver || !isCurrentBot() || botThinking) return;
        botThinking = true;
        Timer t = new Timer(BOT_DELAY_MS, e -> {
            ((Timer) e.getSource()).stop();
            botThinking = false;
            if (!gameOver) botTurn();
        });
        t.setRepeats(false);
        t.start();
    }

    private boolean isCurrentBot() {
        return isBot != null && currentPlayerIndex < isBot.length && isBot[currentPlayerIndex];
    }

    private void botTurn() {
        if (gameOver) return;
        int[] result = dice.roll();
        currentV1 = result[0];
        currentV2 = result[1];
        hasRolled = true;
        v1Used = false; v2Used = false;
        
        ui.updateDiceDisplay(currentV1, currentV2);
        
        canDeployThisTurn = (currentV1 == 1 || currentV1 == 6 || currentV2 == 1 || currentV2 == 6);
        bonusTurnEarned = (currentV1 == currentV2 || currentV1 == 6 || currentV2 == 6);

        List<Horse> validMoves = getValidMovesForCurrentPlayer();
        if (validMoves.isEmpty()) {
            ui.showMessage(players[currentPlayerIndex].getName() + " (Máy) không có nước đi!");
            Timer t = new Timer(1000, e -> { ((Timer) e.getSource()).stop(); endTurn(); });
            t.setRepeats(false);
            t.start();
            return;
        }

        Horse choice = validMoves.get(0);
        executeCommonMove(choice);
    }

    public void rollDiceRequest() {
        if (gameOver) return;
        if (isCurrentBot()) return;
        if (hasRolled) { ui.showMessage("Bạn đã đổ rồi! Hãy bấm chọn quân cờ sáng trên bàn."); return; }

        int[] result = dice.roll();
        currentV1 = result[0];
        currentV2 = result[1];
        hasRolled = true;
        v1Used = false;
        v2Used = false;

        // Chuẩn luật cá ngựa gốc: Phải đổ trúng viên 1 hoặc viên 6 mới được phép ra quân
        canDeployThisTurn = (currentV1 == 1 || currentV1 == 6 || currentV2 == 1 || currentV2 == 6);
        bonusTurnEarned = (currentV1 == currentV2 || currentV1 == 6 || currentV2 == 6);

        ui.updateDiceDisplay(currentV1, currentV2);
        ui.showMessage(players[currentPlayerIndex].getName() + " đổ được: [" + currentV1 + "] và [" + currentV2 + "]");
        
        updateHighlightedHorses();
    }

    private void updateHighlightedHorses() {
        highlightedHorses.clear();
        if (!hasRolled || gameOver) return;

        highlightedHorses.addAll(getValidMovesForCurrentPlayer());
        ui.renderBoard(board, players);

        if (highlightedHorses.isEmpty()) {
            ui.showMessage("Không có nước đi hợp lệ! Tự động chuyển lượt.");
            Timer t = new Timer(1500, e -> { ((Timer) e.getSource()).stop(); endTurn(); });
            t.setRepeats(false);
            t.start();
        }
    }

    private List<Horse> getValidMovesForCurrentPlayer() {
        List<Horse> list = new ArrayList<>();
        Player cp = players[currentPlayerIndex];
        int startPos = board.getStartPosition(cp.getColor());

        for (Horse h : cp.getHorses()) {
            if (h.getState() == HorseState.FINISHED) continue;

            if (h.getState() == HorseState.IN_BASE) {
                if (canDeployThisTurn && !v1Used && !v2Used) {
                    Horse occ = board.getHorseAt(startPos);
                    if (occ == null || occ.getColor() != cp.getColor()) {
                        list.add(h);
                    }
                }
            } else if (h.getState() == HorseState.ON_PATH) {
                boolean targetV1 = !v1Used && canMoveSafe(h, currentV1);
                boolean targetV2 = !v2Used && canMoveSafe(h, currentV2);
                boolean targetBoth = !v1Used && !v2Used && canMoveSafe(h, currentV1 + currentV2);
                
                if (targetV1 || targetV2 || targetBoth) {
                    list.add(h);
                }
            } else if (h.getState() == HorseState.IN_HOME) {
                int curStep = h.getHomeStep();
                if (curStep + 1 <= 6 && canClimbSafe(cp.getColor(), curStep + 1)) {
                    list.add(h);
                }
            }
        }
        return list;
    }

    public void handleHorseClick(Horse clicked) {
        if (gameOver || isCurrentBot() || !hasRolled) return;

        // Chặn bấm nhầm quân cờ của đối thủ
        if (clicked.getColor() != players[currentPlayerIndex].getColor()) {
            ui.showMessage("Không phải quân cờ của bạn! Đang là lượt của màu: " + players[currentPlayerIndex].getName());
            return;
        }

        if (!highlightedHorses.contains(clicked)) {
            ui.showMessage("Quân cờ này không thể di chuyển hợp lệ!");
            return;
        }

        // Thực thi di chuyển tự động thông minh không cần popup hỏi han
        executeCommonMove(clicked);
    }

    // HÀM XỬ LÝ DI CHUYỂN TỰ ĐỘNG THÔNG MINH - KHÔNG POPUP RƯỜM RÀ
    private void executeCommonMove(Horse h) {
        Player cp = players[currentPlayerIndex];

        if (h.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(h.getColor());
            deployHorse(h, startPos);
            v1Used = true; v2Used = true; // Ra quân tiêu thụ hết lượt xúc xắc
            ui.showMessage(cp.getName() + " xuất quân thành công!");
        } 
        else if (h.getState() == HorseState.ON_PATH) {
            int totalSteps = currentV1 + currentV2;
            int maxV = Math.max(currentV1, currentV2);
            int minV = Math.min(currentV1, currentV2);

            // Ưu tiên 1: Đi tổng điểm cả 2 xúc xắc nếu đường đi an toàn không bị chặn
            if (!v1Used && !v2Used && canMoveSafe(h, totalSteps)) {
                moveHorseOnPath(h, totalSteps);
                v1Used = true; v2Used = true;
            } 
            // Ưu tiên 2: Đi viên xúc xắc có điểm lớn hơn
            else if ((maxV == currentV1 ? !v1Used : !v2Used) && canMoveSafe(h, maxV)) {
                moveHorseOnPath(h, maxV);
                if (maxV == currentV1) v1Used = true; else v2Used = true;
            } 
            // Ưu tiên 3: Đi viên xúc xắc nhỏ hơn còn lại
            else if ((minV == currentV1 ? !v1Used : !v2Used) && canMoveSafe(h, minV)) {
                moveHorseOnPath(h, minV);
                if (minV == currentV1) v1Used = true; else v2Used = true;
            }
        } 
        else if (h.getState() == HorseState.IN_HOME) {
            int curStep = h.getHomeStep();
            tryClimbHome(h, curStep + 1);
            v1Used = true; v2Used = true;
        }

        ui.renderBoard(board, players);

        if (cp.hasWon()) {
            checkWinCondition();
            return;
        }

        // Chuyển lượt hoặc cập nhật nước đi cho viên còn lại nếu đi lẻ
        if (v1Used && v2Used) {
            endTurn();
        } else {
            updateHighlightedHorses();
        }
    }

    private boolean canMoveSafe(Horse h, int steps) {
        int dist = h.getDistanceTraveled();
        if (dist + steps > 56) return false;

        int startPos = h.getCurrentPosition();
        if (startPos == -1) return false;

        for (int i = 1; i <= steps; i++) {
            int checkPos = (startPos + i) % 56;
            Horse obs = board.getHorseAt(checkPos);
            if (obs != null) {
                if (i < steps) return false; // Có vật cản đứng chặn giữa đường
                if (obs.getColor() == h.getColor()) return false; // Trùng màu quân mình ở ô đích
                if (board.isSafeCell(checkPos)) return false; // Ô đích là ô an toàn của đối phương
            }
        }
        return true;
    }

    private boolean canClimbSafe(PlayerColor color, int targetStep) {
        if (targetStep > 6) return false;
        for (Player p : players) {
            if (p.getColor() != color) continue;
            for (Horse h : p.getHorses()) {
                if (h.getHomeStep() == targetStep && (h.getState() == HorseState.IN_HOME || h.getState() == HorseState.FINISHED)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void endTurn() {
        if (gameOver) return;

        boolean giveBonus = bonusTurnEarned && !players[currentPlayerIndex].hasWon();

        hasRolled = false;
        v1Used = false;
        v2Used = false;
        highlightedHorses.clear();
        ui.renderBoard(board, players);

        if (giveBonus) {
            String label = isCurrentBot() ? " (Máy)" : " (Được đổ tiếp)";
            ui.showMessage("[LƯỢT THƯỞNG] Nhờ điểm đặc biệt, người chơi " + players[currentPlayerIndex].getName() + label + " được đổ thêm lượt!");
            if (isCurrentBot()) scheduleBot();
        } else {
            nextPlayerTurn();
        }
    }

    public void nextPlayerTurn() {
        if (gameOver) return;
        int n = players.length;
        int attempts = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % n;
            attempts++;
        } while (players[currentPlayerIndex].hasWon() && attempts < n);

        // Reset trạng thái điều khiển sang lượt mới hoàn toàn
        hasRolled = false;
        v1Used = false;
        v2Used = false;

        String label = isCurrentBot() ? " (Máy)" : " (Mời Đổ...)";
        ui.showMessage("Đến lượt của người chơi: " + players[currentPlayerIndex].getName() + label);
        
        scheduleBot();
    }

    private void deployHorse(Horse h, int pos) {
        Horse occ = board.getHorseAt(pos);
        if (occ != null) {
            board.clearPosition(pos);
            occ.sendToBase();
            ui.showMessage("[!] Đá văng quân màu " + occ.getColor() + " về chuồng!");
        }
        h.setCurrentPosition(pos);
        h.setState(HorseState.ON_PATH);
        h.setDistanceTraveled(0);
        board.setHorseAt(pos, h);
    }

    private void moveHorseOnPath(Horse h, int steps) {
        int oldPos = h.getCurrentPosition();
        int dist = h.getDistanceTraveled();
        board.clearPosition(oldPos);

        if (dist + steps == 56) {
            h.setCurrentPosition(-1);
            h.setDistanceTraveled(56);
            h.setHomeStep(0);
            h.setState(HorseState.IN_HOME);
            ui.showMessage("Quân cờ " + h.getColor() + " đã đến cửa chuồng đích!");
        } else {
            int newPos = (oldPos + steps) % 56;
            Horse occ = board.getHorseAt(newPos);
            if (occ != null) {
                board.clearPosition(newPos);
                occ.sendToBase();
                ui.showMessage("[!] Đã đá quân đối thủ màu " + occ.getColor() + " về chuồng xuất phát!");
            }
            h.setCurrentPosition(newPos);
            h.setDistanceTraveled(dist + steps);
            board.setHorseAt(newPos, h);
        }
    }

    private void tryClimbHome(Horse h, int targetStep) {
        h.setHomeStep(targetStep);
        if (targetStep == 6) {
            h.setState(HorseState.FINISHED);
            ui.showMessage("Chúc mừng! Quân màu " + h.getColor() + " đã về đích bậc 6!");
        } else {
            h.setState(HorseState.IN_HOME);
            ui.showMessage("Quân màu " + h.getColor() + " tiến lên bậc chuồng: " + targetStep);
        }
    }

    public boolean checkWinCondition() {
        Player current = players[currentPlayerIndex];
        if (current.hasWon() && !rankings.contains(current.getName())) {
            rankings.add(current.getName());
            ui.showPopup("🏆 Người chơi [" + current.getName() + "] đã chiến thắng!");
            restartGame();
            return true;
        }
        return false;
    }

    public Board getBoard()                   { return board; }
    public Player[] getPlayers()              { return players; }
    public List<Horse> getHighlightedHorses() { return highlightedHorses; }
    public int getCurrentPlayerIndex()        { return currentPlayerIndex; }
    public Dice getDice()                     { return dice; }
    public int getPlayerCount()               { return playerCount; }
    public boolean isCurrentPlayerBot()       { return isCurrentBot(); }
}