package ludo.controller;

import ludo.model.*;
import ludo.view.GameUI;
import javax.swing.Timer;
import javax.swing.JOptionPane;
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
    // Tốc độ Bot được giữ theo tùy chỉnh của NgoThanhVy để game mượt hơn
    private static final int BOT_DELAY_MS = 150; 

    private boolean hasRolled = false;
    private int currentV1 = 0, currentV2 = 0;
    private boolean v1Used = false;
    private boolean v2Used = false;
    
    private boolean bonusTurnEarned = false; 
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
        bonusTurnEarned = dice.isDeployable();
        
        ui.updateDiceDisplay(currentV1, currentV2);

        String playerName = players[currentPlayerIndex].getName();
        if (bonusTurnEarned) {
            ui.showMessage("[*] ĐIỂM ĐẶC BIỆT! " + playerName
                    + " đổ [" + currentV1 + "] + [" + currentV2 + "] — Thưởng thêm 1 lượt!");
        } else {
            ui.showMessage(playerName + " (Máy) đổ: [" + currentV1 + "] + [" + currentV2 + "]");
        }

        botDoMove();
        if (gameOver) return;

        if (!v1Used || !v2Used) {
            botDoMove();
        }
        if (gameOver) return;

        endTurn();
    }

    private void botDoMove() {
        if (gameOver) return;
        List<Horse> moves = getValidMovesForBot();
        if (moves.isEmpty()) return;

        int v1 = v1Used ? 0 : currentV1;
        int v2 = v2Used ? 0 : currentV2;

        Horse best = null;
        int bestScore = -1000;
        for (Horse h : moves) {
            int score = scoreBotMove(h, v1, v2);
            if (score > bestScore) { bestScore = score; best = h; }
        }
        if (best == null) return;

        if (best.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(best.getColor());
            deployHorse(best, startPos);
            if (currentV1 == currentV2) v1Used = true;
            else { v1Used = true; v2Used = true; }
            ui.showMessage("[>] " + players[currentPlayerIndex].getName() + " xuất quân!");

        } else if (best.getState() == HorseState.IN_HOME) {
            int cur = best.getHomeStep();
            int chosen = chooseBestClimb(best.getColor(), cur, v1, v2);
            if (chosen == 0) return;
            tryClimbHome(best, cur + 1); // Đảm bảo luật của Yến: chỉ leo đúng 1 bậc
            consumeDice(chosen);

        } else if (best.getState() == HorseState.ON_PATH) {
            int chosen = chooseBestPathMove(best, v1, v2);
            if (chosen == 0) return;
            moveHorseOnPath(best, chosen);
            consumeDice(chosen);
        }

        ui.renderBoard(board, players);

        if (!gameOver && players[currentPlayerIndex].hasWon()) {
            checkWinCondition();
        }
    }

    private List<Horse> getValidMovesForBot() {
        List<Horse> result = new ArrayList<>();
        int v1 = v1Used ? 0 : currentV1;
        int v2 = v2Used ? 0 : currentV2;
        int startPos = board.getStartPosition(players[currentPlayerIndex].getColor());
        boolean canDeploy = bonusTurnEarned && !v1Used && !v2Used;

        for (Horse h : players[currentPlayerIndex].getHorses()) {
            if (h.getState() == HorseState.FINISHED) continue;
            if (h.getState() == HorseState.IN_BASE) {
                if (canDeploy) {
                    Horse occ = board.getHorseAt(startPos);
                    if (occ == null || occ.getColor() != players[currentPlayerIndex].getColor())
                        result.add(h);
                }
            } else if (h.getState() == HorseState.IN_HOME) {
                if (chooseBestClimb(h.getColor(), h.getHomeStep(), v1, v2) > 0) result.add(h);
            } else if (h.getState() == HorseState.ON_PATH) {
                if (chooseBestPathMove(h, v1, v2) > 0) result.add(h);
            }
        }
        return result;
    }

    private int chooseBestClimb(PlayerColor color, int cur, int v1, int v2) {
        int nextStep = cur + 1;
        if (nextStep > 6 || isHomeStepOccupied(color, nextStep)) return 0;
        
        if (v1 > 0 && v2 > 0 && (v1 + v2) == nextStep) return v1 + v2;
        if (v1 == nextStep) return v1;
        if (v2 == nextStep) return v2;
        return 0;
    }

    private int chooseBestPathMove(Horse h, int v1, int v2) {
        int best = 0, bestScore = -1;
        for (int steps : new int[]{v1 + v2, v1, v2}) {
            if (steps <= 0) continue;
            if (steps == v1 + v2 && (v1Used || v2Used)) continue;
            if (steps == v1 && v1Used) continue;
            if (steps == v2 && v2Used) continue;
            if (!canMove(h, steps)) continue;
            int dest = (h.getCurrentPosition() + steps) % 56;
            Horse occ = board.getHorseAt(dest);
            int sc = (occ != null && occ.getColor() != h.getColor() && !board.isSafeCell(dest)) ? 100 : steps;
            if (sc > bestScore) { bestScore = sc; best = steps; }
        }
        return best;
    }

    private int scoreBotMove(Horse h, int v1, int v2) {
        if (h.getState() == HorseState.IN_BASE) return 100;
        if (h.getState() == HorseState.IN_HOME) return 500 + h.getHomeStep();
        if (h.getState() == HorseState.ON_PATH) {
            int base = h.getDistanceTraveled();
            for (int steps : new int[]{v1, v2, v1 + v2}) {
                if (steps <= 0 || !canMove(h, steps)) continue;
                int dest = (h.getCurrentPosition() + steps) % 56;
                Horse occ = board.getHorseAt(dest);
                if (occ != null && occ.getColor() != h.getColor() && !board.isSafeCell(dest)) {
                    base += 250; break;
                }
            }
            return base;
        }
        return 0;
    }

    public void rollDiceRequest() {
        if (gameOver || isCurrentBot() || hasRolled) return;

        int[] result = dice.roll();
        currentV1 = result[0];
        currentV2 = result[1];
        hasRolled = true;
        v1Used = false;
        v2Used = false;

        bonusTurnEarned = dice.isDeployable();

        ui.updateDiceDisplay(currentV1, currentV2);
        ui.showMessage(players[currentPlayerIndex].getName() + " đổ được: [" + currentV1 + "] và [" + currentV2 + "]");
        
        updateHighlightedHorses();
    }

    private void updateHighlightedHorses() {
        highlightedHorses.clear();
        if (!hasRolled || gameOver) { ui.renderBoard(board, players); return; }

        int v1 = v1Used ? 0 : currentV1;
        int v2 = v2Used ? 0 : currentV2;
        int startPos = board.getStartPosition(players[currentPlayerIndex].getColor());
        boolean canDeploy = bonusTurnEarned && !v1Used && !v2Used;

        for (Horse h : players[currentPlayerIndex].getHorses()) {
            if (h.getState() == HorseState.FINISHED) continue;
            if (h.getState() == HorseState.IN_BASE) {
                if (canDeploy) {
                    Horse occ = board.getHorseAt(startPos);
                    if (occ == null || occ.getColor() != players[currentPlayerIndex].getColor())
                        highlightedHorses.add(h);
                }
            } else if (h.getState() == HorseState.IN_HOME) {
                // Tích hợp logic UC7 của Yến: Chỉ highlight nếu xúc xắc KHỚP với số bậc tiếp theo
                int nextStep = h.getHomeStep() + 1;
                if (nextStep <= 6 && !isHomeStepOccupied(h.getColor(), nextStep)) {
                    if (v1 == nextStep || v2 == nextStep || (v1 + v2) == nextStep) {
                        highlightedHorses.add(h);
                    }
                }
            } else if (h.getState() == HorseState.ON_PATH) {
                boolean ok = (v1 > 0 && canMove(h, v1))
                        || (v2 > 0 && canMove(h, v2))
                        || (v1 > 0 && v2 > 0 && canMove(h, v1 + v2));
                if (ok) highlightedHorses.add(h);
            }
        }

        ui.renderBoard(board, players);

        // Kế thừa xử lý chuyển lượt tự động từ NgoThanhVy nếu bị kẹt
        if (highlightedHorses.isEmpty()) {
            ui.showMessage("Không có nước đi hợp lệ! Tự động chuyển lượt.");
            v1Used = true; v2Used = true;
            javax.swing.SwingUtilities.invokeLater(() -> {
                Timer t = new Timer(500, e -> { 
                    ((Timer) e.getSource()).stop(); 
                    endTurn(); 
                });
                t.setRepeats(false);
                t.start();
            });
        }
    }

    public void handleHorseClick(Horse clicked) {
        if (gameOver || isCurrentBot() || !hasRolled) return;
        if (clicked.getColor() != players[currentPlayerIndex].getColor()) return;
        if (!highlightedHorses.contains(clicked)) return;

        // --- XUẤT QUÂN ---
        if (clicked.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(clicked.getColor());
            deployHorse(clicked, startPos);
            if (currentV1 == currentV2) v1Used = true;
            else { v1Used = true; v2Used = true; }
            ui.showMessage("[>] Xuất quân thành công!");
            checkTurnEnd();

        // --- ĐI TRÊN ĐƯỜNG (Giữ lại Pop-up chọn của Phát) ---
        } else if (clicked.getState() == HorseState.ON_PATH) {
            int dist  = clicked.getDistanceTraveled();
            int toGate = 56 - dist;

            List<Integer> choices = new ArrayList<>();
            List<String>  labels  = new ArrayList<>();

            boolean canV1  = !v1Used && canMove(clicked, currentV1);
            boolean canV2  = !v2Used && canMove(clicked, currentV2);
            boolean canSum = !v1Used && !v2Used && canMove(clicked, currentV1 + currentV2);

            if (canV1) {
                choices.add(currentV1);
                labels.add(currentV1 == toGate ? "[D] Vào Cửa Chuồng (viên " + currentV1 + ")" : "[>] Đi " + currentV1 + " bước (viên 1)");
            }
            if (canV2 && !(currentV2 == currentV1 && !v1Used)) {
                choices.add(currentV2);
                labels.add(currentV2 == toGate ? "[D] Vào Cửa Chuồng (viên " + currentV2 + ")" : "[>] Đi " + currentV2 + " bước (viên 2)");
            }
            if (canSum) {
                choices.add(currentV1 + currentV2);
                labels.add((currentV1 + currentV2) == toGate ? "[D] Gộp điểm vào Cửa Chuồng (tổng " + (currentV1 + currentV2) + ")" : "[>>] Gộp đi " + (currentV1 + currentV2) + " bước");
            }

            if (choices.isEmpty()) return;
            int chosenSteps = choices.get(0);
            if (choices.size() > 1) {
                String[] arr = labels.toArray(new String[0]);
                int idx = JOptionPane.showOptionDialog(ui, "Bạn muốn dùng điểm nào?", "Chọn Nước Đi",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, arr, arr[0]);
                if (idx < 0 || idx >= choices.size()) return;
                chosenSteps = choices.get(idx);
            }

            moveHorseOnPath(clicked, chosenSteps);
            consumeDice(chosenSteps);
            checkTurnEnd();

        // --- LEO CHUỒNG ĐÍCH (Giữ lại luật của Yến) ---
        } else if (clicked.getState() == HorseState.IN_HOME) {
            int cur = clicked.getHomeStep();
            int nextStep = cur + 1; 
            
            int v1  = v1Used ? 0 : currentV1;
            int v2  = v2Used ? 0 : currentV2;

            List<Integer> choices = new ArrayList<>();
            List<String>  labels  = new ArrayList<>();

            if (v1 == nextStep) {
                choices.add(v1);
                labels.add("[^] Dùng viên " + v1 + " — lên Bậc " + nextStep);
            }
            if (v2 == nextStep && (v2 != v1 || v1Used)) {
                choices.add(v2);
                labels.add("[^] Dùng viên " + v2 + " — lên Bậc " + nextStep);
            }
            if ((v1 + v2) == nextStep && v1 > 0 && v2 > 0) {
                choices.add(v1 + v2);
                labels.add("[^^] Gộp tổng " + (v1 + v2) + " — lên thẳng Bậc " + nextStep);
            }

            if (choices.isEmpty()) return;
            int chosenSteps = choices.get(0);
            if (choices.size() > 1) {
                String[] arr = labels.toArray(new String[0]);
                int idx = JOptionPane.showOptionDialog(ui, "Chọn cách dùng điểm để leo chuồng:", "Thăng Bậc Chuồng Đích",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, arr, arr[0]);
                if (idx < 0 || idx >= choices.size()) return;
                chosenSteps = choices.get(idx);
            }

            tryClimbHome(clicked, nextStep);
            consumeDice(chosenSteps);
            if (!gameOver) checkTurnEnd();
        }
    }

    private boolean canMove(Horse h, int steps) {
        int dist = h.getDistanceTraveled();
        if (dist + steps > 56) return false;

        int startPos = h.getCurrentPosition();
        if (startPos == -1) return false;

        for (int i = 1; i <= steps; i++) {
            int checkPos = (startPos + i) % 56;
            Horse obs = board.getHorseAt(checkPos);
            if (obs != null) {
                if (i < steps) return false; 
                if (obs.getColor() == h.getColor()) return false;
                if (board.isSafeCell(checkPos)) return false;
            }
        }
        return true;
    }

    private void consumeDice(int stepsUsed) {
        if (!v1Used && !v2Used && stepsUsed == currentV1 + currentV2) {
            v1Used = true; v2Used = true;
        } else if (!v1Used && stepsUsed == currentV1) {
            v1Used = true;
        } else if (!v2Used && stepsUsed == currentV2) {
            v2Used = true;
        } else {
            if (!v1Used) v1Used = true; else v2Used = true;
        }
    }

    private void checkTurnEnd() {
        if (gameOver) return;

        if (players[currentPlayerIndex].hasWon()) {
            checkWinCondition();
            return; 
        }

        if (v1Used && v2Used) {
            endTurn();
        } else {
            int remaining = !v1Used ? currentV1 : currentV2;
            ui.showMessage("Còn điểm [" + remaining + "]. Hãy chọn ngựa đi tiếp!");
            updateHighlightedHorses();
        }
    }

    private void endTurn() {
        if (gameOver) return;

        boolean giveBonus = bonusTurnEarned && !players[currentPlayerIndex].hasWon();

        hasRolled = false;
        v1Used = false;
        v2Used = false;
        bonusTurnEarned = false;
        dice.reset();
        highlightedHorses.clear();
        ui.renderBoard(board, players);

        if (giveBonus) {
            ui.showMessage(players[currentPlayerIndex].getName() + " được thưởng thêm 1 lượt!");
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

        if (players[currentPlayerIndex].hasWon()) return;

        hasRolled = false;
        v1Used = false;
        v2Used = false;

        String label = isCurrentBot() ? " (Máy)" : " (Mời Đổ...)";
        ui.showMessage("Đến lượt của người chơi: " + players[currentPlayerIndex].getName() + label);
        if (isCurrentBot()) scheduleBot();
    }

    private void deployHorse(Horse h, int pos) {
        Horse occ = board.getHorseAt(pos);
        if (occ != null && occ.getColor() == h.getColor()) return;
        
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
            ui.showMessage("Quân cờ " + h.getColor() + " đã tiến thẳng vào bậc chuồng 1!");
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
        ui.renderBoard(board, players);
    }

    private void tryClimbHome(Horse h, int targetStep) {
        if (h.getState() == HorseState.ON_PATH) {
            board.clearPosition(h.getCurrentPosition());
            h.setCurrentPosition(-1);
        }

        h.setHomeStep(targetStep);
        if (targetStep == 6) {
            h.setState(HorseState.FINISHED);
            ui.showMessage("✅ Ngựa " + h.getColor() + " về đích Bậc 6! Hoàn thành!");
        } else {
            h.setState(HorseState.IN_HOME);
            ui.showMessage("✅ Ngựa " + h.getColor() + " lên bậc " + targetStep + " trong chuồng.");
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

            if (finishedCount >= players.length - 1) { 
                for (Player p : players) {
                    if (!rankings.contains(p.getName())) {
                        rankings.add(p.getName());
                    }
                }
                
                botThinking = false;
                StringBuilder scoreboard = new StringBuilder("🎮 TRẬN ĐẤU KẾT THÚC HOÀN TOÀN! 🎮\n\n");
                scoreboard.append("🏆 BẢNG XẾP HẠNG CHUNG CUỘC:\n");
                for (int i = 0; i < rankings.size(); i++) {
                    scoreboard.append("  Hạng ").append(i + 1).append(": ").append(rankings.get(i)).append("\n");
                }

                ui.showPopup(scoreboard.toString());
                gameOver = true;
                restartGame();
                return true;
            } else {
                nextPlayerTurn();
            }
        }
        return false;
    }

    // Getters
    public Board getBoard()                 { return board; }
    public Player[] getPlayers()              { return players; }
    public List<Horse> getHighlightedHorses() { return highlightedHorses; }
    public int getCurrentPlayerIndex()        { return currentPlayerIndex; }
    public Dice getDice()                     { return dice; }
    public int getPlayerCount()               { return playerCount; }
    public boolean isCurrentPlayerBot()       { return isCurrentBot(); }
}