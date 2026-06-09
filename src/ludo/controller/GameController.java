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
    // BOT XỬ LÝ LƯỢT CHƠI
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

    private void botTurn() {
        if (gameOver) return;

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

        // Thực hiện nước đi thứ 1
        boolean moved1 = botDoMove();
        if (gameOver) return;

        // Chỉ đi tiếp nước thứ 2 nếu nước 1 có đi ĐƯỢC và CHƯA dùng hết xúc xắc
        if (moved1 && (!v1Used || !v2Used)) {
            botDoMove();
        }
        if (gameOver) return;

        // Kết thúc lượt của Bot công bằng
        endTurn();
    }

    /**
     * Trả về true nếu thực hiện di chuyển thành công, false nếu không có nước đi.
     */
    private boolean botDoMove() {
        if (gameOver) return false;
        List<Horse> moves = getValidMovesForBot();
        if (moves.isEmpty()) return false; // Không có nước đi hợp lệ

        int v1 = v1Used ? 0 : currentV1;
        int v2 = v2Used ? 0 : currentV2;

        Horse best = null;
        int bestScore = -1;
        for (Horse h : moves) {
            int score = scoreBotMove(h, v1, v2);
            if (score > bestScore) { bestScore = score; best = h; }
        }
        if (best == null) return false;

        if (best.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(best.getColor());
            deployHorse(best, startPos);
            if (currentV1 == currentV2) v1Used = true;
            else { v1Used = true; v2Used = true; }
            ui.showMessage("[>] " + players[currentPlayerIndex].getName() + " xuất quân!");

        } else if (best.getState() == HorseState.IN_HOME) {
            int cur = best.getHomeStep();
            int chosen = chooseBestClimb(best.getColor(), cur, v1, v2);
            if (chosen == 0) return false;
            tryClimbHome(best, cur + chosen);
            consumeDice(chosen);

        } else if (best.getState() == HorseState.ON_PATH) {
            int chosen = chooseBestPathMove(best, v1, v2);
            if (chosen == 0) return false;
            moveHorseOnPath(best, chosen);
            consumeDice(chosen);
        }

        ui.renderBoard(board, players);

        if (!gameOver && players[currentPlayerIndex].hasWon()) {
            checkWinCondition();
        }
        return true;
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
    // ĐỔ XÚC XẮC (NGƯỜI CHƠI)
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
    // HIGHLIGHT VÀ XỬ LÝ KHÔNG CÓ NƯỚC ĐI
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
                
                // Tích hợp logic UC7 của Yến: Chỉ highlight nếu xúc xắc KHỚP với số bậc tiếp theo
                int nextStep = h.getHomeStep() + 1;
                if (nextStep <= 6 && !isHomeStepOccupied(h, nextStep) && canClimb(h.getColor(), nextStep)) {
                    int stepsNeeded = 1;  
                    if (v1 == stepsNeeded || v2 == stepsNeeded || (v1 + v2) == stepsNeeded) {
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

        /* * =====================================================================
         * [PHÁT TRIỂN MỚI - UC6: CHUYỂN LƯỢT] 
         * ĐẢM NHẬN: Ngô Thanh Vy
         * CHI TIẾT: Xử lý điều kiện kết thúc lượt cưỡng bách khi người chơi không có nước đi hợp lệ.
         * Tích hợp bộ hẹn giờ trễ tự động kích hoạt endTurn() để bảo toàn luồng chạy hệ thống.
         * =====================================================================
         */
        if (highlightedHorses.isEmpty()) {
            if (players[currentPlayerIndex].hasWon()) {
                endTurn(); 
                return;
            }
            ui.showMessage("Không có nước đi! Mất lượt...");
            hasRolled = false; 
            Timer t = new Timer(1200, e -> { ((Timer) e.getSource()).stop(); endTurn(); });
            t.setRepeats(false);
            t.start();
        }
    }

    // =========================================================================
    // XỬ LÝ CLICK CHỌN NGỰA
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

        /* * =====================================================================
         * [NHIỆM VỤ CŨ - UC4: XUẤT QUÂN] 
         * ĐẢM NHẬN: Ngô Thanh Vy
         * CHI TIẾT: Kiểm tra điều kiện điểm đặc biệt và đẩy quân ngựa từ IN_BASE ra ô bắt đầu.
         * =====================================================================
         */
        if (clicked.getState() == HorseState.IN_BASE) {
            int startPos = board.getStartPosition(clicked.getColor());
            deployHorse(clicked, startPos);
            if (currentV1 == currentV2) v1Used = true;
            else { v1Used = true; v2Used = true; }
            ui.showMessage("[>] Xuất quân thành công!");
            checkTurnEnd();

        } else if (clicked.getState() == HorseState.ON_PATH) {
            int dist   = clicked.getDistanceTraveled();
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

        /* * =====================================================================
         * [NHIỆM VỤ CŨ - UC7: LÊN CHUỒNG ĐÍCH] 
         * ĐẢM NHẬN: Ngô Thanh Vy
         * CHI TIẾT: Phân rã điểm xúc xắc đơn lẻ hoặc gộp điểm để tịnh tiến thăng bậc chuồng đích.
         * =====================================================================
         */
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
            if (players[currentPlayerIndex].hasWon()) {
               checkWinCondition();
            } else {
                checkTurnEnd();
            }
        }
    }
    // =========================================================================
    // LOGIC HỖ TRỢ DI CHUYỂN
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

    private void checkTurnEnd() {
    if (gameOver) return;

    // Phải kiểm tra thắng ngay tại đây cho mọi trường hợp (đi đường, xuất quân, v.v.)
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

    /* * =====================================================================
     * [PHÁT TRIỂN MỚI - UC6: CHUYỂN LƯỢT] 
     * ĐẢM NHẬN: Ngô Thanh Vy (Phối hợp cùng Sang tối ưu hóa)
     * CHI TIẾT: Reset các cờ điều khiển lượt, làm sạch bộ xúc xắc đồng thời kiểm tra 
     * điều kiện thưởng thêm lượt (bonusTurnEarned) nếu người chơi gieo được xúc xắc đôi.
     * =====================================================================
     */
    private void endTurn() {
        if (gameOver) return;

        boolean giveBonus = bonusTurnEarned;
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

    private void tryClimbHome(Horse h, int targetStep) {
        if (h.getState() == HorseState.ON_PATH) {
            board.clearPosition(h.getCurrentPosition());
            h.setCurrentPosition(-1);
        }
        h.setHomeStep(targetStep);

        boolean blocked = targetStep == 6 || isHomeStepOccupied(h, targetStep + 1);
        if (blocked) {
            h.setState(HorseState.FINISHED);
            ui.showMessage("[v] Ngựa " + h.getColor() + " về đích Bậc " + targetStep + "! Hoàn thành!");
        } else {
            h.setState(HorseState.IN_HOME);
            ui.showMessage("[+] Ngựa " + h.getColor() + " lên Bậc " + targetStep + ".");
        }
        ui.renderBoard(board, players);
    }

    private boolean isHomeStepOccupied(Horse mover, int step) {
        if (step < 1 || step > 6) return false;
        for (Player p : players) {
            if (p.getColor() != mover.getColor()) continue;
            for (Horse other : p.getHorses()) {
                if (other == mover) continue;
                if ((other.getState() == HorseState.IN_HOME || other.getState() == HorseState.FINISHED)
                        && other.getHomeStep() == step) {
                    return true;
                }
            }
        }
        return false;
    }

    /* * =====================================================================
     * [PHÁT TRIỂN MỚI - UC8: THẮNG CUỘC] 
     * ĐẢM NHẬN: Ngô Thanh Vy (Phối hợp cùng Sang xử lý logic & hiển thị Popup)
     * CHI TIẾT: Kiểm tra điều kiện thắng trận qua hasWon(). Hỗ trợ xếp hạng phân cấp 
     * linh hoạt Top #1 -> #4. Kích hoạt thông báo giao diện đồ họa thông qua ui.showPopup().
     * =====================================================================
     */
    public boolean checkWinCondition() {
    if (gameOver) return false;

    Player current = players[currentPlayerIndex];

    if (!current.hasWon() || rankings.contains(current.getName()))
        return false;

    rankings.add(current.getName());

    ui.showMessage(
            current.getName()
            + " đạt Hạng #"
            + rankings.size()
    );

    int notDone = 0;
    for (Player p : players) {
        if (!p.hasWon()) notDone++;
    }

    boolean gameEnds = (playerCount == 2) || (notDone <= 1);

    if (gameEnds) {

        gameOver = true;

        for (Player p : players) {
            if (!rankings.contains(p.getName())) {
                rankings.add(p.getName());
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("================================\n");
        sb.append("     TRẬN ĐẤU KẾT THÚC!         \n");
        sb.append("================================\n\n");
        sb.append("   BẢNG XẾP HẠNG CHUNG CUỘC:\n\n");

        String[] medals = {
                "[W] HẠNG #1 (VÔ ĐỊCH):",
                "    Hạng #2          :",
                "    Hạng #3          :",
                "    Hạng #4          :"
        };

        for (int i = 0; i < rankings.size(); i++) {
            sb.append(
                    medals[Math.min(i, medals.length - 1)]
            ).append(" ")
             .append(rankings.get(i))
             .append("\n");
        }

        ui.showPopup(sb.toString());

        restartGame();

        return true;

    } else {
    ui.showMessage(
            "[W] "
            + current.getName()
            + " đạt Hạng #"
            + rankings.size()
            + ". Còn lại "
            + notDone
            + " người chơi."
    );

    hasRolled = false;
    v1Used = false;
    v2Used = false;
    bonusTurnEarned = false;
    highlightedHorses.clear();
    dice.reset();

    nextPlayerTurn();

    return false;
    }
}
    /* * =====================================================================
     * [PHÁT TRIỂN MỚI - UC6: CHUYỂN LƯỢT] 
     * ĐẢM NHẬN: Ngô Thanh Vy (Phối hợp cùng Sang tối ưu hóa)
     * CHI TIẾT: Sử dụng vòng lặp tịnh tiến chỉ số currentPlayerIndex để chuyển giao quyền
     * điều khiển cho người kế tiếp. Bỏ qua những người chơi đã thắng cuộc (hasWon() == true).
     * =====================================================================
     */
    public void nextPlayerTurn() {
        if (gameOver) return;
        int n = players.length;
        int attempts = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % n;
            attempts++;
        } while (players[currentPlayerIndex].hasWon() && attempts < n);

        if (players[currentPlayerIndex].hasWon()) return; 

        String label = isCurrentBot() ? " (Máy)" : " (Bạn)";
        ui.showMessage("[o] Lượt của người chơi: " + players[currentPlayerIndex].getName() + label);
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