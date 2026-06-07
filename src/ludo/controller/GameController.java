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
    private static final int BOT_DELAY_MS = 150;

    private boolean hasRolled = false;
    private int currentV1 = 0, currentV2 = 0;
    private boolean v1Used = false;
    private boolean v2Used = false;
    
    private boolean bonusTurnEarned = false; 
    private boolean canDeployThisTurn = false; 
    private boolean botThinking = false;
    private boolean gameOver = false;

    private List<Horse> highlightedHorses = new ArrayList<>();
    private List<String> rankings = new ArrayList<>();

    public GameController() {
        initGame("4b");
    }

    public void setUI(GameUI ui) { 
        this.ui = ui; 
    }

    public void startGame() {
        if (ui != null) {
            ui.resetDiceDisplay();
            ui.showMessage("Trận đấu bắt đầu! Lượt của: " + players[0].getName());
            ui.renderBoard(board, players);
            scheduleBot();
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
            case "2p":
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
            case "all_bots":
            default:
                playerCount = 4;
                players = new Player[]{
                    new Player("Máy Đỏ",   PlayerColor.RED),
                    new Player("Máy Lam",  PlayerColor.BLUE),
                    new Player("Máy Lục",  PlayerColor.GREEN),
                    new Player("Máy Vàng", PlayerColor.YELLOW)
                };
                isBot = new boolean[]{true, true, true, true};
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
        ui.showMessage(players[currentPlayerIndex].getName() + " đổ được [" + currentV1 + "] và [" + currentV2 + "]");
        
        canDeployThisTurn = (currentV1 == currentV2) || (currentV1 == 1 && currentV2 == 6) || (currentV1 == 6 && currentV2 == 1);
        bonusTurnEarned = canDeployThisTurn;

        List<Horse> validMoves = getValidMovesForCurrentPlayer();
        if (validMoves.isEmpty()) {
            ui.showMessage(players[currentPlayerIndex].getName() + " không có nước đi hợp lệ! Tự chuyển lượt.");
            v1Used = true; v2Used = true;
            Timer t = new Timer(BOT_DELAY_MS, e -> { ((Timer) e.getSource()).stop(); endTurn(); });
            t.setRepeats(false);
            t.start();
            return;
        }

        Horse choice = selectBestHorseForBot(validMoves);
        executeCommonMove(choice);
    }

    private Horse selectBestHorseForBot(List<Horse> validMoves) {
        Horse best = validMoves.get(0);
        int bestScore = -1000;
        
        for (Horse h : validMoves) {
            int score = 0;
            if (h.getState() == HorseState.IN_HOME) {
                score += 500 + h.getHomeStep();
            } else if (h.getState() == HorseState.ON_PATH) {
                score += h.getDistanceTraveled();
                int nextPosV1 = (h.getCurrentPosition() + currentV1) % 56;
                int nextPosV2 = (h.getCurrentPosition() + currentV2) % 56;
                if (!v1Used && board.getHorseAt(nextPosV1) != null && board.getHorseAt(nextPosV1).getColor() != h.getColor()) {
                    score += 250;
                }
                if (!v2Used && board.getHorseAt(nextPosV2) != null && board.getHorseAt(nextPosV2).getColor() != h.getColor()) {
                    score += 250;
                }
            } else if (h.getState() == HorseState.IN_BASE) {
                score += 100;
            }
            if (score > bestScore) {
                bestScore = score;
                best = h;
            }
        }
        return best;
    }

    public void rollDiceRequest() {
        if (gameOver || isCurrentBot() || hasRolled) return;

        int[] result = dice.roll();
        currentV1 = result[0];
        currentV2 = result[1];
        hasRolled = true;
        v1Used = false;
        v2Used = false;

        canDeployThisTurn = (currentV1 == currentV2) || (currentV1 == 1 && currentV2 == 6) || (currentV1 == 6 && currentV2 == 1);
        bonusTurnEarned = canDeployThisTurn;

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
            v1Used = true; v2Used = true;
            javax.swing.SwingUtilities.invokeLater(() -> {
                Timer t = new Timer(200, e -> { 
                    ((Timer) e.getSource()).stop(); 
                    endTurn(); 
                });
                t.setRepeats(false);
                t.start();
            });
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
                if (!v1Used) {
                    int targetV1 = curStep + currentV1;
                    if (targetV1 <= 6 && canClimbSafe(cp.getColor(), targetV1)) list.add(h);
                }
                if (!v2Used) {
                    int targetV2 = curStep + currentV2;
                    if (targetV2 <= 6 && canClimbSafe(cp.getColor(), targetV2)) list.add(h);
                }
            }
        }
        return list;
    }

    public void handleHorseClick(Horse clicked) {
        if (gameOver || isCurrentBot() || !hasRolled) return;
        if (clicked.getColor() != players[currentPlayerIndex].getColor()) return;
        if (!highlightedHorses.contains(clicked)) return;
        executeCommonMove(clicked);
    }

    private void executeCommonMove(Horse h) {
        Player cp = players[currentPlayerIndex];
        HorseState prevState = h.getState();

        if (prevState == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(h.getColor());
            deployHorse(h, startPos);
            v1Used = true; v2Used = true; 
        } 
        else if (prevState == HorseState.ON_PATH) {
            int totalSteps = currentV1 + currentV2;
            int maxV = Math.max(currentV1, currentV2);
            int minV = Math.min(currentV1, currentV2);

            if (!v1Used && !v2Used && canMoveSafe(h, totalSteps)) {
                moveHorseOnPath(h, totalSteps);
                v1Used = true; v2Used = true;
            } 
            else if ((maxV == currentV1 ? !v1Used : !v2Used) && canMoveSafe(h, maxV)) {
                moveHorseOnPath(h, maxV);
                if (maxV == currentV1) v1Used = true; else v2Used = true;
            } 
            else if ((minV == currentV1 ? !v1Used : !v2Used) && canMoveSafe(h, minV)) {
                moveHorseOnPath(h, minV);
                if (minV == currentV1) v1Used = true; else v2Used = true;
            }
        } 
        else if (prevState == HorseState.IN_HOME) {
            int curStep = h.getHomeStep();
            int targetV1 = curStep + currentV1;
            int targetV2 = curStep + currentV2;

            if (!v1Used && targetV1 <= 6 && canClimbSafe(cp.getColor(), targetV1)) {
                tryClimbHome(h, targetV1);
                v1Used = true;
            } else if (!v2Used && targetV2 <= 6 && canClimbSafe(cp.getColor(), targetV2)) {
                tryClimbHome(h, targetV2);
                v2Used = true;
            }
        }

        ui.renderBoard(board, players);

        if (checkWinCondition()) {
            return;
        }

        if (v1Used && v2Used) {
            endTurn();
        } else {
            List<Horse> nextValid = getValidMovesForCurrentPlayer();
            if (nextValid.isEmpty()) {
                v1Used = true; v2Used = true;
                endTurn();
            } else {
                if (isCurrentBot()) {
                    Timer t = new Timer(BOT_DELAY_MS, e -> {
                        ((Timer) e.getSource()).stop();
                        if (!gameOver) executeCommonMove(selectBestHorseForBot(nextValid));
                    });
                    t.setRepeats(false);
                    t.start();
                } else {
                    updateHighlightedHorses();
                }
            }
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
                if (i < steps) return false; 
                if (board.isSafeCell(checkPos)) return false;
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
        bonusTurnEarned = false;
        canDeployThisTurn = false;
        dice.reset();
        highlightedHorses.clear();
        ui.renderBoard(board, players);

        if (giveBonus) {
            ui.showMessage(players[currentPlayerIndex].getName() + " được thưởng thêm 1 lượt!");
            if (isCurrentBot()) {
                scheduleBot();
            }
            return;
        } else {
            nextPlayerTurn();
            if (isCurrentBot()) {
                scheduleBot();
            }
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

        hasRolled = false;
        v1Used = false;
        v2Used = false;

        String label = isCurrentBot() ? " (Máy)" : " (Mời Đổ...)";
        ui.showMessage("Đến lượt của người chơi: " + players[currentPlayerIndex].getName() + label);
    }

    private void deployHorse(Horse h, int pos) {
        Horse occ = board.getHorseAt(pos);
        if (occ != null && occ.getColor() == h.getColor()) {
            return;
        }
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
            h.setHomeStep(1);
            h.setState(HorseState.IN_HOME);
            ui.showMessage("Quân cờ " + h.getColor() + " đã tiến thẳng vào bậc chuồng: 1");
        } else {
            int newPos = (oldPos + steps) % 56;
            Horse occ = board.getHorseAt(newPos);
            if (occ != null && occ.getColor() == h.getColor()) {
                board.setHorseAt(oldPos, h);
                return;
            }
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
            ui.showMessage("🏆 " + current.getName() + " đã về đích hạng " + rankings.size());

            if (rankings.size() == players.length - 1) {
                for (Player p : players) {
                    if (!rankings.contains(p.getName())) {
                        rankings.add(p.getName());
                    }
                }
                gameOver = true;
                botThinking = false;
                
                StringBuilder sb = new StringBuilder("🏁 TRẬN ĐẤU KẾT THÚC! BẢNG XẾP HẠNG:\n");
                for (int i = 0; i < rankings.size(); i++) {
                    sb.append("Hạng ").append(i + 1).append(": ").append(rankings.get(i)).append("\n");
                }
                ui.showPopup(sb.toString());
                return true;
            }
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