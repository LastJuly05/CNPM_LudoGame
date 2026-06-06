package ludo.controller;

import ludo.model.*;
import ludo.view.GameUI;
import javax.swing.JOptionPane;
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

    private boolean hasRolled = false;
    private int currentV1 = 0, currentV2 = 0;
    private boolean v1Used = false;
    private boolean v2Used = false;
    private boolean bonusTurnEarned = false;
    private boolean botThinking = false;
    private boolean gameOver = false;

    private List<Horse> highlightedHorses = new ArrayList<>();
    private List<String> rankings = new ArrayList<>();

    public GameController() {}

    public void setUI(GameUI ui) { this.ui = ui; }

    public void startNewGame(String mode) {
        initGame(mode);
        if (ui != null) {
            ui.resetDiceDisplay();
            ui.showMessage("Trận đấu bắt đầu! Lượt của: " + players[0].getName());
            ui.renderBoard(board, players);
            scheduleBot();
        }
    }

    public void startNewGame(int count) { startNewGame(count + "p"); }

    private void initGame(String mode) {
        board = new Board();
        dice  = new Dice();
        botThinking = false;
        gameOver = false;

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
            case "4p":
                playerCount = 4;
                players = new Player[]{
                        new Player("Đỏ",   PlayerColor.RED),
                        new Player("Lam",  PlayerColor.BLUE),
                        new Player("Lục",  PlayerColor.GREEN),
                        new Player("Vàng", PlayerColor.YELLOW)
                };
                isBot = new boolean[]{false, false, false, false};
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
            default: // "bot"
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

        currentPlayerIndex = 0;
        hasRolled = false;
        currentV1 = 0; currentV2 = 0;
        v1Used = false; v2Used = false;
        bonusTurnEarned = false;
        highlightedHorses.clear();
        rankings.clear();
    }

    public void restartGame() {
        if (ui != null) ui.showPlayerSelectionDialog();
    }

    // =========================================================================
    // BOT
    // =========================================================================

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

    /** Bot thực hiện 1 lượt hoàn chỉnh: đổ → đi nước 1 → đi nước 2 (nếu còn) → kết thúc */
    private void botTurn() {
        if (gameOver) return;

        // Đổ xúc xắc
        int[] result = dice.roll();
        currentV1 = result[0];
        currentV2 = result[1];
        hasRolled = true;
        v1Used = false;
        v2Used = false;
        bonusTurnEarned = dice.isDeployable();

        ui.updateDiceDisplay(currentV1, currentV2);
        String playerName = players[currentPlayerIndex].getName();
        if (bonusTurnEarned) {
            ui.showMessage("[*] ĐIỂM ĐẶC BIỆT! " + playerName
                    + " đổ [" + currentV1 + "] + [" + currentV2 + "] — Thưởng thêm 1 lượt!");
        } else {
            ui.showMessage(playerName + " (Máy) đổ: [" + currentV1 + "] + [" + currentV2 + "]");
        }

        // Nước đi thứ 1
        botDoMove();
        if (gameOver) return;

        // Nước đi thứ 2 (nếu còn xúc xắc chưa dùng)
        if (!v1Used || !v2Used) {
            botDoMove();
        }
        if (gameOver) return;

        // Kết thúc lượt
        endTurn();
    }

    /**
     * Bot tìm nước đi tốt nhất và thực hiện.
     * Sau khi đi xong, tự kiểm tra thắng.
     */
    private void botDoMove() {
        if (gameOver) return;
        List<Horse> moves = getValidMovesForBot();
        if (moves.isEmpty()) return;

        int v1 = v1Used ? 0 : currentV1;
        int v2 = v2Used ? 0 : currentV2;

        Horse best = null;
        int bestScore = -1;
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
            tryClimbHome(best, cur + chosen);
            consumeDice(chosen);

        } else if (best.getState() == HorseState.ON_PATH) {
            int chosen = chooseBestPathMove(best, v1, v2);
            if (chosen == 0) return;
            moveHorseOnPath(best, chosen);
            consumeDice(chosen);
        }

        ui.renderBoard(board, players);

        // Kiểm tra thắng sau mỗi nước đi
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
        // Ưu tiên lên cao nhất (bậc 6 trước)
        if (v1 > 0 && v2 > 0 && cur + v1 + v2 <= 6 && canClimb(color, cur + v1 + v2)) return v1 + v2;
        if (v1 > 0 && cur + v1 <= 6 && canClimb(color, cur + v1)) return v1;
        if (v2 > 0 && cur + v2 <= 6 && canClimb(color, cur + v2)) return v2;
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
        if (h.getState() == HorseState.IN_BASE) return 10;
        if (h.getState() == HorseState.IN_HOME) return 80 + h.getHomeStep();
        if (h.getState() == HorseState.ON_PATH) {
            int base = 20 + h.getDistanceTraveled();
            for (int steps : new int[]{v1, v2, v1 + v2}) {
                if (steps <= 0 || !canMove(h, steps)) continue;
                int dest = (h.getCurrentPosition() + steps) % 56;
                Horse occ = board.getHorseAt(dest);
                if (occ != null && occ.getColor() != h.getColor() && !board.isSafeCell(dest)) {
                    base += 50; break;
                }
            }
            return base;
        }
        return 0;
    }

    // =========================================================================
    // ĐỔ XÚC XẮC (người chơi)
    // =========================================================================
    public void rollDiceRequest() {
        if (gameOver) return;
        if (isCurrentBot()) { ui.showMessage("Đang là lượt máy!"); return; }
        if (hasRolled) { ui.showMessage("Bạn đã đổ rồi! Hãy bấm chọn quân cờ để di chuyển."); return; }

        int[] result = dice.roll();
        currentV1 = result[0];
        currentV2 = result[1];
        hasRolled = true;
        v1Used = false;
        v2Used = false;
        bonusTurnEarned = dice.isDeployable();

        ui.updateDiceDisplay(currentV1, currentV2);
        if (bonusTurnEarned) {
            ui.showMessage("[*] ĐIỂM ĐẶC BIỆT! " + players[currentPlayerIndex].getName()
                    + " đổ [" + currentV1 + "] + [" + currentV2 + "] — Thưởng thêm 1 lượt đổ!");
        } else {
            ui.showMessage(players[currentPlayerIndex].getName()
                    + " đổ được: [" + currentV1 + "] + [" + currentV2 + "]");
        }
        updateHighlightedHorses();
    }

    // =========================================================================
    // HIGHLIGHT (chỉ dùng cho người chơi)
    // =========================================================================
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
                int cur = h.getHomeStep();
                boolean ok = (v1 > 0 && cur + v1 <= 6 && canClimb(h.getColor(), cur + v1))
                        || (v2 > 0 && cur + v2 <= 6 && canClimb(h.getColor(), cur + v2))
                        || (v1 > 0 && v2 > 0 && cur + v1 + v2 <= 6 && canClimb(h.getColor(), cur + v1 + v2));
                if (ok) highlightedHorses.add(h);
            } else if (h.getState() == HorseState.ON_PATH) {
                boolean ok = (v1 > 0 && canMove(h, v1))
                        || (v2 > 0 && canMove(h, v2))
                        || (v1 > 0 && v2 > 0 && canMove(h, v1 + v2));
                if (ok) highlightedHorses.add(h);
            }
        }

        ui.renderBoard(board, players);

        if (highlightedHorses.isEmpty()) {
            ui.showMessage("Không có nước đi! Mất lượt...");
            Timer t = new Timer(1200, e -> { ((Timer) e.getSource()).stop(); endTurn(); });
            t.setRepeats(false);
            t.start();
        }
    }

    // =========================================================================
    // CLICK NGỰA (người chơi)
    // =========================================================================
    public void handleHorseClick(Horse clicked) {
        if (gameOver || isCurrentBot()) return;
        if (!hasRolled) { ui.showMessage("Hãy đổ xúc xắc trước!"); return; }
        if (clicked.getColor() != players[currentPlayerIndex].getColor()) {
            ui.showMessage("Không phải quân cờ của bạn!"); return;
        }
        if (!highlightedHorses.contains(clicked)) {
            ui.showMessage("Quân này không thể đi với số điểm hiện tại!"); return;
        }

        // --- XUẤT QUÂN ---
        if (clicked.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(clicked.getColor());
            deployHorse(clicked, startPos);
            if (currentV1 == currentV2) v1Used = true;
            else { v1Used = true; v2Used = true; }
            ui.showMessage("[>] Xuất quân thành công!");
            // Không thể thắng từ nước xuất quân, chỉ cần cập nhật highlight
            checkTurnEnd();

            // --- ĐI TRÊN ĐƯỜNG ---
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
                labels.add(currentV1 == toGate
                        ? "[D] Vào Cửa Chuồng (viên " + currentV1 + ")"
                        : "[>] Đi " + currentV1 + " bước (viên 1)");
            }
            if (canV2 && !(currentV2 == currentV1 && !v1Used)) {
                choices.add(currentV2);
                labels.add(currentV2 == toGate
                        ? "[D] Vào Cửa Chuồng (viên " + currentV2 + ")"
                        : "[>] Đi " + currentV2 + " bước (viên 2)");
            }
            if (canSum) {
                choices.add(currentV1 + currentV2);
                labels.add((currentV1 + currentV2) == toGate
                        ? "[D] Gộp điểm vào Cửa Chuồng (tổng " + (currentV1 + currentV2) + ")"
                        : "[>>] Gộp đi " + (currentV1 + currentV2) + " bước");
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

            // --- LEO CHUỒNG ĐÍCH ---
        } else if (clicked.getState() == HorseState.IN_HOME) {
            int cur = clicked.getHomeStep();
            int v1  = v1Used ? 0 : currentV1;
            int v2  = v2Used ? 0 : currentV2;

            List<Integer> choices = new ArrayList<>();
            List<String>  labels  = new ArrayList<>();

            if (v1 > 0 && cur + v1 <= 6 && canClimb(clicked.getColor(), cur + v1)) {
                choices.add(v1);
                labels.add("[^] Dùng viên " + v1 + " — lên Bậc " + (cur + v1));
            }
            if (v2 > 0 && cur + v2 <= 6 && canClimb(clicked.getColor(), cur + v2)
                    && (v2 != v1 || v1Used)) {
                choices.add(v2);
                labels.add("[^] Dùng viên " + v2 + " — lên Bậc " + (cur + v2));
            }
            if (v1 > 0 && v2 > 0 && cur + v1 + v2 <= 6 && canClimb(clicked.getColor(), cur + v1 + v2)) {
                choices.add(v1 + v2);
                labels.add("[^^] Gộp tổng " + (v1 + v2) + " — lên thẳng Bậc " + (cur + v1 + v2));
            }

            if (choices.isEmpty()) return;
            int chosenSteps = choices.get(0);
            if (choices.size() > 1) {
                String[] arr = labels.toArray(new String[0]);
                int idx = JOptionPane.showOptionDialog(ui, "Chọn số điểm để leo chuồng:", "Thăng Bậc Chuồng Đích",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, arr, arr[0]);
                if (idx < 0 || idx >= choices.size()) return;
                chosenSteps = choices.get(idx);
            }

            tryClimbHome(clicked, cur + chosenSteps);
            consumeDice(chosenSteps);
            // QUAN TRỌNG: chỉ checkTurnEnd nếu chưa game over
            // (tryClimbHome KHÔNG gọi checkWinCondition nữa — để checkTurnEnd lo)
            if (!gameOver) checkTurnEnd();
        }
    }

    // =========================================================================
    // LOGIC DI CHUYỂN
    // =========================================================================
    private boolean canMove(Horse h, int steps) {
        int dist = h.getDistanceTraveled();
        int toGate = 56 - dist;
        if (steps <= 0 || steps > toGate || dist >= 56) return false;
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

    /**
     * Kiểm tra ô `targetStep` trong chuồng đích còn trống không.
     * Ngựa "nhảy thẳng" đến đích — chỉ cần ô đích chưa bị chiếm.
     */
    private boolean canClimb(PlayerColor color, int targetStep) {
        if (targetStep < 1 || targetStep > 6) return false;
        for (Player p : players) {
            if (p.getColor() != color) continue;
            for (Horse h : p.getHorses()) {
                if (h.getHomeStep() == targetStep
                        && (h.getState() == HorseState.IN_HOME
                        || h.getState() == HorseState.FINISHED))
                    return false;
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

    /**
     * Điểm duy nhất kiểm tra thắng và chuyển lượt cho người chơi.
     * KHÔNG gọi checkWinCondition ở nơi khác để tránh double-call.
     */
    private void checkTurnEnd() {
        if (gameOver) return;

        // Kiểm tra thắng trước
        if (players[currentPlayerIndex].hasWon()) {
            checkWinCondition();
            return; // dù thắng hay chưa, không làm gì thêm
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
        v1Used = false; v2Used = false;
        bonusTurnEarned = false;
        highlightedHorses.clear();
        dice.reset();
        ui.renderBoard(board, players);

        if (giveBonus) {
            ui.showMessage("[~] LƯỢT THƯỞNG! " + players[currentPlayerIndex].getName()
                    + " được đổ xúc xắc thêm 1 lần!");
            if (isCurrentBot()) scheduleBot();
        } else {
            nextPlayerTurn();
        }
    }

    private void deployHorse(Horse h, int pos) {
        Horse occ = board.getHorseAt(pos);
        if (occ != null) {
            board.clearPosition(pos);
            occ.sendToBase();
            ui.showMessage("[!] Đá văng ngựa " + occ.getColor() + " về chuồng!");
        }
        h.setCurrentPosition(pos);
        h.setState(HorseState.ON_PATH);
        h.setDistanceTraveled(0);
        board.setHorseAt(pos, h);
        ui.renderBoard(board, players);
    }

    private void moveHorseOnPath(Horse h, int steps) {
        int oldPos = h.getCurrentPosition();
        int dist   = h.getDistanceTraveled();
        int toGate = 56 - dist;
        board.clearPosition(oldPos);

        if (steps == toGate) {
            h.setCurrentPosition(-1);
            h.setDistanceTraveled(56);
            h.setHomeStep(0);
            h.setState(HorseState.IN_HOME);
            ui.showMessage("[D] Ngựa " + h.getColor() + " vào Cửa Chuồng! Thảy xúc xắc để leo bậc.");
        } else {
            int newPos = (oldPos + steps) % 56;
            Horse occ = board.getHorseAt(newPos);
            if (occ != null) {
                board.clearPosition(newPos);
                occ.sendToBase();
                ui.showMessage("[!] Đá ngựa " + occ.getColor() + " về chuồng!");
            }
            h.setCurrentPosition(newPos);
            h.setDistanceTraveled(dist + steps);
            board.setHorseAt(newPos, h);
        }
        ui.renderBoard(board, players);
    }

    /**
     * Leo chuồng đích. KHÔNG tự gọi checkWinCondition —
     * người gọi (checkTurnEnd / botDoMove) sẽ lo việc đó.
     */
    private void tryClimbHome(Horse h, int targetStep) {
        if (h.getState() == HorseState.ON_PATH) {
            board.clearPosition(h.getCurrentPosition());
            h.setCurrentPosition(-1);
        }
        h.setHomeStep(targetStep);
        if (targetStep == 6) {
            h.setState(HorseState.FINISHED);
            ui.showMessage("[v] Ngựa " + h.getColor() + " về đích Bậc 6! Hoàn thành!");
        } else {
            h.setState(HorseState.IN_HOME);
            ui.showMessage("[+] Ngựa " + h.getColor() + " lên Bậc " + targetStep + ".");
        }
        ui.renderBoard(board, players);
    }

    // =========================================================================
    // KIỂM TRA THẮNG — chỉ được gọi từ checkTurnEnd() hoặc botDoMove()
    // =========================================================================
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
    // =========================================================================
    // CHUYỂN LƯỢT
    // =========================================================================
    public void nextPlayerTurn() {
        if (gameOver) return;
        int n = players.length;
        int attempts = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % n;
            attempts++;
        } while (players[currentPlayerIndex].hasWon() && attempts < n);

        if (players[currentPlayerIndex].hasWon()) return; // tất cả đã thắng

        String label = isCurrentBot() ? " (Máy)" : " (Bạn)";
        ui.showMessage("[o] Lượt của: " + players[currentPlayerIndex].getName() + label);
        scheduleBot();
    }

    // Getters
    public Board getBoard()                   { return board; }
    public Player[] getPlayers()              { return players; }
    public List<Horse> getHighlightedHorses() { return highlightedHorses; }
    public int getCurrentPlayerIndex()        { return currentPlayerIndex; }
    public Dice getDice()                     { return dice; }
    public int getPlayerCount()               { return playerCount; }
    public boolean isCurrentPlayerBot()       { return isCurrentBot(); }
}